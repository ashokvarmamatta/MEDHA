# MEDHA — Role-Library Audit & Fix Pass

Branch: `fix/roles-audit` (from `feat/mtp-speculative-decoding`)
Date: 2026-08-12
Role library: `D:\Ashok\VibeCode projects\roles`
Mode: **AUDIT + fix** · Tier: **Standard** (product app, on-device AI + online API, no BaaS)

Roles applied: **11** Build/DevOps · **22** Security · **21** Accessibility · **20** Performance ·
**24** Play Compliance · **15** Compose UI · **09** UX Writer (findings only)

---

## Environment fix (role 11, "environment-first")

The build could not start at all: `JAVA_HOME` pointed at
`…\Programs\Android Studio 4\jbr`, which no longer exists. Two follow-on failures:

| Attempt | Result |
|---|---|
| `Android Studio 4\jbr` | `ERROR: JAVA_HOME is set to an invalid directory` |
| `Android Studio 2\jbr` (JDK **25**) | Gradle 8.13 rejects JDK 25 — `What went wrong: 25.0.3` |
| `~/.gradle/jdks/amazon_com_inc_-21-amd64-windows.2` (Corretto **21**) | ✅ green |

**Working build command (Windows PowerShell):**

```powershell
$env:JAVA_HOME = "C:\Users\DELL\.gradle\jdks\amazon_com_inc_-21-amd64-windows.2"
.\gradlew.bat :app:assembleDebug --console=plain
```

Gradle 8.13 supports up to JDK 24. Do **not** point `JAVA_HOME` at the Android Studio 2 JBR
(JDK 25) until the Gradle wrapper is bumped.

---

## Findings & fixes

### Role 11 — Build / DevOps

| Check | Status | Evidence | Action taken |
|---|---|---|---|
| Release shrunk + obfuscated (**gate**) | ❌ → ✅ | `app/build.gradle.kts:24` `isMinifyEnabled = false`, no `isShrinkResources` | Enabled `isMinifyEnabled`, `isShrinkResources`, `isDebuggable = false` |
| Keep rules for reflective consumers | ❌ → ✅ | `proguard-rules.pro` was the untouched template (all comments) | Wrote full rule set: Moshi (reflect + codegen), Retrofit, Room, **LiteRT-LM JNI**, Koin, coroutines |
| Lint enforced in the pipeline | ❌ → ✅ | no `lint {}` block | `abortOnError = true`, `checkReleaseBuilds = true`, html+text reports |
| Unused dependencies removed | ❌ → ✅ | `accompanist-permissions`, `play-services-location`, `camera-camera2/lifecycle/view/core` — **zero imports** in `app/src` | Removed from `build.gradle.kts` **and** `libs.versions.toml` |
| Build performance flags | ⚠️ → ✅ | `gradle.properties` had only `-Xmx2048m` | Added `org.gradle.parallel`, `org.gradle.caching`, `kotlin.incremental`; heap → 4g + 1g metaspace |
| Config cache | ✅ (deliberately off) | single monolithic `:app` | Left off with a comment — role 11 warns it costs more than it saves pre-modularisation |
| Version catalog is the single source | ✅ | no hardcoded versions in `build.gradle.kts` | — |
| kapt retired | ✅ | KSP only | — |
| compileSdk/targetSdk latest | ✅ | 36 / 36 | lint's `OldTargetApi` hint noted below |
| Java toolchain pinned | ✅ | `jvmToolchain(21)` | — |
| Debug/release side-by-side | ⚠️ **deliberately skipped** | — | See "Decisions" — the release workflow ships the **debug** APK, so an `applicationIdSuffix` would orphan every existing install |

### Role 22 — Security

| Check | Status | Evidence | Action taken |
|---|---|---|---|
| No cleartext traffic (**gate**) | ❌ → ✅ | manifest had no `usesCleartextTraffic` | `android:usesCleartextTraffic="false"` on `<application>` |
| Release minified/obfuscated (**gate**) | ❌ → ✅ | same as role 11 | see above |
| Credentials Keystore-backed | ❌ → ✅ | `SettingsRepository.serializeKeys()` wrote the Gemini API key **verbatim** into the DataStore JSON | New `data/local/KeystoreCrypto.kt` — AES-256/GCM with a hardware-backed `AndroidKeyStore` key; keys encrypted on write, decrypted on read, legacy plaintext still readable and re-encrypted on next save |
| Backup excludes tokens/PII | ⚠️ → ✅ | `data_extraction_rules.xml` + `backup_rules.xml` were unedited templates; `allowBackup=false` blocks cloud backup but **not** device-to-device transfer | Both files now exclude `file`/`database`/`sharedpref`/`external` from cloud-backup **and** device-transfer |
| No PII in release logs | ❌ → ✅ | `addLog()` mirrors prompts into `Log.d/Log.i` — `"User: <prompt text>"` (`ChatViewModel.kt:868`) | `-assumenosideeffects` strips `Log.d/v/i` in release; `Log.e/w` kept for crash triage |
| Exported surface minimal | ✅ | only `MainActivity` (launcher); service `exported="false"` | — |
| API key not logged by the HTTP client | ✅ | `HttpLoggingInterceptor` pinned to `Level.NONE` with a comment explaining the `?key=` leak | — |
| No trust-all TrustManager / WebView | ✅ | none present | — |

### Role 24 — Play Compliance

| Check | Status | Evidence | Action taken |
|---|---|---|---|
| `specialUse` FGS subtype declared | ❌ → ✅ | `<service … foregroundServiceType="specialUse">` with **no** subtype property — Play rejects this at review | Added `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` with a written justification |
| Permission set minimal | ✅ | merged manifest: INTERNET, FOREGROUND_SERVICE(+SPECIAL_USE), WAKE_LOCK, POST_NOTIFICATIONS — all used | — |
| Runtime notification permission | ❌ → ✅ | `POST_NOTIFICATIONS` declared but never requested → the foreground-service notification is silently suppressed on Android 13+ | `MainActivity` now requests it once on launch (denial non-fatal) |

### Role 21 — Accessibility

| Check | Status | Evidence | Action taken |
|---|---|---|---|
| Touch targets ≥48dp (**gate**) | ❌ → ✅ | `IconButton(… Modifier.size(32.dp))` ×6 (`ChatScreen` 988/993/1955/2063, `SettingsScreen` 1043/1551); message Copy/Regenerate at 40dp; key-reorder arrows at 24dp | All 32dp/40dp buttons → **48dp**; reorder arrows 24dp → **40dp** (a 48dp pair would dominate the key card). Glyph sizes unchanged — only the tappable area grew |
| Actionable elements labelled | ✅ | icons use the positional `contentDescription` arg ("New chat", "Regenerate", "Copy message", …) | — |
| TalkBack traversal / headings / liveRegion | ⚠️ not verified | requires a device | Logged as outstanding |
| Contrast + fontScale 2.0 | ⚠️ not verified | requires a device | Logged as outstanding |

### Role 20 / 15 — Performance & Compose

| Check | Status | Evidence | Action taken |
|---|---|---|---|
| No O(n) work per recomposition | ❌ → ✅ | `computeContextUsage(uiState)` in the top bar walks **and tokenises every message**; `streamingText` recomposes the top bar on **every streamed token** | Wrapped in `remember(messages, offlineContextLength, appMode, activeGrandMaster, activeCustomGrandMaster)` |

### Role 13 — Database

| Check | Status | Evidence | Action taken |
|---|---|---|---|
| Schema exported so migrations are possible | ❌ → ✅ | `ChatDatabase.kt:86` `exportSchema = false` — no schema history at all, so no `Migration(1, 2)` could ever be written | `exportSchema = true` + `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` |
| Real migrations instead of destructive fallback | ⚠️ **left as-is, documented** | `fallbackToDestructiveMigration(true)` wipes every saved chat on a schema change | Removing it *now* would crash on the next version bump instead of wiping. Correct sequence: ship this schema export, then write `Migration(1, 2)` and drop the fallback in the same change |

### Correctness bugs found

| Bug | Evidence | Fix |
|---|---|---|
| A successfully loaded model could be thrown away | `MedhaService.start(application)` sat **inside** the engine-init `try`. If the app is backgrounded when loading finishes, `startForegroundService` throws `ForegroundServiceStartNotAllowedException` (API 31+) → the outer `catch` marks the engine failed and calls `destroyEngine()` | Wrapped in its own `try/catch`; a blocked service start now only logs a warning |
| Dead `SDK_INT` branches (minSdk is 31) | `MedhaService` 43 & 96, `ChatViewModel.saveGeneratedImage` 1972 & 1979 | Removed the branches and the now-unused `android.os.Build` imports |
| Redundant manifest label | `MainActivity android:label` duplicated the application label | Removed |

---

## Verification

| Command | Result |
|---|---|
| `:app:assembleDebug` (before fixes) | ✅ green (Corretto 21) |
| `:app:lintDebug` (before fixes) | **0 errors**, 52 warnings, 4 hints |
| `:app:assembleDebug :app:lintDebug` (after fixes) | ✅ **BUILD SUCCESSFUL** in 5m 42s — **0 errors**, 43 warnings, 4 hints, with `abortOnError = true` now enforcing |
| `:app:assembleRelease` (R8 + resource shrinker) | ✅ **BUILD SUCCESSFUL** in 7m 41s |

R8 output — `app/build/outputs/mapping/release/`:

| Artifact | State |
|---|---|
| `missing_rules.txt` | **not generated** — R8 resolved every reference; no blanket `-dontwarn **` was needed |
| `mapping.txt` / `seeds.txt` / `usage.txt` / `resources.txt` | produced (archive `mapping.txt` per release for crash de-obfuscation) |

**Size:** debug APK 135.0 MB → minified+shrunk release APK **59.5 MB** (−56%). A per-ABI/density
AAB would cut this further; see outstanding item 2.

Not verified — needs a physical device:
**on-device R8 smoke test** (model load via the LiteRT-LM JNI keep rules, a live Gemini call,
Moshi JSON parsing) — role 11 requires this every release, and R8 breakage only appears at
runtime. Also unverified: TalkBack pass, contrast, fontScale 2.0.

---

## Decisions

**Accepted**
- Shrink + obfuscate the release build, with hand-written keep rules rather than blanket `-dontwarn **`.
- Encrypt API keys with a Keystore-backed AES key, with transparent migration from plaintext.
- Grow touch targets to 48dp even though it changes row heights — the a11y gate is explicit.

**Shared model folder (added after the audit pass)**

MEDHA can now load a model out of a folder the user owns, so a 2.4 GB `.litertlm` that Koeyomi
already downloaded is used in place instead of downloaded a second time. Relevant because `/data`
on the test device is at 99 % (4.4 GB free) and the catalog's Gemma 4 MTP entry alone is 2.6 GB.

Koeyomi (`D:\Ashok\olama\projects\koeyomi`, `ModelStorage.kt`) solves this with
`MANAGE_EXTERNAL_STORAGE` and raw `/sdcard/AIModels` paths, on the stated grounds that LiteRT-LM
"open[s] models by absolute path through native code, so a SAF document URI cannot be used".

MEDHA does **not** take that permission — it is Play-restricted and routinely refused for apps
that are not file managers (role 24), and Koeyomi only gets away with it by not being Play-bound.
The premise is also incomplete: a document URI can't be passed, but SAF yields an open file
descriptor, and `/proc/self/fd/N` **is** a path native code can `open()` and mmap.

| Route | Works | Note |
|---|---|---|
| `content://…` as `modelPath` | ❌ | native `open()` on a URI string — what Koeyomi's comment describes |
| `/storage/emulated/0/AIModels/x.litertlm` | ⚠️ | real path, but reading a file MEDHA didn't create needs all-files access |
| `/proc/self/fd/N` from `openFileDescriptor()` | ✅ | tried second; **no manifest permission at all** |

`ExternalModelStore.openForEngine` tries the direct path first and falls back to the fd alias.
The descriptor is held in `ChatViewModel.modelHandle` and closed in `destroyEngine()` **after**
the engine — closing it first would pull the mmap out from under native code.

Interop is preserved: pointing the picker at `/sdcard/AIModels` reads exactly the files Koeyomi
writes there. Downloads still land in MEDHA's private folder — resumable multi-GB writes through
SAF buy nothing when the goal is to *use* what another app already fetched.

⚠️ **Unverified:** `/proc/self/fd` + mmap through FUSE-backed SAF is standard Linux behaviour and
is used by comparable on-device LLM apps, but LiteRT-LM's loader has not been observed doing it.
Needs one real load on a device with a model in the shared folder. Failure is handled — the user
gets "Can't open that model from the shared folder. Use Copy into MEDHA" rather than a crash.

### On-device findings (2026-08-12, Redmi 2311DRK48I, Android 16, 7.3 GB RAM)

Everything below was measured on hardware, not reasoned about. Each one had a plausible wrong
answer that inspection alone would have accepted.

| Finding | Evidence | Consequence |
|---|---|---|
| **SAF cannot feed a native path loader** | `MediaProvider: Permission to access file … is denied` once per engine-create attempt; `INVALID_ARGUMENT: Unsupported or unknown file format` | Opening `/proc/self/fd/N` is not `dup()` — Linux re-opens through the filesystem and repeats the permission check. The descriptor is valid (correct 2,588,147,712 bytes) but the loader never reads a byte. **Only two options exist: all-files access, or copy-in.** MEDHA copies in |
| Engine config was NOT the cause | GPU vision → CPU vision → GPU main → CPU main → text-only → 32768→16384→8192→4096→2048, all failing **identically in ~14 ms** | Ruled out the config; identical failure across every combination pointed at the path |
| **KV cache is allocated at first inference, not at load** | RSS 1.0 GB after load; sending "hi" drove it to **7.22 GB** over 9 s, `lowmemorykiller` killed a dozen processes | `maxNumTokens` reserves nothing up front. Allocating the model's advertised 32K context is what killed the device. Context is now `min(advertised, user setting, device budget)` |
| A SIGKILL cannot be caught | `lowmemorykiller: … critical pressure and device is low on memory` | The out-of-memory rung of the retry ladder can never run. The only defence is not over-asking |
| **LiteRT-LM GPU backend segfaults here** | `Fatal signal 11 (SIGSEGV), code 2 (SEGV_ACCERR) … tid RenderThread`, `tombstone_16` | Confirms Koeyomi's comment. GPU default is now **off**, opt-in via Configurations |
| A crash guard that clears too early is no guard | Engine create **succeeds** on GPU; the segfault lands ~0.3 s later on RenderThread | Clearing the marker in a `finally` around `Engine()` recorded nothing and every relaunch repeated the crash. It now clears only after a **generation completes** |
| **Gemma 4 E2B/E4B could never be downloaded** | Catalog 2,583,085,056 vs upstream `Content-Length` 2,588,147,712 | A correct download failed the integrity check, was deleted, retried 5× and reported as failure. The server is now authoritative; catalog size is a UI estimate only |
| Sidecars and other apps' assets listed as models | `…litertlm.vision_encoder_…_mldrift_weight_cache.bin` (0 MB) shown as selectable; Koeyomi's `voices.bin` would have been too | One `ModelInfo.isModelFile(name, size, sharedFolder)` used by both scans |
| "Model not found" for a model that was present | `scanAvailableModels(); delay(500)` — a guess, and SAF is slower than `File.listFiles()` | Init now awaits `scanModelsNow()`; voice/speech directories are skipped so the scan stays fast |

**Verified working end to end:** shared folder → copy-in → real-path load → `Response: 113 tokens,
11.2 tok/s` on CPU with a 1024-token context, no crash.

**Still constrained:** a 2468 MB model on this device leaves ~140 MB spare, so the context budget
lands at 1024. A longer window needs a smaller model (Gemma 3 1B, 584 MB) or more free RAM.

**Rejected / deliberately skipped**
- `applicationIdSuffix = ".debug"` (role 11 checklist). `.github/workflows/build-and-release.yml`
  publishes the **debug** APK as the GitHub release, so changing the debug `applicationId`
  would turn every user's installed app into a stale duplicate. Revisit once release signing exists.
- Toolchain bump (AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01, litertlm 0.16.0, KSP 2.3.11).
  Role 11 requires bumping AGP ↔ Gradle ↔ Kotlin ↔ KSP ↔ Compose **as a unit** against the
  compatibility matrix, with its own on-device verification — that is its own change, not a
  side-effect of an audit pass.
- Deleting `values/colors.xml` and `drawable/placeholder.png` (lint `UnusedResources`).
  `isShrinkResources = true` now strips them from the release artifact automatically.

---

## Outstanding — not fixed in this pass

Ranked by risk.

1. **The shipped artifact is a debug APK.** `build-and-release.yml` runs `assembleDebug` and
   attaches that APK to the GitHub release. It is `debuggable=true`, unminified, unobfuscated and
   debug-signed — so every hardening fix above applies to a build users never receive. Fix: add a
   release keystore to CI secrets, sign `assembleRelease`/`bundleRelease`, ship that.
   *(Role 22 gate — still FAIL until this is done.)*
2. **No Play App Signing / AAB.** Role 11 gate: ship an AAB, never an APK.
3. **All UI copy is hardcoded** — `strings.xml` holds a single entry while `ChatScreen`,
   `SettingsScreen`, `AboutScreen` and `LogsScreen` inline every label. Role 09 gate; blocks the
   "140+ languages" positioning at the UI level. Large mechanical refactor — worth its own branch.
4. **No test coverage** beyond the two generated stubs (role 19: unit / Compose-UI / screenshot pyramid).
5. **No Baseline Profile, no Crashlytics, no `mapping.txt` archival** (roles 11, 17).
6. **TalkBack / contrast / fontScale-2.0 passes never run on a device** (role 21 gates).
7. **52 lint warnings** remain, almost all `GradleDependency`/`NewerVersionAvailable` — see the
   toolchain-bump decision above. Lint's `OldTargetApi` hint at `build.gradle.kts:15` reflects
   SDK 37 now being visible to the tooling.
