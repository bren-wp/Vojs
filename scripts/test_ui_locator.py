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

# Screenshot capture has its own locator; execute just that function from AST.
import ast as _ast
import re as _re
import xml.etree.ElementTree as _ET
from types import SimpleNamespace as _Namespace
_capture = Path("scripts/capture_screenshots.py").read_text("utf-8")
_fn = next(n for n in _ast.parse(_capture).body
           if isinstance(n, _ast.FunctionDef) and n.name == "bounds_of")
_fixture = """<hierarchy>
  <node text="Zadano: tamna tema. Tvoj izbor ostaje spremljen." clickable="false" bounds="[10,10][100,30]"/>
  <node text="Tamna tema (zadana)" clickable="false" bounds="[20,60][240,100]"/>
  <node text="Preslušaj pa podijeli" clickable="false" bounds="[10,130][240,160]"/>
  <node text="▶  Preslušaj" clickable="false" bounds="[20,170][240,220]"/>
</hierarchy>"""

def _fake_adb(*args, **kwargs):
    return _Namespace(stdout=_fixture.encode("utf-8") if args[1] == "cat" else b"")
_globals = {"adb": _fake_adb, "ET": _ET, "re": _re}
exec(compile(_ast.fix_missing_locations(_ast.Module(body=[_fn], type_ignores=[])),
             "capture_locator", "exec"), _globals)
assert _globals["bounds_of"]("Tamna tema") == (130,80), "Must pick radio option"
assert _globals["bounds_of"]("Preslušaj") == (130,195), "Must pick playback button"
print("PASS screenshot-capture locator ignores informational text")
