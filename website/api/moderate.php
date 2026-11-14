<?php
declare(strict_types=1);
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
require __DIR__ . '/_bootstrap.php';
$_SERVER['DOCUMENT_ROOT'] = $_SERVER['DOCUMENT_ROOT'] ?? (getenv('VOJS_DOCUMENT_ROOT') ?: '');
$id = $argv[1] ?? '';
if (!is_string($id) || !vojs_id($id)) {
    fwrite(STDERR, "Usage: php moderate.php <48-char-hex-id>\n");
    exit(2);
}
$store = vojs_store();
$lock = vojs_global_lock($store);
try {
    vojs_remove($store, $id);
    echo "Removed if present: " . $id . PHP_EOL;
} finally {
    flock($lock, LOCK_UN);
    fclose($lock);
}
