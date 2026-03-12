package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiApiService
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiContent
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiPart
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiRequest
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GenerationConfig
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.InlineData
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.AppMode
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogLevel
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class ChatViewModel(private val application: Application) : ViewModel() {

    companion object {
        private const val TAG = "MedhaEngine"
        const val MAX_TOKENS = 1024
        const val APP_VERSION = "1.0.0"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
        val SUPPORTED_EXTENSIONS = listOf(".bin")
    }

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    private var llmInference: LlmInference? = null
    private var geminiApi: GeminiApiService? = null

    init {
        addLog(LogLevel.INFO, TAG, "MEDHA AI Engine starting...")
        initGeminiApi()
        scanAvailableModels()
        initializeEngine()
    }

    private fun initGeminiApi() {
        try {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            geminiApi = Retrofit.Builder()
                .baseUrl(GEMINI_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GeminiApiService::class.java)

            addLog(LogLevel.INFO, TAG, "Online API client initialized")
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Failed to initialize API client: ${e.message}")
        }
    }

    fun scanAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists() || !downloadsDir.canRead()) {
                    addLog(LogLevel.WARNING, TAG, "Cannot access Downloads folder")
                    return@launch
                }

                val models = downloadsDir.listFiles()
                    ?.filter { file ->
                        file.isFile && SUPPORTED_EXTENSIONS.any { file.name.endsWith(it, ignoreCase = true) }
                    }
                    ?.map { file -> ModelInfo.fromFileName(file.name, file.absolutePath, file.length()) }
                    ?: emptyList()

                _uiState.update { it.copy(availableModels = models) }

                if (models.isNotEmpty()) {
                    addLog(LogLevel.INFO, TAG, "Found ${models.size} model(s): ${models.joinToString { "${it.fileName} (${it.sizeInMb}MB)" }}")
                    if (_uiState.value.selectedModel == null) {
                        _uiState.update { it.copy(selectedModel = models.first()) }
                    }
                } else {
                    addLog(LogLevel.WARNING, TAG, "No model files (.bin) found in Downloads folder")
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Model scan failed: ${e.message}")
            }
        }
    }

    fun selectModel(model: ModelInfo) {
        if (_uiState.value.selectedModel?.filePath == model.filePath) return
        addLog(LogLevel.INFO, TAG, "Switching model to: ${model.fileName}")
        _uiState.update { it.copy(selectedModel = model) }
        if (_uiState.value.appMode is AppMode.Offline) {
            llmInference?.close()
            llmInference = null
            initializeEngine()
        }
    }

    fun setAppMode(mode: AppMode) {
        if (_uiState.value.appMode == mode) return
        addLog(LogLevel.INFO, TAG, "Switching to ${if (mode is AppMode.Online) "Online" else "Offline"} mode")
        _uiState.update { it.copy(appMode = mode) }
        when (mode) {
            is AppMode.Online -> {
                llmInference?.close()
                llmInference = null
                if (_uiState.value.apiKey.isNotBlank()) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                    addLog(LogLevel.INFO, TAG, "Online mode ready (${_uiState.value.onlineModelName})")
                } else {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key required. Set it in Settings.")) }
                }
            }
            is AppMode.Offline -> initializeEngine()
        }
    }

    fun setApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key) }
        if (key.isNotBlank() && _uiState.value.appMode is AppMode.Online) {
            _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
            addLog(LogLevel.INFO, TAG, "API key configured - Online mode ready")
        }
    }

    fun setPendingImage(uri: String?) {
        _uiState.update { it.copy(pendingImageUri = uri) }
        if (uri != null) {
            addLog(LogLevel.INFO, TAG, "Image attached for analysis")
        } else {
            addLog(LogLevel.DEBUG, TAG, "Image attachment removed")
        }
    }

    fun togglePromptTemplates() {
        _uiState.update { it.copy(showPromptTemplates = !it.showPromptTemplates) }
    }

    fun hidePromptTemplates() {
        _uiState.update { it.copy(showPromptTemplates = false) }
    }

    private fun addLog(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
        _uiState.update { state ->
            state.copy(logs = (state.logs + LogEntry(level = level, tag = tag, message = message)).takeLast(100))
        }
    }

    fun initializeEngine() {
        if (_uiState.value.appMode is AppMode.Online) {
            _uiState.update {
                it.copy(modelStatus = if (it.apiKey.isNotBlank()) ModelStatus.Ready else ModelStatus.Error("API key required"))
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(modelStatus = ModelStatus.Initializing) }

                if (_uiState.value.selectedModel == null) {
                    scanAvailableModels()
                    kotlinx.coroutines.delay(500)
                }

                val modelToLoad = _uiState.value.selectedModel
                if (modelToLoad == null) {
                    addLog(LogLevel.ERROR, TAG, "No model files found in Downloads folder")
                    _uiState.update { it.copy(modelStatus = ModelStatus.ModelNotFound) }
                    return@launch
                }

                val modelFile = File(modelToLoad.filePath)
                addLog(LogLevel.INFO, TAG, "Loading model: ${modelToLoad.fileName} (${modelToLoad.sizeInMb}MB)")

                if (!modelFile.exists()) {
                    addLog(LogLevel.ERROR, TAG, "Model file not found: ${modelFile.absolutePath}")
                    _uiState.update { it.copy(modelStatus = ModelStatus.ModelNotFound) }
                    return@launch
                }
                if (!modelFile.canRead()) {
                    addLog(LogLevel.ERROR, TAG, "Cannot read model file - permission denied")
                    _uiState.update { it.copy(modelStatus = ModelStatus.PermissionRequired) }
                    return@launch
                }

                addLog(LogLevel.INFO, TAG, "Building inference engine...")
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(MAX_TOKENS)
                    .build()

                llmInference = LlmInference.createFromOptions(application, options)
                _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                addLog(LogLevel.INFO, TAG, "Engine ready: ${modelToLoad.displayName}")

            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Init failed: ${e.message}")
                _uiState.update { it.copy(modelStatus = ModelStatus.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() && _uiState.value.pendingImageUri == null) return
        if (_uiState.value.modelStatus !is ModelStatus.Ready) {
            addLog(LogLevel.WARNING, TAG, "Cannot send - engine not ready")
            return
        }

        val imageUri = _uiState.value.pendingImageUri
        val displayText = prompt.ifBlank { "[Image attached]" }
        val userMessage = Message(text = displayText, user = User.Person, imageUri = imageUri)

        _uiState.update {
            it.copy(
                isGenerating = true,
                messages = it.messages + userMessage,
                pendingImageUri = null
            )
        }
        addLog(LogLevel.DEBUG, TAG, "User: ${displayText.take(80)}${if (imageUri != null) " [+image]" else ""}")

        when (_uiState.value.appMode) {
            is AppMode.Offline -> {
                if (imageUri != null) {
                    appendAiMessage("Image analysis is only available in Online mode. Switch to Online mode in Settings to use image features.")
                    _uiState.update { it.copy(isGenerating = false) }
                } else {
                    sendOfflineMessage(prompt)
                }
            }
            is AppMode.Online -> sendOnlineMessage(prompt, imageUri)
        }
    }

    private fun sendOfflineMessage(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                addLog(LogLevel.INFO, TAG, "Generating offline response...")
                val startTime = System.currentTimeMillis()
                val inference = llmInference
                if (inference == null) {
                    appendAiMessage("Engine lost connection. Please tap retry.")
                    _uiState.update { it.copy(isGenerating = false, modelStatus = ModelStatus.Error("Engine disconnected")) }
                    return@launch
                }

                val response = inference.generateResponse(prompt)
                val elapsed = System.currentTimeMillis() - startTime
                val clean = response?.trim()
                if (clean.isNullOrEmpty()) {
                    addLog(LogLevel.WARNING, TAG, "Empty response in ${elapsed}ms")
                    appendAiMessage("The model returned an empty response. Try rephrasing your question.")
                } else {
                    appendAiMessage(clean)
                    addLog(LogLevel.INFO, TAG, "Response in ${elapsed}ms (${clean.length} chars)")
                }
                _uiState.update { it.copy(isGenerating = false) }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Offline error: ${e.message}")
                appendAiMessage("Error: ${e.message}")
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun sendOnlineMessage(prompt: String, imageUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = _uiState.value.apiKey
                if (apiKey.isBlank()) {
                    appendAiMessage("API key not configured. Go to Settings.")
                    _uiState.update { it.copy(isGenerating = false) }
                    return@launch
                }

                val api = geminiApi ?: run {
                    appendAiMessage("API client not initialized. Restart the app.")
                    _uiState.update { it.copy(isGenerating = false) }
                    return@launch
                }

                addLog(LogLevel.INFO, TAG, "Sending to Gemini API${if (imageUri != null) " (with image)" else ""}...")
                val startTime = System.currentTimeMillis()

                // Build parts for the current message
                val currentParts = mutableListOf<GeminiPart>()

                // Add image if present
                if (imageUri != null) {
                    try {
                        val base64 = encodeImageToBase64(Uri.parse(imageUri))
                        if (base64 != null) {
                            val mimeType = getMimeType(Uri.parse(imageUri)) ?: "image/jpeg"
                            currentParts.add(GeminiPart(inlineData = InlineData(mimeType = mimeType, data = base64)))
                            addLog(LogLevel.DEBUG, TAG, "Image encoded (${base64.length / 1024}KB base64)")
                        } else {
                            addLog(LogLevel.WARNING, TAG, "Failed to encode image")
                        }
                    } catch (e: Exception) {
                        addLog(LogLevel.ERROR, TAG, "Image encoding error: ${e.message}")
                    }
                }

                // Add text
                val textPrompt = prompt.ifBlank { "Analyze this image in detail." }
                currentParts.add(GeminiPart(text = textPrompt))

                // Build contents: previous text messages + current multimodal message
                val contents = mutableListOf<GeminiContent>()
                // Add previous messages as context (text only)
                val prevMessages = _uiState.value.messages.dropLast(1) // exclude current user msg
                for (msg in prevMessages) {
                    if (msg.imageUri == null) {
                        contents.add(
                            GeminiContent(
                                role = if (msg.user is User.Person) "user" else "model",
                                parts = listOf(GeminiPart(text = msg.text))
                            )
                        )
                    }
                }
                // Add current message with parts
                contents.add(GeminiContent(role = "user", parts = currentParts))

                val request = GeminiRequest(
                    contents = contents,
                    generationConfig = GenerationConfig(maxOutputTokens = 2048, temperature = 0.7f)
                )

                val response = api.generateContent(apiKey, request)
                val elapsed = System.currentTimeMillis() - startTime

                if (response.error != null) {
                    val errMsg = response.error.message ?: "API error (code: ${response.error.code})"
                    addLog(LogLevel.ERROR, TAG, "API error: $errMsg")
                    appendAiMessage("API Error: $errMsg")
                } else {
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    if (text.isNullOrEmpty()) {
                        appendAiMessage("The API returned an empty response. Please try again.")
                    } else {
                        appendAiMessage(text)
                        addLog(LogLevel.INFO, TAG, "Online response in ${elapsed}ms (${text.length} chars)")
                    }
                }
                _uiState.update { it.copy(isGenerating = false) }

            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Online error: ${e.message}")
                appendAiMessage("Connection error: ${e.message}\n\nCheck internet and API key.")
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream = application.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Base64 encoding failed: ${e.message}")
            null
        }
    }

    private fun getMimeType(uri: Uri): String? {
        return application.contentResolver.getType(uri)
    }

    private fun appendAiMessage(text: String) {
        _uiState.update { it.copy(messages = it.messages + Message(text = text, user = User.AI)) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), pendingImageUri = null) }
        addLog(LogLevel.INFO, TAG, "Chat cleared")
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        llmInference?.close()
        llmInference = null
    }
}
