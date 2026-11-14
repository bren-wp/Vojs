#!/usr/bin/env node
// Capture real Chromium pixels; never claim 1:1 without a matching reference.
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { chromium } from "playwright";

const out = path.resolve(process.env.VOJS_WEB_SCREENSHOT_DIR || "artifacts/web-visual");
const origin = new URL(process.env.VOJS_VISUAL_BASE_URL || "http://127.0.0.1:8765");
if (!["http:", "https:"].includes(origin.protocol)) throw Error("Invalid origin");
fs.mkdirSync(out,{recursive:true});
const pages = [
  {slug:"naslovnica",url:"/"},
  ...fs.readdirSync("website",{withFileTypes:true})
    .filter(e=>e.isDirectory() && fs.existsSync(path.join("website",e.name,"index.html")))
    .map(e=>({slug:e.name,url:"/"+e.name+"/"}))
].sort((a,b)=>a.slug.localeCompare(b.slug,"hr"));
if (pages.length !== 14) throw Error("Expected 14 routes; got "+pages.length);
const viewports = [
  {name:"small-phone",width:320,height:720},
  {name:"phone",width:390,height:844},
  {name:"tablet",width:820,height:1180},
  {name:"desktop",width:1440,height:900}
];
const themes = ["dark","light"];
const report = {
  captured_at_utc:new Date().toISOString(),origin:origin.origin,
  source:"Real headless Chromium screenshot, not generated design",
  pixel_perfect_verified:false,
  caveat:"No exact source image for every route/viewport/state has been supplied",
  viewports,themes,expected_routes:pages.length,screens:[]
};
const browser = await chromium.launch({headless:true,args:["--no-sandbox"]});
try {
  report.browser_version=browser.version();
  for (const device of viewports) for (const theme of themes) {
    const context=await browser.newContext({
      viewport:{width:device.width,height:device.height},
      deviceScaleFactor:1,locale:"hr-HR",reducedMotion:"reduce",
      colorScheme:theme,permissions:[]
    });
    await context.addInitScript(t=>{
      try {
        if(t==="light") localStorage.setItem("vojs-theme","light");
        else localStorage.removeItem("vojs-theme");
      } catch {}
    },theme);
    try {
      for (const route of pages) {
        const page=await context.newPage();
        const errors=[];
        page.on("pageerror",e=>errors.push(e.message));
        try {
          const response=await page.goto(new URL(route.url,origin).toString(),{
            waitUntil:"domcontentloaded",timeout:30000
          });
          if(!response || response.status()>=400)throw Error("HTTP "+response?.status());
          await page.locator("#vojs-theme-toggle").waitFor({timeout:10000});
          const measured=await page.evaluate(()=>{
            const html=document.documentElement;
            const button=document.getElementById("vojs-theme-toggle");
            return {
              theme:html.dataset.theme,
              background:getComputedStyle(document.body).backgroundColor,
              document_width:html.scrollWidth,
              document_height:html.scrollHeight,
              toggle_height:Math.round(button.getBoundingClientRect().height),
              missing_images:[...document.images].filter(i=>!i.complete||i.naturalWidth===0).length
            };
          });
          const expectedColor=theme==="dark"?"rgb(23, 24, 43)":"rgb(248, 247, 255)";
          if(measured.theme!==theme) throw Error("Wrong theme "+measured.theme);
          if(measured.background!==expectedColor) throw Error("Wrong background "+measured.background);
          if(measured.document_width>device.width+2)
            throw Error("Horizontal overflow "+measured.document_width+" vs "+device.width);
          if(measured.toggle_height<44)throw Error("Theme button shorter than 44px");
          if(measured.missing_images)throw Error("Missing site images "+measured.missing_images);
          if(errors.length)throw Error("Uncaught page errors "+errors.join(" | "));
          const relative=path.join(device.name,theme,route.slug+".png");
          const file=path.join(out,relative);
          fs.mkdirSync(path.dirname(file),{recursive:true});
          const bytes=await page.screenshot({path:file,fullPage:true,
            animations:"disabled",caret:"hide"});
          report.screens.push({
            route:route.slug,viewport:device.name,theme,
            width:device.width,height:device.height,
            png:relative.replaceAll(path.sep,"/"),size_bytes:bytes.length,
            sha256:crypto.createHash("sha256").update(bytes).digest("hex"),
            layout:measured,
            comparison:"not yet matched to exact design reference"
          });
          console.log("PASS",device.name,theme,route.slug);
        } finally {
          await page.close();
        }
      }
    } finally {
      await context.close();
    }
  }
} finally {
  await browser.close();
  fs.writeFileSync(path.join(out,"manifest.json"),JSON.stringify(report,null,2)+"\n");
}
if(report.screens.length !== pages.length*viewports.length*themes.length)
  throw Error("Incomplete visual capture matrix");
console.log("PASS complete visual matrix:",report.screens.length,"real screenshots");
