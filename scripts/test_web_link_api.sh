#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
PORT=18542
mkdir -p "$TMP/data"
export VOJS_SIGNAL_STORAGE="$TMP/data"
php -S "127.0.0.1:$PORT" -t website > "$TMP/php.log" 2>&1 &
PID=$!
trap 'kill "$PID" 2>/dev/null || true; wait "$PID" 2>/dev/null || true; rm -rf "$TMP"' EXIT
BASE="http://127.0.0.1:$PORT"
for i in {1..40}; do
  if curl -fsS "$BASE/api/status.php" > "$TMP/status.json"; then break; fi
  sleep 0.1
done
python3 - "$TMP" <<'PY'
import json,sys
from pathlib import Path
p=Path(sys.argv[1])
s=json.loads((p/'status.json').read_text())
assert s['p2p'] is True and s['audio_storage'] is False
assert s['cron_required'] is False and s['database'] is False
assert s['session_seconds'] == 900
offer={'type':'offer','sdp':'v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=VOJS\r\nt=0 0\r\na=group:BUNDLE 0\r\n'}
answer={'type':'answer','sdp':'v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\ns=VOJS\r\nt=0 0\r\na=group:BUNDLE 0\r\n'}
(p/'offer.json').write_text(json.dumps(offer))
(p/'answer.json').write_text(json.dumps(answer))
PY
status=$(curl -sS -o /dev/null -w '%{http_code}' -F 'audio=@README.md' "$BASE/api/upload.php")
test "$status" = "410"
status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/audio.php?id=bad")
test "$status" = "410"
status=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/delete.php")
test "$status" = "410"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' --data '{bad json' "$BASE/api/signal.php")
test "$status" = "400"
status=$(curl -sS -o "$TMP/create.json" -w '%{http_code}' -H 'Content-Type: application/json' \
  --data '{"action":"create"}' "$BASE/api/signal.php")
test "$status" = "201"
python3 - "$TMP" <<'PY'
from pathlib import Path
import json,sys
p=Path(sys.argv[1]); j=json.loads((p/'create.json').read_text())
assert len(j['id']) == 48 and len(j['owner_token']) == 64
assert j['url'] == "https://vojs.com.hr/poruka/?id="+j['id']
(p/'id').write_text(j['id'])
(p/'token').write_text(j['owner_token'])
for action in ('offer','answer'):
    payload={'action':action,'id':j['id'],'description':json.loads((p/f'{action}.json').read_text())}
    if action=='offer': payload['owner_token']=j['owner_token']
    (p/f'{action}-request.json').write_text(json.dumps(payload))
(p/'close-request.json').write_text(json.dumps({'action':'close','id':j['id'],'owner_token':j['owner_token']}))
PY
ID="$(cat "$TMP/id")"
TOKEN="$(cat "$TMP/token")"
status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/signal.php?id=$ID&view=offer")
test "$status" = "202"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data-binary @"$TMP/answer-request.json" "$BASE/api/signal.php")
test "$status" = "409"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data-binary @"$TMP/offer-request.json" "$BASE/api/signal.php")
test "$status" = "204"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data-binary @"$TMP/offer-request.json" "$BASE/api/signal.php")
test "$status" = "409"
curl -fsS "$BASE/api/signal.php?id=$ID&view=offer" > "$TMP/got-offer.json"
status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/signal.php?id=$ID&view=answer&owner_token=$TOKEN")
test "$status" = "400"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data "{\"action\":\"get_answer\",\"id\":\"$ID\",\"owner_token\":\"$(printf '%064d' 0)\"}" \
  "$BASE/api/signal.php")
test "$status" = "404"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data "{\"action\":\"get_answer\",\"id\":\"$ID\",\"owner_token\":\"$TOKEN\"}" \
  "$BASE/api/signal.php")
test "$status" = "202"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data-binary @"$TMP/answer-request.json" "$BASE/api/signal.php")
test "$status" = "204"
curl -fsS -H 'Content-Type: application/json' --data "{\"action\":\"get_answer\",\"id\":\"$ID\",\"owner_token\":\"$TOKEN\"}" "$BASE/api/signal.php" > "$TMP/got-answer.json"
python3 - "$TMP" <<'PY'
import json,sys
from pathlib import Path
p=Path(sys.argv[1])
assert json.loads((p/'got-offer.json').read_text())['description']['type']=='offer'
assert json.loads((p/'got-answer.json').read_text())['description']['type']=='answer'
room=json.loads((p/'data'/((p/'id').read_text()+'.json')).read_text())
assert set(room)=={'expires','owner_hash','offer','answer','closed'}
assert room['owner_hash'] != (p/'token').read_text()
assert not list((p/'data').glob('*.audio'))
assert not list((p/'data').glob('*.m4a'))
PY
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data-binary @"$TMP/answer-request.json" "$BASE/api/signal.php")
test "$status" = "409"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data-binary @"$TMP/close-request.json" "$BASE/api/signal.php")
test "$status" = "204"
status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/api/signal.php?id=$ID&view=offer")
test "$status" = "404"
python3 - "$TMP" <<'PY'
import json,sys,time
from pathlib import Path
p=Path(sys.argv[1])
room=p/'data'/((p/'id').read_text()+'.json')
assert json.loads(room.read_text())['closed'] is True
assert not list((p/'data').glob('*.audio'))
PY
# New session creation performs opportunistic JSON cleanup, no cron required.
curl -fsS -H 'Content-Type: application/json' --data '{"action":"create"}' "$BASE/api/signal.php" > /dev/null
test ! -f "$TMP/data/$ID.json"
# 2 creations used; 18 more allowed for a single localhost IP.
for i in {1..18}; do
  curl -fsS -H 'Content-Type: application/json' --data '{"action":"create"}' "$BASE/api/signal.php" > /dev/null
done
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  --data '{"action":"create"}' "$BASE/api/signal.php")
test "$status" = "429"
echo "VOJS P2P JSON API, no-audio-storage, locking, expiry and limits: PASSED"
