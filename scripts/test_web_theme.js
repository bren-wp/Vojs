"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const src = fs.readFileSync("website/theme.js", "utf8");
const css = fs.readFileSync("website/style.css", "utf8");
const sw = fs.readFileSync("website/sw.js", "utf8");
const manifest = JSON.parse(fs.readFileSync("website/manifest.webmanifest", "utf8"));
assert(css.includes("--vojs-bg: #17182b"));
assert(css.includes('html[data-theme="light"]'));
assert(sw.includes('"/theme.js"'), "Theme script must work offline");
assert.equal(manifest.background_color, "#17182b");
const pages = fs.readdirSync("website", { withFileTypes: true })
  .filter(e => e.isDirectory()).map(e => "website/"+e.name+"/index.html")
  .filter(file => fs.existsSync(file));
pages.push("website/index.html");
for (const file of pages)
  assert(fs.readFileSync(file,"utf8").includes("theme.js"), "Missing theme on "+file);
function run(initial) {
  const state = {...initial};
  const buttons = [];
  const storage = {getItem: k => state[k] || null,
    setItem: (k,v) => {state[k]=v;}};
  const root = { dataset: {}, style: {} };
  const nav = { appendChild: button => { buttons.push(button); } };
  const document = {
    documentElement: root, readyState: "complete",
    querySelector: () => nav, getElementById: () => null,
    createElement: () => {
      const handlers = {};
      return {
        textContent:"", attributes:{},
        setAttribute(k,v){this.attributes[k]=v;},
        addEventListener(k,callback){handlers[k]=callback;},
        click(){handlers.click();}
      };
    }
  };
  vm.runInNewContext(src,{ document,localStorage:storage });
  assert.equal(buttons.length,1);
  return {root,button:buttons[0],storage:state};
}
const fresh = run({});
assert.equal(fresh.root.dataset.theme,"dark");
assert.equal(fresh.button.attributes["aria-pressed"],"false");
fresh.button.click();
assert.equal(fresh.root.dataset.theme,"light");
assert.equal(fresh.storage["vojs-theme"],"light");
const saved = run(fresh.storage);
assert.equal(saved.root.dataset.theme,"light");
saved.button.click();
assert.equal(saved.root.dataset.theme,"dark");
assert.equal(saved.storage["vojs-theme"],"dark");
assert.equal(run({"vojs-theme":"system"}).root.dataset.theme,"dark");
console.log("PASS dark default, explicit light preference, all web pages and offline");
