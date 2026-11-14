"use strict";
(() => {
  const el = id => document.getElementById(id);
  const MAX_MS = 300000, MAX_BYTES = 8 * 1024 * 1024;
  let stream = null, recorder = null, file = null, fileUrl = null, chunks = [], timerHandle = null;
  let startedAt = 0, pausedAt = 0, pausedTotal = 0, link = null, session = null;
  function message(value) { el("state").textContent = value; }
  function error(value) { el("error").textContent = value; el("error").hidden = !value; }
  function stopTracks() {
    if (timerHandle !== null) clearInterval(timerHandle);
    timerHandle = null;
    if (stream) stream.getTracks().forEach(track => track.stop());
    stream = null;
  }
  function resetFile() {
    if (session) { session.close(); session = null; }
    file = null;
    link = null;
    el("finished").hidden = true;
    el("link-result").hidden = true;
    if (fileUrl) URL.revokeObjectURL(fileUrl);
    fileUrl = null;
    el("preview").removeAttribute("src");
    el("upload").disabled = !el("accept-terms").checked;
    el("pause").textContent = "Pauziraj";
  }
  function updateTimer() {
    if (!recorder || recorder.state === "inactive") return;
    const elapsed = Math.max(0, (pausedAt || Date.now()) - startedAt - pausedTotal);
    const seconds = Math.min(300, Math.floor(elapsed / 1000));
    el("timer").textContent = String(Math.floor(seconds / 60)).padStart(2, "0") +
      ":" + String(seconds % 60).padStart(2, "0") + " / 05:00";
    if (elapsed >= MAX_MS) finish();
  }
  function chooseMime() {
    return ["audio/mp4", "audio/webm;codecs=opus", "audio/webm", "audio/ogg;codecs=opus", "audio/ogg"]
      .find(type => MediaRecorder.isTypeSupported(type)) || "";
  }
  async function start() {
    error("");
    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
      error("Snimanje nije podržano. Otvori stranicu preko HTTPS-a u modernom pregledniku."); return;
    }
    if (recorder && recorder.state !== "inactive") return;
    resetFile();
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mime = chooseMime();
      recorder = new MediaRecorder(stream, { ...(mime ? { mimeType: mime } : {}), audioBitsPerSecond: 96000 });
      chunks = [];
      recorder.ondataavailable = e => { if (e.data.size) chunks.push(e.data); };
      recorder.onerror = () => { error("Snimanje je prekinuto."); stopTracks(); };
      recorder.onstop = () => {
        stopTracks();
        const type = recorder.mimeType.split(";")[0] || "audio/webm";
        const blob = new Blob(chunks, { type });
        chunks = [];
        if (!blob.size || blob.size > MAX_BYTES) {
          error("Snimka je prazna ili prevelika."); message("Snimanje nije spremljeno."); return;
        }
        const ext = type === "audio/mp4" ? "m4a" : type === "audio/ogg" ? "ogg" : "webm";
        file = new File([blob], "vojs-poruka." + ext, { type });
        fileUrl = URL.createObjectURL(file);
        el("preview").src = fileUrl;
        el("finished").hidden = false;
        el("upload").disabled = !el("accept-terms").checked;
        message("Snimka je spremna za dijeljenje.");
      };
      recorder.start(250);
      startedAt = Date.now(); pausedAt = 0; pausedTotal = 0;
      timerHandle = setInterval(updateTimer, 250);
      el("start").disabled = true; el("stop").disabled = false; el("pause").disabled = false;
      message("Snimanje je u tijeku.");
    } catch {
      stopTracks(); error("Pristup mikrofonu nije dostupan. Provjeri dopuštenje i HTTPS.");
    }
  }
  function finish() {
    if (!recorder || recorder.state === "inactive") return;
    recorder.stop();
    el("start").disabled = false; el("stop").disabled = true; el("pause").disabled = true;
    message("Obrada snimke…");
  }
  function pause() {
    if (!recorder || recorder.state === "inactive") return;
    if (recorder.state === "recording") {
      recorder.pause(); pausedAt = Date.now();
      el("pause").textContent = "Nastavi"; message("Snimanje je pauzirano.");
    } else if (recorder.state === "paused") {
      pausedTotal += Date.now() - pausedAt; pausedAt = 0;
      recorder.resume(); el("pause").textContent = "Pauziraj"; message("Snimanje je nastavljeno.");
    }
  }
  function download() {
    if (!fileUrl || !file) return;
    const anchor = document.createElement("a");
    anchor.href = fileUrl; anchor.download = file.name;
    document.body.append(anchor); anchor.click(); anchor.remove();
  }
  async function shareFile() {
    if (!file) return;
    if (navigator.canShare?.({ files: [file] })) {
      try { await navigator.share({ files: [file], title: "VOJS" }); }
      catch (e) { if (e.name !== "AbortError") error("Dijeljenje datoteke nije uspjelo."); }
    } else { download(); message("Datoteka je spremna za lokalno spremanje."); }
  }
  async function upload() {
    if (!file || link || !el('accept-terms').checked) return;
    if (!navigator.onLine) {
      error("Za P2P poveznicu potreban je internet. Datoteka se može dijeliti offline.");
      return;
    }
    error(""); el("upload").disabled = true;
    message("Stvaranje privatne P2P sesije…");
    try {
      session = await window.VojsPeer.create(file, text => message(text));
      link = session;
      el("link").value = session.url;
      el("link-result").hidden = false;
      message("Poveznica je spremna. Ostani na stranici dok primatelj ne preuzme poruku.");
    } catch (e) {
      error(e.message || "P2P povezivanje nije uspjelo.");
      el("upload").disabled = false;
    }
  }
  async function copy() {
    if (!link) return;
    try { await navigator.clipboard.writeText(link.url); message("Poveznica je kopirana."); }
    catch { el("link").select(); message("Označi i kopiraj poveznicu."); }
  }
  async function shareLink() {
    if (!link) return;
    if (navigator.share) {
      try { await navigator.share({ title: "VOJS glasovna poruka", url: link.url }); }
      catch (e) { if (e.name !== "AbortError") error("Dijeljenje poveznice nije uspjelo."); }
    } else copy();
  }
  async function revoke() {
    if (!session) return;
    error(""); el("revoke").disabled = true;
    try {
      await session.close();
      session = null; link = null;
      el("link").value = "";
      el("link-result").hidden = true;
      el("upload").disabled = !el("accept-terms").checked;
      message("P2P sesija je zatvorena. Lokalna snimka ostaje.");
    } catch { error("Prekidanje veze nije uspjelo."); }
    finally { el("revoke").disabled = false; }
  }
  el("accept-terms").addEventListener("change", () => {
    el("upload").disabled = !file || !!link || !el("accept-terms").checked;
  });
  for (const [id, fn] of [["start",start],["stop",finish],["pause",pause],["download",download],
    ["share-file",shareFile],["upload",upload],["copy",copy],["share-link",shareLink],["revoke",revoke]]) {
    el(id).addEventListener("click", fn);
  }
  if ("serviceWorker" in navigator && location.protocol === "https:") {
    navigator.serviceWorker.register("/sw.js").catch(() => {});
  }
})();
