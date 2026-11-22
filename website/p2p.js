"use strict";
/**
 * Anonymous WebRTC DataChannel transfer. Audio stays in the two devices.
 * Server sees SDP session descriptions (short-lived JSON), never media bytes.
 */
window.VojsPeer = (() => {
  const API = "/api/signal.php";
  const PROTOCOL = "vojs-p2p-v1";
  const MAX_BYTES = 8 * 1024 * 1024;
  const CHUNK = 16 * 1024;
  const LIMIT = 1024 * 1024;
  const SDP_TIMEOUT = 20000;
  const ID = /^[a-f0-9]{48}$/;
  const TOKEN = /^[a-f0-9]{64}$/;
  const MIME = /^(audio\/(mp4|webm|ogg))$/;
  // STUN discovers peers. Strict NATs may need an operator-hosted TURN relay.
  // TURN is not bundled: a relay could see encrypted WebRTC packets, not plaintext audio.
  // Localhost E2E tests have host candidates and do not require an external STUN call.
  // Production retains the original STUN configuration; no third-party audio hosting.
  const localHost = ["127.0.0.1", "localhost"].includes(location.hostname);
  const ICE = { iceServers: localHost ? [] : [{ urls: "stun:stun.l.google.com:19302" }] };
  let active = null;
  const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
  const ensure = condition => { if (!condition) throw Error("Neispravni podaci sesije."); };
  async function api(method, params, body) {
    const query = params ? "?" + new URLSearchParams(params) : "";
    const response = await fetch(API + query, {
      method, cache: "no-store", credentials: "omit",
      headers: method === "POST" ? { "Content-Type": "application/json" } : {},
      ...(body ? { body: JSON.stringify(body) } : {}),
      signal: AbortSignal.timeout(15000)
    });
    if (response.status === 202) return null;
    if (!response.ok) throw Error("Poveznica nije dostupna. Pokušaj ponovno.");
    if (response.status === 204) return {};
    return response.json();
  }
  async function assertProtocol() {
    let details;
    try {
      const response = await fetch("/api/status.php", {
        method: "GET", cache: "no-store", credentials: "omit",
        signal: AbortSignal.timeout(15000)
      });
      if (!response.ok) throw Error("HTTP "+response.status);
      details = await response.json();
    } catch {
      throw Error("VOJS poslužitelj nije dostupan. Snimku možeš podijeliti lokalno.");
    }
    if (details?.protocol !== PROTOCOL || details.p2p !== true ||
        details.audio_storage !== false || details.database !== false ||
        details.cron_required !== false || details.session_seconds !== 900) {
      throw Error("VOJS aplikacija i web nisu usklađeni. Potrebno je ažurirati web poslužitelj.");
    }
  }
  function session(id, token, peer) {
    let stopped = false;
    const current = {
      id, token, peer,
      get closed() { return stopped; },
      async close() {
        if (stopped) return;
        stopped = true;
        if (active === current) active = null;
        peer.close();
        if (token) {
          try {
            await api("POST", null, { action: "close", id, owner_token: token });
          } catch { /* A closed peer cannot send more media. JSON auto-expires. */ }
        }
      }
    };
    return current;
  }
  async function iceReady(peer) {
    if (peer.iceGatheringState === "complete") return;
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => { clear(); reject(Error("Veza nije uspostavljena. Pokušaj ponovno.")); }, SDP_TIMEOUT);
      function clear() {
        clearTimeout(timeout);
        peer.removeEventListener("icegatheringstatechange", check);
      }
      function check() {
        if (peer.iceGatheringState === "complete") { clear(); resolve(); }
      }
      peer.addEventListener("icegatheringstatechange", check);
      check();
    });
  }
  async function poll(id, view, token, state) {
    const until = Date.now() + 15 * 60 * 1000;
    while (!state.closed && Date.now() < until) {
      const response = token
        ? await api("POST", null, { action: "get_answer", id, owner_token: token })
        : await api("GET", { id, view });
      if (response !== null) {
        ensure(response.description && response.description.type === view);
        return response.description;
      }
      await delay(1250);
    }
    throw Error("Vrijeme čekanja je isteklo ili je pošiljatelj zatvorio sesiju.");
  }
  function monitor(peer, notify) {
    peer.addEventListener("connectionstatechange", () => {
      if (peer.connectionState === "failed") {
        notify("Povezivanje nije uspjelo. Pokušaj preko druge mreže.");
      } else if (peer.connectionState === "disconnected") {
        notify("Veza s drugim uređajem je prekinuta.");
      }
    });
  }
  async function drained(channel) {
    if (channel.bufferedAmount < LIMIT / 2) return;
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => { done(); reject(Error("Slanje je prekinuto.")); }, 30000);
      function done() { clearTimeout(timer); channel.removeEventListener("bufferedamountlow", low); }
      function low() {
        if (channel.bufferedAmount < LIMIT / 2) { done(); resolve(); }
      }
      channel.addEventListener("bufferedamountlow", low);
      low();
    });
  }
  async function streamAudio(file, channel, notify) {
    const payload = await file.arrayBuffer();
    const digest = await crypto.subtle.digest("SHA-256", payload);
    const hash = Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, "0")).join("");
    if (channel.readyState !== "open") throw Error("Primatelj nije spojen.");
    channel.bufferedAmountLowThreshold = LIMIT / 2;
    channel.send(JSON.stringify({ kind: "header", size: payload.byteLength, mime: file.type, sha256: hash }));
    const bytes = new Uint8Array(payload);
    for (let offset = 0; offset < bytes.length; offset += CHUNK) {
      if (channel.readyState !== "open") throw Error("Primatelj je prekinuo prijenos.");
      await drained(channel);
      channel.send(bytes.slice(offset, offset + CHUNK));
      if (offset % (256 * 1024) === 0) notify("Šaljem snimku izravno primatelju…");
    }
    await drained(channel);
    channel.send(JSON.stringify({ kind: "end" }));
    notify("Snimka je poslana. Čekam potvrdu.");
  }
  async function create(file, notify = () => {}) {
    if (active && !active.closed) throw Error("Poveznica je već aktivna.");
    if (!window.RTCPeerConnection || !crypto.subtle) throw Error("Ovaj uređaj ne podržava izravno povezivanje.");
    if (!(file instanceof Blob) || file.size < 64 || file.size > MAX_BYTES ||
        !MIME.test(file.type)) throw Error("Nepodržana datoteka (maks. 8 MB).");
    await assertProtocol();
    const created = await api("POST", null, { action: "create" });
    ensure(ID.test(created.id) && TOKEN.test(created.owner_token));
    const link = new URL(created.url);
    ensure(link.origin === "https://vojs.com.hr" &&
      link.pathname === "/poruka/" && link.searchParams.get("id") === created.id);
    const peer = new RTCPeerConnection(ICE);
    const current = session(created.id, created.owner_token, peer);
    active = current;
    monitor(peer, notify);
    const channel = peer.createDataChannel("vojs-audio", { ordered: true });
    channel.binaryType = "arraybuffer";
    channel.onopen = () => streamAudio(file, channel, notify).catch(e => notify(e.message));
    channel.onmessage = event => {
      if (event.data === "received") notify("Primatelj je preuzeo snimku.");
    };
    try {
      await peer.setLocalDescription(await peer.createOffer());
      await iceReady(peer);
      await api("POST", null, {
        action: "offer", id: created.id, owner_token: created.owner_token,
        description: { type: "offer", sdp: peer.localDescription.sdp }
      });
      notify("Poveznica je spremna. Ostani na ovoj stranici dok je primatelj ne otvori.");
      (async () => {
        try {
          const answer = await poll(created.id, "answer", created.owner_token, current);
          if (!current.closed) {
            await peer.setRemoteDescription(answer);
            notify("Primatelj se povezuje…");
          }
        } catch (e) {
          if (!current.closed) notify(e.message);
        }
      })();
      return { id: created.id, url: created.url, expires: created.expires, close: () => current.close() };
    } catch (e) {
      await current.close();
      throw e;
    }
  }
  async function receive(id, onStatus, onAudio) {
    if (active && !active.closed) throw Error("Poveznica je već aktivna.");
    ensure(ID.test(id) && typeof onAudio === "function");
    if (!window.RTCPeerConnection || !crypto.subtle) throw Error("Ovaj uređaj ne podržava izravno povezivanje.");
    await assertProtocol();
    const peer = new RTCPeerConnection(ICE);
    const current = session(id, null, peer);
    active = current;
    monitor(peer, onStatus);
    let info = null, blocks = [], received = 0;
    let audioChannel = null;
    peer.addEventListener("datachannel", ({ channel }) => {
      // A room accepts only one audio stream. Extra data channels must not
      // race the shared receiver state or inject competing audio payloads.
      if (channel.label !== "vojs-audio" || audioChannel !== null) { channel.close(); return; }
      audioChannel = channel;
      let finalizing = false, completed = false;
      channel.binaryType = "arraybuffer";
      channel.onmessage = async event => {
        // SHA-256 verification yields to the event loop. Do not process
        // duplicate end markers, late chunks, or callbacks after revocation.
        if (finalizing || completed || current.closed) return;
        if (typeof event.data === "string") {
          let packet;
          try { packet = JSON.parse(event.data); } catch { channel.close(); return; }
          if (packet.kind === "header" && !info) {
            if (!Number.isInteger(packet.size) || packet.size < 64 || packet.size > MAX_BYTES ||
                typeof packet.mime !== "string" || !MIME.test(packet.mime) ||
                typeof packet.sha256 !== "string" || !/^[a-f0-9]{64}$/.test(packet.sha256)) {
              onStatus("Neispravni podaci snimke."); channel.close(); return;
            }
            info = packet;
            onStatus("Primam zvuk izravno od pošiljatelja…");
          } else if (packet.kind === "end" && info) {
            finalizing = true; // Synchronous guard before the first await.
            if (received !== info.size) {
              onStatus("Snimka nije potpuna."); channel.close();
              blocks = []; completed = true; return;
            }
            const blob = new Blob(blocks, { type: info.mime });
            try {
              const hash = Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", await blob.arrayBuffer())), b => b.toString(16).padStart(2, "0")).join("");
              if (hash !== info.sha256) {
                onStatus("Provjera snimke nije uspjela."); channel.close(); return;
              }
              if (current.closed || channel.readyState !== "open") return;
              completed = true;
              onAudio(blob);
              onStatus("Snimka je preuzeta. Sada je možeš preslušati bez mreže.");
              channel.send("received");
            } catch {
              if (!current.closed) onStatus("Provjera snimke nije uspjela.");
              channel.close();
            } finally {
              blocks = [];
              finalizing = false;
              completed = true;
            }
          }
        } else if (info && event.data instanceof ArrayBuffer) {
          received += event.data.byteLength;
          if (received > info.size) {
            onStatus("Prekoračena veličina snimke."); channel.close(); return;
          }
          blocks.push(event.data);
        }
      };
    });
    try {
      onStatus("Čekam pošiljatelja i pripremam sigurnu vezu…");
      const offer = await poll(id, "offer", null, current);
      await peer.setRemoteDescription(offer);
      await peer.setLocalDescription(await peer.createAnswer());
      await iceReady(peer);
      await api("POST", null, { action: "answer", id,
        description: { type: "answer", sdp: peer.localDescription.sdp } });
      onStatus("Veza je uspostavljena. Čekam prijenos zvuka…");
      return { close: () => current.close() };
    } catch (e) { await current.close(); throw e; }
  }
  window.addEventListener("pagehide", () => {
    if (active) {
      const session = active;
      // Peer disconnects immediately. Server JSON expires without cron.
      session.peer.close();
      if (session.token && navigator.sendBeacon) {
        const data = new Blob([JSON.stringify({
          action: "close", id: session.id, owner_token: session.token
        })], { type: "application/json" });
        navigator.sendBeacon(API, data);
      }
    }
  });
  function closeActive() {
    if (!active) return Promise.resolve();
    const current = active;
    active = null;
    return current.close();
  }
  return { create, receive, closeActive };
})();
