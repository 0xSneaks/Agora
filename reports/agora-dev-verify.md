# Agora Dev APK verification

- Source SHA: `95c1606b9e9598902c58270a839be9a26856f26c`
- Workflow run: https://github.com/0xSneaks/Agora/actions/runs/38202863068
- Unit tests: total=3027 failures=0 errors=0 skipped=3 (gradle rc=0)
- Build rc: 0
- Tracked files modified by build: 2

| Check | Result | Detail |
|---|---|---|
| Unit tests pass | PASS | 3027 run, 3024 passed, 3 skipped |
| APK built | PASS | `app/build/outputs/apk/fdroid/release/app-fdroid-release.apk` (33737604 bytes) |
| package == com.sneaks.agora.dev | PASS | com.sneaks.agora.dev |
| label == Agora Dev | PASS | Agora Dev |
| versionName == 2.1.0-webui-dev | PASS | 2.1.0-webui-dev |
| arm64-v8a native libs | PASS | 22 .so files incl. proot_exec/proot_loader/talloc |
| WebUI assets present | PASS | 54 entries, e.g. assets/webui/index.html |
| all provider authorities under com.sneaks.agora.dev | PASS | com.sneaks.agora.dev.fileprovider com.sneaks.agora.dev.documents com.sneaks.agora.dev.androidx-startup com.sneaks.agora.dev.jlatexmathinitprovider  |
| zero references to com.newoether.agora | FAIL | manifest=9 resources=24 files=3: classes2.dex lib/arm64-v8a/libagora_llama.so classes.dex  |

## APK
- APK SHA-256: `0262cd92a1e090b39faa85a70294fe963f7ff5cfd94b8669e96f0755e2b792d2`
- APK size: 33737604 bytes
- APK path: `app/build/outputs/apk/fdroid/release/app-fdroid-release.apk`
- ABIs in APK: arm64-v8a 

## Provider authorities
```
com.sneaks.agora.dev.fileprovider
com.sneaks.agora.dev.documents
com.sneaks.agora.dev.androidx-startup
com.sneaks.agora.dev.jlatexmathinitprovider
```

## newoether hits by file (counts)
```
classes2.dex:       1 com/newoether/agora/model/ToolCallData
      1 com/newoether/agora/model/TokenUsage
      1 com/newoether/agora/model/SelectedAttachment$Companion
      1 com/newoether/agora/model/SelectedAttachment
      1 com/newoether/agora/model/MessageSource
      1 com/newoether/agora/model/MessageSegment
      1 com/newoether/agora/model/AttachmentMeta$Companion
      1 com/newoether/agora/model/AttachmentMeta
      1 com/newoether/agora/data/EmbeddingModelConfig
      1 com/newoether/agora/data/CustomProviderConfig
      1 com/newoether/agora/data/ApiKeyEntry
lib/arm64-v8a/libagora_llama.so:       3 com/newoether/agora/api/ChatTemplateGrammarTrigger
      1 com/newoether/agora/api/LlamaChatTemplateResult
      1 com/newoether/agora/api/ChatTemplateToolCall
      1 com/newoether/agora/api/ChatTemplateTool
      1 com/newoether/agora/api/ChatTemplateMessage
classes.dex:       6 com/newoether/agora/model/RunEffectIdentity
      5 com/newoether/agora/remote/RemoteConversationPage
      4 com/newoether/agora/webui/WebUiServer$SessionResponse
      4 com/newoether/agora/webui/WebUiServer$LoginRequest
      4 com/newoether/agora/webui/WebUiServer$ErrorResponse
      4 com/newoether/agora/webui/WebToolNode$Scalar
      4 com/newoether/agora/webui/WebToolNode$Object
      4 com/newoether/agora/webui/WebToolNode$Array
      4 com/newoether/agora/webui/WebToolBody$Stopped
      4 com/newoether/agora/webui/WebToolBody$Shell
      4 com/newoether/agora/webui/WebToolBody$Search
      4 com/newoether/agora/webui/WebToolBody$Paths
      4 com/newoether/agora/webui/WebToolBody$Muted
      4 com/newoether/agora/webui/WebToolBody$Grep
      4 com/newoether/agora/webui/WebToolBody$FileContent
```

**RESULT: 1 FAIL**
