#!/usr/bin/env node
/**
 * End-to-end test: actual Chromium sender -> PHP JSON signaling ->
 * real WebRTC DataChannel -> Chromium receiver, digest checked byte by byte.
 *
 * The audio payload is synthetic test bytes, NOT a real recording.
 * No user data or audio should be stored by PHP.
 */
import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { chromium } from "playwright";

const origin = process.env.VOJS_VISUAL_BASE_URL || "http://127.0.0.1:8765";
const fileSize = 32768;
const expected = Buffer.allocUnsafe(fileSize);
for (let i=0;i<fileSize;i++) expected[i]=(i*17+39)%256;
const expectedHash = crypto.createHash("sha256").update(expected).digest("hex");
const folder=path.resolve(process.env.VOJS_WEB_SCREENSHOT_DIR || "artifacts/web-visual");
fs.mkdirSync(path.join(folder,"functional"),{recursive:true});
const browser=await chromium.launch({headless:true,args:["--no-sandbox"]});
let sessionId=null;
try {
  const context=await browser.newContext({
    viewport:{width:390,height:844},locale:"hr-HR",deviceScaleFactor:1
  });
  const sender=await context.newPage();
  const receiver=await context.newPage();
  sender.setDefaultTimeout(70000);
  receiver.setDefaultTimeout(70000);
  const senderErrors=[], receiverErrors=[];
  sender.on("pageerror",err=>senderErrors.push(err.message));
  receiver.on("pageerror",err=>receiverErrors.push(err.message));
  try {
    const rsp=await sender.goto(origin+"/snimi/",{waitUntil:"domcontentloaded"});
    assert.equal(rsp?.status(),200);
    const created=await sender.evaluate(async size=>{
      const payload=new Uint8Array(size);
      for(let i=0;i<size;i++)payload[i]=(i*17+39)%256;
      const file=new File([payload],"vojs-integration.webm",{type:"audio/webm"});
      window.__vojsSenderStatuses=[];
      window.__vojsSession=await window.VojsPeer.create(file,
        msg=>window.__vojsSenderStatuses.push(msg));
      return {
        id:window.__vojsSession.id,
        url:window.__vojsSession.url,
        expires:window.__vojsSession.expires
      };
    },fileSize);
    sessionId=created.id;
    assert.match(sessionId,/^[a-f0-9]{48}$/);
    assert.equal(created.url,"https://vojs.com.hr/poruka/?id="+sessionId);
    assert(Number.isInteger(created.expires));
    const response=await receiver.goto(origin+"/poruka/?id="+sessionId,{
      waitUntil:"domcontentloaded"
    });
    assert.equal(response?.status(),200);
    await receiver.waitForFunction(()=>{
      const audio=document.getElementById("shared-audio");
      return audio && !audio.hidden && audio.src.startsWith("blob:");
    },{timeout:70000});
    const transmitted=await receiver.evaluate(async()=>{
      const audio=document.getElementById("shared-audio");
      const response=await fetch(audio.src);
      const bytes=new Uint8Array(await response.arrayBuffer());
      const digest=await crypto.subtle.digest("SHA-256",bytes);
      const sha256=Array.from(new Uint8Array(digest),b=>b.toString(16).padStart(2,"0")).join("");
      return {
        size:bytes.length,sha256,
        status:document.getElementById("message-status").textContent,
        downloadShown:!document.getElementById("received-download").hidden
      };
    });
    assert.equal(transmitted.size,fileSize);
    assert.equal(transmitted.sha256,expectedHash);
    assert(transmitted.downloadShown);
    await receiver.screenshot({
      path:path.join(folder,"functional","p2p-received-dark.png"),
      fullPage:true,animations:"disabled"
    });
    // Receiver-side block must clear audio immediately and persist locally.
    await receiver.locator("#block").click();
    assert.equal(await receiver.locator("#shared-audio").isHidden(),true);
    assert.equal(await receiver.locator("#received-download").isHidden(),true);
    await receiver.reload({waitUntil:"domcontentloaded"});
    assert.match(await receiver.locator("#message-status").textContent(),/blokirao/);
    assert.equal(await receiver.locator("#shared-audio").isHidden(),true);
    // Sender-side closure revokes server room, and audio never entered PHP.
    await sender.evaluate(async()=>window.__vojsSession.close());
    const closeStatus=await sender.evaluate(async id=>
      (await fetch("/api/signal.php?id="+id+"&view=offer",{cache:"no-store"})).status,sessionId);
    assert.equal(closeStatus,404);
    assert.equal(senderErrors.length,0,"Sender runtime errors: "+senderErrors.join("|"));
    assert.equal(receiverErrors.length,0,"Receiver runtime errors: "+receiverErrors.join("|"));
    console.log("PASS real WebRTC transport",fileSize,"bytes, SHA256",expectedHash);
    console.log("PASS receive UI, persistent local block, revocation and no browser exceptions");
  } finally {
    await sender.close();await receiver.close();await context.close();
  }
} finally {
  await browser.close();
}
if(!sessionId)throw Error("No successful P2P session created");
