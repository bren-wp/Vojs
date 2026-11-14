#!/usr/bin/env python3
"""Measure real splash screenshot divergence from the actual VOJS brand artwork.

The system status and navigation bars are OS-owned and excluded from the
pixel-level comparison. Do not interpret this as a 1:1 certification of every
other app screen.
"""
from pathlib import Path
from PIL import Image, ImageChops, ImageStat
import json

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "docs/screenshots"
ASSET = ROOT / "branding/pocetni-zaslon"
PAIRS = [
    ("00-splash-light.png", "pocetni-zaslon-svijetli-1080x2400.png"),
    ("10-splash-dark.png", "pocetni-zaslon-tamni-1080x2400.png"),
]
results = []
for captured, reference in PAIRS:
    got = Image.open(BASE / captured).convert("RGB")
    expected = Image.open(ASSET / reference).convert("RGB")
    if got.size != expected.size:
        raise SystemExit(f"{captured}: dimensions {got.size} vs {expected.size}")
    w, h = got.size
    roi = (0, 115, w, h - 140)  # Android status/nav overlays
    diff = ImageChops.difference(got.crop(roi), expected.crop(roi))
    c = diff.split()
    max_channel = ImageChops.lighter(ImageChops.lighter(c[0], c[1]), c[2])
    changed = max_channel.point(lambda value: 255 if value > 4 else 0)
    changed_fraction = changed.histogram()[255] / (changed.width * changed.height)
    mae = sum(ImageStat.Stat(diff).mean) / 3
    entry = {
        "screenshot": captured,
        "reference": reference,
        "size": [w, h],
        "roi": list(roi),
        "changed_pixels_over_4_percent": round(100 * changed_fraction, 5),
        "mean_absolute_rgb_error": round(mae, 5),
    }
    results.append(entry)
    print(json.dumps(entry, ensure_ascii=False), flush=True)
report = {
    "device": "Pixel 6 emulator, Android API 35",
    "results": results,
    "note": "Android-owned bars excluded; other screens still need ROI calibration",
}
(BASE / "splash-pixel-audit.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
)
if any(r["changed_pixels_over_4_percent"] > 1.0 for r in results):
    raise SystemExit("Splash pixel divergence above 1%; original reference is not displayed")
