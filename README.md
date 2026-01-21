# MEDHA - Offline AI Chat Application

MEDHA is a fully offline, on-device AI chat application for Android, built with Kotlin and Jetpack Compose. It uses Google's MediaPipe GenAI Tasks library to run the Gemma 2B model locally, allowing you to chat with an AI without an internet connection.

## 🚀 Features

- **Offline First:** All AI processing happens directly on your device.
- **On-Device AI:** Powered by the Gemma 2B model via the MediaPipe GenAI library.
- **Modern UI:** A clean, responsive chat interface built with Jetpack Compose and Material 3.
- **MVVM Architecture:** Follows a robust Model-View-ViewModel pattern.
- **Dependency Injection:** Uses Koin for managing dependencies.
- **Error Handling:** Gracefully handles model loading errors and permission issues.

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel)
- **AI Engine:** `com.google.mediapipe:tasks-genai:0.10.14`
- **Dependency Injection:** Koin

## ⚙️ Setup and Installation

To get the application running, follow these steps:

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/medha.git
cd medha
```

### 2. Download the AI Model

This application requires the **Gemma 2B** model file to function.

- **Model File:** `gemma-2b-it-gpu-int4.bin`
- **Download Link:** You can find compatible models on [Kaggle](https://www.kaggle.com/models/google/gemma/frameworks/gemma-cpp). Make sure you download the GPU-compatible `int4` version for the best performance on mobile devices.

### 3. Place the Model on Your Device

The application is hardcoded to look for the model file in the **`Downloads`** folder of your device's external storage.

1.  Connect your Android device to your computer.
2.  Use the following `adb` command to push the model file to the correct location:

    ```bash
    adb push path/to/your/gemma-2b-it-gpu-int4.bin /storage/emulated/0/Download/
    ```

    *Replace `path/to/your/` with the actual path to the downloaded model file.*

### 4. Grant Storage Permissions

On the first launch, the app will request **"All Files Access"** permission. This is required to read the `gemma-2b-it-gpu-int4.bin` model from the Downloads folder.

- A toast message will appear to guide you.
- You will be redirected to the system settings to grant the permission.

### 5. Build and Run

Open the project in Android Studio, and it should build and run without any issues.

## 🏗️ Project Structure

The project follows a standard Android MVVM architecture:

-   **`com.ashes.dev.works.ai.neural.brain.medha`**
    -   **`di`**: Contains the Koin dependency injection modules (`AppModule.kt`).
    -   **`domain`**:
        -   **`model`**: Defines the core data classes (`Message.kt`, `User.kt`, `ChatState.kt`).
    -   **`presentation`**:
        -   **`screens.chat`**: Contains the UI (`ChatScreen.kt`) and the `ChatViewModel.kt`.
    -   **`ui.theme`**: Standard Jetpack Compose theme files.
    -   **`MainActivity.kt`**: The main entry point of the application.
    -   **`MedhaApplication.kt`**: The custom `Application` class where Koin is initialized.

## 🤝 Contributing

Contributions are welcome! If you find any issues or have suggestions for improvements, please open an issue or create a pull request.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for more details.
