<?php
declare(strict_types=1);
require __DIR__ . '/_bootstrap.php';
vojs_headers();
$method = $_SERVER['REQUEST_METHOD'] ?? '';
if ($method !== 'GET' && $method !== 'HEAD') vojs_fail(405, 'Koristi GET.');
$id = (string)($_GET['id'] ?? '');
if (!vojs_id($id)) vojs_fail(404, 'Snimka nije dostupna.');
$store = vojs_store();
$meta = vojs_metadata($store, $id);
$file = $store . '/' . $id . '.audio';
if ($meta === null || !is_file($file)) vojs_fail(404, 'Snimka ne postoji ili je istekla.');
$size = @filesize($file);
if ($size === false || $size < 1) vojs_fail(404, 'Snimka nije dostupna.');

$first = 0;
$last = $size - 1;
if (isset($_SERVER['HTTP_RANGE'])) {
    $range = (string)$_SERVER['HTTP_RANGE'];
    if (!preg_match('/\Abytes=(\d*)-(\d*)\z/D', $range, $match) ||
        ($match[1] === '' && $match[2] === '')) {
        header('Content-Range: bytes */' . $size);
        vojs_fail(416, 'Neispravan raspon.');
    }
    if ($match[1] === '') {
        $suffix = (int)$match[2];
        if ($suffix <= 0) vojs_fail(416, 'Neispravan raspon.');
        $first = max(0, $size - $suffix);
    } else {
        $first = (int)$match[1];
        if ($match[2] !== '') $last = min($last, (int)$match[2]);
    }
    if ($first >= $size || $last < $first) {
        header('Content-Range: bytes */' . $size);
        vojs_fail(416, 'Neispravan raspon.');
    }
    http_response_code(206);
    header('Content-Range: bytes ' . $first . '-' . $last . '/' . $size);
}
header('Accept-Ranges: bytes');
header('Content-Type: ' . $meta['mime']);
header('Content-Disposition: inline; filename="vojs-audio"');
header('Content-Length: ' . ($last - $first + 1));
if ($method === 'HEAD') exit;

$handle = @fopen($file, 'rb');
if ($handle === false) exit;
fseek($handle, $first);
$left = $last - $first + 1;
while ($left > 0 && !feof($handle) && !connection_aborted()) {
    $chunk = fread($handle, min(65536, $left));
    if ($chunk === false || $chunk === '') break;
    echo $chunk;
    $left -= strlen($chunk);
}
fclose($handle);
