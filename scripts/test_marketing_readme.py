#!/usr/bin/env python3
"""Protect marketing assets and customer-facing copy from broken or dummy content."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent
readme = (root / "README.md").read_text("utf-8")
screens = root / "docs/screenshots"
capture = json.loads((screens / "manifest.json").read_text("utf-8"))
captured = {item["file"] for item in capture["screenshots"]}
assert "real adb screencap" in capture["source"], "Marketing needs documented real screen captures"
images = re.findall(r'<img\s+[^>]*src="([^"]+)"', readme)
assert len(images) >= 9, "README must feature icon, artwork and actual app screens"
assert "docs/play-store-assets/app-icon-512.png" in images
assert "docs/play-store-assets/feature-1024x500.png" in images
actual = [Path(value).name for value in images if value.startswith("docs/screenshots/")]
assert len(set(actual)) >= 6, "Use several different real application screens"
assert all(name in captured for name in actual), "Screenshot missing real ADB capture provenance"
for relative in images:
    assert not relative.startswith(("http:", "https:")), "Use versioned first-party media"
    path = root / relative
    assert path.is_file(), f"Broken README image: {relative}"
    assert path.read_bytes()[:8] == b"\x89PNG\r\n\x1a\n", f"Expected PNG: {relative}"

manifest_path = root / "docs/play-store-assets/manifest.json"
assets = json.loads(manifest_path.read_text("utf-8"))
assert assets["created_from"] == "genuine emulator screenshots and original VOJS branding"
for item in assets["assets"]:
    target = root / "docs/play-store-assets" / item["file"]
    assert target.exists(), f"Missing verified Play media: {target}"
    assert hashlib.sha256(target.read_bytes()).hexdigest() == item["sha256"], (
        f"Play media differs from recorded SHA-256: {target}"
    )
assert len([x for x in assets["assets"] if x["file"].startswith("screenshots/")]) >= 6

bad = re.compile(
    r"\b(?:lorem ipsum|debug(?:ger)?|dev mode|development build|"
    r"mockup placeholder|staging server|coming soon|test screen)\b",
    re.IGNORECASE,
)
assert not bad.search(readme), "README contains development or placeholder copy"
native = root / "app/src/main/java/com/brendigo/vojs"
for filename in ("VoiceScreen.kt", "VojsSettingsSheet.kt", "VojsPrivacySheet.kt",
                 "VojsHeader.kt", "PeerSenderActivity.kt"):
    source = (native / filename).read_text("utf-8")
    # Only user-visible Text() literal strings, not comments or internal source.
    for visible in re.findall(r'\bText\s*\(\s*"([^"]+)"', source):
        assert not bad.search(visible), f"Developer copy on {filename}: {visible}"
sender = (root / "app/src/main/assets/p2p-android-sender.html").read_text("utf-8")
# Check the markup content; inline JS is implementation, not displayed text.
body = sender.split("<script", 1)[0].split("<body", 1)[-1]
visible_markups = re.findall(r">([^<>]+)<", body)
for visible in visible_markups:
    assert not bad.search(visible), f"Developer copy in Android sender: {visible}"
print(f"PASS marketing README: {len(images)} local images, {len(set(actual))} real app screens, "
      f"{len(assets['assets'])} SHA-256 verified Play assets, no placeholder UI copy")
