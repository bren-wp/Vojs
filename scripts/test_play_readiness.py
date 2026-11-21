#!/usr/bin/env python3
"""Static Google Play release safety gate; not a Play approval certificate."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
app = root / "app"
manifest = ET.parse(app / "src/main/AndroidManifest.xml").getroot()
a = "{http://schemas.android.com/apk/res/android}"
names = [x.attrib[a+"name"] for x in manifest.findall("uses-permission")]
assert names == ["android.permission.RECORD_AUDIO", "android.permission.INTERNET"], \
    f"Unexpected sensitive permission: {names}"
application = manifest.find("application")
assert application is not None
for k, wanted in {
    "allowBackup":"false", "usesCleartextTraffic":"false",
    "hasFragileUserData":"false", "dataExtractionRules":"@xml/data_extraction_rules",
    "fullBackupContent":"@xml/backup_rules"
}.items():
    assert application.get(a+k) == wanted, f"Privacy setting {k} must be {wanted}"
for file, tag in [
    ("data_extraction_rules.xml", "data-extraction-rules"),
    ("backup_rules.xml","full-backup-content")
]:
    parsed=ET.parse(app / "src/main/res/xml" / file).getroot()
    assert parsed.tag==tag
    children=parsed.findall(".//exclude")
    assert children and all(e.get("domain")=="root" and e.get("path")=="." for e in children)
data=ET.parse(app / "src/main/res/xml/data_extraction_rules.xml").getroot()
assert data.find("cloud-backup") is not None
assert data.find("device-transfer") is not None
gradle = (app / "build.gradle.kts").read_text("utf-8")
assert "targetSdk = 36" in gradle and "minSdk = 24" in gradle
assert 'isMinifyEnabled = false' in gradle or 'isMinifyEnabled = true' in gradle
assert "VOJS_KEYSTORE_PATH" in gradle and "VOJS_KEY_ALIAS" in gradle
activity = (app / "src/main/java/com/brendigo/vojs/MainActivity.kt").read_text("utf-8")
assert "https://vojs.com.hr/privatnost/" in activity
assert 'Manifest.permission.RECORD_AUDIO' in activity
recording = (app / "src/main/java/com/brendigo/vojs/VoiceRecorder.kt").read_text("utf-8")
assert "UUID.randomUUID()" in recording
assert "onBackground()" in recording
print("PASS: Play minimum SDK/permissions/privacy/extraction rules/local random recording files")
print("NOTE: human Play Console declarations, upload signing and production URL still unverified")
