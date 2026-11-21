#!/usr/bin/env python3
"""Create genuine Play listing assets from user-owned branding and real ADB captures.

Converts channels only (no resizing, recoloring, overlays or invented designs).
Original media remains byte-for-byte unchanged.
"""
from __future__ import annotations
from pathlib import Path
import hashlib
import json
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
DEST = ROOT / "docs/play-store-assets"
SHOTS = ROOT / "docs/screenshots"
SOURCE = ROOT / "branding/google-play"
SELECTION = (
    "01-home.png",
    "02-recording.png",
    "06-paused.png",
    "03-ready.png",
    "08-dark-ready.png",
    "07-settings-dark.png",
)
def save(source: Path, destination: Path, mode: str, dimensions: tuple[int,int]) -> dict:
    if not source.is_file():
        raise ValueError(f"Missing genuine original: {source}")
    with Image.open(source) as src:
        src.load()
        if src.size != dimensions:
            raise ValueError(f"Invalid original dimensions {source}: {src.size}")
        if src.mode in ("RGBA", "LA"):
            alpha = src.getchannel("A")
            if alpha.getextrema() != (255,255):
                raise ValueError(f"Non-opaque original would change appearance: {source}")
        out = src.convert(mode)
        destination.parent.mkdir(parents=True, exist_ok=True)
        out.save(destination,"PNG",optimize=True)
        assert out.size == src.size, "Original pixels resized"
        assert out.convert("RGB").tobytes() == src.convert("RGB").tobytes(), \
            "RGB pixels changed while preparing Play asset"
    size = destination.stat().st_size
    if size > (1024*1024 if destination.name == "app-icon-512.png" else 8*1024*1024):
        raise ValueError(f"Play asset too large: {destination}")
    return {
        "file":str(destination.relative_to(DEST)),
        "original":str(source.relative_to(ROOT)),
        "width": dimensions[0], "height":dimensions[1],
        "channel_mode":mode, "bytes":size,
        "sha256":hashlib.sha256(destination.read_bytes()).hexdigest(),
        "pixel_source":"unaltered genuine original"
    }
records = [
    save(SOURCE/"ikona-512.png",DEST/"app-icon-512.png","RGBA",(512,512)),
    save(SOURCE/"grafika-istaknuto-1024x500.png",
         DEST/"feature-1024x500.png","RGB",(1024,500))
]
with Image.open(SHOTS/"01-home.png") as sample:
    dimensions=sample.size
width,height=dimensions
if not (320<=width<=3840 and 320<=height<=3840 and
        max(width,height) <= 2.3 * min(width,height)):
    raise ValueError(f"Genuine Android screenshots violate Play limits: {dimensions}")
for name in SELECTION:
    records.append(save(SHOTS/name,DEST/"screenshots"/name,"RGB",dimensions))
manifest = {
    "created_from":"genuine emulator screenshots and original VOJS branding",
    "no_manipulated_ui":True,
    "no_synthetic_image_generation":True,
    "full_screenshot_aspect_ratio":round(height/width,5),
    "promotion_9_16_recommendation_met":height*9==width*16,
    "notes":"Play accepts ratios <=2.3; 9:16 recommended for promotional eligibility.",
    "assets":records
}
DEST.mkdir(parents=True,exist_ok=True)
(DEST/"manifest.json").write_text(
    json.dumps(manifest,ensure_ascii=False,indent=2)+"\n","utf-8")
print("PASS generated",len(records),
      "Play assets from genuine images, preserving every RGB pixel")
if not manifest["promotion_9_16_recommendation_met"]:
    print("NOTE: 9:16 original device captures recommended for full Play merchandising.")
