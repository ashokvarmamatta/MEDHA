<div align="center">

# 🧠 MEDHA

### On-Device AI with Gemma 4 — Vision, Audio, Thinking. 100% Offline.

<img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&weight=500&size=18&pause=1000&color=00D4AA&center=true&vCenter=true&width=550&lines=Gemma+4+running+on+your+phone;Vision+%E2%80%94+analyze+images+offline;Thinking+mode+%E2%80%94+chain+of+thought;32K+context+window;140%2B+languages+supported;Pick+output+language+%E2%80%94+45%2B+options;No+internet+needed;100%25+private+%E2%80%94+nothing+leaves+your+phone" />

<br/>

[![Android](https://img.shields.io/badge/Android_12+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin_2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![LiteRT LM](https://img.shields.io/badge/LiteRT_LM_0.10-FF6F00?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/edge/litert)
[![Gemini](https://img.shields.io/badge/Gemini_API-4285F4?style=for-the-badge&logo=googlegemini&logoColor=white)](https://ai.google.dev)
[![License](https://img.shields.io/badge/MIT-yellow?style=for-the-badge)](LICENSE)

</div>

---

## 🤔 Why This Project Exists

> **Can you run a real AI model — with vision and thinking — entirely on your phone?**
> Yes. MEDHA does exactly that.

Most AI apps are just API wrappers — they send your data to a cloud server and charge you for it. MEDHA takes a different approach. It runs Google's Gemma 4 model **directly on your Android phone** using the LiteRT LM SDK. Your prompts, your images, your conversations — nothing ever leaves your device.

For users who want faster or more capable responses, MEDHA also offers an **optional online mode** via Google Gemini API with multi-key failover. But the core experience is 100% offline.

> This is an actively developed project with real features, real bugs, and real limitations.

---

## 📱 What is MEDHA?

**MEDHA** (Mobile Edge Device Hybrid AI) is an Android AI chat app that runs Gemma 4 on your phone. You can ask it questions, analyze images, process audio, and use specialized AI personas — all without internet.

```
📱 You type/attach image/audio
    │
    ├── 🔒 OFFLINE ──→ 🧠 Gemma 4 (LiteRT LM on your phone)
    │                       ├── Text: CPU backend
    │                       ├── Vision: GPU backend
    │                       └── Thinking: chain-of-thought
    │
    └── ☁️ ONLINE ──→ 🌐 Gemini API (Google cloud)
                          ├── Multi-key failover
                          └── Image generation
    │
    ▼
💬 AI Response (with token stats, streaming)
```

---

## 📸 Screenshots

| Home | Settings | Model Catalog |
|:---:|:---:|:---:|
| <a href="screenshots/01_home.png"><img src="screenshots/01_home.png" width="250"/></a> | <a href="screenshots/02_settings.png"><img src="screenshots/02_settings.png" width="250"/></a> | <a href="screenshots/03_model_catalog.png"><img src="screenshots/03_model_catalog.png" width="250"/></a> |

| Online API Keys | Chat History | Offline Chat |
|:---:|:---:|:---:|
| <a href="screenshots/04_settings_online.png"><img src="screenshots/04_settings_online.png" width="250"/></a> | <a href="screenshots/05_chat_history.png"><img src="screenshots/05_chat_history.png" width="250"/></a> | <a href="screenshots/06_chat_offline.png"><img src="screenshots/06_chat_offline.png" width="250"/></a> |

| Image Analysis (Offline!) | Quick Actions | Grand Masters |
|:---:|:---:|:---:|
| <a href="screenshots/07_image_analysis.png"><img src="screenshots/07_image_analysis.png" width="250"/></a> | <a href="screenshots/08_quick_actions.png"><img src="screenshots/08_quick_actions.png" width="250"/></a> | <a href="screenshots/09_grandmasters.png"><img src="screenshots/09_grandmasters.png" width="250"/></a> |

---

## 🧠 On-Device AI (Gemma 4 + LiteRT LM) ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) ![](https://img.shields.io/badge/5_Models-7F52FF?style=flat-square)

| Feature | What it does |
|---------|-------------|
| 🔥 **Gemma 4 E2B / E4B** | Google's latest on-device model — 32K context, vision, audio, thinking |
| 👁️ **On-Device Vision** | Analyze images, OCR, describe art — fully offline via `visionBackend = GPU` |
| 🎤 **Audio Input** | Process audio files on-device with audio-capable models |
| 💭 **Thinking Mode** | Chain-of-thought reasoning — watch the model think in real-time |
| 🌐 **140+ Languages** | Pre-trained on 140+ languages, 35+ ready out-of-the-box |
| 🗣️ **Output Language Lock** | Pick from 45+ languages — AI replies ONLY in chosen language (input auto-detected) |
| ⚡ **Streaming** | Token-by-token response display as the model generates |
| 📊 **Token Stats** | Tokens, tok/s, latency, TTFT, provider badge on every response |

### 📦 Model Catalog ![](https://img.shields.io/badge/5_Models-7F52FF?style=flat-square) ![](https://img.shields.io/badge/Free-4CAF50?style=flat-square)

| Model | Size | Capabilities | Best For |
|-------|------|-------------|----------|
| 🥇 **Gemma 4 E2B** | 2.6 GB | Text + Vision + Audio + Thinking + 140 languages | Most phones (8GB+ RAM) |
| 🥈 **Gemma 4 E4B** | 3.7 GB | Text + Vision + Audio + Thinking + 140 languages | Flagship phones (12GB+ RAM) |
| 🥉 **Gemma 3n E2B** | 3.7 GB | Text + Vision + Audio | Previous gen, still solid |
| 🪶 **Gemma 3 1B** | 584 MB | Text only | Low-end phones (6GB RAM) |
| 🧪 **DeepSeek R1 1.5B** | 1.8 GB | Text + Reasoning | Reasoning tasks |

---

## ⚙️ Model Configurations ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square)

| Setting | Range | What it controls |
|---------|-------|-----------------|
| 🎚️ **Max Tokens** | 256 — 8192 | Maximum response length |
| 🎯 **TopK** | 1 — 128 | Number of top tokens to consider |
| 📊 **TopP** | 0.00 — 1.00 | Nucleus sampling threshold |
| 🌡️ **Temperature** | 0.00 — 2.00 | Creativity vs accuracy |
| 🖥️ **Accelerator** | GPU / CPU | Inference backend |
| 💭 **Enable Thinking** | On / Off | Chain-of-thought mode |
| 🌐 **Output Language** | 45+ languages | Force AI to reply in a specific language (input auto-detected) |

---

## 🎭 Grand Masters — AI Expert Personas ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) ![](https://img.shields.io/badge/4_Built--In-7F52FF?style=flat-square) ![](https://img.shields.io/badge/Custom-blue?style=flat-square)

| Persona | Specialty |
|---------|----------|
| ♟️ **Chess Master** | Openings, tactics, endgames, game analysis |
| 🧬 **Health Advisor** | Fitness, nutrition, mental wellness |
| 💻 **Code Guru** | Debug, refactor, architect, explain code |
| 🚀 **Career Coach** | Resume, interview prep, career roadmap |
| 🛠️ **Create Your Own** | Custom persona via form or JSON config |
| 📤 **Import / Export** | Share Grand Masters across devices |
| 💾 **Chat Persistence** | Resume or reset GM conversations |

---

## ⚡ Quick Actions ![](https://img.shields.io/badge/24_Templates-7F52FF?style=flat-square)

<details>
<summary>Click to expand all 24 templates</summary>

| Category | Templates |
|----------|----------|
| ✍️ **Writing** | Write Email, Fix Grammar, Translate, Rewrite Formal |
| 📊 **Analysis** | Summarize, Explain Simply, Pros & Cons, Key Takeaways |
| 💻 **Code** | Write Code, Debug, Refactor, Explain Code |
| 🔧 **Utility** | Compare, Make Plan, Quiz Me, Make List |
| 👁️ **Image** | Upload & Analyze Image, Extract Text (OCR), Describe Art Style |
| 🎤 **Audio** | Upload & Transcribe Audio, Analyze Audio |

</details>

---

## 🔑 Multi-API Key Management ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) ![](https://img.shields.io/badge/Opt--In-blue?style=flat-square)

| Feature | What it does |
|---------|-------------|
| 🔄 **Multi-Key Failover** | Add multiple Gemini keys, auto-rotates on rate limit (429/500/503) |
| ✅ **Per-Key Model Check** | Test which models work with each key |
| 🔃 **Key Reordering** | Drag to prioritize keys |
| 🔗 **cURL Paste Mode** | Paste cURL commands to extract API keys |
| 🌐 **Custom Base URL** | OpenAI-compatible proxy support |

---

## 💬 Chat Experience ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square)

| Feature | What it does |
|---------|-------------|
| 📜 **Chat History** | Room DB persistence — load, delete, clear sessions |
| 🔄 **Streaming Display** | Watch responses appear token by token |
| 💭 **Thinking Display** | See chain-of-thought in real-time (expandable) |
| 📊 **Performance Stats** | `34 tokens · 5.5 tok/s · 8.1s · TTFT 2439ms · OFFLINE` |
| 🖼️ **Image Generation** | Save AI-generated images to gallery (Gemini online) |
| 🔔 **Background Service** | Foreground service keeps model alive when screen off |

---

## 🚧 Work in Progress

| Component | Status | Notes |
|-----------|--------|-------|
| Offline Text Chat | ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) | Gemma 4 via LiteRT LM |
| On-Device Vision | ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) | Requires `visionBackend = GPU` |
| On-Device Audio | ![](https://img.shields.io/badge/%F0%9F%94%A7_WIP-FF9800?style=flat-square) | Audio backend configured, playback TBD |
| Thinking Mode | ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) | Chain-of-thought with streaming |
| Online Gemini API | ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) | Multi-key failover |
| Grand Masters | ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) | Built-in + custom + persistence |
| Chat History | ![](https://img.shields.io/badge/%E2%9C%85_Stable-4CAF50?style=flat-square) | Room DB |

> ⚠️ Found a bug? [Open an issue](https://github.com/ashokvarmamatta/MEDHA/issues)

---

## 🏗️ Tech Stack

[![Kotlin](https://img.shields.io/badge/Kotlin_2.2-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![LiteRT](https://img.shields.io/badge/LiteRT_LM-FF6F00?style=flat-square&logo=google&logoColor=white)](https://ai.google.dev/edge/litert)
[![Room](https://img.shields.io/badge/Room_DB-4CAF50?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Koin](https://img.shields.io/badge/Koin_DI-F58220?style=flat-square)](https://insert-koin.io)

| Layer | Technology |
|-------|-----------|
| 🗣️ Language | Kotlin 2.2 |
| 🎨 UI | Jetpack Compose + Material 3 |
| 🏛️ Architecture | MVVM |
| 🧠 On-Device AI | LiteRT LM 0.10.0 (Gemma 4) |
| ☁️ Cloud AI | Google Gemini API (Retrofit + Moshi) |
| 💉 DI | Koin |
| ⚡ Async | Kotlin Coroutines + Flow |
| 🗄️ Storage | Room Database + DataStore Preferences |
| 📱 Min SDK | 31 (Android 12) |
| 🎯 Target SDK | 36 |

<details>
<summary>📂 Architecture</summary>

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
   chat/            → 💬 Chat UI + streaming + history + config dialog
   settings/        → ⚙️ Mode toggle, API keys, model catalog
   about/           → ℹ️ App info
 service/
   MedhaService.kt  → 🔔 Foreground service for background inference
 di/
   AppModule.kt     → 💉 Koin DI
```

</details>

---

## 🔥 Key Technical Highlights

| Highlight | Details |
|-----------|---------|
| 🧠 **LiteRT LM Engine** | CPU for text, `visionBackend = GPU` for images. Streaming via `MessageCallback` |
| 👁️ **On-Device Vision** | Images downscaled to 512px PNG → `Content.ImageBytes` → Gemma 4 GPU vision encoder |
| 💭 **Thinking Mode** | `enable_thinking` extra context. Thought stream via `message.channels["thought"]` |
| 🔄 **Multi-Key Failover** | Tries each validated key + model combo. Auto-advances on 429/500/503 |
| 📥 **Resume Downloads** | HTTP Range requests for model downloads. Progress tracked in UI |

> ⚠️ **Critical: `visionBackend` is required for image input!** Without `visionBackend = Backend.GPU()` in `EngineConfig`, `Content.ImageBytes` causes a native SIGSEGV crash. See [GEMMA4_GUIDE.md](GEMMA4_GUIDE.md) for details.

---

## 🚀 Quick Start

```bash
git clone https://github.com/ashokvarmamatta/MEDHA.git
```

1. 📂 Open in **Android Studio** (Ladybug or later)
2. 🔄 Sync Gradle
3. 📱 Run on device (Android 12+, 8GB+ RAM recommended)

### 🔒 Offline Mode
1. Go to **Settings → Model Catalog**
2. Download **Gemma 4 E2B** (2.6 GB)
3. Start chatting — fully offline!

### ☁️ Online Mode
1. Get a free key from [aistudio.google.com](https://aistudio.google.com/apikey)
2. **Settings → Online Mode → Add API Key**
3. Test & validate, start chatting

### 📖 Full Guide
See **[GEMMA4_GUIDE.md](GEMMA4_GUIDE.md)** — complete beginner's guide to running Gemma 4 on Android.

---

<div align="center">

### 👨‍💻 Built by Matta Ashok Varma

<p align="center">
<a href="https://github.com/ashokvarmamatta"><img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white" /></a>&nbsp;
<a href="https://www.linkedin.com/in/ashokvarmamatta"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" /></a>&nbsp;
<a href="https://ashokvarmamatta.github.io/portfolio/"><img src="https://img.shields.io/badge/Portfolio-00D4AA?style=for-the-badge&logo=googlechrome&logoColor=white" /></a>
</p>

**🧠 MEDHA — Your AI. Your Device. Your Privacy.**

<sub>Built with Jetpack Compose, LiteRT LM & Gemma 4 by Google</sub>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d1117,50:00D4AA,100:7C5CFC&height=80&section=footer" width="100%"/>

</div>
