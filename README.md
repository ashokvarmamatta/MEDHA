# MEDHA - AI Chat Application (Offline + Online)

MEDHA is a premium AI chat application for Android that supports both **offline on-device inference** and **online cloud-powered AI**. Built with Kotlin, Jetpack Compose, and Material 3, it offers a rich, full-featured chat experience with image analysis, prompt templates, and more.

## Features

### Dual Mode AI
- **Offline Mode:** Run AI locally on your device using Google's MediaPipe GenAI with Gemma 2B model - no internet required
- **Online Mode:** Connect to Google Gemini API (gemini-2.0-flash) for powerful cloud-based responses
- Switch between modes seamlessly from Settings

### Image Analysis
- Upload images from your gallery using Android Photo Picker
- Analyze images with AI-powered descriptions (Online mode via Gemini Vision API)
- Extract text from images (OCR)
- Analyze artistic styles and compositions
- Image previews displayed inline in chat bubbles

### Pre-built Prompt Templates
22 ready-to-use prompt templates across 6 categories:
- **Analysis:** Summarize Text, Explain Simply, Pros & Cons, Key Takeaways
- **Writing:** Write Email, Fix Grammar, Translate, Rewrite Formal
- **Code:** Explain Code, Debug Code, Refactor Code, Write Code
- **Creative:** Brainstorm Ideas, Write Story, Write Poem
- **Utility:** Compare, Make Plan, Quiz Me, Make List
- **Image:** Analyze Image, Extract Text from Image, Describe Art Style

### Premium UI
- Custom dark/light Material 3 theme with premium color palette
- Animated status indicators and smooth transitions
- Chat bubbles with Markdown-style formatting
- Welcome screen with setup guides for each mode
- Edge-to-edge display with proper system bar handling

### Additional Screens
- **Settings:** Mode selection, API key management, offline model picker with rescan
- **Logs:** Real-time engine log viewer with color-coded severity levels
- **About:** Full app guide, feature documentation, setup instructions, privacy info

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Offline AI:** MediaPipe GenAI Tasks (`com.google.mediapipe:tasks-genai:0.10.14`)
- **Online AI:** Google Gemini REST API via Retrofit + OkHttp + Moshi
- **Image Loading:** Coil (Compose integration)
- **Navigation:** Navigation Compose with animated transitions
- **Dependency Injection:** Koin
- **Min SDK:** 24 | **Target SDK:** 35

## Setup and Installation

### 1. Clone the Repository

```bash
git clone https://github.com/ashokvarmamatta/MEDHA.git
cd MEDHA
```

### 2. Offline Mode Setup

This requires the **Gemma 2B** model file on your device.

**Download the model:**
- Model file: `gemma-2b-it-gpu-int4.bin`
- Source: [Kaggle - Google Gemma](https://www.kaggle.com/models/google/gemma/frameworks/gemma-cpp) (download the GPU-compatible `int4` version)

**Push to device:**
```bash
adb push path/to/gemma-2b-it-gpu-int4.bin /storage/emulated/0/Download/
```

The app scans the Downloads folder for compatible `.bin` model files. If multiple models are found, you can choose which one to load from Settings.

**Grant permissions:** On first launch, grant "All Files Access" when prompted - this is needed to read model files from storage.

### 3. Online Mode Setup

1. Get a free API key from [Google AI Studio](https://aistudio.google.com/apikey)
2. Open MEDHA > Settings
3. Switch to Online mode
4. Enter your Gemini API key

No model download required - responses are generated in the cloud.

### 4. Build and Run

Open the project in Android Studio and build. Requires:
- Android Studio Ladybug or newer
- JDK 17+
- Android device or emulator (API 24+)

## Project Structure

```
com.ashes.dev.works.ai.neural.brain.medha
├── data/
│   └── remote/
│       └── GeminiApi.kt          # Retrofit interface + Gemini request/response models
├── di/
│   └── AppModule.kt              # Koin dependency injection
├── domain/
│   └── model/
│       ├── AppMode.kt            # Offline/Online sealed class
│       ├── ChatState.kt          # UI state holder
│       ├── LogEntry.kt           # Log entry + severity levels
│       ├── Message.kt            # Chat message with image support
│       ├── ModelInfo.kt          # Offline model file metadata
│       ├── ModelStatus.kt        # Model lifecycle states
│       ├── PromptTemplate.kt     # 22 prompt templates + categories
│       └── User.kt               # User enum (Human/AI)
├── presentation/
│   ├── navigation/
│   │   └── NavGraph.kt           # Navigation with animated transitions
│   └── screens/
│       ├── about/
│       │   └── AboutScreen.kt    # App guide and documentation
│       ├── chat/
│       │   ├── ChatScreen.kt     # Main chat UI with image picker
│       │   └── ChatViewModel.kt  # Dual-mode chat logic
│       ├── logs/
│       │   └── LogsScreen.kt     # Real-time log viewer
│       └── settings/
│           └── SettingsScreen.kt # Mode, API key, model selection
├── ui/theme/
│   ├── Color.kt                  # Premium color palette
│   ├── Theme.kt                  # Dark/light Material 3 themes
│   └── Type.kt                   # Full typography system
├── MainActivity.kt               # Entry point with edge-to-edge
└── MedhaApplication.kt           # Koin initialization
```

## Contributing

Contributions are welcome! Open an issue or create a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
