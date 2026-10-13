# Handoff: Sneaks AI (fork of Agora) — finish & ship

Repo: `0xSneaks/Agora` · Work branch: `feat/sneaks-ai-brand` (PR #2, open, CI green) · Target: `master`

## Goal
Ship one signed **Sneaks AI** APK that updates in place forever, restore the owner's data into it,
and hand the provider API to Muse (on-device agent in Termux).

## Done (verified)
- **Provider API** (merged to master, PR #1): `GET/POST /api/providers` on the WebUI.
  Session + same-origin gated, keys write-only (`hasKey` + last-4 `keyHint`), HTTPS base URLs only
  (HTTP to loopback allowed), built-in names refused, 409 if a name already points at another URL.
  Files: `webui/WebUiProviderAdmin.kt`, wired in `WebUiServer.kt`, `WebUiController.kt`, `di/AppContainer.kt`.
  **Tested on the phone:** login → `{"providers":[]}` → POST Space Bunny → `HTTP 201`.
- **Branding** (on `feat/sneaks-ai-brand`, not merged): app label "Sneaks AI" (all locales),
  pfp launcher icon (`mipmap-*/ic_launcher_sneaks_layer.png`, `drawable-nodpi/sneaks_pfp.png`),
  About → "Tip the Dev" copies Solana address for sneaks.skr
  (`FtuidxSaoNDeTuNTDuf9jLACVwqjgQeBwKTYC54f3GYp`, validated 32-byte key).
- **Two intro animations, both in the branch** (owner has NOT picked yet):
  - A — Grok video `res/raw/sneaks_splash.mp4` (1080×1920, 3.1s), played once at app start by
    `ui/onboarding/SneaksLaunchSplash.kt` (overlay in `MainActivity.kt`; tap to skip, 5s cap,
    off under reduced motion and screenshot runs).
  - B — Grok code `ui/onboarding/SneaksIntro.kt`, first onboarding page (glitch → assemble → neon sign).
  - Test APK with both: Actions run 38372808686, artifact `agora-fdroid-release`.

## Device state (owner's Solana Seeker)
- **Agora Dev** (`com.sneaks.agora.dev`, branch `muse/agora-dev-webui`): the owner's REAL data. Its WebUI holds port **8686**. Do not uninstall until Sneaks AI is restored and confirmed.
- **Agora** (`com.newoether.agora`, master build): test install only; WebUI on **8687**; holds only the Space Bunny test provider. Safe to uninstall.
- Backup: exported (no credentials) to the owner's Google Drive. API keys must be re-entered after import.

## Remaining (in order)
1. **Owner picks intro:** A only / B only / both. Remove the unpicked one (A: delete `SneaksLaunchSplash.kt`,
   its call in `MainActivity.kt`, and `res/raw/sneaks_splash.mp4`; B: revert page 0 in
   `WelcomeScreen.kt` to a plain page and delete `SneaksIntro.kt`). Keep CI green.
2. **Permanent signing key — ASK OWNER BEFORE DOING.** Current builds are signed with a throwaway debug
   key (`Restore signing key` step is skipped: secrets unset), so no build can update another.
   Generate a keystore, owner stores it safely + adds repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD` (workflow already consumes them). Never commit the keystore or print passwords.
3. **Decide applicationId — ASK OWNER.** Recommended `com.sneaks.ai` (avoids clashing with official Agora;
   free to change now since it's a fresh install). Change in `app/build.gradle.kts`.
4. Merge PR #2 → master, confirm CI build step "Restore signing key" ran (not skipped), get the APK.
5. On device: uninstall test "Agora" → install Sneaks AI → Settings → Data Control → Import the Drive backup
   → re-enter API keys → Settings → WebUI on (port 8686 once Agora Dev is gone).
6. Confirm chats/memories/prompt restored, then owner uninstalls Agora Dev.
7. Hand Muse the API (below).

## Muse instructions (Termux on the phone)
```
export AGORA_WEBUI_PW='…'; export OPENCODE_API_KEY='…'   # typed by owner, never pasted into chat
A=https://127.0.0.1:8686; J=~/.agora_jar; umask 077
curl -sk -c $J -H 'Content-Type: application/json' -d "{\"password\":\"$AGORA_WEBUI_PW\"}" $A/api/login
curl -sk -b $J $A/api/providers
curl -sk -b $J -H 'Content-Type: application/json' $A/api/providers \
  -d "{\"name\":\"Space Bunny\",\"baseUrl\":\"https://opencode.ai/zen/go/v1\",\"apiKey\":\"$OPENCODE_API_KEY\",\"modelId\":\"glm-5.1\",\"alias\":\"Space Bunny\"}"
```
Rules: unique names; 409 = report, don't work around; never print/log keys; GET the list after every add.

## Gotchas
- Termux paste may inject `[200~` — long-press → Paste, or `bind 'set enable-bracketed-paste off'`.
- `curl -s` hides errors; use `-i` to see status codes.
- Two apps can't share a WebUI port; a 404 on `/api/providers` usually means the old app answered.
- Play Protect warns on sideload: "Install anyway".
- Model ID `glm-5.1` was not verified to answer; check OpenCode Go's model list.
- Kotlin files must stay ≤ 800 lines (`verifyKotlinFileSize`); new strings need every locale (`SettingsResourceContractTest`).
