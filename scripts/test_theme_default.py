#!/usr/bin/env python3
"""Guard brand-dark first launch without resetting users' saved appearance."""
from pathlib import Path
source = Path("app/src/main/java/com/brendigo/vojs/MainActivity.kt").read_text("utf-8")
palette = Path("app/src/main/res/values/colors.xml").read_text("utf-8")
screenshot = Path("scripts/capture_screenshots.py").read_text("utf-8")
assert 'getString("vojs_appearance", "dark")' in source, "Fresh installs must start dark"
assert '?.takeIf { it in setOf("system", "light", "dark") } ?: "dark"' in source
assert 'putString("vojs_appearance", key)' in source, "Existing user choice must remain"
assert '<color name="vojs_splash_background">#20213B</color>' in palette
assert screenshot.index('capture_branded_splash("10-splash-dark.png", "dark")') < screenshot.index('tap("Svijetla tema")') < screenshot.index('capture_branded_splash("00-splash-light.png", "light")')
print("PASS: dark is the fresh-install default, light/system choices preserved, both real splashes tested")
