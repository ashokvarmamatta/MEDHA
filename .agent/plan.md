# Project Plan: I want to build a completely offline, on-device AI chat application using the Google MediaPipe GenAI Tasks library with the Gemma 2B model.

Please write the full Kotlin code for a functional prototype.

Tech Stack:
- Language: Kotlin
- UI: Jetpack Compose (Material3)
- Architecture: MVVM (Model-View-ViewModel)
- AI Engine: com.google.mediapipe:tasks-genai

Requirements:
1.  Dependencies: List the exact implementation required in build.gradle.kts for MediaPipe GenAI.
2.  ViewModel: Create a `ChatViewModel`.
    -   It must initialize the `LlmInference` engine asynchronously on a background thread.
    -   Assume the Gemma model file (.bin) is located at this path: "/data/local/tmp/gemma-2b-it-gpu-int4.bin" (I will push the file there manually).
    -   Implement a function `sendMessage(prompt: String)` that calls `llmInference.generateResponse()`.
    -   Expose a `uiState` flow containing the list of messages and a loading state.
3.  UI: Create a `ChatScreen` composable.
    -   Use a LazyColumn to display chat history.
    -   Distinguish visually between "User" messages and "AI" messages (e.g., different alignments or colors).
    -   Include a TextField and a Send button at the bottom.
4.  Error Handling: Ensure the app doesn't crash if the model file is missing; just show an error message in the UI.

Please provide the code for:
1.  Module-level build.gradle.kts
2.  ChatViewModel.kt
3.  ChatScreen.kt

for model
instruct the app to read it from the device's external storage downloads folder

develop android app in this architecture

1.  build.gradle.kts (Module :app)
    This file contains all the necessary dependencies, including Koin, Retrofit, and Jetpack Compose.
2.  AndroidManifest.xml
    You need to declare the custom Application class here.
3.  Application Class
    This is the entry point for Koin.
4.  Koin Modules
    These files define your dependency graph.
5.  Data Layer
    These files handle network and repository logic. Like receivers,utils,repositaries.constants
6.  Domain Layer
    These are the model(classes useful for app),repository,Use Cases which contain your app's core business logic as per required.
7.  Presentation Layer
    This layer contains the UI state, the ViewModel, and the Jetpack Compose UI. for each feature
8.  MainActivity.kt
    This is the standard entry point for your Compose UI.

//my app details

app name MEDHA

my packagename is : com.ashes.dev.works.ai.neural.brain.medha

give from scratch dependencies,permissions,all class,screens and requirements
viewmodel class,state class,screen class and appmodule in di, use Koin injection

## Project Brief

### Features
- **On-Device AI Chat:** Core functionality for offline chat using the Gemma 2B model via the MediaPipe GenAI library.
- **Chat Interface:** A simple, clean UI built with Jetpack Compose to display the conversation history, differentiating between user and AI messages.
- **Model Loading:** The ability to load the AI model file from the device's external storage.
- **State Handling:** Display loading indicators while the AI is processing and show clear error messages if the model fails to load.

### High-Level Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose with
 Material 3
- **Asynchronous Operations:** Kotlin Coroutines
- **Architecture:** Model-View-ViewModel (MVVM)
- **AI Engine:** MediaPipe Tasks GenAI
- **Dependency Injection:** Koin

## Implementation Steps

### Task_1_Setup: Set up the project structure, add all necessary dependencies for MediaPipe, Koin, and Jetpack Compose to the build.gradle.kts file, and configure the AndroidManifest.xml with required permissions.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - All dependencies are added to build.gradle.kts.
  - Koin is configured in a custom Application class.
  - The project builds successfully without errors.
- **StartTime:** 2026-01-12 15:35:59 IST

### Task_2_CoreLogic: Implement the data and domain layers, including the ChatViewModel. This includes initializing the MediaPipe LlmInference engine, handling model loading from external storage, and defining the logic for sending and receiving messages.
- **Status:** PENDING
- **Acceptance Criteria:**
  - ChatViewModel is created with a Koin module.
  - LlmInference engine is initialized asynchronously.
  - sendMessage function correctly calls the AI model.
  - UI state flow exposes messages, loading, and error states.

### Task_3_UI: Develop the Jetpack Compose UI for the chat screen. This includes creating a LazyColumn for the conversation, visually distinguishing between user and AI messages, and adding an input field and send button.
- **Status:** PENDING
- **Acceptance Criteria:**
  - ChatScreen composable is created and displays chat history.
  - User and AI messages have distinct visual styles.
  - Input field and send button are functional.
  - UI correctly reflects loading and error states from the ViewModel.

### Task_4_RunAndVerify: Integrate all components, run the application on an emulator or device, and perform a final verification to ensure the app is stable and meets all core requirements. The critic_agent will be instructed to manually place the Gemma model file in the device's download folder to test the full functionality.
- **Status:** PENDING
- **Acceptance Criteria:**
  - App builds and runs without crashing.
  - Sending a message to the AI and receiving a response works correctly.
  - The UI updates properly with the conversation flow.
  - A clear error is displayed if the model file is not found.

