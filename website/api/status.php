<?php
declare(strict_types=1);
require __DIR__ . '/_signal.php';
vojs_headers();
if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'GET') vojs_fail(405, 'Koristi GET.');
vojs_store();
header('Content-Type: application/json; charset=utf-8');
echo json_encode([
    'available' => true, 'anonymous' => true, 'p2p' => true,
    'audio_storage' => false, 'database' => false, 'cron_required' => false,
    'session_seconds' => VOJS_ROOM_TTL
]);
