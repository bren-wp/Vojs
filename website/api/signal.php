<?php
declare(strict_types=1);
require __DIR__ . '/_signal.php';
vojs_headers();
header('Content-Type: application/json; charset=utf-8');
$method = (string)($_SERVER['REQUEST_METHOD'] ?? '');
if (!in_array($method, ['GET','POST'], true)) vojs_fail(405, 'Metoda nije podržana.');
$store = vojs_store();
if ($method === 'GET') {
    $id = (string)($_GET['id'] ?? '');
    $view = (string)($_GET['view'] ?? '');
    if (!in_array($view, ['offer','answer'], true)) vojs_fail(400, 'Neispravan zahtjev.');
    $room = vojs_read_room($store, $id);
    if ($view === 'answer') {
        $owner = (string)($_GET['owner_token'] ?? '');
        if (!vojs_token($owner) ||
            !hash_equals((string)$room['owner_hash'], hash('sha256', $owner))) {
            vojs_fail(404, 'Poveznica nije dostupna.');
        }
    }
    $value = $room[$view] ?? null;
    if (!is_array($value)) {
        http_response_code(202);
        echo json_encode(['pending' => true, 'expires' => $room['expires']]);
    } else {
        echo json_encode(['description' => $value, 'expires' => $room['expires']]);
    }
    exit;
}
if ((int)($_SERVER['CONTENT_LENGTH'] ?? 0) > VOJS_MAX_POST) vojs_fail(413, 'Zahtjev je prevelik.');
if (!str_starts_with((string)($_SERVER['CONTENT_TYPE'] ?? ''), 'application/json')) {
    vojs_fail(415, 'Očekuje se JSON.');
}
$raw = file_get_contents('php://input', false, null, 0, VOJS_MAX_POST + 1);
if (!is_string($raw) || strlen($raw) > VOJS_MAX_POST) vojs_fail(413, 'Zahtjev je prevelik.');
$request = json_decode($raw, true);
if (!is_array($request)) vojs_fail(400, 'Neispravan JSON.');
$action = (string)($request['action'] ?? '');
if ($action === 'create') {
    vojs_rate($store);
    $lock = @fopen($store . '/.room-lock', 'c+');
    if (!$lock || !flock($lock, LOCK_EX)) vojs_fail(503, 'Usluga trenutno nije dostupna.');
    try {
        if (vojs_gc($store) >= VOJS_MAX_ROOMS) vojs_fail(503, 'Previše aktivnih sesija.');
        $id = bin2hex(random_bytes(24));
        $token = bin2hex(random_bytes(32));
        $expires = time() + VOJS_ROOM_TTL;
        $data = [
            'expires' => $expires,
            'owner_hash' => hash('sha256', $token),
            'offer' => null,
            'answer' => null,
            'closed' => false
        ];
        $path = vojs_room_path($store, $id);
        $fp = @fopen($path, 'x');
        if (!$fp) vojs_fail(503, 'Stvaranje sesije nije uspjelo.');
        @chmod($path, 0600);
        fwrite($fp, json_encode($data, JSON_THROW_ON_ERROR));
        fclose($fp);
    } finally {
        flock($lock, LOCK_UN); fclose($lock);
    }
    http_response_code(201);
    echo json_encode([
        'id' => $id,
        'url' => 'https://vojs.com.hr/poruka/?id=' . $id,
        'owner_token' => $token,
        'expires' => $expires
    ], JSON_UNESCAPED_SLASHES);
    exit;
}
$id = (string)($request['id'] ?? '');
if (!vojs_id($id)) vojs_fail(400, 'Neispravan identifikator.');
if ($action === 'offer') {
    $owner = (string)($request['owner_token'] ?? '');
    $sdp = vojs_sdp($request['description'] ?? null, 'offer');
    vojs_change_room($store, $id, static function (array &$room) use ($owner, $sdp) {
        if (!vojs_token($owner) ||
            !hash_equals((string)$room['owner_hash'], hash('sha256', $owner))) {
            vojs_fail(404, 'Poveznica nije dostupna.');
        }
        if (is_array($room['offer'])) vojs_fail(409, 'Ponuda je već predana.');
        $room['offer'] = $sdp;
    });
    http_response_code(204);
    exit;
}
if ($action === 'answer') {
    $sdp = vojs_sdp($request['description'] ?? null, 'answer');
    vojs_change_room($store, $id, static function (array &$room) use ($sdp) {
        if (!is_array($room['offer'])) vojs_fail(409, 'Ponuda nije spremna.');
        if (is_array($room['answer'])) vojs_fail(409, 'Sesija već ima primatelja.');
        $room['answer'] = $sdp;
    });
    http_response_code(204);
    exit;
}
if ($action === 'close') {
    $owner = (string)($request['owner_token'] ?? '');
    vojs_change_room($store, $id, static function (array &$room) use ($owner) {
        if (!vojs_token($owner) ||
            !hash_equals((string)$room['owner_hash'], hash('sha256', $owner))) {
            vojs_fail(404, 'Poveznica nije dostupna.');
        }
        $room['closed'] = true;
    });
    http_response_code(204);
    exit;
}
vojs_fail(400, 'Nepoznata naredba.');
