#!/usr/bin/env python3
"""Compare real Chromium screenshots to exact optional user-provided references.

No reference is NOT a pass. Missing references are explicitly unverified.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

parser = argparse.ArgumentParser()
parser.add_argument("--captures", type=Path, required=True)
parser.add_argument("--references", type=Path, required=True)
parser.add_argument("--strict", action="store_true",
                    help="Fail for any missing or differing exact reference")
args = parser.parse_args()
captured = args.captures.resolve()
references = args.references.resolve()
manifest = json.loads((captured / "manifest.json").read_text("utf-8"))
screens = manifest.get("screens", [])
if len(screens) != 112:
    raise SystemExit("Incomplete web screenshot set (expected 112, found %s)" % len(screens))
report = {
    "method": "Per-pixel RGB comparison of genuine screenshot to exactly sized reference",
    "reference_source": str(args.references),
    "unverified_when_missing_reference": True,
    "strict": args.strict,
    "screens": [],
    "counts": {"exact": 0, "different": 0, "wrong_size": 0, "missing_reference": 0}
}
for entry in screens:
    rel = Path(entry["png"])
    if rel.is_absolute() or ".." in rel.parts:
        raise SystemExit("Invalid screenshot path")
    shot = (captured / rel).resolve()
    target = (references / rel).resolve()
    if not shot.is_relative_to(captured) or not target.is_relative_to(references):
        raise SystemExit("Screenshot path escaped approved directory")
    if not shot.is_file():
        raise SystemExit("Missing real capture: " + str(shot))
    row = {"png": rel.as_posix(), "status": None}
    if not target.is_file():
        row["status"] = "missing_reference"
    else:
        with Image.open(shot) as original, Image.open(target) as expected:
            current = original.convert("RGB")
            truth = expected.convert("RGB")
        row["captured_size"] = list(current.size)
        row["reference_size"] = list(truth.size)
        if current.size != truth.size:
            row["status"] = "wrong_size"
        else:
            delta = ImageChops.difference(current, truth)
            bands = delta.split()
            maximum = ImageChops.lighter(ImageChops.lighter(bands[0], bands[1]), bands[2])
            changed = len(maximum.getbbox() or ()) > 0
            mask = maximum.point(lambda pixel: 255 if pixel != 0 else 0)
            different_pixels = mask.histogram()[255]
            total = current.width * current.height
            row["status"] = "different" if changed else "exact"
            row["changed_pixels"] = different_pixels
            row["changed_pct"] = round(100 * different_pixels / total, 6)
            row["mean_absolute_rgb_error"] = round(
                sum(ImageStat.Stat(delta).mean) / 3, 6)
    report["screens"].append(row)
    report["counts"][row["status"]] += 1
report["all_exact"] = report["counts"]["exact"] == len(screens)
report["all_compared"] = report["counts"]["missing_reference"] == 0
out = captured / "pixel-audit.json"
out.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", "utf-8")
print("VOJS WEB PIXEL AUDIT:", report["counts"], "total:", len(screens))
print("All exact:", report["all_exact"], "(not claimed if references missing)")
if args.strict and not report["all_exact"]:
    raise SystemExit("Strict pixel audit did not match all 112 references exactly.")
