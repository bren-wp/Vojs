#!/usr/bin/env python3
"""Quantify VOJS recording screen differences against the *embedded* brand concept.

The original 1080x1920 marketing image shows a framed phone, rather than
an Android screenshot. Only the white display area of the embedded phone
is used. Images are normalized to that geometry for regression tracking.
This metric is NOT an exact one-to-one pixel proof because fonts, system
bars, text/timer and live audio are dynamic.
"""
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

ROOT = Path(__file__).resolve().parent.parent
CAPTURES = ROOT / "docs" / "screenshots"
INPUT = ROOT / "branding" / "google-play" / "snimke-zaslona" / "01-snimi-1080x1920.png"
LIVE = CAPTURES / "02-recording.png"
OUTPUT = CAPTURES / "recording-pixel-audit.json"

concept = Image.open(INPUT).convert("RGB")
actual = Image.open(LIVE).convert("RGB")
if concept.size != (1080, 1920) or actual.size != (1080, 2400):
    raise SystemExit(f"Unrecognized reference or emulator resolution: {concept.size} {actual.size}")

# Bounds of the phone's interior white display in the branded marketing image.
interior = (214, 349, 866, 1752)
reference = concept.crop(interior)
rendered = actual.resize(reference.size, Image.Resampling.LANCZOS)
w, h = reference.size

zones = {
    "header": (0, 0, w, 240),
    "waveform": (0, 240, w, 640),
    "time": (0, 640, w, 970),
    "controls": (0, 970, w, h),
}
result = {
    "source": "Live ADB emulator screenshot versus VOJS conceptual phone interior",
    "reference": "branding/google-play/snimke-zaslona/01-snimi-1080x1920.png",
    "actual": "docs/screenshots/02-recording.png",
    "reference_crop": list(interior),
    "normalized_resolution": [w, h],
    "caveat": "Conceptual render and live UI are not identical source media; clock, recorder state, status bar and live waveform vary.",
    "zones": {}
}
for name, roi in zones.items():
    a = reference.crop(roi)
    b = rendered.crop(roi)
    diff = ImageChops.difference(a, b)
    split = diff.split()
    maximum = ImageChops.lighter(ImageChops.lighter(split[0], split[1]), split[2])
    altered = maximum.point(lambda v: 255 if v > 8 else 0).histogram()[255]
    percent = round(100 * altered / (a.width * a.height), 3)
    mae = round(sum(ImageStat.Stat(diff).mean) / 3, 3)
    result["zones"][name] = {
        "roi": list(roi),
        "changed_pixels_over_8_percent": percent,
        "mean_absolute_rgb_error": mae,
    }
    print(f"{name}: changed pixels {percent:.3f}% (threshold 8), RGB MAE {mae:.3f}", flush=True)

OUTPUT.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
# The reference is a marketing composite and includes non-app chrome:
# report all deviations but only fail for a major visual regression.
if result["zones"]["waveform"]["changed_pixels_over_8_percent"] > 18:
    raise SystemExit("Waveform diverges >18% from the approximate original concept; pixel QA regression")
