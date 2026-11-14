<?php
declare(strict_types=1);
require __DIR__ . '/_bootstrap.php';
vojs_headers();
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') vojs_fail(405, 'Koristi GET.');
vojs_store(); // Return unavailable if private storage has not been configured.
header('Content-Type: application/json; charset=utf-8');
echo json_encode(['available' => true, 'anonymous' => true, 'max_bytes' => VOJS_MAX_BYTES, 'retention_seconds' => VOJS_TTL_SECONDS]);
