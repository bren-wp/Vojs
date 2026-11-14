#!/usr/bin/env python3
"""Check real dark-first behavior and preserve existing explicit theme choice."""
from pathlib import Path
source = Path("app/src/main/java/com/brendigo/vojs/MainActivity.kt").read_text("utf-8")
theme = Path("app/src/main/java/com/brendigo/vojs/VojsAppearance.kt").read_text("utf-8")
palette = Path("app/src/main/res/values/colors.xml").read_text("utf-8")
styles = Path("app/src/main/res/values/styles.xml").read_text("utf-8")
capture = Path("scripts/capture_screenshots.py").read_text("utf-8")
assert 'const val DEFAULT = DARK' in theme, "Fresh install must default to dark"
assert 'getString(KEY, DEFAULT)' in theme, "Stored choice must be read"
assert 'it == DARK || it == LIGHT || it == SYSTEM' in theme
assert 'putString(KEY, appearance).apply()' in theme, "Explicit choices must persist"
assert 'LIGHT -> false' in theme and 'SYSTEM -> systemDark' in theme
assert 'else -> true' in theme, "Unknown preference must safely fall back to dark"
assert 'VojsAppearance.load(this@MainActivity)' in source
assert 'VojsAppearance.isDark(appearance, isSystemInDarkTheme())' in source
assert source.count('VojsAppearance.save(this@MainActivity, value)') == 1
assert 'VojsSettingsSheet(' in source, 'Native settings must be extracted into own file'
assert 'heightIn(max = maxHeight)' in Path("app/src/main/java/com/brendigo/vojs/VojsSettingsSheet.kt").read_text("utf-8"), "Settings must be scrollable on compact screens"
assert '<color name="vojs_splash_background">#20213B</color>' in palette
assert 'android:style/Theme.Material.NoActionBar' in styles, "Platform default must be dark"
assert capture.index('capture_branded_splash("10-splash-dark.png", "dark")') < capture.index('tap("Svijetla tema")') < capture.index('capture_branded_splash("00-splash-light.png", "light")')
print("PASS dark-first native theme, stored choices, dark platform, and exact splash reference checks")
