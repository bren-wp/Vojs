"use strict";
(() => {
  const KEY = "vojs-theme";
  const root = document.documentElement;
  const read = () => {
    try { return localStorage.getItem(KEY) === "light" ? "light" : "dark"; }
    catch { return "dark"; }
  };
  const apply = value => {
    const mode = value === "light" ? "light" : "dark";
    root.dataset.theme = mode;
    root.style.colorScheme = mode;
    return mode;
  };
  let current = apply(read());
  function mount() {
    const nav = document.querySelector(".nav nav");
    if (!nav || document.getElementById("vojs-theme-toggle")) return;
    const button = document.createElement("button");
    button.id = "vojs-theme-toggle";
    button.type = "button";
    button.className = "vojs-theme-toggle";
    button.setAttribute("aria-label", "Promijeni temu stranice");
    const update = () => {
      button.textContent = current === "dark" ? "☀ Svijetla tema" : "☾ Tamna tema";
      button.setAttribute("aria-pressed", current === "light" ? "true" : "false");
    };
    update();
    button.addEventListener("click", () => {
      current = apply(current === "dark" ? "light" : "dark");
      try { localStorage.setItem(KEY, current); } catch { /* Optional */ }
      update();
    });
    nav.appendChild(button);
  }
  if (document.readyState === "loading")
    document.addEventListener("DOMContentLoaded", mount, { once: true });
  else mount();
})();
