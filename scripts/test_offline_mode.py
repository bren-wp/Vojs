#!/usr/bin/env python3
"""Real Android airplane-mode test: no VOJS website or synthetic UI."""
import datetime
import json
import time
from pathlib import Path

from verify_responsive import PACKAGE, ACTIVITY, shell, locate, wait, tap, shot, check_crash

OUT = Path("docs/offline-qa")
OUT.mkdir(parents=True, exist_ok=True)
result = {"started":datetime.datetime.now(datetime.timezone.utc).isoformat(),
          "network_required":False,"real_emulator":True,"checks":[]}

def checked(label):
    result["checks"].append(label)
    print("PASS OFFLINE:",label,flush=True)

def run():
    try:
        shell("cmd","connectivity","airplane-mode","enable")
        shell("svc","wifi","disable")
        shell("svc","data","disable")
        time.sleep(2)
        if shell("settings","get","global","airplane_mode_on").strip()!="1":
            raise AssertionError("Airplane mode not enabled")
        checked("airplane mode before first launch")
        shell("pm","clear",PACKAGE)
        shell("pm","grant",PACKAGE,"android.permission.RECORD_AUDIO")
        shell("am","start","-n",ACTIVITY)
        time.sleep(4)
        wait("Nova poruka")
        wait("Započni snimanje")
        shot(OUT,"01-offline-home.png")
        checked("app starts without internet or website")
        tap("Započni snimanje")
        wait("Završi snimanje")
        time.sleep(2)
        tap("Završi snimanje")
        wait("Poruka je spremna")
        wait("Preslušaj")
        shot(OUT,"02-offline-ready.png")
        checked("microphone records a local audio file")
        names=shell("run-as",PACKAGE,"ls","cache/shared_audio")
        if ".m4a" not in names:
            raise AssertionError("No app-private real M4A file")
        checked("private app-local M4A exists")
        tap("Podijeli glasovnu poruku")
        time.sleep(1.4)
        shot(OUT,"03-offline-share.png")
        checked("native Android sharing available offline")
        shell("input","keyevent","4")
        time.sleep(.8)
        tap("Postavke aplikacije")
        wait("Pravila privatnosti")
        tap("Pravila privatnosti")
        wait("Snimanje i reprodukcija")
        shot(OUT,"04-offline-privacy.png")
        # Verify the entire scrollable policy, not just its title.
        # The final paragraph is below the fold on compact screens.
        for _ in range(5):
            if locate("Kontakt i puna pravila"):
                break
            shell("input","swipe","540","1800","540","700","430")
            time.sleep(.4)
        wait("Kontakt i puna pravila")
        shot(OUT,"05-offline-privacy-end.png")
        checked("privacy information accessible without website")
        shell("input","keyevent","4")
        time.sleep(.8)
        wait("Poruka je spremna")
        checked("recording preserved after privacy view")
        check_crash()
        checked("no AndroidRuntime fatal exception")
        result["result"]="passed"
    finally:
        for command in [
            ("cmd","connectivity","airplane-mode","disable"),
            ("svc","wifi","enable"),
            ("svc","data","enable")
        ]:
            try:
                shell(*command)
            except Exception as exc:
                print("Network restoration warning:",repr(exc),flush=True)

if __name__=="__main__":
    try:
        run()
    except Exception as exc:
        result["result"]="failed"
        result["error"]=repr(exc)
        try:
            shot(OUT,"debug-offline-failure.png")
            result["crash_log"]=check_crash()[-6000:]
        except Exception:
            pass
        raise
    finally:
        result["ended"]=datetime.datetime.now(datetime.timezone.utc).isoformat()
        (OUT/"manifest.json").write_text(
            json.dumps(result,indent=2,ensure_ascii=False)+"\n","utf-8")
