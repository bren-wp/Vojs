#!/usr/bin/env python3
"""Regression guard for Android launcher ANRs and genuine splash captures."""
from pathlib import Path
source = Path("scripts/capture_screenshots.py").read_text("utf-8")
for expected in (
    '("Quickstep", "Pixel Launcher")',
    'am", "force-stop", launcher',
    'capture_branded_splash("00-splash-light.png", "light")',
    'capture_branded_splash("10-splash-dark.png", "dark")',
    'screenshot("00-splash-light.png"',
    'screenshot("10-splash-dark.png"',
):
    if expected not in source:
        raise SystemExit(f"Missing safeguard: {expected}")
assert "VOJS isn't responding" not in source
print("PASS: launcher recovery and genuine splash capture checks")
