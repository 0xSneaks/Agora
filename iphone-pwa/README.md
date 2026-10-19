# Sneaks AI — iPhone PWA

Independent, static, installable BYOK chat client. No application server, telemetry, crash uploads, ratings, third-party update checks, remote fonts, CDNs or automatic provider/model calls. Vercel serves static assets; the browser talks directly to the chosen provider. Hosting still involves normal access logs and same-origin service-worker update requests.

Source: https://github.com/0xSneaks/Agora/tree/feat/sneaks-ai-iphone-pwa/iphone-pwa

## Reference and license

VERIFIED reference: 0xSneaks/Agora branch feat/welcome-name-greeting at ce19a838e7465ccd2be7147bf47446437e3d1fb6, the successful Android Build APK run https://github.com/0xSneaks/Agora/actions/runs/39207318234.

Read README.md, PRIVACY.md, provider classes/defaults, CustomProviderConfig.kt, WelcomeScreen.kt, SneaksIntro.kt, UserProfileStore.kt, Greeting.kt, and values/sneaks_strings.xml before implementing. Greeting boundaries follow Greeting.kt: morning 05–11, afternoon 12–16, evening 17–21, late night 22–04 uses “Hey, Name”. Blank name uses “Welcome to Sneaks AI”.

This is newly written JavaScript/HTML/CSS, not compiled Android code. Behavior and endpoint defaults follow the reference; the sign is a new text-only implementation, without Agora artwork or logos. Distributed under GPL-3.0-only; complete license is LICENSE and is shipped as /LICENSE.txt. New web files by Sneaks AI contributors, 2026. Upstream Agora by newo-ether and contributors: https://github.com/newo-ether/Agora . Sneaks AI is not affiliated with or endorsed by upstream Agora. No existing Android files are modified, and this work is not merged into an existing branch.

## Included

- Text-only “0x Sneaky Alpha” neon intro, reduced-motion support, optional local name (40 UTF-16 characters, matching Android's limit), editable later.
- Local-time greetings, no keyboard forced open on launch; dark cyan/pink CRT appearance.
- Add/edit/delete providers and exact model IDs, manual IDs or user-triggered model fetch. Eight endpoint presets from Android plus custom HTTPS endpoints with OpenAI Chat Completions, Anthropic Messages or Gemini protocol. No guessed model IDs.
- Direct text chat, streaming replies, stop, conversation history, copy, optional per-conversation system instruction, switching provider with explicit history-sharing notice.
- Keys only in localStorage under sneaks-ai.keys.v1. History/settings/name in localStorage under sneaks-ai.data.v1. No credentials in URLs, logs, exports, service-worker caches or server requests. HTTP redirects blocked. Provider response errors shown as sanitized messages.
- Export/import a versioned JSON backup without keys, delete conversations/providers, erase all app data, storage-failure recovery and persistence request.
- Manifest, PNG icons, Apple icon, standalone display, safe-area/keyboard-aware viewport and static-only service worker.
- Offline shell and saved conversation access after a successful initial online installation. AI replies and model sync need internet.

## Security and iPhone limits

localStorage is NOT a secure vault or Android Keystore. Keys are readable text, available to code on this origin and people/tools with access to the browser. TLS protects transport, not this storage. The app renders messages as plain text, does not execute model HTML, loads no third-party code, sets CSP, blocks embeds and omits browser credentials from provider fetches. connect-src permits HTTPS to support custom endpoints; the actual application routes requests only to the selected, validated endpoint. Trust the saved origin and use limited/revocable keys. Hosting/source changes must be trusted.

The selected provider receives the conversation history, system instruction and key when a message is sent; it applies its own data-retention policies. A provider may be a router that forwards requests internally. Name is not automatically included in prompts. Model listing is user-triggered. No background title generation, key tests or provider checks occur.

Safari can block providers that do not allow this web origin via CORS. Native Android does not have this restriction. Workaround under the no-server rule: use a provider/custom HTTPS endpoint that explicitly supports browser access. No automatic relay fallback is implemented. Anthropic browser opt-in header is sent, but no paid provider request has been verified without an owner-supplied key. OpenAI Responses-only models/options are not supported by the Chat Completions adapter.

Data is per-origin and per-browser/web-app storage container. Safari and Home Screen storage may differ. INSTALL FIRST, then add keys. Clearing website data, deleting the web app, private browsing, or OS storage eviction can lose data. Persistence is requested but not guaranteed. Export regularly. Backups exclude keys and the JSON format is specific to this PWA, not .agora archives.

The service worker caches an explicit list of static assets only. No provider URL, key, prompt or response is handled by it. Browser checks for this origin's app updates are normal PWA operation, not third-party update checks. See WebKit’s primary documentation: https://webkit.org/blog/17333/webkit-features-in-safari-26-0/ and https://webkit.org/blog/14403/updates-to-storage-policy/ . A waiting update takes over after existing app windows close, avoiding mid-conversation replacement.

## Android feature gaps and workarounds

| Reference feature | PWA v1 status / workaround |
| --- | --- |
| Android/Termux sandbox, SSH, Conch/Filo control, remote WebUI server | Not ported; iPhone PWA cannot host the Android shell or local server. Keep the Android app for those tasks. An explicitly configured remote HTTPS service could be a later opt-in, outside this no-server build. |
| Local GGUF/llama.cpp models and embeddings | Android native engine unavailable here. Use a compatible provider. A future browser inference engine needs separate memory/performance testing. |
| Background Tasks/Loops, scheduled backups, foreground generation service | No reliable long-running background work in an iPhone PWA. Keep the app visible during a reply; export manually; retain Android for unattended tasks. |
| General MCP/search/shell/agent tools, RAG/memory, image/audio/attachment features | Not in v1. Many could be separate browser features, but direct services must allow CORS; server relays conflict with this build's boundary. Use provider text chat or Android meanwhile. |
| Message tree editing/regeneration, context compact/token budget, provider-specific thinking/cache controls | Not in v1; these are feasible future client work. History is linear and sent in full; start a new conversation when approaching model context limits. |
| .agora ZIP, ChatGPT/Claude import, scheduled backups | Not implemented; use this app's manual JSON backup without keys. Keep original archives for a future audited importer. |
| Android keystore | Not available to web code. Device-local readable keys as explicitly requested; restrict and revoke keys. |
| Android themes/fonts/haptics and translations | Neon CRT English interface only. Native haptics/Android theming are not ported; reduced motion is respected. |
| Ollama HTTP/LAN endpoint | Insecure HTTP is intentionally blocked on this HTTPS PWA. Use an owner-controlled HTTPS endpoint with CORS, or retain native Android. |
| App Store / APK installation | This is a Home Screen PWA, not an IPA or App Store release. No Mac required. |

## Build and deploy

Requires Node 22+; zero npm dependencies. Run from iphone-pwa:

    npm test
    npm run build

Set Vercel project root to iphone-pwa when deploying from this repository/branch; output dist; no server functions, environment variables or secrets needed. A static-files API deployment can publish the already-built dist with the same headers in vercel.json. Do not connect the Android branch as production inadvertently.

## Phone installation

OPEN the deployed URL in Safari.
TAP Share.
SCROLL to Add to Home Screen.
TAP Add to Home Screen.
KEEP Open as Web App on, if shown.
TAP Add.
OPEN Sneaks AI from its Home Screen icon.
TAP Enter Sneaks AI; enter or skip your name.
TAP Add your first provider; add your key and exact model ID; Save.

## Verification boundaries

Node tests cover greeting boundaries, name limit, schema validation, endpoint/key routing, streaming chunk boundaries, protocol formats, safe errors, local persistence/exports, offline service-worker cache boundaries, PWA manifest/icons and security headers. Real test/build outcomes are recorded in VERIFICATION.md after execution. Mock provider tests prove request/response behavior, not current paid-provider acceptance or CORS. No iPhone hardware or live owner key is available in this build environment. Physical Safari installation, keyboard behavior, actual provider requests and offline reopening must be confirmed on the owner's phone.
