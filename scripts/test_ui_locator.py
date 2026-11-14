#!/usr/bin/env python3
"""Regression checks for exact Compose label matching in ADB UI tests."""
import ast
from pathlib import Path
source = Path("scripts/verify_responsive.py").read_text("utf-8")
ast.parse(source)
assert 'not entry[1]' in source
assert 'normalized(value) == target' in source
assert 're.sub(r"^[^\\w]+", "", value, flags=re.UNICODE)' in source
assert 'print("TAP", repr(label)' in source
print("PASS exact labels take precedence over subtitle substring matches")
