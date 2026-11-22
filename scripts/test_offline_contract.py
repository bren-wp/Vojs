#!/usr/bin/env python3
"""Fail when native recording or privacy accidentally becomes web-dependent."""
from pathlib import Path
root=Path("app/src/main/java/com/brendigo/vojs")
def read(name):return (root/name).read_text("utf-8")
main=read("MainActivity.kt")
screen=read("VoiceScreen.kt")
settings=read("VojsSettingsSheet.kt")
privacy=read("VojsPrivacySheet.kt")
recorder=read("VoiceRecorder.kt")
sender=Path("app/src/main/assets/p2p-android-sender.html").read_text("utf-8")
assert "showPrivacy = true" in main and "VojsPrivacySheet(" in main
assert "android.content.Intent.ACTION_VIEW" not in main
assert "Snimanje i reprodukcija" in privacy
assert "TextButton(onClick = onDismiss)" in privacy
assert "weight(1f, fill = false)" in privacy
assert 'tap("Zatvori")' in Path("scripts/test_offline_mode.py").read_text("utf-8")
assert "Snimanje i preslušavanje rade bez interneta" in settings
assert "Stvori poveznicu (internet)" in screen
assert "onClick = recorder::share" in screen
assert "onClick = recorder::togglePlayback" in read("VojsPlaybackButton.kt")
assert "FileProvider.getUriForFile" in recorder
assert "VOJS P2P poveznica" not in read("VoiceLinkService.kt")
assert "contentDescription = \"VOJS\"" in read("VojsBrandedSplash.kt")
assert "MediaRecorder" in recorder and "MediaPlayer" in recorder
# A failed microphone restart cannot delete a previously completed recording.
record_start=recorder.split("fun start(): Boolean {",1)[1].split("\n    fun tick()",1)[0]
assert "discardCurrent()" not in record_start
assert record_start.index("                prepare()") < record_start.index("                start()")
assert record_start.index("                start()") < record_start.index("if (!shared) workingFile?.delete()")
assert record_start.index("if (!shared) workingFile?.delete()") < record_start.index("workingFile = audio")
assert "candidate?.runCatching { release() }" in record_start
assert "workingFile = null" not in record_start
assert "stage = RecordingStage.IDLE" not in record_start
assert "context.startActivity(chooser)" in recorder
assert not any(x in screen+settings for x in ["P2P","WebRTC","JSON","TURN","DEBUG","Lorem ipsum"])
assert not any(x in sender for x in ["PRIVATNA P2P PORUKA","Stvori P2P poveznicu",
    "kratkotrajne JSON podatke","TURN poslužitelja"])
check=Path("scripts/test_offline_mode.py").read_text("utf-8")
assert 'airplane-mode","enable"' in check and 'airplane_mode_on' in check
assert '"run-as",PACKAGE' in check
assert "onBackground()" in main
assert 'assets.open("p2p-android-sender.html")' in read("PeerSenderActivity.kt")
print("PASS offline-first recorder/share, in-app privacy and clean customer UI")
