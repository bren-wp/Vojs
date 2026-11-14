"use strict";
/** Guards the visual UI contract without claiming pixel equality. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const read = p => fs.readFileSync(p,"utf8");
const screen = read("app/src/main/java/com/brendigo/vojs/VoiceScreen.kt");
const header = read("app/src/main/java/com/brendigo/vojs/VojsHeader.kt");
const wave = read("app/src/main/java/com/brendigo/vojs/VoiceWaveform.kt");
const sender = read("app/src/main/assets/p2p-android-sender.html");
const capture = read("scripts/capture_screenshots.py");
const audit = read("scripts/audit_all_screens.py");

assert(screen.includes("VojsHeader(dark = dark, onSettings = onSettings)"));
assert(header.includes("R.drawable.vojs_logo_dark"));
assert(header.includes("R.drawable.vojs_logo_light"));
assert(header.includes("Modifier.size(48.dp)"), "Native settings target must be >=48dp");
assert(!screen.includes('Text("⚙"'),"Emoji gear varies by vendor and breaks pixel matching");
assert(wave.includes("VOJS_REFERENCE_BARS") && wave.includes(".963f"));
assert(screen.includes("0.84f") && screen.includes("0.92f"),"Waveform reference geometry lost");
assert(capture.includes('screenshot("11-p2p-sender.png"'),"Real P2P capture missing");
assert(capture.includes('wait_ui("Poruka je spremna", 15)'),"Back navigation must preserve ready audio");
assert(audit.includes('"11-p2p-sender.png"'),"Full visual audit must include P2P");
assert(sender.includes('id="create"') && sender.includes('id="end"'));
assert(sender.includes('id="link"') && sender.includes('id="result" hidden'));
assert(sender.includes('id="accept"'),"User consent must be required");
assert(sender.includes('role="status"') && sender.includes('aria-live="polite"'));
assert(sender.includes('aria-label="Uvjeti prijenosa"'));
assert(sender.includes("15 min"),"Match real 15-minute signaling expiry");
assert(sender.includes("Bez pohrane zvuka"));
assert(sender.includes("@media(max-width:340px)"),"Compact Android WebView requires adaptation");
assert(sender.includes("@media(max-height:700px)"),"Short display requires adaptation");
assert(!sender.includes("1 dan") && !sender.includes("Nikada"),
  "Long-lived hosted links cannot be promised without audio storage");
const inline = [...sender.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)]
  .map(match => match[1].trim()).filter(Boolean);
assert.equal(inline.length,1,"Expected exactly one local inline controller");
new Function(inline[0]);  // Syntax validation without executing browser code.
assert(inline[0].includes("session.expires * 1000"));
assert(inline[0].includes("const totalSeconds = Math.ceil(remaining / 1000)"));
assert(inline[0].includes("if (!canCreate()) return"));
console.log("PASS visual UI contracts, accessible P2P, genuine screenshot coverage and JS syntax");
