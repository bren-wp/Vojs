#!/usr/bin/env node
/**
 * Real end-to-end smoke on localhost:
 * 1. Browser microphone (Chromium fake test device) records actual encoded audio.
 * 2. Web UI obtains a room from PHP, creates WebRTC sender DataChannel.
 * 3. Another real Chromium page receives the audio, SHA-256 byte-for-byte.
 * 4. Receiver blocks locally; sender revokes; PHP stores no audio.
 *
 * Fake microphone produces test audio only, not user recordings.
 */
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { chromium } from "playwright";

const origin=process.env.VOJS_VISUAL_BASE_URL || "http://127.0.0.1:8765";
const output=path.resolve(process.env.VOJS_WEB_SCREENSHOT_DIR || "artifacts/web-visual");
fs.mkdirSync(path.join(output,"functional"),{recursive:true});
const browser=await chromium.launch({
  headless:true,
  args:["--no-sandbox","--use-fake-ui-for-media-stream","--use-fake-device-for-media-stream"]
});
let sessionId=null;
try {
  const context=await browser.newContext({
    viewport:{width:390,height:844},locale:"hr-HR",deviceScaleFactor:1,
    permissions:["microphone"], colorScheme:"dark"
  });
  const sender=await context.newPage();
  const receiver=await context.newPage();
  sender.setDefaultTimeout(70000);
  receiver.setDefaultTimeout(70000);
  const senderErrors=[],receiverErrors=[];
  sender.on("pageerror",e=>senderErrors.push(e.message));
  receiver.on("pageerror",e=>receiverErrors.push(e.message));
  try {
    const response=await sender.goto(origin+"/snimi/",{waitUntil:"domcontentloaded"});
    assert.equal(response?.status(),200);
    await sender.locator("#start").click();
    await sender.locator("#stop").waitFor({state:"visible"});
    await sender.waitForFunction(()=>!document.getElementById("stop").disabled);
    // Verify pause and resume states of the same actual recording.
    await sender.locator("#pause").click();
    assert.match(await sender.locator("#pause").textContent(),/Nastavi/);
    await sender.locator("#pause").click();
    assert.match(await sender.locator("#pause").textContent(),/Pauziraj/);
    await sender.waitForTimeout(1400);
    await sender.locator("#stop").click();
    await sender.locator("#finished").waitFor({state:"visible",timeout:20000});
    const original=await sender.evaluate(async()=>{
      const audio=document.getElementById("preview");
      const array=await (await fetch(audio.src)).arrayBuffer();
      const hash=await crypto.subtle.digest("SHA-256",array);
      return {
        length:array.byteLength,
        sha256:Array.from(new Uint8Array(hash),b=>b.toString(16).padStart(2,"0")).join(""),
        mime:(await fetch(audio.src)).headers.get("content-type")
      };
    });
    assert(original.length>64 && original.length<=8*1024*1024);
    assert.match(original.mime,/^audio\/(mp4|webm|ogg)$/);

    // Regression: replacing a recorded message must be transactional.
    // Reject microphone access and verify that the existing blob and all
    // offline save controls remain available, with identical audio bytes.
    const beforeDeniedStart=await sender.locator("#preview").getAttribute("src");
    await sender.evaluate(()=>{
      Object.defineProperty(navigator.mediaDevices,"getUserMedia",{
        configurable:true,
        value:()=>Promise.reject(new DOMException("Permission denied by E2E test","NotAllowedError"))
      });
    });
    await sender.locator("#start").click();
    await sender.waitForFunction(()=>!document.getElementById("start").disabled);
    assert.equal(await sender.locator("#finished").isVisible(),true,
      "Microphone denial must preserve the ready recording");
    assert.equal(await sender.locator("#preview").getAttribute("src"),beforeDeniedStart,
      "A failed replacement must not revoke the original audio");
    const afterDeniedStart=await sender.evaluate(async()=>{
      const bytes=await (await fetch(document.getElementById("preview").src)).arrayBuffer();
      const digest=await crypto.subtle.digest("SHA-256",bytes);
      return { length:bytes.byteLength,
        sha256:Array.from(new Uint8Array(digest),b=>b.toString(16).padStart(2,"0")).join("") };
    });
    assert.equal(afterDeniedStart.length,original.length);
    assert.equal(afterDeniedStart.sha256,original.sha256,
      "Microphone denial changed the previous recording");
    assert.equal(await sender.locator("#download").isDisabled(),false);
    assert.match(await sender.locator("#error").textContent(),/mikrofon/i);
    await sender.evaluate(()=>{delete navigator.mediaDevices.getUserMedia;});
    console.log("PASS denied replacement leaves previous recorded audio intact");

    await sender.locator("#accept-terms").check();
    await sender.locator("#upload").click();
    await sender.locator("#link-result").waitFor({state:"visible",timeout:60000});
    const url=await sender.locator("#link").inputValue();
    assert.match(url,/^https:\/\/vojs\.com\.hr\/poruka\/\?id=[a-f0-9]{48}$/);
    sessionId=new URL(url).searchParams.get("id");
    assert.match(await sender.locator("#link-expiry").textContent(),/Preostalo vrijeme: \d\d:\d\d/);
    // The web sender UI and Android share UI use the same P2P protocol.
    const recv=await receiver.goto(origin+"/poruka/?id="+sessionId,{
      waitUntil:"domcontentloaded"
    });
    assert.equal(recv?.status(),200);
    await receiver.waitForFunction(()=>{
      const audio=document.getElementById("shared-audio");
      return audio && !audio.hidden && audio.src.startsWith("blob:");
    },null,{timeout:70000});
    const result=await receiver.evaluate(async()=>{
      const audio=document.getElementById("shared-audio");
      const response=await fetch(audio.src);
      const bytes=await response.arrayBuffer();
      const sha=await crypto.subtle.digest("SHA-256",bytes);
      return {
        length:bytes.byteLength,
        sha256:Array.from(new Uint8Array(sha),b=>b.toString(16).padStart(2,"0")).join(""),
        mime:response.headers.get("content-type"),
        downloadShown:!document.getElementById("received-download").hidden
      };
    });
    assert.equal(result.length,original.length,"Transfer byte count differs");
    assert.equal(result.sha256,original.sha256,"Real encoded recording SHA mismatch");
    assert.equal(result.mime,original.mime,"Sender/receiver MIME mismatch");
    assert(result.downloadShown);
    await receiver.screenshot({
      path:path.join(output,"functional","p2p-received-dark.png"),
      fullPage:true,animations:"disabled"
    });
    await receiver.locator("#block").click();
    assert.equal(await receiver.locator("#shared-audio").isHidden(),true);
    assert.equal(await receiver.locator("#received-download").isHidden(),true);
    await receiver.reload({waitUntil:"domcontentloaded"});
    assert.match(await receiver.locator("#message-status").textContent(),/blokirao/);
    assert.equal(await receiver.locator("#shared-audio").isHidden(),true);
    await sender.locator("#revoke").click();
    await sender.locator("#link-result").waitFor({state:"hidden"});
    const status=await sender.evaluate(async id=>
      (await fetch("/api/signal.php?id="+id+"&view=offer",{cache:"no-store"})).status,sessionId);
    assert.equal(status,404,"Server room must close on sender revoke");
    assert.deepEqual(senderErrors,[],"Sender JS errors");
    assert.deepEqual(receiverErrors,[],"Receiver JS errors");
    console.log("PASS actual browser microphone recording, pause/resume, WebRTC binary transfer");
    console.log("PASS bytes",result.length,"SHA256",result.sha256,"mime",result.mime);
    console.log("PASS shared Android/Web protocol, blocking, revocation, no JS exceptions");
  } finally {
    await sender.close();await receiver.close();await context.close();
  }
} finally {
  await browser.close();
}
if(!sessionId)throw Error("Could not establish a real P2P session");
