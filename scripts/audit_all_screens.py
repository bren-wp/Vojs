#!/usr/bin/env python3
"""Real screenshot ROI coverage for all VOJS states.

A difference from previous emulator output is regression drift, NOT proof
of a deviation from an original design reference.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

ROOT = Path(__file__).resolve().parent.parent
SCREENSHOTS = ROOT / "docs/screenshots"
NAMES = (
    "00-splash-light.png", "01-home.png", "02-recording.png",
    "03-ready.png", "04-android-share.png", "05-settings.png",
    "06-paused.png", "07-settings-dark.png", "08-dark-ready.png",
    "09-dark-home.png", "10-splash-dark.png", "11-p2p-sender.png"
)
REGIONS = {
    "header": (0, .05, 1, .18),
    "content": (0, .18, 1, .68),
    "controls": (0, .68, 1, .94)
}
CONCEPT = {
    "02-recording.png": "01-snimi-1080x1920.png",
    "03-ready.png": "03-podijeli-1080x1920.png",
    "11-p2p-sender.png": "03-podijeli-1080x1920.png"
}

def roi(size, fractions):
    w, h = size
    return tuple(round(value * dim) for value, dim in
                 zip(fractions, (w, h, w, h)))

def difference(first, second, box):
    delta = ImageChops.difference(first.crop(box), second.crop(box))
    red, green, blue = delta.split()
    largest = ImageChops.lighter(ImageChops.lighter(red, green), blue)
    changed = largest.point(lambda x: 255 if x > 8 else 0)
    pixels = (box[2] - box[0]) * (box[3] - box[1])
    return {
        "changed_pixels_over_8_pct": round(100 * changed.histogram()[255] / pixels, 3),
        "mean_absolute_rgb_error": round(sum(ImageStat.Stat(delta).mean) / 3, 3)
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path)
    args = parser.parse_args()
    result = {
        "source": "Real Android emulator screenshots captured with adb",
        "baseline_is_previous_genuine_capture_not_design_reference": True,
        "branding_reference_is_marketing_composite_not_native_screen": True,
        "missing_expiry_screen": "Long-lived expiry choices are intentionally absent in P2P-only VOJS",
        "screen_count": len(NAMES),
        "screens": []
    }
    for filename in NAMES:
        target = SCREENSHOTS / filename
        if not target.exists():
            raise SystemExit("Missing genuine screenshot: " + filename)
        with Image.open(target) as img:
            actual = img.convert("RGB")
        entry = {"file": filename, "size": list(actual.size),
                 "reference_type": "real_device_only", "zones": {}}
        old = args.baseline / filename if args.baseline else None
        if old and old.exists():
            with Image.open(old) as image:
                previous = image.convert("RGB")
            if previous.size == actual.size:
                entry["previous_run_drift"] = {
                    key: difference(actual, previous, roi(actual.size, bounds))
                    for key, bounds in REGIONS.items()
                }
            else:
                entry["previous_run_drift"] = "resolution_mismatch"
        else:
            entry["previous_run_drift"] = "unavailable"
        if filename in CONCEPT:
            path = ROOT / "branding/google-play/snimke-zaslona" / CONCEPT[filename]
            with Image.open(path) as image:
                marketing = image.convert("RGB")
            if marketing.size != (1080, 1920):
                raise SystemExit("Unexpected supplied reference size")
            phone = marketing.crop((214, 349, 866, 1752))
            scaled = actual.resize(phone.size, Image.Resampling.LANCZOS)
            entry["marketing_reference"] = {
                "file": CONCEPT[filename],
                "approximate_comparison_only": True,
                "roi": {name: difference(scaled, phone, roi(phone.size, bounds))
                        for name, bounds in REGIONS.items()},
            }
        elif "splash" in filename:
            entry["reference_type"] = "exact_original_fullframe"
        result["screens"].append(entry)
        print("PASS captured real screen", filename, actual.size, flush=True)
    splash = SCREENSHOTS / "splash-pixel-audit.json"
    if splash.exists():
        result["exact_original_splash_report"] = json.loads(splash.read_text("utf-8"))
    path = SCREENSHOTS / "all-screens-roi-audit.json"
    path.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n",
                    encoding="utf-8")
    print("Wrote complete ROI coverage:", path, flush=True)

if __name__ == "__main__":
    main()
