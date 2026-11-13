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
adb("shell","am","force-stop","com.brendigo.vojs")
adb("shell","am","start","-n","com.brendigo.vojs/.MainActivity")
time.sleep(4)
# Older emulator images may display Android's immersive tutorial left
# behind by an earlier build; dismiss only this system prompt, if present.
if bounds_of("Viewing full screen") and bounds_of("Got it"):
    tap("Got it")
time.sleep(3)
screenshot("01-home.png","Home / idle")

def wait_ui(label, timeout_s=30):
    for second in range(timeout_s):
        if bounds_of(label):
            return True
        time.sleep(1)
    print("Timed out waiting for", repr(label), flush=True)
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/window.xml", timeout=40)
        current = adb("shell", "cat", "/sdcard/window.xml").stdout.decode("utf-8")
        (OUT / "debug-ui-hierarchy.xml").write_text(current, encoding="utf-8")
        print("UI hierarchy:", current[:14000], flush=True)
        log = adb("logcat", "-d", "-s", "AndroidRuntime:E", "*:S").stdout.decode("utf-8")
        (OUT / "debug-android-crashes.log").write_text(log[-25000:], encoding="utf-8")
        print("Recent app crashes:", log[-6000:], flush=True)
    except Exception as problem:
        print("Debug collection failed:", problem, flush=True)
    screenshot("debug-missing-control.png","Diagnostic screenshot when control is unavailable")
    return False

if not wait_ui("Započni snimanje"):
    raise RuntimeError("Home did not render native start-recording button")
if tap("Započni snimanje") and bounds_of("Završi snimanje") is not None:
    screenshot("02-recording.png","Recording")
    if not tap("Pauziraj") or not bounds_of("Nastavi snimanje"):
        raise RuntimeError("Pause control failed")
    screenshot("06-paused.png","Recording paused")
    if not tap("Nastavi snimanje"):
        raise RuntimeError("Resume control failed")
    time.sleep(4)
    if tap("Završi snimanje") and bounds_of("Podijeli glasovnu poruku") is not None:
        screenshot("03-ready.png","Recorded audio / ready to share")
        if not tap("Preslušaj") or not bounds_of("Zaustavi"):
            raise RuntimeError("Audio playback control failed")
        if not tap("Zaustavi"):
            raise RuntimeError("Stop playback failed")
        if tap("Podijeli glasovnu poruku"):
            screenshot("04-android-share.png","Android native Sharesheet")
            adb("shell", "input", "keyevent", "4")
            time.sleep(2)
            if not (tap("Postavke aplikacije") or tap("VOJS ⚙")):
                raise RuntimeError("Settings control failed")
            screenshot("05-settings.png", "Native settings and theme controls")
            if not tap("Tamna tema"):
                raise RuntimeError("Dark theme option failed")
            screenshot("07-settings-dark.png", "Native dark theme settings")
            adb("shell", "input", "keyevent", "4")
            time.sleep(2)
            screenshot("08-dark-ready.png", "Native dark theme ready screen")
            if not tap("Snimi novu poruku"):
                raise RuntimeError("Start new message failed")
            screenshot("09-dark-home.png", "Native dark mode home")

(OUT/"manifest.json").write_text(json.dumps({
    "source": "Android emulator; real adb screencap screenshots",
    "device": "Pixel 6 profile, Android API 35 emulator",
    "captured_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "screenshots": saved
},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
if len(saved) < 9:
    raise RuntimeError(f"Expected 9 functional screens, captured {len(saved)}")
