<?php
declare(strict_types=1);
require __DIR__ . '/_bootstrap.php';
umask(0077);
vojs_headers();
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') vojs_fail(405, 'Koristi POST.');
if ((int)($_SERVER['CONTENT_LENGTH'] ?? 0) > VOJS_MAX_BYTES + 65536) {
    vojs_fail(413, 'Snimka je prevelika.');
}
if (!isset($_FILES['audio']) || !is_array($_FILES['audio'])) {
    vojs_fail(400, 'Pošalji audio datoteku.');
}
$file = $_FILES['audio'];
if ((int)($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK ||
    !is_string($file['tmp_name'] ?? null) ||
    !is_uploaded_file($file['tmp_name'])) {
    vojs_fail(400, 'Prijenos audio datoteke nije uspio.');
}
$bytes = (int)($file['size'] ?? 0);
if ($bytes < 64 || $bytes > VOJS_MAX_BYTES) vojs_fail(413, 'Nepodržana veličina snimke.');
$mime = vojs_audio_type($file['tmp_name']);
if ($mime === null) vojs_fail(415, 'Dopuštene su samo M4A, WebM i Ogg snimke.');

$store = vojs_store();
vojs_rate_limit($store);
$lock = vojs_global_lock($store);
try {
    vojs_cleanup($store);
    if (vojs_used_bytes($store) + $bytes > VOJS_STORAGE_QUOTA) {
        vojs_fail(503, 'Trenutačno nema prostora za nove snimke.');
    }
    $id = bin2hex(random_bytes(24));
    $deleteToken = bin2hex(random_bytes(32));
    $audio = $store . '/' . $id . '.audio';
    $metadata = $store . '/' . $id . '.json';
    if (!move_uploaded_file($file['tmp_name'], $audio)) {
        vojs_fail(503, 'Nije moguće pohraniti snimku.');
    }
    @chmod($audio, 0600);
    $expires = time() + VOJS_TTL_SECONDS;
    $record = [
        'expires' => $expires,
        'mime' => $mime,
        'bytes' => $bytes,
        'delete_hash' => hash('sha256', $deleteToken),
    ];
    $saved = @file_put_contents($metadata, json_encode($record, JSON_THROW_ON_ERROR), LOCK_EX);
    if ($saved === false) {
        @unlink($audio);
        vojs_fail(503, 'Nije moguće spremiti podatke o snimci.');
    }
    @chmod($metadata, 0600);
} finally {
    flock($lock, LOCK_UN);
    fclose($lock);
}
header('Content-Type: application/json; charset=utf-8');
http_response_code(201);
echo json_encode([
    'id' => $id,
    'url' => 'https://vojs.com.hr/poruka/?id=' . $id,
    'delete_token' => $deleteToken,
    'expires_at' => gmdate('Y-m-d\TH:i:s\Z', $expires),
], JSON_UNESCAPED_SLASHES);
