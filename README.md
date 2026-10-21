# Sneaks AI

**A bring-your-own-key AI chat app for Android.** Use your own accounts and endpoints, keep your conversations on your device.

> **Unofficial modified version.** Sneaks AI is a modified version of [Agora](https://github.com/newo-ether/Agora) by newo-ether, licensed under GPL-3.0. It is not affiliated with, endorsed by, or supported by the original author. Changes in this version are the responsibility of this project. See [NOTICE.md](NOTICE.md).

## What it does

- **Many providers, your keys:** OpenAI, Anthropic, Google Gemini, DeepSeek, Qwen/DashScope, OpenRouter, OpenCode, Groq, Ollama and local llama.cpp, plus custom OpenAI-, Google- or Anthropic-compatible endpoints.
- **Local storage:** conversations, settings and keys stay in app storage on the device. Requests go straight from your phone to the provider you chose.
- **Branching conversations:** edit or regenerate earlier messages without losing the other branches.
- **Tools:** web search, memory, MCP servers, scheduled tasks and more, all optional.
- **Provider setup API:** an optional local WebUI endpoint lets an on-device agent add providers for you.
- **Sneaks AI touches:** a neon launch intro, an optional name step on first run (saved on the device only), and a time-of-day greeting.

## What is different from the original

- No crash reports, rating feedback or other data are sent to the original author's servers. The crash reporter and rating prompt are switched off.
- The update check reads this repository's GitHub releases instead of the original's.
- New branding, launch intro, welcome flow and name/greeting feature.
- Links point to this repository. The credit to the original project stays in **Settings → About**.

## Privacy

There is no analytics and no relay server. The app only contacts the providers, tools and servers you configure, plus GitHub for the daily update check. Read [PRIVACY.md](PRIVACY.md).

## Install

Build it yourself (below), or check this repository's **Releases** page when a signed build is published.

**iPhone:** there's an installable web app version in [`web/`](web/README.md). Open it in Safari and tap **Share → Add to Home Screen**. See [web/INSTALL-IPHONE.md](web/INSTALL-IPHONE.md).

## Build from source

Needs JDK 21, Android Studio, and the Android SDK 36 and NDK components.

```
./gradlew :app:testFdroidDebugUnitTest
./gradlew assembleFdroidDebug
```

A release build (`assembleFdroidRelease`) needs your own signing keystore. Never commit a keystore or its passwords.

## License and credit

Licensed under the [GNU General Public License v3.0](LICENSE). Sneaks AI is built on Agora, copyright its authors, and keeps their copyright and license notices. The Agora name, logo and screenshots belong to the original project and are not included here. Bundled third-party components keep their own licenses.

Thanks to newo-ether and the Agora contributors for the foundation.
