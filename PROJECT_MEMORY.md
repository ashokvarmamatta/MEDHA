# MEDHA — Project Memory

## Session 2026-06-10 — MTP branch, stop button, context viewer, full app inspection fixes + Medha Web spin-off  ·  updates: 1
**Summary:** Long session on branch `feat/mtp-speculative-decoding`: implemented MTP/speculative decoding, fixed the MTP-on-standard-models speed regression, added stop/regenerate/copy/markdown/context-viewer, ran a 3-agent professional inspection and fixed all wave-1–3 findings, then built a brand-new sibling project **Medha Web** (`D:\Ashok\olama\projects\AiwebOffline`) that runs the web-only Gemma models in the browser. All Android work is still UNCOMMITTED on `feat/mtp-speculative-decoding`.

**Update log:**
- upd 1: 10 requests · 6 decisions · 14 features · 7 fixes · 8 Android files + new web project

**Requests — what you asked:**
- Fix the downloaded "Lite" model that won't load; tell me each model's max context; show context length in the chat screen; fix online-mode issues.
- Analyse Google's MTP (multi-token prediction) blog — can it apply to our Android app? Then: "ok create new branch lets implement it".
- MTP felt slow (0.2 tok/s) — is the downloaded model proper? Also: I'm stuck in chat during a long generation, can't go back — add a STOP and add Log.e to check live.
- Install again and track (logcat) — led to discovering the MTP regression on standard models.
- Show context like Claude desktop's context viewer in our app.
- Back-press in chat should first clear/return to home screen, only then exit; show context % in the chat header, tap for full details.
- "Analyse this code find bugs and also analyse top chat apps what we missing — you are professional app inspector ui, code, ux" → then "ya fix all of them use agents if needed".
- Create a web app at `D:\Ashok\olama\projects\AiwebOffline` — like MEDHA: download web models, save in cache, use them (the web-only models we found).
- Fix the web app "Task cancelled" generation error.
- Save everything from this session to project memory.

**Decisions — accepted / rejected / changed:**
- Accepted: MTP kept as an explicit experimental catalog model (`usesMtp` flag); speculative decoding is enabled ONLY for models with `usesMtp = true` — never from the capability probe alone (the probe returns true even for standard Gemma 4 and tanks CPU speed to 0.2 tok/s).
- Changed: the two "Lite" (web) catalog entries were REMOVED from the Android catalog — they are WASM/WebGPU builds without the `TF_LITE_PREFILL_DECODE` signature and can never run on the Android runtime. They became the basis of the new Medha Web project instead.
- Accepted: wake lock only during active inference (10-min cap), `START_NOT_STICKY`, `allowBackup=false`, OkHttp logging `Level.NONE` (was leaking the API key in URLs).
- Accepted: vendor the web runtimes locally in the web app rather than CDN imports (offline-first + reliable wasm paths).
- Deferred (pending your go-ahead): committing `feat/mtp-speculative-decoding`; next Android batch = process-death restore, onTrimMemory, theme toggle, history search/rename; large items (ChatViewModel refactor, release signing, crash reporting) need your decisions.
- Standing rule: commits attributed only to ashokvarmamatta — never a Claude co-author trailer.

**Features added:**
- Gemma 4 E2B MTP catalog model (community build, `usesMtp`, MTP badge, 8k context)
- STOP generation button (works mid-stream, saves partial reply)
- Regenerate last response
- Copy button on every AI message (+ selectable text)
- Markdown rendering in chat (new MarkdownText component: code blocks w/ copy, headings, lists, links…)
- Context window viewer bottom sheet (Claude-desktop style segmented bar + legend)
- Context % pill in chat header (red ≥85%), tap title → full sheet
- Back-press: in-chat → clears to home; home → exits
- Auto-scroll during streaming + scroll-to-bottom FAB
- Download integrity: free-space precheck, byte verify before rename, temp-file sweep, download error surfaced
- Online history cap (30 messages) + online chats now saved to history
- Image downscale to 1024px JPEG before base64 (was sending full-size)
- Wake-lock scoped to active inference only
- Friendly engine-error messages (e.g. web-build model → "pick a different model")

**Bugs / issues fixed:**
- Lite model fails: `NOT_FOUND TF_LITE_PREFILL_DECODE` → cause: "-web" builds are WASM-only → fix: removed from catalog + friendly error [file: data/ModelCatalog.kt]
- MTP regression 0.2 tok/s on STANDARD model → cause: capability probe enables speculative decoding for all Gemma 4 → fix: gate on `catalogModel?.usesMtp == true`; standard E2B back to 1.9 tok/s [file: ChatViewModel.kt]
- Couldn't stop a runaway generation → cause: no stop path existed → fix: `generationJob` + `stopGeneration()` (cancelProcess + job cancel + save partial) [file: ChatViewModel.kt]
- Online chats missing from history → fix: added `saveCurrentChatSession()` on the online success path [file: ChatViewModel.kt]
- API key leaked to logcat → cause: OkHttp `Level.BASIC` logs full URL → fix: `Level.NONE` [file: ChatViewModel.kt]
- MarkdownText wouldn't compile → cause: `*/` inside KDoc (`*italic*/_italic_`) closed the comment → fix: reworded comment [file: presentation/components/MarkdownText.kt]
- MIUI `INSTALL_FAILED_USER_RESTRICTED` → enable "Install via USB" + accept on-device prompt (procedure, no code)

**UI / UX changes:**
- Chat header: tappable title + context % pill + MTP indicator in subtitle
- AI bubbles render markdown; user bubbles selectable; action row (Copy / Regenerate) under messages
- Streaming bubble + auto-scroll only when near bottom; FAB to jump down
- Context sheet with segmented usage bar and legend

**Behavior / logic changes:**
- Engine init: context = catalog `maxContext` (32K default), OOM-halving retry down to 2048; MTP probe via `Capabilities(path).hasSpeculativeDecodingSupport()` only for `usesMtp` models; `ExperimentalFlags.enableSpeculativeDecoding` set accordingly
- All engine-teardown paths (`selectModel`, `setAppMode`, `applyConfig`, session load/delete, new chat) first `cancelActiveGeneration()` (cancelProcess + cancelAndJoin)
- Both send paths: `MedhaService.inferenceOn/Off` around generation; CancellationException rethrown before generic catch; `generationJob` cleared via `coroutineContext[Job]` identity check
- MedhaService: wake lock only between INFERENCE_ON/OFF actions, 10-min timeout, `START_NOT_STICKY`

**Code changes — files:**
### app/src/main/java/.../data/ModelCatalog.kt — edited
Added `remoteFileName`/`usesMtp` fields; removed 2 broken Lite(web) entries; added Gemma 4 E2B MTP entry.
```kotlin
CatalogModel(
    id = "gemma-4-e2b-mtp", name = "Gemma 4 E2B MTP",
    description = "Experimental. Multi-token prediction (speculative decoding) for faster generation. Community build, text only.",
    sizeBytes = 2_584_805_376L,
    fileName = "gemma-4-E2B-it-128k-mtp.litertlm",
    remoteFileName = "model.litertlm",
    huggingFaceRepo = "metricspace/gemma4-E2B-it-litert-128k-mtp",
    supportsThinking = true, usesMtp = true,
    maxContext = 8192, minRamGb = 8, badge = "MTP",
    accelerators = listOf("cpu", "gpu"),
    taskTypes = listOf("llm_chat", "llm_prompt_lab")
)
```
### app/src/main/java/.../domain/model/ChatState.kt — edited
Added `offlineContextLength: Int = 0`, `offlineMtpActive: Boolean = false`, `downloadError: String? = null`.
### app/src/main/java/.../presentation/screens/chat/ChatViewModel.kt — edited (core, many edits)
MTP gating, stopGeneration/cancelActiveGeneration, regenerateLastResponse, download integrity + sweep, online history cap + session save, OkHttp logging off, image downscale, inferenceOn/Off wiring. Key MTP gate:
```kotlin
val mtpActive = if (modelToLoad.isLiteRtFormat && catalogModel?.usesMtp == true) {
    try { Capabilities(modelFile.absolutePath).use { it.hasSpeculativeDecodingSupport() } }
    catch (e: Exception) { addLog(...); false }
} else false
ExperimentalFlags.enableSpeculativeDecoding = mtpActive
```
### app/src/main/java/.../presentation/screens/chat/ChatScreen.kt — edited (many edits)
BackHandler (in-session → startNewChat/exitGrandMaster, else exit), header ctx pill + tappable title, MessageBubble(isLast) with MarkdownText/SelectionContainer/Copy/Regenerate, streaming bubble markdown, auto-scroll + scroll-down FAB, ContextWindowSheet (segmented bar, estimateTokens = chars/4, online total 1,048,576).
### app/src/main/java/.../presentation/components/MarkdownText.kt — created
Dependency-free Compose markdown renderer: fenced code (copy button), headings, lists, quotes, hr, inline bold/italic/code/strikethrough/links; try/catch fallback to plain Text.
### app/src/main/java/.../service/MedhaService.kt — edited
`inferenceOn/Off` companion + ACTION_INFERENCE_ON/OFF; wake lock only while inferring, `WAKE_LOCK_TIMEOUT_MS = 10*60*1000L`; `START_NOT_STICKY`.
### app/src/main/AndroidManifest.xml — edited
`android:allowBackup="false"`.
### (sibling project) D:\Ashok\olama\projects\AiwebOffline — created
Full offline-AI web app using the web-only models — see its own `PROJECT_MEMORY.md` for the complete file-by-file log.

**Files changed (handoff):**
- data/ModelCatalog.kt — MTP entry added, web-only Lite entries removed, remoteFileName/usesMtp fields
- domain/model/ChatState.kt — context length / MTP active / download error state
- presentation/screens/chat/ChatViewModel.kt — MTP gating fix, stop/cancel lifecycle, download integrity, online history cap + save, key-leak fix, image downscale
- presentation/screens/chat/ChatScreen.kt — back-press, ctx pill + sheet, markdown bubbles, copy/regenerate, auto-scroll + FAB
- presentation/components/MarkdownText.kt — NEW markdown renderer
- service/MedhaService.kt — scoped wake lock
- AndroidManifest.xml — allowBackup=false
- STILL PENDING: commit of branch `feat/mtp-speculative-decoding` (all of the above uncommitted); next batch = process-death restore, onTrimMemory, theme toggle, history search/rename; decisions needed for refactor/signing/crash-reporting.
