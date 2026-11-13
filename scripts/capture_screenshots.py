#!/usr/bin/env python3
"""Capture genuine on-device screenshots via Android emulator, never mockups."""
import datetime
import json
import re
import subprocess
import time
from pathlib import Path
import xml.etree.ElementTree as ET

OUT = Path("docs/screenshots")
OUT.mkdir(parents=True, exist_ok=True)
saved = []

def adb(*args, timeout=30):
    return subprocess.run(["adb", *args], capture_output=True, timeout=timeout, check=True)

def screenshot(name, state):
    data = adb("exec-out", "screencap", "-p").stdout
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("Invalid emulator screenshot data")
    output = OUT / name
    output.write_bytes(data)
    saved.append({"file": name, "screen": state})
    print("Captured", name, len(data), "bytes", flush=True)

def bounds_of(label):
    adb("shell", "uiautomator", "dump", "/sdcard/window.xml", timeout=40)
    ui = adb("shell", "cat", "/sdcard/window.xml").stdout.decode("utf-8")
    root = ET.fromstring(ui)
    for node in root.iter("node"):
        if label.casefold() in (node.attrib.get("text","")+" "+node.attrib.get("content-desc","")).casefold():
            bounds = re.findall(r"\d+", node.attrib.get("bounds",""))
            if len(bounds)==4:
                a,b,c,d = [int(x) for x in bounds]
                return ((a+c)//2,(b+d)//2)
    return None

def tap(label):
    pos = bounds_of(label)
    if pos is None:
        print("State not available:", label, flush=True)
        return False
    adb("shell","input","tap",str(pos[0]),str(pos[1]))
    time.sleep(3)
    return True

adb("wait-for-device", timeout=120)
adb("shell","pm","grant","com.brendigo.vojs","android.permission.RECORD_AUDIO")
adb("shell","am","start","-n","com.brendigo.vojs/.MainActivity")
time.sleep(7)
screenshot("01-home.png","Home / idle")
if tap("Započni snimanje") and bounds_of("Završi snimanje") is not None:
    screenshot("02-recording.png","Recording")
    time.sleep(4)
    if tap("Završi snimanje") and bounds_of("Podijeli glasovnu poruku") is not None:
        screenshot("03-ready.png","Recorded audio / ready to share")
        if tap("Podijeli glasovnu poruku"):
            screenshot("04-android-share.png","Android native Sharesheet")
            adb("shell", "input", "keyevent", "4")
            time.sleep(2)
            if tap("Postavke aplikacije"):
                screenshot("05-settings.png", "Native settings and theme controls")
(OUT/"manifest.json").write_text(json.dumps({
    "source": "Android emulator; real adb screencap screenshots",
    "device": "Pixel 6 profile, Android API 35 emulator",
    "captured_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "screenshots": saved
},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
if len(saved) < 4:
    raise RuntimeError(f"Expected at least 4 functional screens, captured {len(saved)}")
