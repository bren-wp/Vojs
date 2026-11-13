#!/usr/bin/env python3
"""Verify and import original VOJS web and branding without deploying a website."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import json
import shutil
import sys

repo = Path(__file__).resolve().parent.parent
archives = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else repo / "source-archives"
web_zip = archives / "VOJS.zip"
brand_zip = archives / "VOJS-Android-brending-1.0.0.zip"
checksums = archives / "SHA256SUMS"

if not web_zip.is_file() or not brand_zip.is_file() or not checksums.is_file():
    raise SystemExit("Original ZIP archives / SHA256SUMS are missing.")

expected = {}
for row in checksums.read_text(encoding="ascii").splitlines():
    digest, name = row.split(maxsplit=1)
    expected[name.strip()] = digest
for source in [web_zip, brand_zip]:
    actual = hashlib.sha256(source.read_bytes()).hexdigest()
    if actual != expected.get(source.name):
        raise SystemExit(f"SHA256 mismatch for {source.name}")
    with ZipFile(source) as zipfile:
        bad = zipfile.testzip()
        if bad:
            raise SystemExit(f"ZIP CRC mismatch: {bad}")

def import_entries(source: Path, prefix: str, target: Path):
    count = 0
    with ZipFile(source) as archive:
        for item in archive.infolist():
            name = item.filename
            if item.is_dir() or not name.startswith(prefix):
                continue
            rel = Path(name[len(prefix):])
            if rel.is_absolute() or ".." in rel.parts or not rel.parts:
                raise SystemExit(f"Unsafe path in ZIP: {name}")
            dest = target.joinpath(*rel.parts)
            dest.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(item) as src, dest.open("wb") as out:
                shutil.copyfileobj(src, out)
            count += 1
    return count

web_count = import_entries(web_zip, "public_html/", repo / "original-web" / "public_html")
brand_count = import_entries(brand_zip, "VOJS-Android-brending-1.0.0/", repo / "branding")
original_res = repo / "branding" / "android" / "res"
android_res = repo / "app" / "src" / "main" / "res"
assets_copied = 0
for folder in original_res.iterdir():
    if folder.name.startswith(("mipmap-", "drawable-")):
        for source in folder.iterdir():
            if source.suffix.lower() not in (".png", ".xml"):
                continue
            dest = android_res / folder.name / source.name
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, dest)
            assets_copied += 1

# On Android 7/8 use supplied raster launcher PNG, on Android 8+ use
# the supplied adaptive icon that references original foreground/background PNG.
for launcher in ("ic_launcher.xml", "ic_launcher_round.xml"):
    fallback = android_res / "mipmap-anydpi" / launcher
    if fallback.is_file():
        fallback.unlink()

metadata = {
    "original_web_zip_sha256": expected[web_zip.name],
    "original_branding_zip_sha256": expected[brand_zip.name],
    "web_files": web_count,
    "branding_files": brand_count,
    "android_resources_copied": assets_copied,
    "deploy_pages": False,
}
out = repo / "docs" / "IMPORTED-ASSETS.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
print(json.dumps(metadata, indent=2))
if web_count != 87 or brand_count != 55:
    raise SystemExit("Expected 87 original website and 55 branding files.")
