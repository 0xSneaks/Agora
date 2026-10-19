// SPDX-License-Identifier: GPL-3.0-only
// Build replaces the revision marker with a content hash.
const CACHE = 'sneaks-ai-__REVISION__';
const SHELL = ['/', '/index.html', '/style.css', '/src/app.js', '/src/core.js', '/src/providers.js', '/manifest.webmanifest', '/icons/icon.svg', '/icons/icon-192.png', '/icons/icon-512.png', '/icons/apple-touch-icon.png', '/LICENSE.txt', '/SOURCE.txt'];
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(SHELL)));
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(names => Promise.all(names.filter(name => name.startsWith('sneaks-ai-') && name !== CACHE).map(name => caches.delete(name)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', event => {
  const req = event.request, url = new URL(req.url);
  // Never intercept, cache or proxy provider traffic or any payload-bearing request.
  if (req.method !== 'GET' || url.origin !== self.location.origin || url.search) return;
  if (req.mode === 'navigate') {
    event.respondWith(caches.open(CACHE).then(cache => cache.match('/index.html')).then(hit => hit || fetch(req)));
    return;
  }
  if (!SHELL.includes(url.pathname)) return;
  event.respondWith(caches.open(CACHE).then(cache => cache.match(url.pathname)).then(hit => hit || fetch(req)));
});
