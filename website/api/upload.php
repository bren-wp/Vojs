<?php
declare(strict_types=1);
/** Removed legacy audio-storage endpoint. P2P never sends audio to this server. */
http_response_code(410);
header('Cache-Control: no-store');
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');
echo '{"error":"Ova funkcija je uklonjena. Koristi izravno WebRTC dijeljenje."}';
