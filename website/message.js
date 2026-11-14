"use strict";
(async () => {
  const id = new URLSearchParams(location.search).get("id") || "";
  const state = document.getElementById("message-status");
  const player = document.getElementById("shared-audio");
  const download = document.getElementById("received-download");
  if (!/^[a-f0-9]{48}$/.test(id)) {
    state.textContent = "Poveznica nije valjana. Zatraži novu od pošiljatelja.";
    return;
  }
  let url = null;
  try {
    await window.VojsPeer.receive(id,
      text => { state.textContent = text; },
      blob => {
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
    state.textContent = e.message || "Pošiljatelj nije dostupan. Potreban je istovremeni pristup internetu.";
  }
  window.addEventListener("pagehide", () => { if (url) URL.revokeObjectURL(url); });
})();
