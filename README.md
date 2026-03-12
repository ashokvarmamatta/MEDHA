<div align="center">

# MEDHA — On-Device AI Chat for Android

**Privacy-first AI chat that runs entirely on your phone. No internet, no data collection, no servers. Powered by MediaPipe + Gemma for true offline intelligence — with optional Gemini cloud API for enhanced capabilities.**

[![Android](https://img.shields.io/badge/Platform-Android%2024%2B-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=jetpack-compose)](https://developer.android.com/compose)
[![MediaPipe](https://img.shields.io/badge/Offline_AI-MediaPipe_GenAI-orange?logo=google)](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference)
[![Gemini](https://img.shields.io/badge/Online_AI-Gemini_API-4285F4?logo=google)](https://ai.google.dev)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

</div>

---

## What is MEDHA?

MEDHA (**M**obile **E**dge **D**evice **H**ybrid **A**I) is an Android AI chat app built for **offline-first** usage. Your prompts never leave your phone — the Gemma model runs entirely on-device via MediaPipe GenAI Tasks.

For users who want faster, more capable responses, MEDHA also offers an **optional online mode** via Google Gemini API — with multi-key failover, image analysis, and image generation.

```
You -> Type a message / Upload an image
         |
   +-----------------------------+
   |     MEDHA App (Android)     |
   |                             |
   |  +--- PRIMARY -----------+  |
   |  | Offline (On-Device)   |  |
   |  | MediaPipe + Gemma 2B  |  |
   |  | Zero internet needed  |  |
   |  +-----------------------+  |
   |                             |
   |  +--- SECONDARY ---------+  |
   |  | Online (Cloud API)    |  |
   |  | Gemini API + Images   |  |
   |  | Requires API key      |  |
   |  +-----------------------+  |
   +-----------------------------+
                  |
        AI Response in Chat
```

---

## Core Features

### Offline AI Chat (Primary)
- **On-device LLM** — Gemma 2B runs via MediaPipe GenAI Tasks. Your data stays on your phone
- **Zero internet required** — Works in airplane mode, no server, no cloud
- **Private by design** — No data collection, no telemetry, no accounts
- Auto-scans Downloads folder for `.bin` model files
- Conversation history preserved within Grand Master sessions

### Online AI Chat (Secondary)
- **Google Gemini API** — Connect for faster, more capable responses when needed
- **Multi-API key management** — Add multiple keys with labels, validation, and automatic failover
- **Image analysis** — Upload images for AI-powered description, OCR, and analysis
- **Image generation** — AI-generated images with save-to-gallery
- **Model selection** — Browse and pick from available Gemini models
- Switch between offline and online seamlessly from Settings

---

## Grand Master Mode

Grand Masters are **specialized AI personas** with deep expertise in specific domains. When you activate a Grand Master, the AI adopts that persona's knowledge, rules, and communication style — making conversations feel like talking to a real expert, not a generic chatbot.

### Built-in Grand Masters

| Grand Master | Expertise |
|---|---|
| **Chess Grand Master** | Opening theory, tactics, endgames, positional play, game analysis |
| **Health Grand Master** | Fitness, nutrition, mental health, wellness (with medical disclaimers) |
| **Code Grand Master** | Multi-language programming, architecture, debugging, DevOps |
| **Career Grand Master** | Career planning, interview prep, salary negotiation, personalized roadmaps |

### Create Your Own Grand Master

You can **create custom Grand Masters** with your own rules, personality, and expertise:

**Form mode** — Fill in the fields (title, system prompt, welcome message, etc.)

**JSON mode** — Paste a JSON configuration for quick setup:

```json
{
  "icon": "🎯",
  "title": "Marketing Guru",
  "subtitle": "Digital Marketing Strategist",
  "description": "SEO, social media, content strategy, analytics",
  "systemPrompt": "You are a world-class digital marketing strategist with 15 years of experience. Your expertise includes SEO, content marketing, social media strategy, paid advertising, email marketing, and analytics. Always provide data-driven advice with specific, actionable steps. When discussing strategies, include estimated timelines and KPIs. Never give vague advice — be specific about tools, platforms, and metrics.",
  "welcomeMessage": "Welcome! I'm your Marketing Guru. Tell me about your business or product, and I'll craft a tailored marketing strategy for you."
}
```

The `systemPrompt` is the most important field — it defines **exactly how the Grand Master behaves**, what rules it follows, what expertise it has, and how it responds.

### Chat Persistence

- When you exit a Grand Master session, your **chat is automatically saved**
- Next time you open the same Grand Master, you'll be asked: **Continue previous chat** or **Start fresh**
- Chat history is stored locally on your device
- Use the **Reset** button to clear saved chat and begin a new session

---

## Prompt Templates

22 one-tap prompt templates across 6 categories:

| Category | Templates |
|---|---|
| **Analysis** | Summarize Text, Explain Simply (ELI5), Pros & Cons, Key Takeaways |
| **Writing** | Write Email, Fix Grammar, Translate, Rewrite Formal |
| **Code** | Explain Code, Debug Code, Refactor Code, Write Code |
| **Creative** | Brainstorm Ideas, Write Story, Write Poem |
| **Utility** | Compare, Make Plan, Quiz Me, Make List |
| **Image** | Analyze Image, Extract Text from Image, Describe Art Style *(online only)* |

---

## UI & Design

- Custom **Material 3 dark/light theme** with premium color palette
- Animated status dots, smooth transitions, gradient accents
- Chat bubbles with timestamps, image previews, and AI-generated image display
- **Welcome screen** with Grand Master quick-access cards
- Grand Master picker bottom sheet with create/delete/resume
- Edge-to-edge display with proper system bar handling

### Screens

| Screen | Purpose |
|--------|---------|
| **Chat** | Main conversation with input bar, image picker, templates, Grand Master header |
| **Settings** | Mode toggle, API key management with test/validate, model picker, status panel |
| **Logs** | Real-time engine log viewer with color-coded severity levels |
| **About** | App guide, feature list, setup instructions, privacy info |

---

## Architecture

MEDHA follows **MVVM** architecture with **Koin** dependency injection.

```
app/src/main/java/com/ashes/dev/works/ai/neural/brain/medha/
|
+-- MainActivity.kt                          # Entry point
+-- MedhaApplication.kt                      # Koin initialization
|
+-- data/
|   +-- remote/
|   |   +-- GeminiApi.kt                     # Retrofit interface + Gemini models
|   +-- repository/
|       +-- SettingsRepository.kt            # DataStore persistence (keys, chat history, custom GMs)
|
+-- di/
|   +-- AppModule.kt                         # Koin DI module
|
+-- domain/model/
|   +-- AppMode.kt                           # Sealed class: Offline | Online
|   +-- ChatState.kt                         # Full UI state holder
|   +-- Message.kt                           # Chat message with UUID, imageUri
|   +-- GrandMaster.kt                       # Built-in Grand Master enum (4 experts)
|   +-- CustomGrandMaster.kt                 # User-created Grand Master data class
|   +-- ApiKeyEntry.kt                       # API key with validation state
|   +-- ModelInfo.kt                         # Offline model file metadata
|   +-- ModelStatus.kt                       # Sealed: Idle -> Initializing -> Ready -> Error
|   +-- PromptTemplate.kt                    # 22 templates + categories
|   +-- LogEntry.kt, User.kt
|
+-- presentation/
|   +-- navigation/
|   |   +-- NavGraph.kt                      # 4 routes with animated transitions
|   +-- screens/
|       +-- chat/
|       |   +-- ChatScreen.kt                # Chat UI, Grand Master picker, create GM sheet
|       |   +-- ChatViewModel.kt             # Dual-mode logic, GM management, chat persistence
|       +-- settings/
|       |   +-- SettingsScreen.kt            # API keys, model selection, mode toggle
|       +-- about/AboutScreen.kt
|       +-- logs/LogsScreen.kt
|
+-- ui/theme/
    +-- Color.kt, Theme.kt, Type.kt
```

---

## Getting Started

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

# 2. Open in Android Studio — File -> Open -> select the MEDHA folder

# 3. Wait for Gradle sync to complete

# 4. Connect your Android device (USB debugging ON) or start an emulator

# 5. Click Run
```

---

## Setup Guide

### Offline Mode (Primary — No Internet Required)

1. **Download the Gemma 2B model**
   - Model file: `gemma-2b-it-gpu-int4.bin`
   - Source: [Kaggle — Google Gemma](https://www.kaggle.com/models/google/gemma/frameworks/gemma-cpp)
   - Download the GPU-compatible `int4` variant for best mobile performance

2. **Push the model to your device**
   ```bash
   adb push gemma-2b-it-gpu-int4.bin /storage/emulated/0/Download/
   ```

3. **Grant storage permission** — On first launch, allow "All Files Access" when prompted

4. **Select the model** — The app auto-scans your Downloads folder for `.bin` model files

5. **Start chatting** — The model loads in ~10-30 seconds depending on your device

### Online Mode (Secondary — Gemini API)

1. **Get a free API key** from [Google AI Studio](https://aistudio.google.com/apikey)
2. Open **MEDHA -> Settings**
3. Switch to **Online** mode
4. Add your **Gemini API key** with an optional label
5. **Test & validate** the key — the app will fetch available models
6. Start chatting — responses come from Gemini API

> **Multi-key support:** Add multiple API keys. If one key hits a rate limit, MEDHA automatically fails over to the next validated key.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material Design 3 |
| **Architecture** | MVVM + StateFlow |
| **Offline AI** | MediaPipe GenAI Tasks (`tasks-genai:0.10.14`) — Gemma 2B |
| **Online AI** | Google Gemini REST API |
| **Networking** | Retrofit + OkHttp + Moshi |
| **Image Loading** | Coil (Compose integration) |
| **Navigation** | Jetpack Navigation Compose (animated transitions) |
| **DI** | Koin |
| **Storage** | DataStore Preferences (API keys, chat history, custom GMs) |
| **Permissions** | Accompanist Permissions |
| **Build** | Gradle KTS + KSP |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |

---

## Roadmap

### High Priority
- [ ] Streaming responses — show AI response token-by-token as it generates
- [ ] Multiple chat sessions — create, switch, and manage multiple conversations
- [ ] More online providers — OpenAI, Anthropic Claude, OpenRouter support

### Medium Priority
- [ ] Voice input — speech-to-text for hands-free prompting
- [ ] Export chat — share or save conversations as text/PDF
- [ ] Multi-image support — send multiple images in a single message
- [ ] Camera capture — take photos directly from the app for analysis
- [ ] Grand Master import/export — share custom Grand Masters with others

### Quality of Life
- [ ] Chat search — search through message history
- [ ] Copy/share messages — long-press to copy or share individual messages
- [ ] Home screen widget — quick-access chat widget
- [ ] Model download manager — download Gemma models directly from the app

### Advanced / Future
- [ ] RAG (Retrieval Augmented Generation) — chat with your documents
- [ ] Function calling / tool use — let the AI interact with device features
- [ ] Plugin system — extensible prompt templates and tools
- [ ] Tablet / foldable optimization — adaptive layouts for large screens

---

## Contributing

Contributions are welcome! Here's how:

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'Add my feature'`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

Please open an issue first for large changes so we can discuss the approach.

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## Acknowledgements

- [Google MediaPipe](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference) — On-device LLM inference
- [Google Gemini API](https://ai.google.dev) — Cloud AI with vision capabilities
- [Gemma Models](https://www.kaggle.com/models/google/gemma) — Open-weight LLMs by Google
- [Jetpack Compose](https://developer.android.com/compose) — Modern Android UI toolkit
- [Koin](https://insert-koin.io) — Lightweight dependency injection
- [Coil](https://coil-kt.github.io/coil/) — Image loading for Compose
- [Retrofit](https://square.github.io/retrofit/) — Type-safe HTTP client

---

<div align="center">

**Built with care by [ashokvarmamatta](https://github.com/ashokvarmamatta)**

*Your pocket AI — offline first, always private, always ready*

</div>
