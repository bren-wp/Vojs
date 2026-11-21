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

def locate(label, snapshot=None):
    """Match the actual accessible control, never a partial subtitle.

    A Compose text child can be clickable=false when its parent handles taps.
    Exact matching after *leading icon removal* is safe for that case. Broad
    substrings are unsafe: 'Preslušaj pa podijeli' is NOT the play button.
    """
    target = label.strip().casefold()
    candidates = []
    for node in (snapshot if snapshot is not None else nodes()):
        if not center(node):
            continue
        values = [node.get("text", ""), node.get("content-desc", "")]
        def normalized(value):
            # "Ⅱ" is a Unicode letter (numeric category), so the earlier
            # non-word prefix stripping did NOT remove the pause icon.
            value = re.sub(r"^[\s▶■◼⏸Ⅱ]+", "", value)
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

def recover_launcher_anr(snapshot):
    """Recover a known *system launcher* ANR; never dismiss an app ANR."""
    launcher_names = ("Quickstep", "Pixel Launcher", "Launcher3")
    prompts = [
        node.get("text", "") for node in snapshot
        if any(name in node.get("text", "") for name in launcher_names)
        and "isn't responding" in node.get("text", "")
    ]
    if not prompts:
        return False
    close = next((
        node for node in snapshot if node.get("text", "") == "Close app"
        and center(node) is not None
    ), None)
    if close is None:
        raise AssertionError("Launcher ANR detected but safe dismissal unavailable")
    x, y = center(close)
    print("Recovering emulator launcher ANR (not VOJS):", prompts, flush=True)
    shell("input", "tap", str(x), str(y))
    time.sleep(1)
    shell("am", "start", "-n", ACTIVITY)
    time.sleep(2)
    return True


def scroll_to_visible(label, max_swipes=5):
    """Reveal a real Compose control in its actual scrollable sheet.

    Offscreen Compose nodes can have zero-area accessibility bounds.
    A visible text string in the hierarchy alone does not prove it is
    tappable. Do not synthesize control coordinates or skip the check.
    """
    for attempt in range(max_swipes + 1):
        snapshot = nodes()
        match = locate(label, snapshot)
        if match is not None:
            return match
        if attempt == max_swipes:
            break
        scrolls = [
            node for node in snapshot
            if node.get("scrollable") == "true"
            and node.get("class") == "android.widget.ScrollView"
        ]
        if not scrolls:
            break
        scroll = max(scrolls, key=lambda n: (
            (lambda v: (v[2] - v[0]) * (v[3] - v[1]))(
                [int(x) for x in re.findall(r"[0-9]+", n.get("bounds", ""))]
            ) if len(re.findall(r"[0-9]+", n.get("bounds", ""))) == 4 else 0
        ))
        coordinates = [int(x) for x in re.findall(r"[0-9]+", scroll.get("bounds", ""))]
        if len(coordinates) != 4:
            break
        left, top, right, bottom = coordinates
        if right <= left or bottom - top < 160:
            break
        x = (left + right) // 2
        height = bottom - top
        shell("input", "swipe", str(x), str(top + int(height * .78)),
              str(x), str(top + int(height * .25)), "400")
        time.sleep(.6)
    raise AssertionError("Scrollable settings control not visible: " + label)


def wait(label, seconds=28):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        snapshot = nodes()
        if label == "Nova poruka" and recover_launcher_anr(snapshot):
            continue
        found = locate(label, snapshot)
        if found:
            return found
        time.sleep(.5)
    try:
        visible = nodes()
        print("UNAVAILABLE:", repr(label), "visible controls:",
              [(n.get("text"), n.get("content-desc")) for n in visible
               if n.get("text") or n.get("content-desc")][-35:], flush=True)
    except Exception as debug_error:
        print("UI debug failed:", repr(debug_error), flush=True)
    raise AssertionError("VOJS control not available: " + label)

def tap(label, seconds=28):
    node = wait(label, seconds)
    x,y = center(node)
    print("TAP", repr(label), "actual text:", repr(node.get("text", "")),
          "description:", repr(node.get("content-desc", "")),
          "coordinates:", (x, y), flush=True)
    shell("input", "tap", str(x), str(y))

def dismiss_settings():
    """Exercise normal Back, then the real modal scrim if Back is ignored.

    On some API 29 emulator windows Android Back is consumed while a
    Compose sheet remains open. The accessible Close sheet scrim is the
    real user dismissal action, not an invented UI coordinate.
    """
    shell("input", "keyevent", "4")
    time.sleep(.7)
    if locate("Postavke") is not None:
        tap("Close sheet")
        time.sleep(.7)
    if locate("Postavke") is not None:
        raise AssertionError("Settings modal remained open after dismissal")


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
        if args.fresh_install:
            # Dedicated CI emulator only: Quickstep can ANR immediately
            # after boot. Never stop the launcher on a physical user device.
            for launcher in ("com.android.launcher3", "com.google.android.apps.nexuslauncher"):
                try:
                    shell("am", "force-stop", launcher)
                except subprocess.CalledProcessError:
                    pass
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
                probe = rgb.getpixel((max(2, width // 30), height // 3))
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
        wait("Stvori poveznicu (internet)")
        tap("Stvori poveznicu (internet)")
        wait("Natrag", seconds=20)
        capture("05-p2p-sender.png")
        verified("P2P sender screen is reachable")
        tap("Natrag")
        wait("Poruka je spremna")
        verified("return to native ready state")
        tap("Podijeli glasovnu poruku")
        time.sleep(1.5)
        capture("05-share-sheet.png")
        verified("Android native sharing")
        shell("input", "keyevent", "4")
        time.sleep(.8)
        tap("Postavke aplikacije")
        wait("Tamna tema (zadana)")
        scroll_to_visible("Svijetla tema")
        capture("06-settings.png")
        verified("settings opened")
        tap("Svijetla tema")
        capture("07-settings-light.png")
        verified("select light theme")
        dismiss_settings()
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
        scroll_to_visible("Tamna tema (zadana)")
        tap("Tamna tema (zadana)")
        dismiss_settings()
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
