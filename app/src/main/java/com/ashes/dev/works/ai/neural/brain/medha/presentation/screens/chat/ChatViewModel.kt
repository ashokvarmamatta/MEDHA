package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiApiService
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiContent
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiPart
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiRequest
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiResponse
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GenerationConfig
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.InlineData
import com.ashes.dev.works.ai.neural.brain.medha.data.repository.SettingsRepository
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ApiKeyEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.AppMode
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GeneratedImage
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GrandMaster
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogLevel
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplate
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class ChatViewModel(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
        loadSavedSettings()
    }

    // ==================== PERSISTENCE ====================

    private fun loadSavedSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedKeys = settingsRepository.apiKeysFlow.first()
                val savedModel = settingsRepository.selectedModelFlow.first()
                val savedMode = settingsRepository.appModeFlow.first()

                val appMode = if (savedMode == "online") AppMode.Online else AppMode.Offline

                _uiState.update {
                    it.copy(
                        apiKeys = savedKeys,
                        onlineModelName = savedModel,
                        appMode = appMode
                    )
                }

                addLog(LogLevel.INFO, TAG, "Loaded ${savedKeys.size} saved API key(s), mode: $savedMode, model: $savedModel")

                // Initialize based on saved mode
                if (appMode is AppMode.Online && savedKeys.any { it.isValidated }) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                    addLog(LogLevel.INFO, TAG, "Online mode restored with ${savedKeys.count { it.isValidated }} validated key(s)")
                } else if (appMode is AppMode.Offline) {
                    scanAvailableModels()
                    initializeEngine()
                } else {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("No validated API keys. Go to Settings.")) }
                    scanAvailableModels()
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Failed to load settings: ${e.message}")
                scanAvailableModels()
                initializeEngine()
            }
        }
    }

    private fun saveKeys() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveApiKeys(_uiState.value.apiKeys)
        }
    }

    private fun saveModel() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveSelectedModel(_uiState.value.onlineModelName)
        }
    }

    private fun saveMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = if (_uiState.value.appMode is AppMode.Online) "online" else "offline"
            settingsRepository.saveAppMode(mode)
        }
    }

    // ==================== API CLIENT ====================

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

    // ==================== MODEL SCANNING ====================

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
                    addLog(LogLevel.INFO, TAG, "Found ${models.size} model(s)")
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

    // ==================== MODE MANAGEMENT ====================

    fun setAppMode(mode: AppMode) {
        if (_uiState.value.appMode == mode) return
        addLog(LogLevel.INFO, TAG, "Switching to ${if (mode is AppMode.Online) "Online" else "Offline"} mode")
        _uiState.update { it.copy(appMode = mode) }
        saveMode()
        when (mode) {
            is AppMode.Online -> {
                llmInference?.close()
                llmInference = null
                if (_uiState.value.hasAnyValidatedKey) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                    addLog(LogLevel.INFO, TAG, "Online mode ready (${_uiState.value.onlineModelName})")
                } else if (_uiState.value.apiKeys.isNotEmpty()) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key(s) not validated. Test in Settings.")) }
                } else {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("No API keys. Add one in Settings.")) }
                }
            }
            is AppMode.Offline -> initializeEngine()
        }
    }

    // ==================== MULTI-KEY MANAGEMENT ====================

    fun addApiKey(key: String, label: String = "") {
        if (key.isBlank()) return
        // Don't add duplicate keys
        if (_uiState.value.apiKeys.any { it.key == key }) {
            addLog(LogLevel.WARNING, TAG, "API key already exists")
            _uiState.update { it.copy(apiKeyTestResult = "This API key already exists.") }
            return
        }
        val newEntry = ApiKeyEntry(key = key, label = label.ifBlank { "Key ${_uiState.value.apiKeys.size + 1}" })
        _uiState.update { it.copy(apiKeys = it.apiKeys + newEntry) }
        saveKeys()
        addLog(LogLevel.INFO, TAG, "Added API key: ${newEntry.label}")
        // Fetch models using this key
        fetchOnlineModels(key)
    }

    fun removeApiKey(id: String) {
        _uiState.update {
            val updated = it.apiKeys.filter { entry -> entry.id != id }
            it.copy(
                apiKeys = updated,
                activeKeyIndex = it.activeKeyIndex.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
            )
        }
        saveKeys()
        // Update status if no keys left
        if (_uiState.value.apiKeys.isEmpty() && _uiState.value.appMode is AppMode.Online) {
            _uiState.update { it.copy(modelStatus = ModelStatus.Error("No API keys. Add one in Settings.")) }
        } else if (!_uiState.value.hasAnyValidatedKey && _uiState.value.appMode is AppMode.Online) {
            _uiState.update { it.copy(modelStatus = ModelStatus.Error("No validated API keys. Test in Settings.")) }
        }
        addLog(LogLevel.INFO, TAG, "Removed API key")
    }

    fun fetchOnlineModels(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isFetchingOnlineModels = true, apiKeyTestResult = null) }
                addLog(LogLevel.INFO, TAG, "Fetching available models from Gemini API...")

                val api = geminiApi ?: run {
                    _uiState.update { it.copy(isFetchingOnlineModels = false, apiKeyTestResult = "API client error") }
                    return@launch
                }

                val response = api.listModels(apiKey)
                val models = response.models
                    ?.filter { it.supportsContentGeneration }
                    ?.sortedBy { it.modelId }
                    ?: emptyList()

                if (models.isEmpty()) {
                    _uiState.update {
                        it.copy(isFetchingOnlineModels = false, apiKeyTestResult = "No compatible models found.")
                    }
                } else {
                    val currentModelId = _uiState.value.onlineModelName
                    val autoSelect = models.find { it.modelId == currentModelId }
                        ?: models.find { it.modelId.contains("flash") }
                        ?: models.first()

                    _uiState.update {
                        it.copy(
                            isFetchingOnlineModels = false,
                            onlineAvailableModels = models,
                            onlineModelName = autoSelect.modelId,
                            apiKeyTestResult = "Found ${models.size} models."
                        )
                    }
                    saveModel()
                    addLog(LogLevel.INFO, TAG, "Found ${models.size} models")
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Failed to fetch models: ${e.message}")
                _uiState.update {
                    it.copy(isFetchingOnlineModels = false, apiKeyTestResult = "Error: ${e.message?.take(80)}")
                }
            }
        }
    }

    fun selectOnlineModel(model: GeminiModelInfo) {
        _uiState.update { it.copy(onlineModelName = model.modelId, apiKeyTestResult = null) }
        saveModel()
        addLog(LogLevel.INFO, TAG, "Selected online model: ${model.modelId}")
    }

    fun testApiKey(keyId: String) {
        val entry = _uiState.value.apiKeys.find { it.id == keyId } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isTestingKeyId = keyId, apiKeyTestResult = "Testing ${entry.label}...") }
                val modelName = _uiState.value.onlineModelName

                addLog(LogLevel.INFO, TAG, "Testing ${entry.label} with model: $modelName")

                val api = geminiApi ?: run {
                    _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "API client error") }
                    return@launch
                }

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Say hello in one word.")))
                    ),
                    generationConfig = GenerationConfig(maxOutputTokens = 10, temperature = 0.1f)
                )

                val response = api.generateContent(modelName, entry.key, request)

                if (response.error != null) {
                    val errMsg = response.error.message ?: "API error (code: ${response.error.code})"
                    addLog(LogLevel.ERROR, TAG, "Test failed for ${entry.label}: $errMsg")
                    updateKeyState(keyId, isValidated = false, lastError = errMsg)
                    _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "Failed: $errMsg") }
                } else {
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        addLog(LogLevel.INFO, TAG, "Test passed for ${entry.label}: $text")
                        updateKeyState(keyId, isValidated = true, lastError = null)
                        _uiState.update {
                            it.copy(
                                isTestingKeyId = null,
                                apiKeyTestResult = "${entry.label} validated! Response: \"$text\"",
                                modelStatus = ModelStatus.Ready
                            )
                        }
                    } else {
                        updateKeyState(keyId, isValidated = false, lastError = "Empty response")
                        _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "Empty response. Try a different model.") }
                    }
                }
                saveKeys()
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Test error for ${entry.label}: ${e.message}")
                updateKeyState(keyId, isValidated = false, lastError = e.message)
                _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "Error: ${e.message?.take(100)}") }
                saveKeys()
            }
        }
    }

    private fun updateKeyState(keyId: String, isValidated: Boolean, lastError: String?) {
        _uiState.update { state ->
            state.copy(
                apiKeys = state.apiKeys.map {
                    if (it.id == keyId) it.copy(isValidated = isValidated, lastError = lastError) else it
                }
            )
        }
    }

    // ==================== PROMPT TEMPLATES & IMAGE ====================

    fun setPendingImage(uri: String?) {
        _uiState.update { it.copy(pendingImageUri = uri) }
        if (uri != null) {
            addLog(LogLevel.INFO, TAG, "Image attached for analysis")
            if (_uiState.value.pendingImageTemplate != null) {
                _uiState.update { it.copy(showImageResponseStylePicker = true) }
            }
        } else {
            addLog(LogLevel.DEBUG, TAG, "Image attachment removed")
            _uiState.update { it.copy(pendingImageTemplate = null, showImageResponseStylePicker = false) }
        }
    }

    fun setPendingImageTemplate(template: PromptTemplate?) {
        _uiState.update { it.copy(pendingImageTemplate = template) }
    }

    fun dismissImageResponseStylePicker() {
        _uiState.update { it.copy(showImageResponseStylePicker = false, pendingImageTemplate = null) }
    }

    fun sendImageWithStyle(style: ImageResponseStyle) {
        val template = _uiState.value.pendingImageTemplate ?: return
        val styleInstruction = when (style) {
            ImageResponseStyle.SHORT -> "Keep the response brief and concise (2-3 sentences max)."
            ImageResponseStyle.DETAILED -> "Provide a detailed and thorough response."
            ImageResponseStyle.FULL -> "Provide the most comprehensive and exhaustive analysis possible. Cover every detail."
            ImageResponseStyle.BULLET_POINTS -> "Format the response as organized bullet points."
            ImageResponseStyle.TECHNICAL -> "Provide a technical analysis with precise terminology."
        }
        val finalPrompt = "${template.promptPrefix}\n\n$styleInstruction"
        _uiState.update { it.copy(showImageResponseStylePicker = false, pendingImageTemplate = null) }
        sendMessage(finalPrompt)
    }

    fun togglePromptTemplates() {
        _uiState.update { it.copy(showPromptTemplates = !it.showPromptTemplates) }
    }

    fun hidePromptTemplates() {
        _uiState.update { it.copy(showPromptTemplates = false) }
    }

    // ==================== GRAND MASTER ====================

    fun showGrandMasterPicker() {
        _uiState.update { it.copy(showGrandMasterPicker = true) }
    }

    fun hideGrandMasterPicker() {
        _uiState.update { it.copy(showGrandMasterPicker = false) }
    }

    fun activateGrandMaster(grandMaster: GrandMaster) {
        _uiState.update {
            it.copy(
                activeGrandMaster = grandMaster,
                showGrandMasterPicker = false,
                messages = listOf(
                    Message(text = grandMaster.welcomeMessage, user = User.AI)
                )
            )
        }
        addLog(LogLevel.INFO, TAG, "Activated Grand Master: ${grandMaster.title}")
    }

    fun exitGrandMaster() {
        _uiState.update { it.copy(activeGrandMaster = null, messages = emptyList()) }
        addLog(LogLevel.INFO, TAG, "Exited Grand Master mode")
    }

    // ==================== LOGGING ====================

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

    // ==================== ENGINE INIT ====================

    fun initializeEngine() {
        if (_uiState.value.appMode is AppMode.Online) {
            _uiState.update {
                it.copy(
                    modelStatus = when {
                        it.hasAnyValidatedKey -> ModelStatus.Ready
                        it.apiKeys.isNotEmpty() -> ModelStatus.Error("API key(s) not validated. Test in Settings.")
                        else -> ModelStatus.Error("No API keys. Add one in Settings.")
                    }
                )
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
                    _uiState.update { it.copy(modelStatus = ModelStatus.ModelNotFound) }
                    return@launch
                }
                if (!modelFile.canRead()) {
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

    // ==================== SEND MESSAGE ====================

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
            it.copy(isGenerating = true, messages = it.messages + userMessage, pendingImageUri = null)
        }
        addLog(LogLevel.DEBUG, TAG, "User: ${displayText.take(80)}${if (imageUri != null) " [+image]" else ""}")

        when (_uiState.value.appMode) {
            is AppMode.Offline -> {
                if (imageUri != null) {
                    appendAiMessage("Image analysis is only available in Online mode. Switch to Online mode in Settings.")
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

                // Prepend Grand Master context + conversation history for offline
                val grandMaster = _uiState.value.activeGrandMaster
                val fullPrompt = if (grandMaster != null) {
                    val recentMessages = _uiState.value.messages
                        .drop(1) // skip welcome message
                        .takeLast(6) // keep last 3 exchanges to fit in context
                        .dropLast(1) // drop the user message we just added
                    val historyText = if (recentMessages.isNotEmpty()) {
                        recentMessages.joinToString("\n") { msg ->
                            if (msg.user is User.Person) "User: ${msg.text}" else "Assistant: ${msg.text}"
                        } + "\n"
                    } else ""
                    "${grandMaster.systemPrompt}\n\n${historyText}User: $prompt\nAssistant:"
                } else prompt

                val response = inference.generateResponse(fullPrompt)
                val elapsed = System.currentTimeMillis() - startTime
                val clean = response?.trim()
                if (clean.isNullOrEmpty()) {
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
                val validKeys = _uiState.value.validatedKeys
                if (validKeys.isEmpty()) {
                    appendAiMessage("No validated API keys. Go to Settings.")
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

                // Build parts
                val currentParts = mutableListOf<GeminiPart>()
                if (imageUri != null) {
                    try {
                        val base64 = encodeImageToBase64(Uri.parse(imageUri))
                        if (base64 != null) {
                            val mimeType = getMimeType(Uri.parse(imageUri)) ?: "image/jpeg"
                            currentParts.add(GeminiPart(inlineData = InlineData(mimeType = mimeType, data = base64)))
                        }
                    } catch (e: Exception) {
                        addLog(LogLevel.ERROR, TAG, "Image encoding error: ${e.message}")
                    }
                }

                val textPrompt = prompt.ifBlank { "Analyze this image in detail." }
                currentParts.add(GeminiPart(text = textPrompt))

                // Build conversation context
                val contents = mutableListOf<GeminiContent>()

                // Inject Grand Master system prompt as first user-model exchange
                val grandMaster = _uiState.value.activeGrandMaster
                if (grandMaster != null) {
                    contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "System instruction: ${grandMaster.systemPrompt}"))))
                    contents.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = "Understood. I will act as the ${grandMaster.title} as instructed."))))
                }

                val prevMessages = _uiState.value.messages.dropLast(1)
                // Skip the welcome message (first AI message from Grand Master)
                val messagesToSend = if (grandMaster != null && prevMessages.isNotEmpty()) prevMessages.drop(1) else prevMessages
                for (msg in messagesToSend) {
                    if (msg.imageUri == null) {
                        contents.add(
                            GeminiContent(
                                role = if (msg.user is User.Person) "user" else "model",
                                parts = listOf(GeminiPart(text = msg.text))
                            )
                        )
                    }
                }
                contents.add(GeminiContent(role = "user", parts = currentParts))

                // Image output modalities
                val modelName = _uiState.value.onlineModelName
                val supportsImageOutput = (modelName == "gemini-2.0-flash" ||
                        modelName == "gemini-2.0-flash-001" ||
                        modelName == "gemini-2.5-flash" ||
                        modelName == "gemini-2.5-flash-preview-04-17")
                val modalities = if (supportsImageOutput) listOf("TEXT", "IMAGE") else null

                val request = GeminiRequest(
                    contents = contents,
                    generationConfig = GenerationConfig(maxOutputTokens = 2048, temperature = 0.7f, responseModalities = modalities)
                )

                // FAILOVER: try each validated key
                val response = sendWithFailover(api, request, modelName, validKeys)
                val elapsed = System.currentTimeMillis() - startTime

                if (response == null) {
                    // All keys exhausted — already handled in sendWithFailover
                } else if (response.error != null) {
                    val errMsg = response.error.message ?: "API error (code: ${response.error.code})"
                    addLog(LogLevel.ERROR, TAG, "API error: $errMsg")
                    appendAiMessage("API Error: $errMsg")
                } else {
                    val parts = response.candidates?.firstOrNull()?.content?.parts ?: emptyList()
                    val textParts = parts.mapNotNull { it.text?.trim() }.filter { it.isNotEmpty() }
                    val responseText = textParts.joinToString("\n\n")
                    val imageParts = parts.mapNotNull { part ->
                        part.inlineData?.let { data ->
                            if (data.mimeType.startsWith("image/")) GeneratedImage(base64Data = data.data, mimeType = data.mimeType) else null
                        }
                    }

                    if (responseText.isEmpty() && imageParts.isEmpty()) {
                        appendAiMessage("The API returned an empty response. Please try again.")
                    } else {
                        val displayText = responseText.ifEmpty { if (imageParts.isNotEmpty()) "Here's the generated image:" else "" }
                        appendAiMessageWithImages(displayText, imageParts)
                        addLog(LogLevel.INFO, TAG, "Online response in ${elapsed}ms (${responseText.length} chars, ${imageParts.size} images)")
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

    private suspend fun sendWithFailover(
        api: GeminiApiService,
        request: GeminiRequest,
        modelName: String,
        validKeys: List<ApiKeyEntry>
    ): GeminiResponse? {
        val startIndex = _uiState.value.activeKeyIndex.coerceIn(0, validKeys.lastIndex)
        val errors = mutableListOf<String>()

        for (i in validKeys.indices) {
            val idx = (startIndex + i) % validKeys.size
            val entry = validKeys[idx]
            try {
                addLog(LogLevel.DEBUG, TAG, "Trying ${entry.label}...")
                val response = api.generateContent(modelName, entry.key, request)

                if (response.error != null) {
                    val code = response.error.code
                    // Retryable errors: rate limit, server errors
                    if (code in listOf(429, 500, 503)) {
                        errors.add("${entry.label}: ${response.error.message}")
                        addLog(LogLevel.WARNING, TAG, "${entry.label} failed ($code), trying next key...")
                        updateKeyState(entry.id, isValidated = true, lastError = "Error $code")
                        continue
                    }
                    // Non-retryable (400, 403, etc) — return as-is
                    _uiState.update { it.copy(activeKeyIndex = idx) }
                    return response
                }

                // Success
                _uiState.update { it.copy(activeKeyIndex = idx) }
                updateKeyState(entry.id, isValidated = true, lastError = null)
                return response

            } catch (e: Exception) {
                errors.add("${entry.label}: ${e.message?.take(60)}")
                addLog(LogLevel.WARNING, TAG, "${entry.label} exception: ${e.message}, trying next...")
                continue
            }
        }

        // All keys exhausted
        addLog(LogLevel.ERROR, TAG, "All ${validKeys.size} API keys exhausted")
        appendAiMessage("All API keys exhausted:\n\n${errors.joinToString("\n") { "- $it" }}\n\nAdd more keys or wait for rate limits to reset.")
        return null
    }

    // ==================== UTILITIES ====================

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

    private fun appendAiMessageWithImages(text: String, images: List<GeneratedImage>) {
        _uiState.update {
            it.copy(messages = it.messages + Message(text = text, user = User.AI, generatedImages = images))
        }
    }

    fun saveGeneratedImage(image: GeneratedImage): String? {
        return try {
            val bytes = Base64.decode(image.base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            val extension = when {
                image.mimeType.contains("png") -> "png"
                image.mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val fileName = "MEDHA_${System.currentTimeMillis()}.$extension"
            val compressFormat = when (extension) {
                "png" -> android.graphics.Bitmap.CompressFormat.PNG
                "webp" -> android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
                else -> android.graphics.Bitmap.CompressFormat.JPEG
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, image.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MEDHA")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = application.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(compressFormat, 95, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            addLog(LogLevel.INFO, TAG, "Image saved: $fileName")
            uri.toString()
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Save image failed: ${e.message}")
            null
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), pendingImageUri = null, activeGrandMaster = null) }
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

enum class ImageResponseStyle(val label: String, val icon: String, val description: String) {
    SHORT("Short", "\u26A1", "Quick 2-3 sentence summary"),
    DETAILED("Detailed", "\uD83D\uDD0D", "Thorough and detailed response"),
    FULL("Full Analysis", "\uD83D\uDCDD", "Comprehensive, covers everything"),
    BULLET_POINTS("Bullet Points", "\uD83D\uDCCB", "Organized as bullet points"),
    TECHNICAL("Technical", "\u2699\uFE0F", "Technical with precise terms")
}
