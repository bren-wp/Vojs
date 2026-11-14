"use strict";
(async () => {
  const id = new URLSearchParams(location.search).get("id") || "";
  const state = document.getElementById("message-status");
  const player = document.getElementById("shared-audio");
  const download = document.getElementById("received-download");
  let blockedByUser = false;
  const blockButton = document.getElementById("block");
  const report = document.getElementById("report");
  report.href = "../prijava-sadrzaja/?id=" + encodeURIComponent(id);
  blockButton.addEventListener("click", () => {
    blockedByUser = true;
    try {
      const list = JSON.parse(localStorage.getItem("vojs-blocked") || "[]");
      const ids = Array.isArray(list) ? list.filter(value => /^[a-f0-9]{48}$/.test(value)) : [];
      localStorage.setItem("vojs-blocked", JSON.stringify([...new Set([...ids.slice(-99), id])]));
    } catch { /* Private browser mode may deny storage. */ }
    state.textContent = "Ova P2P poveznica je blokirana na ovom uređaju.";
    player.pause();
    player.removeAttribute("src");
    player.hidden = true;
    download.hidden = true;
    download.removeAttribute("href");
    if (url) { URL.revokeObjectURL(url); url = null; }
    if (window.VojsPeer) window.VojsPeer.closeActive();
  });
  try {
    const blocked = JSON.parse(localStorage.getItem("vojs-blocked") || "[]");
    if (Array.isArray(blocked) && blocked.includes(id)) {
      state.textContent = "Ovu poveznicu blokirao si na ovom uređaju.";
      blockButton.disabled = true;
      return;
    }
  } catch { /* Proceed without persistent block in private browsing. */ }
  if (!/^[a-f0-9]{48}$/.test(id)) {
    state.textContent = "Poveznica nije valjana. Zatraži novu od pošiljatelja.";
    return;
  }
  let url = null;
  try {
    await window.VojsPeer.receive(id,
      text => { if (!blockedByUser) state.textContent = text; },
      blob => {
        if (blockedByUser) return;
        if (url) URL.revokeObjectURL(url);
        url = URL.createObjectURL(blob);
        player.src = url;
        player.hidden = false;
        download.href = url;
        download.download = blob.type === "audio/mp4" ? "vojs.m4a" :
          blob.type === "audio/ogg" ? "vojs.ogg" : "vojs.webm";
        download.hidden = false;
      });
  } catch (e) {
    if (!blockedByUser) state.textContent = e.message || "Pošiljatelj nije dostupan. Potreban je istovremeni pristup internetu.";
  }
  window.addEventListener("pagehide", () => { if (url) URL.revokeObjectURL(url); });
})();
