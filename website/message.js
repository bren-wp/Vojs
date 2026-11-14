"use strict";
(async () => {
  const id = new URLSearchParams(location.search).get("id") || "";
  const state = document.getElementById("message-status");
  const player = document.getElementById("shared-audio");
  if (!/^[a-f0-9]{48}$/.test(id)) {
    state.textContent = "Poveznica nije valjana. Zatraži novu od pošiljatelja."; return;
  }
  const audioUrl = "../api/audio.php?id=" + encodeURIComponent(id);
  try {
    const response = await fetch(audioUrl, { method: "HEAD", cache: "no-store", credentials: "omit" });
    if (!response.ok || !/^audio\/(mp4|webm|ogg)$/.test(response.headers.get("content-type") || "")) throw Error("expired");
    player.src = audioUrl; player.hidden = false;
    state.textContent = "Poruka je dostupna. Pokreni reprodukciju.";
    player.addEventListener("error", () => { state.textContent = "Snimka više nije dostupna."; });
  } catch { state.textContent = "Snimka ne postoji ili je istekla. Otvaranje poveznica zahtijeva internet."; }
})();
