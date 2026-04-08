package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashes.dev.works.ai.neural.brain.medha.data.CatalogModel
import com.ashes.dev.works.ai.neural.brain.medha.data.ModelCatalog
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiApiService
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiContent
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiPart
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiRequest
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GenerationConfig
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.InlineData
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.AppMode
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GeneratedImage
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogLevel
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplate
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatViewModel(private val application: Application) : ViewModel() {

    companion object {
        private const val TAG = "MedhaEngine"
        const val APP_VERSION = "2.0.0"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
        private const val MODELS_DIR = "medha_models"
        private const val TMP_EXT = ".medhatmp"
    }

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    // LiteRT LM engine state
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var geminiApi: GeminiApiService? = null

    // Inference config (adjustable from settings)
    var topK: Int = 64
    var topP: Double = 0.95
    var temperature: Double = 1.0
    var maxTokens: Int = 4096
    var enableThinking: Boolean = false

    init {
        addLog(LogLevel.INFO, TAG, "MEDHA AI Engine v$APP_VERSION starting (LiteRT LM)...")
        initGeminiApi()
        scanAvailableModels()
        initializeEngine()
    }

    // ── Gemini Online API ────────────────────────────────────────────────

    private fun initGeminiApi() {
        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            geminiApi = Retrofit.Builder()
                .baseUrl(GEMINI_BASE_URL).client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build().create(GeminiApiService::class.java)
            addLog(LogLevel.INFO, TAG, "Online API client initialized")
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Failed to init API client: ${e.message}")
        }
    }

    // ── Model Scanning ──────────────────────────────────────────────────

    private fun modelsDir(): File {
        val dir = File(application.filesDir, MODELS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun scanAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appModels = modelsDir().listFiles()
                    ?.filter { f -> f.isFile && f.length() > 0 && ModelInfo.SUPPORTED_EXTENSIONS.any { f.name.endsWith(it, true) } }
                    ?.map { ModelInfo.fromFileName(it.name, it.absolutePath, it.length()) }
                    ?: emptyList()

                // Also scan Downloads for legacy .bin files
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val downloadModels = if (downloadsDir.exists() && downloadsDir.canRead()) {
                    downloadsDir.listFiles()
                        ?.filter { f -> f.isFile && ModelInfo.SUPPORTED_EXTENSIONS.any { f.name.endsWith(it, true) } }
                        ?.map { ModelInfo.fromFileName(it.name, it.absolutePath, it.length()) }
                        ?: emptyList()
                } else emptyList()

                val allModels = (appModels + downloadModels).distinctBy { it.fileName }
                _uiState.update { it.copy(availableModels = allModels) }

                if (allModels.isNotEmpty()) {
                    addLog(LogLevel.INFO, TAG, "Found ${allModels.size} model(s)")
                    if (_uiState.value.selectedModel == null) {
                        // Prefer LiteRT format models
                        val best = allModels.firstOrNull { it.isLiteRtFormat } ?: allModels.first()
                        _uiState.update { it.copy(selectedModel = best) }
                    }
                } else {
                    addLog(LogLevel.INFO, TAG, "No local models found. Download one from the catalog.")
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Model scan failed: ${e.message}")
            }
        }
    }

    fun selectModel(model: ModelInfo) {
        if (_uiState.value.selectedModel?.filePath == model.filePath) return
        addLog(LogLevel.INFO, TAG, "Switching to: ${model.fileName}")
        _uiState.update { it.copy(selectedModel = model) }
        if (_uiState.value.appMode is AppMode.Offline) {
            destroyEngine()
            initializeEngine()
        }
    }

    // ── Mode Switching ──────────────────────────────────────────────────

    fun setAppMode(mode: AppMode) {
        if (_uiState.value.appMode == mode) return
        addLog(LogLevel.INFO, TAG, "Switching to ${if (mode is AppMode.Online) "Online" else "Offline"} mode")
        _uiState.update { it.copy(appMode = mode) }
        when (mode) {
            is AppMode.Online -> {
                destroyEngine()
                if (_uiState.value.apiKeyValidated) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                } else if (_uiState.value.apiKey.isNotBlank()) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key not validated.")) }
                    fetchOnlineModels(_uiState.value.apiKey)
                } else {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key required.")) }
                }
            }
            is AppMode.Offline -> initializeEngine()
        }
    }

    // ── LiteRT LM Engine (Offline) ──────────────────────────────────────

    fun initializeEngine() {
        if (_uiState.value.appMode is AppMode.Online) {
            _uiState.update {
                it.copy(modelStatus = when {
                    it.apiKeyValidated -> ModelStatus.Ready
                    it.apiKey.isNotBlank() -> ModelStatus.Error("API key not validated.")
                    else -> ModelStatus.Error("API key required.")
                })
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
                    addLog(LogLevel.INFO, TAG, "No model found. Download from catalog.")
                    _uiState.update { it.copy(modelStatus = ModelStatus.ModelNotFound) }
                    return@launch
                }

                val modelFile = File(modelToLoad.filePath)
                if (!modelFile.exists()) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.ModelNotFound) }
                    return@launch
                }

                addLog(LogLevel.INFO, TAG, "Loading ${modelToLoad.fileName} (${modelToLoad.sizeInMb}MB)...")
                _uiState.update { it.copy(modelStatus = ModelStatus.Loading(0f, "Preparing engine...")) }

                val modelMaxTokens = if (modelToLoad.isLiteRtFormat) maxTokens else 1024
                val engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    maxNumTokens = modelMaxTokens
                )

                _uiState.update { it.copy(modelStatus = ModelStatus.Loading(0.3f, "Loading weights...")) }
                engine = Engine(engineConfig)
                engine!!.initialize()

                _uiState.update { it.copy(modelStatus = ModelStatus.Loading(0.8f, "Creating conversation...")) }
                val samplerConfig = SamplerConfig(topK = topK, topP = topP, temperature = temperature)
                conversation = engine!!.createConversation(
                    ConversationConfig(samplerConfig = samplerConfig)
                )

                _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                addLog(LogLevel.INFO, TAG, "Engine ready: ${modelToLoad.displayName} (LiteRT LM, CPU, ${modelMaxTokens} tokens)")

            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Init failed: ${e.message}")
                _uiState.update { it.copy(modelStatus = ModelStatus.Error(e.message ?: "Unknown error")) }
                destroyEngine()
            }
        }
    }

    private fun destroyEngine() {
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        conversation = null
        engine = null
    }

    fun resetConversation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                conversation?.close()
                val samplerConfig = SamplerConfig(topK = topK, topP = topP, temperature = temperature)
                conversation = engine?.createConversation(ConversationConfig(samplerConfig = samplerConfig))
                _uiState.update { it.copy(messages = emptyList(), streamingText = "", streamingThinking = "") }
                addLog(LogLevel.INFO, TAG, "Conversation reset")
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Reset failed: ${e.message}")
            }
        }
    }

    // ── Send Message ────────────────────────────────────────────────────

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() && _uiState.value.pendingImageUri == null) return
        if (_uiState.value.modelStatus !is ModelStatus.Ready) {
            addLog(LogLevel.WARNING, TAG, "Cannot send — engine not ready")
            return
        }

        val imageUri = _uiState.value.pendingImageUri
        val displayText = prompt.ifBlank { "[Image attached]" }
        val userMessage = Message(text = displayText, user = User.Person, imageUri = imageUri)

        _uiState.update {
            it.copy(isGenerating = true, messages = it.messages + userMessage,
                pendingImageUri = null, streamingText = "", streamingThinking = "", isThinking = false)
        }
        addLog(LogLevel.DEBUG, TAG, "User: ${displayText.take(80)}${if (imageUri != null) " [+image]" else ""}")

        when (_uiState.value.appMode) {
            is AppMode.Offline -> sendOfflineMessage(prompt, imageUri)
            is AppMode.Online -> sendOnlineMessage(prompt, imageUri)
        }
    }

    // ── Offline: LiteRT LM Streaming ────────────────────────────────────

    private fun sendOfflineMessage(prompt: String, imageUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conv = conversation
                if (conv == null) {
                    appendAiMessage("Engine not loaded. Go to Settings to initialize.", provider = "offline")
                    _uiState.update { it.copy(isGenerating = false) }
                    return@launch
                }

                val startTime = System.currentTimeMillis()
                addLog(LogLevel.INFO, TAG, "Generating (LiteRT LM streaming)...")

                // Build contents: image + text
                val contentParts = mutableListOf<Content>()

                // Add image if present (Gemma 4 vision)
                if (imageUri != null) {
                    try {
                        val imageBytes = encodeImageToPngBytes(Uri.parse(imageUri))
                        if (imageBytes != null) {
                            contentParts.add(Content.ImageBytes(imageBytes))
                            addLog(LogLevel.DEBUG, TAG, "Image attached (${imageBytes.size / 1024}KB)")
                        }
                    } catch (e: Exception) {
                        addLog(LogLevel.WARNING, TAG, "Image encode failed: ${e.message}")
                    }
                }

                val textPrompt = if (imageUri != null && prompt.isBlank()) "Describe this image in detail." else prompt
                contentParts.add(Content.Text(textPrompt))
                val contents = Contents.of(contentParts)

                val extraContext: Map<String, Any> = if (enableThinking) {
                    mapOf("enable_thinking" to "true")
                } else emptyMap()

                // Streaming inference
                val fullResponse = StringBuilder()
                val fullThinking = StringBuilder()
                var tokenCount = 0
                var firstTokenTime = 0L

                val result = suspendCancellableCoroutine { cont ->
                    val callback = object : MessageCallback {
                        override fun onMessage(message: com.google.ai.edge.litertlm.Message) {
                            val text = message.toString()
                            val thought = try { message.channels["thought"]?.toString() } catch (_: Exception) { null }

                            if (text.isNotEmpty()) {
                                if (tokenCount == 0) firstTokenTime = System.currentTimeMillis()
                                tokenCount++
                                fullResponse.append(text)
                                _uiState.update { it.copy(streamingText = fullResponse.toString(), isThinking = false) }
                            }
                            if (!thought.isNullOrEmpty()) {
                                fullThinking.append(thought)
                                _uiState.update { it.copy(streamingThinking = fullThinking.toString(), isThinking = true) }
                            }
                        }

                        override fun onDone() {
                            if (cont.isActive) cont.resume(fullResponse.toString())
                        }

                        override fun onError(throwable: Throwable) {
                            if (cont.isActive) {
                                if (throwable is kotlinx.coroutines.CancellationException) {
                                    cont.resume(fullResponse.toString())
                                } else {
                                    cont.resumeWithException(throwable)
                                }
                            }
                        }
                    }

                    try {
                        conv.sendMessageAsync(contents, callback, extraContext)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }

                    cont.invokeOnCancellation {
                        try { conv.cancelProcess() } catch (_: Exception) {}
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val tps = if (elapsed > 0) (tokenCount * 1000f) / elapsed else 0f
                val ttft = if (firstTokenTime > 0) firstTokenTime - startTime else elapsed
                val clean = result.trim()

                if (clean.isEmpty()) {
                    appendAiMessage("Empty response. Try rephrasing.", provider = "offline")
                } else {
                    val thinking = fullThinking.toString().trim().ifEmpty { null }
                    appendAiMessageWithStats(clean, tokenCount, elapsed, tps, ttft, "offline", thinking)
                    addLog(LogLevel.INFO, TAG, "Response: ${tokenCount} tokens, ${"%.1f".format(tps)} tok/s, ${elapsed}ms")
                }

                _uiState.update { it.copy(isGenerating = false, streamingText = "", streamingThinking = "", isThinking = false) }

            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Offline error: ${e.message}")
                appendAiMessage("Error: ${e.message}", provider = "offline")
                _uiState.update { it.copy(isGenerating = false, streamingText = "", streamingThinking = "", isThinking = false) }
            }
        }
    }

    private fun encodeImageToPngBytes(uri: Uri): ByteArray? {
        return try {
            val input = application.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        } catch (e: Exception) { null }
    }

    // ── Online: Gemini API ──────────────────────────────────────────────

    private fun sendOnlineMessage(prompt: String, imageUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = _uiState.value.apiKey
                if (apiKey.isBlank()) {
                    appendAiMessage("API key not configured. Go to Settings.", provider = "gemini")
                    _uiState.update { it.copy(isGenerating = false) }
                    return@launch
                }
                val api = geminiApi ?: run {
                    appendAiMessage("API client not initialized.", provider = "gemini")
                    _uiState.update { it.copy(isGenerating = false) }
                    return@launch
                }

                val startTime = System.currentTimeMillis()
                addLog(LogLevel.INFO, TAG, "Sending to Gemini API${if (imageUri != null) " (+image)" else ""}...")

                val currentParts = mutableListOf<GeminiPart>()
                if (imageUri != null) {
                    try {
                        val base64 = encodeImageToBase64(Uri.parse(imageUri))
                        if (base64 != null) {
                            val mimeType = application.contentResolver.getType(Uri.parse(imageUri)) ?: "image/jpeg"
                            currentParts.add(GeminiPart(inlineData = InlineData(mimeType = mimeType, data = base64)))
                        }
                    } catch (e: Exception) { addLog(LogLevel.ERROR, TAG, "Image encode: ${e.message}") }
                }
                currentParts.add(GeminiPart(text = prompt.ifBlank { "Analyze this image in detail." }))

                val contents = mutableListOf<GeminiContent>()
                _uiState.value.messages.dropLast(1).forEach { msg ->
                    if (msg.imageUri == null) {
                        contents.add(GeminiContent(
                            role = if (msg.user is User.Person) "user" else "model",
                            parts = listOf(GeminiPart(text = msg.text))
                        ))
                    }
                }
                contents.add(GeminiContent(role = "user", parts = currentParts))

                val modelName = _uiState.value.onlineModelName
                val supportsImageOutput = modelName in listOf("gemini-2.0-flash", "gemini-2.0-flash-001", "gemini-2.5-flash", "gemini-2.5-flash-preview-04-17")
                val request = GeminiRequest(
                    contents = contents,
                    generationConfig = GenerationConfig(
                        maxOutputTokens = 2048, temperature = 0.7f,
                        responseModalities = if (supportsImageOutput) listOf("TEXT", "IMAGE") else null
                    )
                )

                val response = api.generateContent(modelName, apiKey, request)
                val elapsed = System.currentTimeMillis() - startTime

                if (response.error != null) {
                    appendAiMessage("API Error: ${response.error.message ?: "code ${response.error.code}"}", provider = "gemini")
                } else {
                    val parts = response.candidates?.firstOrNull()?.content?.parts ?: emptyList()
                    val textParts = parts.mapNotNull { it.text?.trim() }.filter { it.isNotEmpty() }
                    val responseText = textParts.joinToString("\n\n")
                    val imageParts = parts.mapNotNull { p ->
                        p.inlineData?.let { if (it.mimeType.startsWith("image/")) GeneratedImage(base64Data = it.data, mimeType = it.mimeType) else null }
                    }

                    if (responseText.isEmpty() && imageParts.isEmpty()) {
                        appendAiMessage("Empty API response. Try again.", provider = "gemini")
                    } else {
                        val words = responseText.split("\\s+".toRegex()).size
                        val tokens = (words * 0.75).toInt().coerceAtLeast(1)
                        val tps = if (elapsed > 0) (tokens * 1000f) / elapsed else 0f
                        val display = responseText.ifEmpty { if (imageParts.isNotEmpty()) "Here's the generated image:" else "" }
                        val msg = Message(text = display, user = User.AI, generatedImages = imageParts,
                            tokenCount = tokens, latencyMs = elapsed, tokensPerSec = tps, provider = "gemini")
                        _uiState.update { it.copy(messages = it.messages + msg) }
                        addLog(LogLevel.INFO, TAG, "Online: ${elapsed}ms, ~$tokens tokens")
                    }
                }
                _uiState.update { it.copy(isGenerating = false) }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Online error: ${e.message}")
                appendAiMessage("Connection error: ${e.message}", provider = "gemini")
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val bytes = application.contentResolver.openInputStream(uri)?.readBytes() ?: return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    // ── Model Download ──────────────────────────────────────────────────

    fun downloadCatalogModel(catalogModel: CatalogModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress + (catalogModel.id to 0f)) }
                addLog(LogLevel.INFO, TAG, "Downloading ${catalogModel.name} (${catalogModel.sizeLabel})...")

                val destDir = modelsDir()
                val tmpFile = File(destDir, "${catalogModel.fileName}$TMP_EXT")
                val finalFile = File(destDir, catalogModel.fileName)

                if (finalFile.exists() && finalFile.length() > 0) {
                    addLog(LogLevel.INFO, TAG, "${catalogModel.fileName} already exists")
                    _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress - catalogModel.id) }
                    scanAvailableModels()
                    return@launch
                }

                val maxRetries = 5
                var attempt = 0
                while (attempt < maxRetries) {
                    attempt++
                    try {
                        var startByte = if (tmpFile.exists()) tmpFile.length() else 0L
                        val conn = URL(catalogModel.downloadUrl).openConnection() as HttpURLConnection
                        conn.connectTimeout = 30_000
                        conn.readTimeout = 60_000
                        conn.setRequestProperty("User-Agent", "MEDHA-Android/$APP_VERSION")
                        if (startByte > 0) conn.setRequestProperty("Range", "bytes=$startByte-")
                        conn.connect()

                        if (conn.responseCode !in listOf(200, 206)) {
                            conn.disconnect()
                            throw Exception("HTTP ${conn.responseCode}")
                        }
                        if (conn.responseCode == 200 && startByte > 0) { startByte = 0; tmpFile.delete() }

                        conn.inputStream.use { input ->
                            FileOutputStream(tmpFile, startByte > 0).use { output ->
                                val buffer = ByteArray(8192)
                                var received = startByte
                                var lastUpdate = System.currentTimeMillis()
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n == -1) break
                                    output.write(buffer, 0, n)
                                    received += n
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdate >= 300) {
                                        val p = if (catalogModel.sizeBytes > 0) (received.toFloat() / catalogModel.sizeBytes).coerceIn(0f, 1f) else 0f
                                        _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress + (catalogModel.id to p)) }
                                        lastUpdate = now
                                    }
                                }
                            }
                        }
                        conn.disconnect()
                        tmpFile.renameTo(finalFile)
                        addLog(LogLevel.INFO, TAG, "${catalogModel.name} downloaded!")
                        _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress - catalogModel.id) }
                        scanAvailableModels()
                        return@launch

                    } catch (e: Exception) {
                        val saved = if (tmpFile.exists()) tmpFile.length() / (1024 * 1024) else 0
                        addLog(LogLevel.WARNING, TAG, "Download attempt $attempt failed ($saved MB saved): ${e.message}")
                        if (attempt < maxRetries) {
                            kotlinx.coroutines.delay(attempt * 3000L)
                            continue
                        }
                        throw e
                    }
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Download failed: ${e.message}")
                _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress - catalogModel.id) }
            }
        }
    }

    fun isModelDownloaded(catalogModel: CatalogModel): Boolean {
        return File(modelsDir(), catalogModel.fileName).exists()
    }

    fun activateCatalogModel(catalogModel: CatalogModel) {
        val file = File(modelsDir(), catalogModel.fileName)
        if (!file.exists()) return
        val info = ModelInfo.fromFileName(file.name, file.absolutePath, file.length())
        selectModel(info)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun appendAiMessage(text: String, provider: String = "") {
        _uiState.update { it.copy(messages = it.messages + Message(text = text, user = User.AI, provider = provider)) }
    }

    private fun appendAiMessageWithStats(text: String, tokens: Int, latency: Long, tps: Float, ttft: Long, provider: String, thinking: String? = null) {
        _uiState.update {
            it.copy(messages = it.messages + Message(
                text = text, user = User.AI,
                tokenCount = tokens, latencyMs = latency, tokensPerSec = tps,
                thinkingText = thinking, provider = provider,
                timeToFirstTokenMs = ttft
            ))
        }
    }

    private fun appendAiMessageWithImages(text: String, images: List<GeneratedImage>) {
        _uiState.update { it.copy(messages = it.messages + Message(text = text, user = User.AI, generatedImages = images)) }
    }

    fun addLog(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
        _uiState.update { s -> s.copy(logs = (s.logs + LogEntry(level = level, tag = tag, message = message)).takeLast(100)) }
    }

    // ── Online model management ─────────────────────────────────────────

    fun setApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, apiKeyValidated = false, apiKeyTestResult = null, onlineAvailableModels = emptyList()) }
        if (key.isNotBlank()) fetchOnlineModels(key)
        else _uiState.update { it.copy(modelStatus = ModelStatus.Error("API key required")) }
    }

    private fun fetchOnlineModels(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isFetchingOnlineModels = true, apiKeyTestResult = null) }
                val models = geminiApi?.listModels(apiKey)?.models
                    ?.filter { it.supportsContentGeneration }?.sortedBy { it.modelId } ?: emptyList()

                val autoSelect = models.find { it.modelId == _uiState.value.onlineModelName }
                    ?: models.find { it.modelId.contains("flash") } ?: models.firstOrNull()

                _uiState.update {
                    it.copy(isFetchingOnlineModels = false, onlineAvailableModels = models,
                        onlineModelName = autoSelect?.modelId ?: it.onlineModelName,
                        apiKeyTestResult = if (models.isEmpty()) "No models found." else "Found ${models.size} models.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingOnlineModels = false, apiKeyTestResult = "Error: ${e.message?.take(80)}") }
            }
        }
    }

    fun selectOnlineModel(model: GeminiModelInfo) {
        _uiState.update { it.copy(onlineModelName = model.modelId, apiKeyValidated = false, apiKeyTestResult = null) }
    }

    fun testApiKeyAndModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isTestingApiKey = true, apiKeyTestResult = "Testing...") }
                val request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Say hello in one word.")))),
                    generationConfig = GenerationConfig(maxOutputTokens = 10, temperature = 0.1f)
                )
                val response = geminiApi?.generateContent(_uiState.value.onlineModelName, _uiState.value.apiKey, request)
                val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!text.isNullOrEmpty()) {
                    _uiState.update { it.copy(isTestingApiKey = false, apiKeyValidated = true, apiKeyTestResult = "Passed! \"$text\"", modelStatus = ModelStatus.Ready) }
                } else {
                    _uiState.update { it.copy(isTestingApiKey = false, apiKeyValidated = false, apiKeyTestResult = "Empty response.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isTestingApiKey = false, apiKeyValidated = false, apiKeyTestResult = "Error: ${e.message?.take(100)}") }
            }
        }
    }

    // ── Image/Template helpers ───────────────────────────────────────────

    fun setPendingImage(uri: String?) {
        _uiState.update { it.copy(pendingImageUri = uri) }
        if (uri != null && _uiState.value.pendingImageTemplate != null) {
            _uiState.update { it.copy(showImageResponseStylePicker = true) }
        } else if (uri == null) {
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
        val instruction = when (style) {
            ImageResponseStyle.SHORT -> "Keep brief (2-3 sentences)."
            ImageResponseStyle.DETAILED -> "Provide detailed response."
            ImageResponseStyle.FULL -> "Comprehensive analysis. Cover every detail."
            ImageResponseStyle.BULLET_POINTS -> "Format as bullet points."
            ImageResponseStyle.TECHNICAL -> "Technical analysis with precise terms."
        }
        _uiState.update { it.copy(showImageResponseStylePicker = false, pendingImageTemplate = null) }
        sendMessage("${template.promptPrefix}\n\n$instruction")
    }

    fun togglePromptTemplates() { _uiState.update { it.copy(showPromptTemplates = !it.showPromptTemplates) } }
    fun hidePromptTemplates() { _uiState.update { it.copy(showPromptTemplates = false) } }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), pendingImageUri = null, streamingText = "", streamingThinking = "") }
        if (_uiState.value.appMode is AppMode.Offline) resetConversation()
        addLog(LogLevel.INFO, TAG, "Chat cleared")
    }

    fun clearLogs() { _uiState.update { it.copy(logs = emptyList()) } }

    fun saveGeneratedImage(image: GeneratedImage): String? {
        return try {
            val bytes = Base64.decode(image.base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val ext = if (image.mimeType.contains("png")) "png" else "jpg"
            val fileName = "MEDHA_${System.currentTimeMillis()}.$ext"
            val format = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, image.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MEDHA")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = application.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            application.contentResolver.openOutputStream(uri)?.use { bitmap.compress(format, 95, it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                application.contentResolver.update(uri, values, null, null)
            }
            uri.toString()
        } catch (e: Exception) { null }
    }

    override fun onCleared() {
        super.onCleared()
        destroyEngine()
    }
}

enum class ImageResponseStyle(val label: String, val icon: String, val description: String) {
    SHORT("Short", "\u26A1", "Quick 2-3 sentence summary"),
    DETAILED("Detailed", "\uD83D\uDD0D", "Thorough and detailed response"),
    FULL("Full Analysis", "\uD83D\uDCDD", "Comprehensive, covers everything"),
    BULLET_POINTS("Bullet Points", "\uD83D\uDCCB", "Organized as bullet points"),
    TECHNICAL("Technical", "\u2699\uFE0F", "Technical with precise terms")
}
