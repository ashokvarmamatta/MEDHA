<div align="center">

# 🧠 MEDHA — AI Chat for Android

**A premium dual-mode AI chat app — run AI offline on your device with MediaPipe + Gemma, or connect to Google Gemini cloud API. Image analysis, prompt templates, and a stunning Material 3 UI.**

[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=jetpack-compose)](https://developer.android.com/compose)
[![MediaPipe](https://img.shields.io/badge/Offline_AI-MediaPipe_GenAI-orange?logo=google)](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference)
[![Gemini](https://img.shields.io/badge/Online_AI-Gemini_API-4285F4?logo=google)](https://ai.google.dev)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

</div>

---

## 📖 What is MEDHA?

MEDHA (**M**obile **E**dge **D**evice **H**ybrid **A**I) is an Android-native AI chat application that gives you **two ways to talk to AI** — completely offline using on-device inference, or online through Google's Gemini cloud API. No subscriptions, no data collection, no server required for offline mode.

```
You → Type a message / Upload an image
         ↓
   ┌─────────────────────────────┐
   │     MEDHA App (Android)     │
   │                             │
   │  ┌─────────┐ ┌───────────┐ │
   │  │ Offline  │ │  Online   │ │
   │  │ MediaPipe│ │ Gemini API│ │
   │  │ Gemma 2B │ │ 2.0-flash │ │
   │  └────┬─────┘ └─────┬─────┘ │
   │       └──────┬───────┘       │
   └──────────────┼───────────────┘
                  ↓
        AI Response in Chat
```

**Offline mode** — Your prompt never leaves your phone. The Gemma 2B model runs entirely on-device via MediaPipe GenAI Tasks.

**Online mode** — Send prompts (and images) to Google Gemini 2.0 Flash via REST API for faster, more capable responses.

---

## ✨ Features

### 🤖 Dual-Mode AI Chat
- **Offline Mode** — On-device LLM inference using MediaPipe GenAI + Gemma 2B model. Zero internet required
- **Online Mode** — Google Gemini API (gemini-2.0-flash) for powerful cloud responses
- Switch between modes seamlessly from the Settings screen
- Real-time model status indicators (Initializing → Ready → Error)

### 🖼️ Image Analysis
- **Upload images** from gallery using Android Photo Picker
- **AI-powered image analysis** — describe, analyze, and extract insights from any image
- **OCR** — extract and transcribe text from images
- **Art style analysis** — identify artistic techniques, colors, and composition
- Inline image previews in chat bubbles via Coil

### 📋 22 Pre-built Prompt Templates
One-tap prompt templates organized across 6 categories:

| Category | Templates |
|---|---|
| 📊 **Analysis** | Summarize Text, Explain Simply (ELI5), Pros & Cons, Key Takeaways |
| ✍️ **Writing** | Write Email, Fix Grammar, Translate, Rewrite Formal |
| 💻 **Code** | Explain Code, Debug Code, Refactor Code, Write Code |
| 🎨 **Creative** | Brainstorm Ideas, Write Story, Write Poem |
| 🔧 **Utility** | Compare, Make Plan, Quiz Me, Make List |
| 🖼️ **Image** | Analyze Image, Extract Text from Image, Describe Art Style |

### 🎨 Premium UI
- Custom **Material 3 dark/light theme** with premium color palette
- Animated status dots, smooth screen transitions, gradient accents
- Chat bubbles with timestamps and image previews
- **Welcome screen** with mode-specific setup guides
- Edge-to-edge display with proper system bar handling
- Bottom sheet for prompt template selection

### 📱 Additional Screens
- **⚙️ Settings** — Mode toggle (Online/Offline), API key input with show/hide, offline model picker with rescan
- **📋 Logs** — Real-time engine log viewer with color-coded severity levels (DEBUG, INFO, WARNING, ERROR)
- **ℹ️ About** — Full app guide, feature list, setup instructions, tips, limitations, and privacy info

---

## 📸 Screens

| Chat | Settings | Logs | About |
|------|----------|------|-------|
| Main chat with image upload, templates, mode badge | Mode selection, API key, model picker | Color-coded real-time logs | App guide & documentation |

---

## 🏗️ Architecture

MEDHA follows **MVVM (Model-View-ViewModel)** architecture with **Koin** dependency injection.

```
app/src/main/java/com/ashes/dev/works/ai/neural/brain/medha/
│
├── MainActivity.kt                          # Entry point — edge-to-edge, nav host
├── MedhaApplication.kt                      # Koin initialization
│
├── data/
│   └── remote/
│       └── GeminiApi.kt                     # Retrofit interface + Gemini request/response models
│                                            #   (GeminiRequest, GeminiPart, InlineData for images)
│
├── di/
│   └── AppModule.kt                         # Koin DI module — ViewModel provider
│
├── domain/
│   └── model/
│       ├── AppMode.kt                       # Sealed class: Offline | Online
│       ├── ChatState.kt                     # Full UI state holder
│       ├── LogEntry.kt                      # Log entry data class + LogLevel enum
│       ├── Message.kt                       # Chat message with UUID, imageUri support
│       ├── ModelInfo.kt                     # Offline model file metadata + display name
│       ├── ModelStatus.kt                   # Sealed class: Idle → Initializing → Ready → Error
│       ├── PromptTemplate.kt                # 22 templates + TemplateCategory enum
│       └── User.kt                          # User enum (Human / AI)
│
├── presentation/
│   ├── navigation/
│   │   └── NavGraph.kt                      # 4 routes with animated slide transitions
│   │
│   └── screens/
│       ├── about/
│       │   └── AboutScreen.kt               # App guide, features, setup docs, privacy
│       ├── chat/
│       │   ├── ChatScreen.kt                # Main chat UI — input bar, image picker,
│       │   │                                #   template sheet, welcome screen, bubbles
│       │   └── ChatViewModel.kt             # Dual-mode logic — offline (MediaPipe) +
│       │                                    #   online (Gemini API), image encoding, model scan
│       ├── logs/
│       │   └── LogsScreen.kt                # Real-time log viewer, auto-scroll, timestamps
│       └── settings/
│           └── SettingsScreen.kt            # Mode radio, API key, model picker, status panel
│
└── ui/theme/
    ├── Color.kt                             # Premium palette — accents, gradients, chat colors
    ├── Theme.kt                             # Dark/light Material 3 color schemes
    └── Type.kt                              # Full 12-style typography system
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 21+
- Android device or emulator running **Android 7.0 (API 24)+**
- For offline mode: a compatible Gemma model file
- For online mode: a Google Gemini API key (free)

### Build & Run

```bash
# 1. Clone the repo
git clone https://github.com/ashokvarmamatta/MEDHA.git
cd MEDHA

# 2. Open in Android Studio
#    File → Open → select the MEDHA folder

# 3. Wait for Gradle sync to complete

# 4. Connect your Android device (USB debugging ON) or start an emulator

# 5. Click ▶ Run
```

---

## ⚙️ Setup Guide

### 🔌 Offline Mode (No Internet Required)

1. **Download the Gemma 2B model**
   - Model file: `gemma-2b-it-gpu-int4.bin`
   - Source: [Kaggle — Google Gemma](https://www.kaggle.com/models/google/gemma/frameworks/gemma-cpp)
   - Download the GPU-compatible `int4` variant for best mobile performance

2. **Push the model to your device**
   ```bash
   adb push gemma-2b-it-gpu-int4.bin /storage/emulated/0/Download/
   ```

3. **Grant storage permission** — On first launch, allow "All Files Access" when prompted

4. **Select the model** — The app auto-scans your Downloads folder for `.bin` model files. If multiple are found, pick one from Settings

5. **Start chatting** — The model loads in ~10-30 seconds depending on your device

### ☁️ Online Mode (Gemini API)

1. **Get a free API key** from [Google AI Studio](https://aistudio.google.com/apikey)
2. Open **MEDHA → Settings**
3. Switch to **Online** mode
4. Paste your **Gemini API key**
5. Start chatting — responses come from Gemini 2.0 Flash

> **Tip:** Online mode also supports image analysis. Upload a photo and the AI will describe, analyze, or extract text from it.

---

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material Design 3 |
| **Architecture** | MVVM + StateFlow |
| **Offline AI** | MediaPipe GenAI Tasks (`tasks-genai:0.10.14`) — Gemma 2B |
| **Online AI** | Google Gemini REST API (gemini-2.0-flash) |
| **Networking** | Retrofit + OkHttp + Moshi |
| **Image Loading** | Coil (Compose integration) |
| **Navigation** | Jetpack Navigation Compose (animated transitions) |
| **DI** | Koin |
| **Storage** | Room + DataStore Preferences |
| **Permissions** | Accompanist Permissions |
| **Build** | Gradle KTS + KSP |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |

---

## 🛣️ Roadmap

### 🔴 High Priority
- [ ] **Conversation persistence** — save chat history to Room database across sessions
- [ ] **Streaming responses** — show AI response token-by-token as it generates
- [ ] **Multiple chat sessions** — create, switch, and manage multiple conversations
- [ ] **More online providers** — add OpenAI, Anthropic Claude, OpenRouter support

### 🟡 Medium Priority
- [ ] **Voice input** — speech-to-text for hands-free prompting
- [ ] **Export chat** — share or save conversations as text/PDF
- [ ] **Custom system prompts** — configure the AI's persona from Settings
- [ ] **Multi-image support** — send multiple images in a single message
- [ ] **Camera capture** — take photos directly from the app for analysis

### 🟢 Quality of Life
- [ ] **Chat search** — search through message history
- [ ] **Copy/share messages** — long-press to copy or share individual messages
- [ ] **Notification replies** — reply to ongoing conversations from notifications
- [ ] **Home screen widget** — quick-access chat widget
- [ ] **Model download manager** — download Gemma models directly from the app

### 🔵 Advanced / Future
- [ ] **RAG (Retrieval Augmented Generation)** — chat with your documents
- [ ] **Function calling / tool use** — let the AI interact with device features
- [ ] **Plugin system** — extensible prompt templates and tools
- [ ] **Tablet / foldable optimization** — adaptive layouts for large screens
- [ ] **Wear OS companion** — quick prompts from your wrist

---

## 🤝 Contributing

Contributions are welcome! Here's how:

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'Add my feature'`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

Please open an issue first for large changes so we can discuss the approach.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- [Google MediaPipe](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference) — On-device LLM inference
- [Google Gemini API](https://ai.google.dev) — Cloud AI with vision capabilities
- [Gemma Models](https://www.kaggle.com/models/google/gemma) — Open-weight LLMs by Google
- [Jetpack Compose](https://developer.android.com/compose) — Modern Android UI toolkit
- [Koin](https://insert-koin.io) — Lightweight dependency injection
- [Coil](https://coil-kt.github.io/coil/) — Image loading for Compose
- [Retrofit](https://square.github.io/retrofit/) — Type-safe HTTP client

---

<div align="center">

**Built with ❤️ by [ashokvarmamatta](https://github.com/ashokvarmamatta)**

*Your pocket AI — online or offline, always ready*

</div>
