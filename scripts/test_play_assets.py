#!/usr/bin/env python3
"""Validate VOJS Play assets without decoding or altering user media."""
from pathlib import Path
import struct

ROOT=Path(__file__).resolve().parent.parent
def png(path):
    content=path.read_bytes()
    if content[:8]!=b"\x89PNG\r\n\x1a\n" or content[12:16]!=b"IHDR":
        raise AssertionError(f"Not PNG: {path}")
    width,height=struct.unpack(">II",content[16:24])
    bit_depth,color_type=content[24:26]
    return width,height,bit_depth,color_type,len(content)
icon=ROOT/"branding/google-play/ikona-512.png"
feature=ROOT/"branding/google-play/grafika-istaknuto-1024x500.png"
assert png(icon)[:2]==(512,512) and png(icon)[4]<=1024*1024
assert png(feature)[:2]==(1024,500) and png(feature)[4]<=15*1024*1024
originals=[
    "01-home.png", "02-recording.png", "06-paused.png",
    "03-ready.png", "08-dark-ready.png","07-settings-dark.png"
]
for name in originals:
    width,height,depth,color,size=png(ROOT/"docs/screenshots"/name)
    assert depth==8 and 320<=width<=3840 and 320<=height<=3840
    assert max(width,height)<=2.3*min(width,height)
    assert size<=8*1024*1024
dest=ROOT/"docs/play-store-assets"
if dest.exists():
    assert png(dest/"app-icon-512.png")[:4]==(512,512,8,6)
    assert png(dest/"feature-1024x500.png")[:4]==(1024,500,8,2)
    for name in originals:
        width,height,depth,color,size=png(dest/"screenshots"/name)
        assert depth==8 and color==2 and size<=8*1024*1024
print("PASS original dimensions and format; derived Play assets checked when present")
