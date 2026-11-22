#!/usr/bin/env python3
"""Exercise the real GitHub release shell script without publishing a release.

A mocked gh binary records the exact release arguments. Both signed and
unsigned paths must require explicit QA approval and real artifacts.
"""
import hashlib
import os
import pathlib
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parent.parent
script = root / "scripts/publish_verified_release.sh"
shell = script.read_text("utf-8")
assert "VOJS_QA_GATE_PASSED" in shell
assert "gh release create" in shell and "SHA256SUMS" in shell
assert "FLAGS=(--prerelease)" in shell

def check(signed: bool, approved: bool, artifact_missing: bool = False):
    with tempfile.TemporaryDirectory(prefix="vojs-release-check-") as td:
        work = pathlib.Path(td)
        relative = [
            "app/build/outputs/apk/debug/app-debug.apk",
            "app/build/outputs/bundle/release/app-release.aab",
            ("app/build/outputs/apk/release/app-release.apk" if signed
             else "app/build/outputs/apk/release/app-release-unsigned.apk"),
        ]
        for index, name in enumerate(relative):
            f = work / name
            f.parent.mkdir(parents=True, exist_ok=True)
            if not (artifact_missing and index == 1):
                f.write_bytes(b"TEST FIXTURE -- NOT AN ANDROID PACKAGE\n" + bytes([index]))
        gh_mock = work / "gh"
        gh_mock.write_text('#!/bin/sh\nprintf "%s\\n" "$@" > "$MOCK_GH_ARGS"\n', "utf-8")
        gh_mock.chmod(0o755)
        log = work / "gh-args"
        env = os.environ.copy()
        env.update({
            "PATH": str(work) + os.pathsep + env.get("PATH", ""),
            "MOCK_GH_ARGS": str(log),
            "VOJS_VERSION": "1.0.999",
            "VOJS_RELEASE_TARGET": "a" * 40,
            "VOJS_SIGNING_READY": "true" if signed else "false",
            "VOJS_QA_GATE_PASSED": "true" if approved else "false",
            "GITHUB_STEP_SUMMARY": str(work / "summary.md"),
        })
        result = subprocess.run(
            ["bash", str(script)], cwd=work, env=env,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
            check=False,
        )
        allowed = approved and not artifact_missing
        assert (result.returncode == 0) == allowed, result.stdout
        assert log.exists() == allowed, "gh release was invoked without all gates"
        if not allowed:
            return
        args = log.read_text("utf-8").splitlines()
        tag = "v1.0.999" if signed else "v1.0.999-test"
        assert args[:3] == ["release", "create", tag], args
        assert ("--prerelease" in args) == (not signed), args
        assert args[args.index("--target") + 1] == "a" * 40
        assert "Play" in (work / "dist/RELEASE-NOTES.md").read_text()
        files = [
            work / "dist" / f"VOJS-{tag}-debug.apk",
            work / "dist" / f"VOJS-{tag}-{'release' if signed else 'unsigned'}.apk",
            work / "dist" / f"VOJS-{tag}-{'release' if signed else 'unsigned'}.aab",
        ]
        checksums = (work / "dist/SHA256SUMS").read_text().splitlines()
        assert len(checksums) == 3
        for f, line in zip(files, checksums):
            assert f.exists() and line == f"{hashlib.sha256(f.read_bytes()).hexdigest()}  {f.name}"
        if not signed:
            notes = (work / "dist/RELEASE-NOTES.md").read_text()
            assert "UNSIGNED" in notes and "NOT FOR GOOGLE PLAY" in notes
        print("PASS", "signed production" if signed else "unsigned test prerelease",
              "requires QA and labels assets correctly")

for signed in (False, True):
    check(signed, True)
    check(signed, False)
    check(signed, True, artifact_missing=True)
print("PASS: no release without completed QA or required APK/AAB artifacts")
