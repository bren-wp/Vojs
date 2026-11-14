#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
PORT=18542
mkdir -p "$TMP/private"
export VOJS_PRIVATE_STORAGE="$TMP/private"
php -S "127.0.0.1:$PORT" -t website > "$TMP/php.log" 2>&1 &
PID=$!
trap 'kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true; rm -rf "$TMP"' EXIT
BASE="http://127.0.0.1:$PORT"
for i in {1..30}; do
  if curl -fsS "$BASE/api/status.php" > "$TMP/status.json"; then break; fi
  sleep 0.1
done
python3 - "$TMP" <<'PY'
from pathlib import Path
import sys
root = Path(sys.argv[1])
(root / 'valid.m4a').write_bytes(b'\0\0\0\x18ftypM4A ' + b'\0' * 128)
(root / 'invalid.txt').write_bytes(b'Not audio!' * 16)
PY
status=$(curl -sS -o "$TMP/no-audio.json" -w '%{http_code}' -X POST "$BASE/api/upload.php")
test "$status" = "400"
status=$(curl -sS -o "$TMP/bad.json" -w '%{http_code}' -F "audio=@$TMP/invalid.txt" "$BASE/api/upload.php")
test "$status" = "415"
status=$(curl -sS -o "$TMP/bad-id.json" -w '%{http_code}' "$BASE/api/audio.php?id=../secret")
test "$status" = "404"
curl -fsS -F "audio=@$TMP/valid.m4a;type=audio/mp4" "$BASE/api/upload.php" > "$TMP/upload.json"
python3 - "$TMP" <<'PY'
import json, sys
from pathlib import Path
root = Path(sys.argv[1])
result = json.loads((root/'upload.json').read_text())
assert len(result['id']) == 48
assert len(result['delete_token']) == 64
assert result['url'].startswith("https://vojs.com.hr/poruka/?id=")
(root/'id').write_text(result['id'])
(root/'delete.json').write_text(json.dumps({"id":result['id'],"delete_token":result['delete_token']}))
PY
ID="$(cat "$TMP/id")"
curl -fsSI "$BASE/api/audio.php?id=$ID" | grep -iq 'Content-Type: audio/mp4'
status=$(curl -sS -o "$TMP/partial" -w '%{http_code}' -H 'Range: bytes=0-15' "$BASE/api/audio.php?id=$ID")
test "$status" = "206"
test "$(wc -c < "$TMP/partial")" = "16"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' --data-binary @"$TMP/delete.json" "$BASE/api/delete.php")
test "$status" = "204"
status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/audio.php?id=$ID")
test "$status" = "404"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data '{"id":"111111111111111111111111111111111111111111111111","delete_token":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}' \
  "$BASE/api/delete.php")
test "$status" = "404"
# First valid upload consumed one of ten slots. Nine more are allowed.
for i in {1..9}; do
  curl -fsS -F "audio=@$TMP/valid.m4a;type=audio/mp4" "$BASE/api/upload.php" > /dev/null
done
status=$(curl -sS -o /dev/null -w '%{http_code}' -F "audio=@$TMP/valid.m4a;type=audio/mp4" "$BASE/api/upload.php")
test "$status" = "429"
echo "VOJS PHP API integration test passed"
