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
    matches = []
    query = label.casefold()
    for node in root.iter("node"):
        raw_text = node.attrib.get("text", "").strip()
        raw_description = node.attrib.get("content-desc", "").strip()
        content = (raw_text + " " + raw_description).casefold()
        if query not in content:
            continue
        bounds = re.findall(r"[0-9]+", node.attrib.get("bounds", ""))
        if len(bounds) != 4:
            continue
        a, b, c, d = (int(x) for x in bounds)
        exact = raw_text.casefold() == query or raw_description.casefold() == query
        clickable = node.attrib.get("clickable") == "true"
        suffix = raw_text.casefold().endswith(query)
        priority = (100 if exact else 0) + (40 if clickable else 0) + (15 if suffix else 0)
        matches.append((priority, (a+c)//2, (b+d)//2, raw_text))
    if matches:
        winner = max(matches, key=lambda row: row[0])
        print("UI match", repr(label), "->", repr(winner[3]), "score", winner[0], flush=True)
        return (winner[1], winner[2])
    return None

def tap(label):
    pos = bounds_of(label)
    if pos is None:
        print("State not available:", label, flush=True)
        return False
    adb("shell","input","tap",str(pos[0]),str(pos[1]))
    time.sleep(0.8)
    return True

def recover_launcher_anr():
    # Emulator infrastructure issue, not a VOJS app exception.
    # Google Pixel Launcher may hang while the emulator is booting,
    # covering the foreground app with an OS-controlled ANR dialog.
    if not bounds_of("Pixel Launcher isn't responding"):
        return False
    print("Recovering Pixel Launcher ANR overlay", flush=True)
    if bounds_of("Close app"):
        tap("Close app")
    elif bounds_of("Wait"):
        tap("Wait")
    else:
        return False
    time.sleep(1)
    adb("shell", "am", "start", "-n", "com.brendigo.vojs/.MainActivity")
    return True


def fail(reason):
    print("TEST FAILED:", reason, flush=True)
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/window.xml", timeout=40)
        current = adb("shell", "cat", "/sdcard/window.xml").stdout.decode("utf-8")
        (OUT / "debug-ui-hierarchy.xml").write_text(current, encoding="utf-8")
        print("UI hierarchy:", current[:14000], flush=True)
        log = adb("logcat", "-d", "-s", "AndroidRuntime:E", "*:S").stdout.decode("utf-8")
        (OUT / "debug-android-crashes.log").write_text(log[-25000:], encoding="utf-8")
        screenshot("debug-failed-control.png", "Diagnostic screenshot on failed control")
    except Exception as exception:
        print("Could not capture diagnostics:", repr(exception), flush=True)
    raise RuntimeError(reason)

adb("wait-for-device", timeout=120)
adb("shell","pm","grant","com.brendigo.vojs","android.permission.RECORD_AUDIO")
adb("shell","am","force-stop","com.brendigo.vojs")
adb("shell","am","start","-n","com.brendigo.vojs/.MainActivity")
# Real screenshot of the native full-frame VOJS splash, while it is visible.
time.sleep(1.35)
screenshot("00-splash-light.png", "Original VOJS splash - light")
time.sleep(2.5)
# Older emulator images may display Android's immersive tutorial left
# behind by an earlier build; dismiss only this system prompt, if present.
if bounds_of("Viewing full screen") and bounds_of("Got it"):
    tap("Got it")
time.sleep(3)
screenshot("01-home.png","Home / idle")

def wait_ui(label, timeout_s=9):
    for second in range(timeout_s):
        if bounds_of(label):
            return True
        if recover_launcher_anr():
            continue
        if bounds_of("Viewing full screen") and bounds_of("Got it"):
            tap("Got it")
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

if not wait_ui("Započni snimanje", 20):
    raise RuntimeError("Home did not render native start-recording button")
if tap("Započni snimanje") and wait_ui("Završi snimanje", 12):
    screenshot("02-recording.png","Recording")
    if not tap("Pauziraj") or not bounds_of("Nastavi snimanje"):
        fail("Pause control failed")
    screenshot("06-paused.png","Recording paused")
    if not tap("Nastavi snimanje"):
        fail("Resume control failed")
    time.sleep(8)
    if tap("Završi snimanje") and wait_ui("Podijeli glasovnu poruku", 12):
        screenshot("03-ready.png","Recorded audio / ready to share")
        if not tap("Preslušaj") or not bounds_of("Zaustavi"):
            fail("Audio playback control failed")
        if not tap("Zaustavi"):
            fail("Stop playback failed")
        if tap("Podijeli glasovnu poruku"):
            screenshot("04-android-share.png","Android native Sharesheet")
            adb("shell", "input", "keyevent", "4")
            time.sleep(2)
            if not (tap("Postavke aplikacije") or tap("VOJS ⚙")):
                fail("Settings control failed")
            screenshot("05-settings.png", "Native settings and theme controls")
            if not tap("Tamna tema"):
                fail("Dark theme option failed")
            screenshot("07-settings-dark.png", "Native dark theme settings")
            adb("shell", "input", "keyevent", "4")
            time.sleep(2)
            screenshot("08-dark-ready.png", "Native dark theme ready screen")
            if not tap("Snimi novu poruku"):
                fail("Start new message failed")
            screenshot("09-dark-home.png", "Native dark mode home")
            # Confirm dark theme persisted across a fresh app launch.
            adb("shell", "am", "force-stop", "com.brendigo.vojs")
            adb("shell", "am", "start", "-n", "com.brendigo.vojs/.MainActivity")
            time.sleep(1.35)
            screenshot("10-splash-dark.png", "Original VOJS splash - dark")

# A successful UI flow must also be free of app-process Java crashes.
crashes = adb("logcat", "-d", "-s", "AndroidRuntime:E", "*:S").stdout.decode(
    "utf-8", errors="replace"
)
if re.search(r"FATAL EXCEPTION[\s\S]{0,800}Process: com\.brendigo\.vojs", crashes):
    fail("VOJS encountered a fatal exception during smoke tests")

(OUT/"manifest.json").write_text(json.dumps({
    "source": "Android emulator; real adb screencap screenshots",
    "device": "Pixel 6 profile, Android API 35 emulator",
    "captured_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "screenshots": saved
},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
if len(saved) < 11:
    fail(f"Expected 9 functional screens and both splash themes, captured {len(saved)}")
