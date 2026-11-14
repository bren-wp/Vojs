<?php
declare(strict_types=1);

/**
 * VOJS P2P signaling. Stores ONLY short-lived SDP handshakes as JSON.
 * NEVER receives, stores, or proxies audio bytes.
 */
const VOJS_ROOM_TTL = 900;
const VOJS_MAX_ROOMS = 100;
const VOJS_MAX_SDP = 12000;
const VOJS_MAX_POST = 32768;
const VOJS_ROOM_LIMIT = 20;

function vojs_headers(): void {
    header('Cache-Control: no-store, private');
    header('X-Content-Type-Options: nosniff');
    header('Referrer-Policy: no-referrer');
    header('X-Frame-Options: DENY');
    header("Content-Security-Policy: default-src 'none'");
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
    $root = realpath((string)($_SERVER['DOCUMENT_ROOT'] ?? ''));
    if ($root === false || $root === DIRECTORY_SEPARATOR) {
        vojs_fail(503, 'Neispravna konfiguracija DocumentRoot.');
    }
    $env = getenv('VOJS_SIGNAL_STORAGE');
    $dir = $env !== false && $env !== '' ? $env : dirname($root) . '/vojs-signal-data';
    if (!str_starts_with($dir, DIRECTORY_SEPARATOR)) {
        vojs_fail(503, 'Privatna pohrana mora koristiti apsolutnu putanju.');
    }
    if (!is_dir($dir) && !@mkdir($dir, 0700, true) && !is_dir($dir)) {
        vojs_fail(503, 'Privatna pohrana nije dostupna.');
    }
    $real = realpath($dir);
    if ($real === false || $real === $root ||
        str_starts_with($real . '/', $root . '/') || !is_writable($real)) {
        vojs_fail(503, 'JSON pohrana mora biti izvan javnog web korijena.');
    }
    return $real;
}
function vojs_id(string $id): bool {
    return preg_match('/\A[a-f0-9]{48}\z/D', $id) === 1;
}
function vojs_token(string $token): bool {
    return preg_match('/\A[a-f0-9]{64}\z/D', $token) === 1;
}
function vojs_room_path(string $store, string $id): string {
    if (!vojs_id($id)) vojs_fail(404, 'Nepoznata poveznica.');
    return $store . '/' . $id . '.json';
}
function vojs_gc(string $store): int {
    $live = 0;
    foreach (glob($store . '/*.json') ?: [] as $file) {
        $json = @json_decode((string)@file_get_contents($file), true);
        if (!is_array($json) || (int)($json['expires'] ?? 0) <= time() ||
            !empty($json['closed'])) {
            @unlink($file);
        } else {
            $live++;
        }
    }
    return $live;
}
function vojs_rate(string $store): void {
    $secretPath = $store . '/.rate-secret';
    $secretFp = @fopen($secretPath, 'c+');
    if (!$secretFp || !flock($secretFp, LOCK_EX)) vojs_fail(503, 'Sigurnosno ograničenje nije dostupno.');
    $secret = stream_get_contents($secretFp);
    if (!is_string($secret) || strlen($secret) < 64) {
        $secret = bin2hex(random_bytes(32));
        rewind($secretFp); ftruncate($secretFp, 0);
        fwrite($secretFp, $secret); fflush($secretFp);
        @chmod($secretPath, 0600);
    }
    flock($secretFp, LOCK_UN); fclose($secretFp);
    $ip = (string)($_SERVER['REMOTE_ADDR'] ?? 'unknown');
    $ratePath = $store . '/.rate-' . hash_hmac('sha256', $ip, $secret);
    $fp = @fopen($ratePath, 'c+');
    if (!$fp || !flock($fp, LOCK_EX)) vojs_fail(503, 'Sigurnosno ograničenje nije dostupno.');
    $old = json_decode((string)stream_get_contents($fp), true);
    $start = (int)($old['start'] ?? time());
    $count = (int)($old['count'] ?? 0);
    if ($start > time() || time() - $start >= 86400) {
        $start = time(); $count = 0;
    }
    if ($count >= VOJS_ROOM_LIMIT) {
        flock($fp, LOCK_UN); fclose($fp);
        vojs_fail(429, 'Previše zahtjeva. Pokušaj kasnije.');
    }
    rewind($fp); ftruncate($fp, 0);
    fwrite($fp, json_encode(['start' => $start, 'count' => $count + 1]));
    fflush($fp); flock($fp, LOCK_UN); fclose($fp);
}
function vojs_read_room(string $store, string $id): array {
    $handle = @fopen(vojs_room_path($store, $id), 'rb');
    if (!$handle || !flock($handle, LOCK_SH)) vojs_fail(404, 'Poveznica nije dostupna.');
    $data = json_decode((string)stream_get_contents($handle), true);
    flock($handle, LOCK_UN); fclose($handle);
    if (!is_array($data) || !empty($data['closed']) ||
        (int)($data['expires'] ?? 0) <= time()) vojs_fail(404, 'Poveznica je istekla.');
    return $data;
}
function vojs_change_room(string $store, string $id, callable $change): void {
    $handle = @fopen(vojs_room_path($store, $id), 'rb+');
    if (!$handle || !flock($handle, LOCK_EX)) vojs_fail(404, 'Poveznica nije dostupna.');
    try {
        $data = json_decode((string)stream_get_contents($handle), true);
        if (!is_array($data) || !empty($data['closed']) ||
            (int)($data['expires'] ?? 0) <= time()) vojs_fail(404, 'Poveznica je istekla.');
        $change($data);
        rewind($handle); ftruncate($handle, 0);
        fwrite($handle, json_encode($data, JSON_THROW_ON_ERROR));
        fflush($handle);
    } finally {
        flock($handle, LOCK_UN); fclose($handle);
    }
}
function vojs_sdp(mixed $input, string $type): array {
    if (!is_array($input) || ($input['type'] ?? null) !== $type ||
        !is_string($input['sdp'] ?? null) ||
        strlen($input['sdp']) < 40 || strlen($input['sdp']) > VOJS_MAX_SDP ||
        !str_starts_with($input['sdp'], 'v=0')) {
        vojs_fail(400, 'Neispravan WebRTC opis veze.');
    }
    return ['type' => $type, 'sdp' => $input['sdp']];
}
