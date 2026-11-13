#!/usr/bin/env python3
"""Import supplied VOJS archives, preserving the current compatible website."""
from pathlib import Path
from zipfile import ZipFile
import shutil
import sys

repo = Path(__file__).resolve().parent.parent
root = Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
web_candidates = list(root.glob("VOJS*.zip"))
def find_archive(needle):
    for path in web_candidates:
        with ZipFile(path) as z:
            if any(n.startswith(needle) for n in z.namelist()):
                return path
    raise SystemExit(f"Missing ZIP in {root} containing {needle!r}")

def extract_matching(zip_path, prefix, output):
    with ZipFile(zip_path) as archive:
        for info in archive.infolist():
            n = info.filename
            if info.is_dir() or not n.startswith(prefix):
                continue
            rel = Path(n[len(prefix):])
            if rel.is_absolute() or ".." in rel.parts:
                raise ValueError("Unsafe ZIP path")
            to = output / rel
            to.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(info) as src, to.open("wb") as dest:
                shutil.copyfileobj(src, dest)

web = find_archive("public_html/")
brand = find_archive("VOJS-Android-brending-1.0.0/")
extract_matching(web,"public_html/",repo/"original-web"/"public_html")
extract_matching(brand,"VOJS-Android-brending-1.0.0/",repo/"branding")
brand_res = repo/"branding"/"android"/"res"
app_res = repo/"app"/"src"/"main"/"res"
for group in ("mipmap-*","drawable-*"):
    for f in brand_res.glob(group+"/*.png"):
        target = app_res / f.parent.name / f.name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(f, target)
print("Original 13-page web and branding archived. Android bitmap assets imported.")
print("IMPORTANT: original web claims share links/expiration unsupported by offline app.")
print("Do not replace the published website with the legacy archive until web claims are adjusted.")
