"use strict";
const CACHE_NAME = "vojs-offline-v3";
const STATIC_FILES = ["/", "/snimi/", "/poruka/", "/recorder.js", "/message.js", "/p2p.js", "/manifest.webmanifest", "/style.css", "/logo.svg"];
self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(STATIC_FILES))
    .then(() => self.skipWaiting()));
});
self.addEventListener("activate", event => {
  event.waitUntil(caches.keys().then(names =>
    Promise.all(names.filter(name => name.startsWith("vojs-offline-") && name !== CACHE_NAME)
      .map(name => caches.delete(name)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", event => {
  const request = event.request, url = new URL(request.url);
  if (request.method !== "GET" || url.origin !== self.location.origin ||
      url.pathname.startsWith("/api/") || url.search) return;
  event.respondWith(fetch(request).then(response => {
    if (response.ok && STATIC_FILES.includes(url.pathname)) {
      const copy = response.clone();
      event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.put(request, copy)));
    }
    return response;
  }).catch(() => caches.match(request)));
});
