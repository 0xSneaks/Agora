# Privacy Policy

**Last updated: October 19, 2026**

Sneaks AI (a modified version of Agora) is a BYOK Android client. It does not operate a relay for chat completions: model requests go from your device to the provider or endpoint you configure.

## Local data

Conversations, message trees, tasks, loops, memories, prompts, attachments, tool media, settings, and imported models are stored in app-managed databases/files on the device. Secret settings normally use an AES-256-GCM envelope backed by the Android Keystore. Legacy values are accepted as plaintext, and encryption failure deliberately falls back to plaintext rather than losing the value, so device storage protection remains important.

Clearing app data or uninstalling removes app-managed data unless Android backup or a user-created export preserves it.

## Network destinations

Data leaves the device only through features you use:

- messages and attachments go to the selected AI provider;
- title, transcription, image-generation, and embedding requests go to their selected providers;
- search queries go to the selected web-search service;
- MCP calls go to enabled MCP servers;
- Conch and SSH operations go to configured remote devices;
- release metadata can be checked from this project's GitHub releases when the app starts, at most once per day;

This build has no crash-report or rating submission: those features are switched off and nothing is sent to the original author's servers. There is no general analytics path. Third-party endpoints have their own privacy and retention policies.

## Backups and exports

A `.agora` export is a ZIP archive. If you explicitly include API keys or other secrets, those values are unencrypted inside the archive. Protect and delete exported copies as appropriate.

## Transport and proxy

The configured proxy applies to the app's shared HTTP-client traffic, not direct SSH, local inference, or processes inside the Alpine sandbox. Conch application-layer encryption requires an API key; a blank-key endpoint uses plain JSON and relies on HTTPS for transport confidentiality.

## Permissions

- **Internet**: provider, search, MCP, update-check and remote HTTP connections.
- **Notifications / foreground service**: ongoing generation, automation, and user-visible completion behavior.
- **Files and media**: only when you select/import attachments, models, backups, fonts, or shared sandbox storage.
- **Exact alarms**: optional automation scheduling where supported and explicitly enabled.

## Children, changes, and contact

Sneaks AI is not directed to children under 13. This policy may be updated with the repository/application. Questions can be opened at [github.com/0xSneaks/sneaks-ai](https://github.com/0xSneaks/sneaks-ai).
