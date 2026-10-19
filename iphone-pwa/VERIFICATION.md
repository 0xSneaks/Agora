# Verification — 2026-10-19

VERIFIED: reference branch feat/welcome-name-greeting at ce19a838e7465ccd2be7147bf47446437e3d1fb6; linked Android run 39207318234 completed successfully.

VERIFIED: Node v24.19.0; `npm test` builds first, then passes 31/31 tests, 0 failed, 0 skipped. Explicit `npm run build` also passed. `node --check` passed for app.js, providers.js and sw.js. npm emitted a host-environment warning about an unknown http-proxy configuration; it did not affect the results.

VERIFIED: generated static PWA revision b7726b04c317f16c. Includes manifest, 192/512 PNG icons, 180 PNG Apple icon, license, public source reference, client modules and revisioned service worker. No production npm dependencies or server functions.

VERIFIED by deterministic tests: all 24 greeting hours, optional/40-character name, endpoint validation, local history/keys separation, corrupt/full storage failure handling, backup without credentials, schema rejection, all three protocol request bodies/auth headers, streaming split UTF-8 and CRLF, stop/abort propagation, provider errors without echoed secrets, model list requests, manifest/icon sizes, security headers, cache installation, offline cached navigation, no service-worker interception of provider requests, and targeted old-cache removal.

INTENT: user launches the installed PWA, adds a provider and model, then sends a message directly from Safari to that provider while history stays local. Mock protocol tests cover those network formats but do not establish current live-provider acceptance.

NOT VERIFIED: browser DOM flows, visual rendering, physical iPhone Safari/Home Screen installation, keyboard behavior, storage partition behavior, real iPhone offline reopening, live owner API key, real-provider CORS and billable generation. The environment lacks the supported browser-control capability and an owner-supplied provider key; browser QA was skipped, and no private keys were searched for or used.

Source is isolated under iphone-pwa on feat/sneaks-ai-iphone-pwa. No Android branch merge is performed. Vercel deployment is a separate step after source is saved; final deployment readiness and public HTTP results are reported in the task response.
