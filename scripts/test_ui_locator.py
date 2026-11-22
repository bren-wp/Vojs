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

# Real controls can be present but clipped to zero bounds on compact phones.
from unittest.mock import patch as _patch
_scroll_actions = []
_scrolled = {"value": False}
def _settings_nodes():
    light_bounds = "[20,300][180,360]" if _scrolled["value"] else "[0,0][0,0]"
    return [
        {"text": "Svijetla tema", "content-desc": "", "bounds": light_bounds,
         "clickable": "false"},
        {"text": "", "content-desc": "", "class": "android.widget.ScrollView",
         "scrollable": "true", "bounds": "[0,100][400,850]"},
    ]
def _settings_shell(*args):
    _scroll_actions.append(args)
    if args[:2] == ("input", "swipe"):
        _scrolled["value"] = True
    return ""
module.nodes = _settings_nodes
module.shell = _settings_shell
with _patch.object(module.time, "sleep", return_value=None):
    assert module.scroll_to_visible("Svijetla tema")["text"] == "Svijetla tema"
assert any(action[:2] == ("input", "swipe") for action in _scroll_actions)
print("PASS offscreen settings row revealed through actual ScrollView swipe")

_anr_nodes = [
    {"text": "Quickstep isn't responding", "content-desc": "",
     "bounds": "[0,0][300,100]"},
    {"text": "Close app", "content-desc": "", "bounds": "[30,100][300,180]"},
]
_scroll_actions.clear()
with _patch.object(module.time, "sleep", return_value=None):
    assert module.recover_launcher_anr(_anr_nodes) is True
    assert module.recover_launcher_anr([
        {"text": "VOJS isn't responding", "content-desc": "",
         "bounds": "[0,0][300,100]"}
    ]) is False, "Application ANRs must not be hidden"
assert ("am", "start", "-n", module.ACTIVITY) in _scroll_actions
print("PASS launcher-only recovery; VOJS failures remain visible")

# API 29 may consume Back without dismissing the Compose sheet.
# The test must use its real accessibility scrim, not skip the assertion.
def _simulate_settings_close(back_closes):
    actions = []
    state = {"open": True}
    def snapshot():
        if not state["open"]:
            return [{"text": "Snimi novu poruku", "content-desc": "",
                     "bounds": "[20,350][240,440]"}]
        return [
            {"text": "Postavke", "content-desc": "", "bounds": "[20,250][220,320]"},
            {"text": "", "content-desc": "Close sheet", "clickable": "true",
             "bounds": "[0,0][400,220]"},
        ]
    def fake_shell(*args):
        actions.append(args)
        if args[:3] == ("input", "keyevent", "4") and back_closes:
            state["open"] = False
        if args[:2] == ("input", "tap"):
            state["open"] = False
        return ""
    module.nodes = snapshot
    module.shell = fake_shell
    with _patch.object(module.time, "sleep", return_value=None):
        module.dismiss_settings()
    assert state["open"] is False
    assert (("input", "tap") in [action[:2] for action in actions]) is (not back_closes)
_simulate_settings_close(False)
_simulate_settings_close(True)
print("PASS native Back or accessibility scrim dismisses settings without losing state")

# High-resolution Android emulator may kill uiautomator with exit code 137.
# Previous tests patch module.nodes to use synthetic UI trees. Load the original
# module again so we test its real parser and bounded error-handling code.
_real_spec = importlib.util.spec_from_file_location(
    "responsive_retry", Path("scripts/verify_responsive.py")
)
_real_module = importlib.util.module_from_spec(_real_spec)
_real_spec.loader.exec_module(_real_module)
_retry_attempts = {"count": 0}
def _flaky_dump(*args, **kwargs):
    _retry_attempts["count"] += 1
    if _retry_attempts["count"] == 1:
        raise _real_module.subprocess.CalledProcessError(137, ["adb", *args])
    return _Namespace(stdout=b"")
_xml_nodes = '<hierarchy><node text="Natrag" bounds="[20,30][200,70]"/></hierarchy>'
with _patch.object(_real_module, "adb", side_effect=_flaky_dump), \
     _patch.object(_real_module, "shell", return_value=_xml_nodes), \
     _patch.object(_real_module.time, "sleep", return_value=None):
    _recovered = _real_module.nodes()
assert _retry_attempts["count"] == 2
assert _recovered[0]["text"] == "Natrag", "Must return the real parsed UI"

def _permanent_dump_error(*args, **kwargs):
    raise _real_module.subprocess.CalledProcessError(137, ["adb", *args])
with _patch.object(_real_module, "adb", side_effect=_permanent_dump_error), \
     _patch.object(_real_module.time, "sleep", return_value=None):
    try:
        _real_module.nodes()
        raise AssertionError("Permanent uiautomator failure must fail the test")
    except _real_module.subprocess.CalledProcessError as err:
        assert err.returncode == 137
print("PASS emulator UI dump recovery, bounded retries and fail-closed behavior")

# Android 16: the emulator OS may ANR before VOJS is ever visible.
# Only a designated fresh-install CI startup may dismiss the exact
# "System UI isn't responding" dialog. Never dismiss a VOJS app ANR.
_os_anr = [
    {"text": "System UI isn't responding", "content-desc": "",
     "bounds": "[10,10][300,80]"},
    {"text": "Close app", "content-desc": "",
     "bounds": "[20,100][280,170]"}
]
_os_actions = []
module.shell = lambda *args: (_os_actions.append(args), "")[1]
with _patch.object(module.time, "sleep", return_value=None):
    assert module.recover_launcher_anr(_os_anr) is False, (
        "System UI recovery must be opt-in for fresh CI emulators")
    assert not _os_actions, "No OS process can be dismissed by default"
    assert module.recover_launcher_anr(_os_anr, allow_system_ui=True) is True
    assert ("am", "start", "-n", module.ACTIVITY) in _os_actions
    assert module.recover_launcher_anr([
        {"text": "VOJS isn't responding", "content-desc": "",
         "bounds": "[10,10][300,80]"},
        _os_anr[1]
    ], allow_system_ui=True) is False, "Never suppress VOJS ANRs"
print("PASS Android 16 System UI recovery is CI-only and never hides VOJS failures")
