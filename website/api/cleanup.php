<?php
declare(strict_types=1);
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
require __DIR__ . '/_bootstrap.php';
$_SERVER['DOCUMENT_ROOT'] = $_SERVER['DOCUMENT_ROOT'] ?? (getenv('VOJS_DOCUMENT_ROOT') ?: '');
$store = vojs_store();
$lock = vojs_global_lock($store);
try {
    echo 'Expired audio removed: ' . vojs_cleanup($store) . PHP_EOL;
} finally {
    flock($lock, LOCK_UN);
    fclose($lock);
}
