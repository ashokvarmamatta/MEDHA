# 🧠 MEDHA — On-Device AI with Gemma 4

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_12+-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/On--Device_AI-Gemma_4_LiteRT_LM-FF6F00?style=for-the-badge&logo=google&logoColor=white" />
  <img src="https://img.shields.io/badge/Cloud_AI-Gemini_API-4285F4?style=for-the-badge&logo=googlegemini&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" />
</p>

<p align="center">
  <b>Run Gemma 4 on your phone. Vision. Audio. Thinking. 100% offline.</b><br/>
  <sub>Privacy-first AI chat with on-device Gemma 4 via LiteRT LM — plus optional Gemini cloud API for enhanced capabilities.</sub>
</p>

---

## 📸 Screenshots

<p align="center">
  <img src="screenshots/01_home.png" width="200" />
  <img src="screenshots/02_settings.png" width="200" />
  <img src="screenshots/03_model_catalog.png" width="200" />
  <img src="screenshots/04_settings_online.png" width="200" />
</p>
<p align="center">
  <img src="screenshots/05_chat_history.png" width="200" />
  <img src="screenshots/06_chat_offline.png" width="200" />
  <img src="screenshots/07_image_analysis.png" width="200" />
  <img src="screenshots/08_quick_actions.png" width="200" />
</p>
<p align="center">
  <img src="screenshots/09_grandmasters.png" width="200" />
</p>

---

## ✨ Features

### 🧠 Gemma 4 On-Device AI (LiteRT LM)
> No internet. No API keys. No data leaves your phone.
- 🔥 **Gemma 4 E2B/E4B** — Google's latest on-device model with 32K context
- 👁️ **Vision** — Analyze images, extract text (OCR), describe art styles — fully offline with `visionBackend = GPU`
- 🎤 **Audio** — Process audio files on-device
- 💭 **Thinking Mode** — Chain-of-thought reasoning with visible thought process
- ⚡ **Streaming** — Real-time token-by-token response display
- 📊 **Token Stats** — Tokens, tok/s, latency, TTFT shown on every response

### 📦 Model Catalog
> Download and manage models directly in the app.
- 🥇 **Gemma 4 E2B** (2.6 GB) — Vision + Audio + Thinking. Best all-rounder.
- 🥈 **Gemma 4 E4B** (3.7 GB) — Larger, smarter. Needs 12GB RAM.
- 🥉 **Gemma 3n E2B** (3.7 GB) — Previous gen, vision + audio.
- 🪶 **Gemma 3 1B** (584 MB) — Tiny & fast, text only.
- 🧪 **DeepSeek R1 1.5B** (1.8 GB) — Reasoning model.
- 📥 Download with resume support from HuggingFace
- 📂 SAF-based import — no storage permissions needed

### ⚙️ Model Configurations
> Tune the model like a pro.
- 🎚️ **Max Tokens** — 256 to 8192
- 🎯 **TopK / TopP / Temperature** — Fine-tune creativity and accuracy
- 🖥️ **Accelerator** — Switch between GPU and CPU
- 💭 **Enable Thinking** — Toggle chain-of-thought mode
- Applied instantly with engine reload

### 🔀 Dual Mode — Offline + Online
> One app. Two brains. Switch in one tap.
- 🔒 **Offline** — Gemma 4 via LiteRT LM. Zero internet. Zero data leaks.
- ☁️ **Online** — Gemini API (Flash, Pro, etc.) for cloud-powered responses.
- 🔄 **Seamless** — Toggle in Settings. Chat preserved.

### 🎭 Grand Masters — AI Expert Personas
> Not just prompts. Personalities with memory.
- ♟️ **Chess Master** — Openings, tactics, endgames, game analysis
- 🧬 **Health Advisor** — Fitness, nutrition, mental wellness
- 💻 **Code Guru** — Debug, refactor, architect, explain code
- 🚀 **Career Coach** — Resume, interview prep, career roadmap
- 🛠️ **Create Your Own** — Custom Grand Masters with JSON import/export
- 💾 **Chat Persistence** — Resume or reset Grand Master conversations

### ⚡ 24 Quick Action Templates
> One tap. Instant prompt.
- ✍️ **Writing** — Emails, grammar fix, translate, rewrite formal
- 📊 **Analysis** — Summarize, explain simply, pros & cons, key takeaways
- 💻 **Code** — Write, debug, refactor, explain code
- 🔧 **Utility** — Compare, plan, quiz, make list
- 👁️ **Image** — Upload & analyze, extract text (OCR), describe art style — with VISION badge
- 🎤 **Audio** — Upload & transcribe, analyze audio — with AUDIO badge

### 🔑 Multi-API Key Management
> Enterprise-grade key failover.
- 🔄 **Multi-Key Failover** — Add multiple keys, auto-rotates on rate limit
- ✅ **Per-Key Model Check** — Test which models work with each key
- 🔃 **Key Reordering** — Drag to prioritize keys
- 🔗 **cURL Paste Mode** — Paste cURL commands to extract keys
- 📊 **Custom Base URL** — OpenAI-compatible proxy support

### 💬 Chat Experience
> Conversations that feel alive.
- 📜 **Chat History** — Room DB persistence with sessions, load/delete
- 🔄 **Streaming Display** — Watch responses appear token by token
- 💭 **Thinking Display** — See the model's chain-of-thought in real-time
- 📊 **Performance Stats** — Tokens, tok/s, latency, TTFT, provider badge
- 🖼️ **Image Generation** — Save AI-generated images to gallery (Gemini)
- 🔔 **Background Service** — Keep model alive when screen is off

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| 🗣️ **Language** | Kotlin 2.2 |
| 🎨 **UI** | Jetpack Compose + Material 3 |
| 🏛️ **Architecture** | MVVM |
| 🧠 **On-Device AI** | LiteRT LM 0.10.0 + Gemma 4 |
| ☁️ **Cloud AI** | Google Gemini API (Retrofit + Moshi) |
| 💉 **DI** | Koin |
| ⚡ **Async** | Kotlin Coroutines + Flow |
| 🗄️ **Storage** | Room Database + DataStore Preferences |
| 📱 **Min SDK** | 31 (Android 12) |
| 🎯 **Target SDK** | 36 (Android 16) |

---

## 🧬 Architecture

```
app/
 data/
   local/           → 💾 Room ChatDatabase (sessions + messages)
   remote/          → ☁️ Gemini API (Retrofit + Moshi)
   repository/      → 📂 SettingsRepository (DataStore)
   ModelCatalog.kt  → 📦 Model catalog with download URLs
 domain/model/
   ChatState.kt     → 🔄 UI state
   Message.kt       → 💬 Chat message with token stats
   GrandMaster.kt   → 🎭 Built-in AI personas
   ModelInfo.kt     → 📄 Model metadata + capabilities
   ApiKeyEntry.kt   → 🔑 Multi-key management
 presentation/screens/
   chat/
     ChatScreen.kt     → 💬 Main chat UI + streaming + history
     ChatViewModel.kt  → 🧠 LiteRT LM engine + Gemini API + GM logic
   settings/
     SettingsScreen.kt → ⚙️ Mode toggle, API keys, model catalog
   about/
     AboutScreen.kt    → ℹ️ App info
 service/
   MedhaService.kt  → 🔔 Foreground service for background inference
 di/
   AppModule.kt     → 💉 Koin DI
```

---

## 🔥 Key Technical Highlights

- 🧠 **LiteRT LM Engine** — Gemma 4 runs on-device with CPU for text and `visionBackend = GPU` for image analysis. Streaming via `MessageCallback` with `sendMessageAsync`.
- 👁️ **On-Device Vision** — Images downscaled to 512px PNG, sent via `Content.ImageBytes` with GPU vision backend. No cloud needed.
- 💭 **Thinking Mode** — `enable_thinking` extra context enables chain-of-thought. Thinking text streamed via `message.channels["thought"]`.
- 🔄 **Multi-Key Failover** — Tries each validated key with its selected models. On 429/500/503, moves to next key+model combo automatically.
- 🎭 **Grand Master Persistence** — Chat history saved per GM via DataStore. Resume or reset on re-entry.
- 📊 **Token Stats** — Real tokens counted during streaming, tok/s calculated from wall clock, TTFT from first token arrival.
- 📥 **Model Download** — HTTP range requests for resume support. Progress tracked in UI.

> ⚠️ **Critical: `visionBackend` is required for image input!** Without `visionBackend = Backend.GPU()` in `EngineConfig`, `Content.ImageBytes` causes a native SIGSEGV crash. See [GEMMA4_GUIDE.md](GEMMA4_GUIDE.md) for details.

---

## 🚀 Quick Start

```bash
git clone https://github.com/ashokvarmamatta/MEDHA.git
```

1. Open in **Android Studio** (Ladybug or later)
2. Sync Gradle
3. Run on device (Android 12+, 8GB+ RAM recommended)

### Offline Mode
1. Go to **Settings → Model Catalog**
2. Download **Gemma 4 E2B** (2.6 GB)
3. Start chatting — fully offline!

### Online Mode
1. Get a free API key from [aistudio.google.com](https://aistudio.google.com/apikey)
2. Go to **Settings → Online Mode → Add API Key**
3. Test & validate, then start chatting

### 📖 Full Guide
See **[GEMMA4_GUIDE.md](GEMMA4_GUIDE.md)** — complete beginner's guide to running Gemma 4 on Android with LiteRT LM.

---

## 👨‍💻 Author

**Matta Ashok Varma** — Senior Android Developer

[![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/ashokvarmamatta)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=flat-square&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/ashokvarmamatta)
[![Portfolio](https://img.shields.io/badge/Portfolio-00D4AA?style=flat-square&logo=googlechrome&logoColor=white)](https://ashokvarmamatta.github.io/portfolio/)

---

<p align="center">
  <sub>Built with Jetpack Compose, LiteRT LM & Gemma 4 by Google</sub><br/>
  <sub>⭐ Star this repo if MEDHA helped you!</sub>
</p>
