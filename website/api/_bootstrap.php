<?php
declare(strict_types=1);

/**
 * VOJS anonymous, consent-based sharing service.
 * PHP 8.1+. Storage MUST resolve outside DOCUMENT_ROOT.
 */
const VOJS_MAX_BYTES = 8 * 1024 * 1024;
const VOJS_TTL_SECONDS = 86400;
const VOJS_MAX_REQUESTS_PER_DAY = 10;
const VOJS_STORAGE_QUOTA = 1024 * 1024 * 1024;

function vojs_headers(): void {
    header('Cache-Control: no-store, private');
    header('X-Content-Type-Options: nosniff');
    header('Referrer-Policy: no-referrer');
    header('X-Frame-Options: DENY');
    header('Content-Security-Policy: default-src \'none\'');
    header('X-Robots-Tag: noindex, nofollow');
}

function vojs_fail(int $status, string $message): never {
    http_response_code($status);
    vojs_headers();
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['error' => $message], JSON_UNESCAPED_UNICODE);
    exit;
}

function vojs_store(): string {
    $documentRoot = realpath((string)($_SERVER['DOCUMENT_ROOT'] ?? ''));
    if ($documentRoot === false || $documentRoot === DIRECTORY_SEPARATOR) {
        vojs_fail(503, 'Neispravna konfiguracija web poslužitelja.');
    }
    $configured = getenv('VOJS_PRIVATE_STORAGE');
    $folder = ($configured !== false && $configured !== '')
        ? $configured
        : dirname($documentRoot) . DIRECTORY_SEPARATOR . 'vojs-private';
    if (!str_starts_with($folder, DIRECTORY_SEPARATOR)) {
        vojs_fail(503, 'Privatna pohrana mora imati apsolutnu putanju.');
    }
    if (!is_dir($folder) && !@mkdir($folder, 0700, true) && !is_dir($folder)) {
        vojs_fail(503, 'Privatna pohrana nije dostupna.');
    }
    $real = realpath($folder);
    if ($real === false ||
        $real === $documentRoot ||
        str_starts_with($real . DIRECTORY_SEPARATOR, $documentRoot . DIRECTORY_SEPARATOR) ||
        !is_writable($real)) {
        vojs_fail(503, 'Privatna pohrana mora biti izvan web korijena.');
    }
    return $real;
}

function vojs_id(string $value): bool {
    return preg_match('/\A[a-f0-9]{48}\z/D', $value) === 1;
}

function vojs_metadata(string $store, string $id): ?array {
    if (!vojs_id($id)) return null;
    $path = $store . '/' . $id . '.json';
    $encoded = @file_get_contents($path);
    if (!is_string($encoded)) return null;
    $meta = json_decode($encoded, true);
    if (!is_array($meta) || !isset($meta['expires'], $meta['mime'], $meta['delete_hash'])) return null;
    if ((int)$meta['expires'] <= time()) return null;
    if (!in_array($meta['mime'], ['audio/mp4','audio/webm','audio/ogg'], true)) return null;
    return $meta;
}

function vojs_remove(string $store, string $id): void {
    if (!vojs_id($id)) return;
    @unlink($store . '/' . $id . '.audio');
    @unlink($store . '/' . $id . '.json');
}

function vojs_cleanup(string $store): int {
    $count = 0;
    foreach (glob($store . '/*.json') ?: [] as $metadata) {
        $id = basename($metadata, '.json');
        if (!vojs_id($id)) continue;
        $data = json_decode((string)@file_get_contents($metadata), true);
        if (!is_array($data) || (int)($data['expires'] ?? 0) <= time()) {
            vojs_remove($store, $id);
            $count++;
        }
    }
    return $count;
}

function vojs_audio_type(string $path): ?string {
    $handle = @fopen($path, 'rb');
    if ($handle === false) return null;
    $header = fread($handle, 16);
    fclose($handle);
    if (!is_string($header)) return null;
    if (strlen($header) >= 12 && substr($header, 4, 4) === 'ftyp') return 'audio/mp4';
    if (str_starts_with($header, "\x1A\x45\xDF\xA3")) return 'audio/webm';
    if (str_starts_with($header, 'OggS')) return 'audio/ogg';
    return null;
}

function vojs_secret(string $store): string {
    $path = $store . '/.rate-secret';
    $handle = @fopen($path, 'c+');
    if ($handle === false || !flock($handle, LOCK_EX)) vojs_fail(503, 'Pohrana nije dostupna.');
    $value = stream_get_contents($handle);
    if (!is_string($value) || strlen($value) < 64) {
        $value = bin2hex(random_bytes(32));
        rewind($handle);
        ftruncate($handle, 0);
        fwrite($handle, $value);
        fflush($handle);
        @chmod($path, 0600);
    }
    flock($handle, LOCK_UN);
    fclose($handle);
    return $value;
}

function vojs_rate_limit(string $store): void {
    // Use the actual socket IP, never user-supplied forwarding headers.
    $ip = (string)($_SERVER['REMOTE_ADDR'] ?? 'unknown');
    $name = hash_hmac('sha256', $ip, vojs_secret($store));
    $file = $store . '/.rate-' . $name;
    $handle = @fopen($file, 'c+');
    if ($handle === false || !flock($handle, LOCK_EX)) vojs_fail(503, 'Ograničenje zahtjeva nije dostupno.');
    $raw = stream_get_contents($handle);
    $data = json_decode(is_string($raw) ? $raw : '', true);
    $now = time();
    $since = (int)($data['since'] ?? $now);
    $count = (int)($data['count'] ?? 0);
    if ($now - $since >= VOJS_TTL_SECONDS || $since > $now) {
        $since = $now;
        $count = 0;
    }
    if ($count >= VOJS_MAX_REQUESTS_PER_DAY) {
        flock($handle, LOCK_UN);
        fclose($handle);
        vojs_fail(429, 'Dnevno ograničenje anonimnih dijeljenja je dosegnuto.');
    }
    rewind($handle);
    ftruncate($handle, 0);
    fwrite($handle, json_encode(['since' => $since, 'count' => $count + 1]));
    fflush($handle);
    flock($handle, LOCK_UN);
    fclose($handle);
}

function vojs_global_lock(string $store) {
    $handle = @fopen($store . '/.upload-lock', 'c+');
    if ($handle === false || !flock($handle, LOCK_EX)) vojs_fail(503, 'Privremena nedostupnost usluge.');
    return $handle;
}

function vojs_used_bytes(string $store): int {
    $used = 0;
    foreach (glob($store . '/*.audio') ?: [] as $file) {
        $size = @filesize($file);
        if ($size !== false) $used += (int)$size;
    }
    return $used;
}
