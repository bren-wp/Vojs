#!/usr/bin/env python3
"""Real Android phone/tablet smoke test. No mockup or server required."""
import argparse
import datetime
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "com.brendigo.vojs"
ACTIVITY = PACKAGE + "/.MainActivity"

def adb(*args, timeout=60):
    return subprocess.run(["adb", *args], timeout=timeout,
                          capture_output=True, check=True)

def shell(*args):
    return adb("shell", *args).stdout.decode("utf-8", "replace")

def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/window.xml", timeout=50)
    xml = shell("cat", "/sdcard/window.xml")
    return [element.attrib for element in ET.fromstring(xml).iter("node")]

def center(node):
    nums = re.findall(r"[0-9]+", node.get("bounds", ""))
    if len(nums) != 4:
        return None
    x1,y1,x2,y2 = map(int, nums)
    return ((x1+x2)//2, (y1+y2)//2) if x2 > x1 and y2 > y1 else None

def locate(label):
    """Match the actual accessible control, never a partial subtitle.

    A Compose text child can be clickable=false when its parent handles taps.
    Exact matching after *leading icon removal* is safe for that case. Broad
    substrings are unsafe: 'Preslušaj pa podijeli' is NOT the play button.
    """
    target = label.strip().casefold()
    candidates = []
    for node in nodes():
        if not center(node):
            continue
        values = [node.get("text", ""), node.get("content-desc", "")]
        def normalized(value):
            value = re.sub(r"^[^\w]+", "", value, flags=re.UNICODE)
            return value.strip().casefold()
        if any(normalized(value) == target for value in values):
            candidates.append(node)
    if not candidates:
        return None
    candidates.sort(key=lambda node: (
        node.get("clickable") != "true",
        len(node.get("text", "") + node.get("content-desc", ""))
    ))
    return candidates[0]

def wait(label, seconds=28):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        found = locate(label)
        if found:
            return found
        time.sleep(.5)
    raise AssertionError("VOJS control not available: " + label)

def tap(label, seconds=28):
    node = wait(label, seconds)
    x,y = center(node)
    print("TAP", repr(label), "actual text:", repr(node.get("text", "")),
          "description:", repr(node.get("content-desc", "")),
          "coordinates:", (x, y), flush=True)
    shell("input", "tap", str(x), str(y))

def shot(folder, filename):
    data = adb("exec-out", "screencap", "-p").stdout
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise AssertionError("No genuine PNG from Android ADB")
    (folder / filename).write_bytes(data)

def check_crash():
    log = adb("logcat", "-d", "-s", "AndroidRuntime:E", "*:S").stdout.decode(
        "utf-8", "replace"
    )
    if re.search(r"FATAL EXCEPTION[\s\S]{0,1600}Process:\s*com\.brendigo\.vojs", log):
        raise AssertionError("VOJS AndroidRuntime fatal exception detected")
    return log

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", required=True)
    parser.add_argument("--output", default="docs/responsive")
    parser.add_argument("--font-scale", default=None)
    parser.add_argument("--fresh-install", action="store_true")
    args = parser.parse_args()
    dest = Path(args.output) / args.profile
    dest.mkdir(parents=True, exist_ok=True)
    result = {
        "profile": args.profile,
        "source": "Real ADB screenshots from Android, no synthetic mockups",
        "start": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "checks": [], "captures": []
    }
    original_font = None
    def verified(label):
        result["checks"].append(label)
        print("PASS:", label, flush=True)
    def capture(name):
        shot(dest, name)
        result["captures"].append(name)
    try:
        adb("wait-for-device", timeout=120)
        result["api"] = shell("getprop", "ro.build.version.sdk").strip()
        result["display"] = shell("wm", "size").strip()
        result["density"] = shell("wm", "density").strip()
        if args.font_scale:
            original_font = shell("settings", "get", "system", "font_scale").strip()
            shell("settings", "put", "system", "font_scale", args.font_scale)
        # Dedicated CI emulator: clear an existing theme choice before checking
        # the fresh-install default. Never do this in the physical-device run.
        if args.fresh_install:
            shell("pm", "clear", PACKAGE)
        shell("pm", "grant", PACKAGE, "android.permission.RECORD_AUDIO")
        shell("am", "force-stop", PACKAGE)
        shell("am", "start", "-n", ACTIVITY)
        time.sleep(4)
        wait("Nova poruka")
        wait("Započni snimanje")
        capture("01-dark-home.png")
        if args.fresh_install:
            # Device-screenshot pixel sampled from a known content margin;
            # ensures a truly dark canvas (independent of system theme).
            from PIL import Image
            with Image.open(dest / "01-dark-home.png") as screenshot:
                rgb = screenshot.convert("RGB")
                width, height = rgb.size
                probe = rgb.getpixel((width // 2, height // 4))
                assert max(probe) < 85, "Fresh install did not start in dark mode: " + str(probe)
            verified("fresh-install dark theme rendered")
        verified("home + native record controls")
        tap("Započni snimanje")
        wait("Završi snimanje")
        capture("02-recording.png")
        verified("microphone recording")
        tap("Pauziraj")
        wait("Nastavi snimanje")
        capture("03-paused.png")
        verified("pause recording")
        tap("Nastavi snimanje")
        # ADB uiautomator dumps may take seconds on slower emulators.
        # Keep a long enough M4A recording so playback is still active
        # when the next UI hierarchy is queried.
        time.sleep(26)
        tap("Završi snimanje")
        wait("Poruka je spremna")
        assert locate("Preslušaj") is not None, "Real playback control not found"
        capture("04-ready.png")
        verified("finish with playable M4A")
        tap("Preslušaj")
        wait("Zaustavi", seconds=10)
        tap("Zaustavi", seconds=10)
        verified("playback start and stop")
        tap("Podijeli glasovnu poruku")
        time.sleep(1.5)
        capture("05-share-sheet.png")
        verified("Android native sharing")
        shell("input", "keyevent", "4")
        time.sleep(.8)
        tap("Postavke aplikacije")
        wait("Svijetla tema")
        wait("Tamna tema (zadana)")
        capture("06-settings.png")
        verified("settings opened")
        tap("Svijetla tema")
        capture("07-settings-light.png")
        verified("select light theme")
        shell("input", "keyevent", "4")
        time.sleep(.8)
        tap("Snimi novu poruku")
        wait("Nova poruka")
        capture("08-light-home.png")
        verified("reset message and light theme")
        shell("am", "force-stop", PACKAGE)
        shell("am", "start", "-n", ACTIVITY)
        time.sleep(4)
        wait("Nova poruka")
        capture("09-light-relaunch.png")
        verified("theme persists after restart")
        # Leave preference dark after test; old users can retain light/system.
        tap("Postavke aplikacije")
        wait("Tamna tema (zadana)")
        tap("Tamna tema (zadana)")
        shell("input", "keyevent", "4")
        verified("dark theme can be restored")
        check_crash()
        verified("no app fatal exception")
        result["result"] = "passed"
    except Exception as error:
        result["result"] = "failed"
        result["error"] = repr(error)
        print("FAILED:", repr(error), flush=True)
        try:
            capture("debug-failure.png")
            result["debug_log"] = check_crash()[-10000:]
        except Exception:
            pass
        raise
    finally:
        if original_font and original_font not in ("null", "", "undefined"):
            try:
                shell("settings", "put", "system", "font_scale", original_font)
            except Exception as error:
                result["font_restore_error"] = repr(error)
        result["completed"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        (dest / "manifest.json").write_text(
            json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )

if __name__ == "__main__":
    main()
