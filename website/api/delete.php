<?php
declare(strict_types=1);
require __DIR__ . '/_bootstrap.php';
vojs_headers();
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') vojs_fail(405, 'Koristi POST.');
if ((int)($_SERVER['CONTENT_LENGTH'] ?? 0) > 1024) vojs_fail(413, 'Zahtjev je prevelik.');
if (!str_starts_with((string)($_SERVER['CONTENT_TYPE'] ?? ''), 'application/json')) {
    vojs_fail(415, 'Potreban je JSON.');
}
$input = json_decode((string)file_get_contents('php://input'), true);
$id = is_array($input) ? (string)($input['id'] ?? '') : '';
$token = is_array($input) ? (string)($input['delete_token'] ?? '') : '';
if (!vojs_id($id) || preg_match('/\A[a-f0-9]{64}\z/D', $token) !== 1) {
    vojs_fail(400, 'Neispravan zahtjev.');
}
$store = vojs_store();
$meta = vojs_metadata($store, $id);
if ($meta === null || !hash_equals((string)$meta['delete_hash'], hash('sha256', $token))) {
    vojs_fail(404, 'Snimka nije dostupna.');
}
vojs_remove($store, $id);
http_response_code(204);
