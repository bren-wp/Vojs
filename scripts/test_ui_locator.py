#!/usr/bin/env python3
"""Test actual label resolution, not merely the selector's source text."""
import importlib.util
from pathlib import Path
spec = importlib.util.spec_from_file_location("responsive", Path("scripts/verify_responsive.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
def node(text, clickable="false", description=""):
    return {"text":text,"content-desc":description,"bounds":"[20,30][200,70]",
            "clickable":clickable}
module.nodes = lambda: [
    node("Preslušaj pa podijeli"),
    node("▶  Preslušaj"),
    node("Podijeli glasovnu poruku", "true"),
    node("", "true", "Postavke aplikacije"),
    node("Ⅱ Pauziraj"),
]
assert module.locate("Preslušaj")["text"] == "▶  Preslušaj", "Must ignore subtitle"
assert module.locate("Započni snimanje") is None, "Unrelated substring not allowed"
assert module.locate("Pauziraj")["text"] == "Ⅱ Pauziraj"
assert module.locate("Postavke aplikacije")["clickable"] == "true"
assert module.locate("Podijeli glasovnu poruku")["clickable"] == "true"
print("PASS exact accessible control matching; subtitle is never mistaken for play")
