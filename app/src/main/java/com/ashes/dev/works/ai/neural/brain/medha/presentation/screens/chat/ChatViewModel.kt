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
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GenerationConfig
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.InlineData
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.AppMode
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogLevel
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GeneratedImage
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
                if (_uiState.value.apiKeyValidated) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                    addLog(LogLevel.INFO, TAG, "Online mode ready (${_uiState.value.onlineModelName})")
                } else if (_uiState.value.apiKey.isNotBlank()) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key not validated. Test it in Settings.")) }
                    fetchOnlineModels(_uiState.value.apiKey)
                } else {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key required. Set it in Settings.")) }
                }
            }
            is AppMode.Offline -> initializeEngine()
        }
    }

    fun setApiKey(key: String) {
        _uiState.update {
            it.copy(
                apiKey = key,
                apiKeyValidated = false,
                apiKeyTestResult = null,
                onlineAvailableModels = emptyList()
            )
        }
        if (key.isNotBlank()) {
            fetchOnlineModels(key)
        } else {
            _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key required")) }
        }
    }

    private fun fetchOnlineModels(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isFetchingOnlineModels = true, apiKeyTestResult = null) }
                addLog(LogLevel.INFO, TAG, "Fetching available models from Gemini API...")

                val api = geminiApi ?: run {
                    addLog(LogLevel.ERROR, TAG, "API client not initialized")
                    _uiState.update { it.copy(isFetchingOnlineModels = false, apiKeyTestResult = "API client error") }
                    return@launch
                }

                val response = api.listModels(apiKey)
                val models = response.models
                    ?.filter { it.supportsContentGeneration }
                    ?.sortedBy { it.modelId }
                    ?: emptyList()

                if (models.isEmpty()) {
                    addLog(LogLevel.WARNING, TAG, "No compatible models found for this API key")
                    _uiState.update {
                        it.copy(
                            isFetchingOnlineModels = false,
                            onlineAvailableModels = emptyList(),
                            apiKeyTestResult = "No compatible models found. Check your API key."
                        )
                    }
                } else {
                    addLog(LogLevel.INFO, TAG, "Found ${models.size} models")
                    // Auto-select best model: prefer gemini-2.0-flash, then first available
                    val currentModelId = _uiState.value.onlineModelName
                    val autoSelect = models.find { it.modelId == currentModelId }
                        ?: models.find { it.modelId.contains("flash") }
                        ?: models.first()

                    _uiState.update {
                        it.copy(
                            isFetchingOnlineModels = false,
                            onlineAvailableModels = models,
                            onlineModelName = autoSelect.modelId,
                            apiKeyTestResult = "Found ${models.size} models. Select one and tap Test."
                        )
                    }
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Failed to fetch models: ${e.message}")
                _uiState.update {
                    it.copy(
                        isFetchingOnlineModels = false,
                        apiKeyTestResult = "Invalid API key or network error: ${e.message?.take(80)}"
                    )
                }
            }
        }
    }

    fun selectOnlineModel(model: GeminiModelInfo) {
        _uiState.update {
            it.copy(
                onlineModelName = model.modelId,
                apiKeyValidated = false,
                apiKeyTestResult = null
            )
        }
        addLog(LogLevel.INFO, TAG, "Selected online model: ${model.modelId}")
    }

    fun testApiKeyAndModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isTestingApiKey = true, apiKeyTestResult = "Testing...") }
                val apiKey = _uiState.value.apiKey
                val modelName = _uiState.value.onlineModelName

                addLog(LogLevel.INFO, TAG, "Testing API key with model: $modelName")

                val api = geminiApi ?: run {
                    _uiState.update { it.copy(isTestingApiKey = false, apiKeyTestResult = "API client error") }
                    return@launch
                }

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Say hello in one word.")))
                    ),
                    generationConfig = GenerationConfig(maxOutputTokens = 10, temperature = 0.1f)
                )

                val response = api.generateContent(modelName, apiKey, request)

                if (response.error != null) {
                    val errMsg = response.error.message ?: "API error (code: ${response.error.code})"
                    addLog(LogLevel.ERROR, TAG, "Test failed: $errMsg")
                    _uiState.update {
                        it.copy(
                            isTestingApiKey = false,
                            apiKeyValidated = false,
                            apiKeyTestResult = "Test failed: $errMsg"
                        )
                    }
                } else {
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        addLog(LogLevel.INFO, TAG, "Test passed! Model responded: $text")
                        _uiState.update {
                            it.copy(
                                isTestingApiKey = false,
                                apiKeyValidated = true,
                                apiKeyTestResult = "Test passed! Model responded: \"$text\"",
                                modelStatus = ModelStatus.Ready
                            )
                        }
                    } else {
                        addLog(LogLevel.WARNING, TAG, "Test returned empty response")
                        _uiState.update {
                            it.copy(
                                isTestingApiKey = false,
                                apiKeyValidated = false,
                                apiKeyTestResult = "Model returned empty response. Try a different model."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Test error: ${e.message}")
                _uiState.update {
                    it.copy(
                        isTestingApiKey = false,
                        apiKeyValidated = false,
                        apiKeyTestResult = "Error: ${e.message?.take(100)}"
                    )
                }
            }
        }
    }

    fun setPendingImage(uri: String?) {
        _uiState.update { it.copy(pendingImageUri = uri) }
        if (uri != null) {
            addLog(LogLevel.INFO, TAG, "Image attached for analysis")
            // If there's a pending image template, show the response style picker
            if (_uiState.value.pendingImageTemplate != null) {
                _uiState.update { it.copy(showImageResponseStylePicker = true) }
            }
        } else {
            addLog(LogLevel.DEBUG, TAG, "Image attachment removed")
            // Clear template flow if image is removed
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
                it.copy(
                    modelStatus = when {
                        it.apiKeyValidated -> ModelStatus.Ready
                        it.apiKey.isNotBlank() -> ModelStatus.Error("API key not validated. Test it in Settings.")
                        else -> ModelStatus.Error("API key required. Set it in Settings.")
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

                // Enable image output only for models known to support it
                // Only gemini-2.0-flash and gemini-2.5-flash (non-lite, non-thinking) support image generation
                val modelName = _uiState.value.onlineModelName
                val supportsImageOutput = (modelName == "gemini-2.0-flash" ||
                        modelName == "gemini-2.0-flash-001" ||
                        modelName == "gemini-2.5-flash" ||
                        modelName == "gemini-2.5-flash-preview-04-17")
                val modalities = if (supportsImageOutput) listOf("TEXT", "IMAGE") else null

                val request = GeminiRequest(
                    contents = contents,
                    generationConfig = GenerationConfig(
                        maxOutputTokens = 2048,
                        temperature = 0.7f,
                        responseModalities = modalities
                    )
                )

                val response = api.generateContent(modelName, apiKey, request)
                val elapsed = System.currentTimeMillis() - startTime

                if (response.error != null) {
                    val errMsg = response.error.message ?: "API error (code: ${response.error.code})"
                    addLog(LogLevel.ERROR, TAG, "API error: $errMsg")
                    appendAiMessage("API Error: $errMsg")
                } else {
                    val parts = response.candidates?.firstOrNull()?.content?.parts ?: emptyList()

                    // Extract text parts
                    val textParts = parts.mapNotNull { it.text?.trim() }.filter { it.isNotEmpty() }
                    val responseText = textParts.joinToString("\n\n")

                    // Extract image parts
                    val imageParts = parts.mapNotNull { part ->
                        part.inlineData?.let { data ->
                            if (data.mimeType.startsWith("image/")) {
                                GeneratedImage(
                                    base64Data = data.data,
                                    mimeType = data.mimeType
                                )
                            } else null
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
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: run {
                    addLog(LogLevel.ERROR, TAG, "Failed to decode image bytes")
                    return null
                }

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
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: run {
                    addLog(LogLevel.ERROR, TAG, "Failed to create MediaStore entry")
                    return null
                }

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

enum class ImageResponseStyle(val label: String, val icon: String, val description: String) {
    SHORT("Short", "\u26A1", "Quick 2-3 sentence summary"),
    DETAILED("Detailed", "\uD83D\uDD0D", "Thorough and detailed response"),
    FULL("Full Analysis", "\uD83D\uDCDD", "Comprehensive, covers everything"),
    BULLET_POINTS("Bullet Points", "\uD83D\uDCCB", "Organized as bullet points"),
    TECHNICAL("Technical", "\u2699\uFE0F", "Technical with precise terms")
}
