package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.content.ContentValues
import android.content.Intent
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
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ChatDatabase
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ChatMessageEntity
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ChatSessionEntity
import com.ashes.dev.works.ai.neural.brain.medha.data.ModelCatalog
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
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.CustomGrandMaster
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GeneratedImage
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GrandMaster
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogLevel
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplate
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.ashes.dev.works.ai.neural.brain.medha.service.MedhaService
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
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

class ChatViewModel(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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

    // Chat history
    private val chatDb = ChatDatabase.getInstance(application)
    val chatSessions = chatDb.chatDao().getAllSessions()
    private var currentSessionId: String = java.util.UUID.randomUUID().toString()

    // Inference config (adjustable from settings)
    var topK: Int = 64
    var topP: Double = 0.95
    var temperature: Double = 1.0
    var maxTokens: Int = 4096
    var enableThinking: Boolean = false
    var outputLanguage: String = "Auto"  // "Auto" = no enforcement, otherwise force responses in that language

    init {
        addLog(LogLevel.INFO, TAG, "MEDHA AI Engine v$APP_VERSION starting (LiteRT LM)...")
        initGeminiApi()
        loadSavedSettings()
    }

    // ── Persistence ─────────────────────────────────────────────────

    private fun loadSavedSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedKeys = settingsRepository.apiKeysFlow.first()
                val savedModel = settingsRepository.selectedModelFlow.first()
                val savedMode = settingsRepository.appModeFlow.first()
                val customGMs = settingsRepository.customGrandMastersFlow.first()

                val appMode = if (savedMode == "online") AppMode.Online else AppMode.Offline

                _uiState.update {
                    it.copy(
                        apiKeys = savedKeys,
                        onlineModelName = savedModel,
                        appMode = appMode,
                        customGrandMasters = customGMs
                    )
                }

                addLog(LogLevel.INFO, TAG, "Loaded ${savedKeys.size} saved API key(s), mode: $savedMode, model: $savedModel")

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

    // ── Gemini Online API ────────────────────────────────────────────

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

    // ── Model Scanning ──────────────────────────────────────────────

    private fun modelsDir(): File {
        val dir = File(application.filesDir, MODELS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun scanAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val models = modelsDir().listFiles()
                    ?.filter { f -> f.isFile && f.length() > 0 && ModelInfo.SUPPORTED_EXTENSIONS.any { f.name.endsWith(it, true) } }
                    ?.map { ModelInfo.fromFileName(it.name, it.absolutePath, it.length()) }
                    ?: emptyList()

                _uiState.update { it.copy(availableModels = models) }

                if (models.isNotEmpty()) {
                    addLog(LogLevel.INFO, TAG, "Found ${models.size} model(s)")
                    if (_uiState.value.selectedModel == null) {
                        val best = models.firstOrNull { it.isLiteRtFormat } ?: models.first()
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

    fun importModelFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                addLog(LogLevel.INFO, TAG, "Importing model: $fileName")
                val destFile = File(modelsDir(), fileName)
                application.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                } ?: throw Exception("Cannot read file")
                addLog(LogLevel.INFO, TAG, "Imported: $fileName (${destFile.length() / (1024 * 1024)} MB)")
                scanAvailableModels()
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Import failed: ${e.message}")
            }
        }
    }

    fun deleteModel(model: ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(model.filePath)
                if (file.delete()) {
                    addLog(LogLevel.INFO, TAG, "Deleted model: ${model.fileName}")
                    if (_uiState.value.selectedModel?.filePath == model.filePath) {
                        destroyEngine()
                        _uiState.update { it.copy(selectedModel = null, modelStatus = ModelStatus.ModelNotFound) }
                    }
                    scanAvailableModels()
                }
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Delete failed: ${e.message}")
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

    // ── Mode Switching ──────────────────────────────────────────────

    fun setAppMode(mode: AppMode) {
        if (_uiState.value.appMode == mode) return
        addLog(LogLevel.INFO, TAG, "Switching to ${if (mode is AppMode.Online) "Online" else "Offline"} mode")
        _uiState.update { it.copy(appMode = mode) }
        saveMode()
        when (mode) {
            is AppMode.Online -> {
                destroyEngine()
                MedhaService.stop(application)
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

    // ── Multi-Key Management ────────────────────────────────────────

    fun addApiKey(key: String, label: String = "", baseUrl: String = "") {
        if (key.isBlank()) return
        if (_uiState.value.apiKeys.any { it.key == key }) {
            addLog(LogLevel.WARNING, TAG, "API key already exists")
            _uiState.update { it.copy(apiKeyTestResult = "This API key already exists.") }
            return
        }
        val newEntry = ApiKeyEntry(
            key = key,
            label = label.ifBlank { "Key ${_uiState.value.apiKeys.size + 1}" },
            baseUrl = baseUrl
        )
        _uiState.update { it.copy(apiKeys = it.apiKeys + newEntry) }
        saveKeys()
        addLog(LogLevel.INFO, TAG, "Added API key: ${newEntry.label}")
        fetchOnlineModels(key)
    }

    fun toggleApiKeyEnabled(id: String) {
        _uiState.update { state ->
            state.copy(apiKeys = state.apiKeys.map {
                if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
            })
        }
        saveKeys()
        syncActiveModel()
    }

    fun removeApiKey(id: String) {
        _uiState.update {
            val updated = it.apiKeys.filter { entry -> entry.id != id }
            it.copy(
                apiKeys = updated,
                activeKeyIndex = it.activeKeyIndex.coerceIn(0, (updated.size - 1).coerceAtLeast(0)),
                onlineAvailableModels = if (updated.isEmpty()) emptyList() else it.onlineAvailableModels
            )
        }
        saveKeys()
        if (_uiState.value.apiKeys.isEmpty() && _uiState.value.appMode is AppMode.Online) {
            _uiState.update { it.copy(modelStatus = ModelStatus.Error("No API keys. Add one in Settings.")) }
        } else if (!_uiState.value.hasAnyValidatedKey && _uiState.value.appMode is AppMode.Online) {
            _uiState.update { it.copy(modelStatus = ModelStatus.Error("No validated API keys. Test in Settings.")) }
        }
        addLog(LogLevel.INFO, TAG, "Removed API key")
    }

    fun moveApiKey(id: String, direction: Int) {
        _uiState.update { state ->
            val keys = state.apiKeys.toMutableList()
            val fromIndex = keys.indexOfFirst { it.id == id }
            if (fromIndex < 0) return@update state
            val toIndex = fromIndex + direction
            if (toIndex < 0 || toIndex >= keys.size) return@update state
            val temp = keys[fromIndex]
            keys[fromIndex] = keys[toIndex]
            keys[toIndex] = temp
            state.copy(apiKeys = keys)
        }
        saveKeys()
        syncActiveModel()
    }

    private fun syncActiveModel() {
        _uiState.update { state ->
            val firstKey = state.apiKeys.firstOrNull { it.isValidated && it.selectedModels.isNotEmpty() }
            val activeModel = firstKey?.selectedModels?.firstOrNull()
            if (activeModel != null && activeModel != state.onlineModelName) {
                state.copy(onlineModelName = activeModel, activeKeyIndex = 0)
            } else state
        }
        saveModel()
    }

    fun fetchOnlineModels(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isFetchingOnlineModels = true, apiKeyTestResult = null) }
                val models = geminiApi?.listModels(apiKey)?.models
                    ?.filter { it.supportsContentGeneration }?.sortedBy { it.modelId } ?: emptyList()

                val autoSelect = models.find { it.modelId == _uiState.value.onlineModelName }
                    ?: models.find { it.modelId.contains("flash") } ?: models.firstOrNull()

                _uiState.update {
                    it.copy(
                        isFetchingOnlineModels = false,
                        onlineAvailableModels = models,
                        onlineModelName = autoSelect?.modelId ?: it.onlineModelName,
                        apiKeyTestResult = if (models.isEmpty()) "No models found." else "Found ${models.size} models."
                    )
                }
                saveModel()
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingOnlineModels = false, apiKeyTestResult = "Error: ${e.message?.take(80)}") }
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
        if (_uiState.value.isTestingKeyId == keyId) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isTestingKeyId = keyId, apiKeyTestResult = "Testing ${entry.label}...") }
                val modelName = _uiState.value.onlineModelName.ifBlank { "gemini-2.0-flash" }
                val api = geminiApi ?: run {
                    _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "API client error") }
                    return@launch
                }
                val request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Say hello in one word.")))),
                    generationConfig = GenerationConfig(maxOutputTokens = 10, temperature = 0.1f)
                )
                val response = api.generateContent(modelName, entry.key, request)

                if (response.error != null) {
                    val errMsg = response.error.message ?: "API error (code: ${response.error.code})"
                    updateKeyState(keyId, isValidated = false, lastError = errMsg)
                    _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "Failed: $errMsg") }
                } else {
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        updateKeyState(keyId, isValidated = true, lastError = null)
                        _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "${entry.label} validated! Response: \"$text\"", modelStatus = ModelStatus.Ready) }
                    } else {
                        updateKeyState(keyId, isValidated = false, lastError = "Empty response")
                        _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "Empty response. Try a different model.") }
                    }
                }
                saveKeys()
            } catch (e: Exception) {
                updateKeyState(keyId, isValidated = false, lastError = e.message)
                _uiState.update { it.copy(isTestingKeyId = null, apiKeyTestResult = "Error: ${e.message?.take(100)}") }
                saveKeys()
            }
        }
    }

    fun checkAllModels(keyId: String) {
        val entry = _uiState.value.apiKeys.find { it.id == keyId } ?: return
        if (_uiState.value.checkingAllModelsKeyId != null) return

        val models = _uiState.value.onlineAvailableModels
        if (models.isEmpty()) {
            _uiState.update { it.copy(apiKeyTestResult = "No models loaded. Add a key & fetch models first.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val api = geminiApi ?: run {
                _uiState.update { it.copy(apiKeyTestResult = "API client error") }
                return@launch
            }

            val initialProgress = models.associate { it.modelId to null as String? }
            _uiState.update { it.copy(checkingAllModelsKeyId = keyId, modelCheckProgress = initialProgress) }

            val checkedResults = mutableMapOf<String, String?>()
            val passedModels = mutableListOf<String>()

            for (model in models) {
                try {
                    val request = GeminiRequest(
                        contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Hi")))),
                        generationConfig = GenerationConfig(maxOutputTokens = 5, temperature = 0.1f)
                    )
                    val response = api.generateContent(model.modelId, entry.key, request)

                    if (response.error != null) {
                        val errMsg = response.error.message ?: "Error code: ${response.error.code}"
                        checkedResults[model.modelId] = errMsg
                        _uiState.update { it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to errMsg)) }
                    } else if (response.candidates.isNullOrEmpty() || response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text == null) {
                        checkedResults[model.modelId] = "Empty response"
                        _uiState.update { it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to "Empty response")) }
                    } else {
                        checkedResults[model.modelId] = null
                        passedModels.add(model.modelId)
                        _uiState.update { it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to "")) }
                    }
                } catch (e: Exception) {
                    val errMsg = e.message?.take(80) ?: "Unknown error"
                    checkedResults[model.modelId] = errMsg
                    _uiState.update { it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to errMsg)) }
                }
            }

            _uiState.update { state ->
                state.copy(
                    apiKeys = state.apiKeys.map {
                        if (it.id == keyId) it.copy(
                            checkedModels = checkedResults, selectedModels = passedModels,
                            isValidated = passedModels.isNotEmpty(),
                            lastError = if (passedModels.isEmpty()) "No models passed" else null
                        ) else it
                    },
                    checkingAllModelsKeyId = null, modelCheckProgress = emptyMap(),
                    apiKeyTestResult = "${passedModels.size}/${models.size} models working for ${entry.label}",
                    modelStatus = if (passedModels.isNotEmpty()) ModelStatus.Ready else _uiState.value.modelStatus
                )
            }
            saveKeys()
            syncActiveModel()
        }
    }

    fun testSingleModel(keyId: String, modelId: String) {
        val entry = _uiState.value.apiKeys.find { it.id == keyId } ?: return
        val testingKey = "$keyId:$modelId"
        if (_uiState.value.testingSingleModel == testingKey) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(testingSingleModel = testingKey) }
                val api = geminiApi ?: run {
                    _uiState.update { it.copy(testingSingleModel = null, apiKeyTestResult = "API client error") }
                    return@launch
                }
                val request = GeminiRequest(
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Hi")))),
                    generationConfig = GenerationConfig(maxOutputTokens = 5, temperature = 0.1f)
                )
                val response = api.generateContent(modelId, entry.key, request)

                val errorMsg: String? = when {
                    response.error != null -> response.error.message ?: "Error code: ${response.error.code}"
                    response.candidates.isNullOrEmpty() -> "Empty response"
                    response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text == null -> "Empty response"
                    else -> null
                }

                _uiState.update { state ->
                    state.copy(
                        apiKeys = state.apiKeys.map {
                            if (it.id == keyId) {
                                val updatedChecked = it.checkedModels + (modelId to errorMsg)
                                val updatedSelected = if (errorMsg == null && modelId !in it.selectedModels) {
                                    it.selectedModels + modelId
                                } else if (errorMsg != null) {
                                    it.selectedModels - modelId
                                } else it.selectedModels
                                it.copy(checkedModels = updatedChecked, selectedModels = updatedSelected, isValidated = updatedSelected.isNotEmpty() || it.isValidated)
                            } else it
                        },
                        testingSingleModel = null,
                        apiKeyTestResult = if (errorMsg == null) "$modelId working for ${entry.label}" else "$modelId failed: $errorMsg",
                        modelStatus = if (errorMsg == null) ModelStatus.Ready else _uiState.value.modelStatus
                    )
                }
                saveKeys()
                syncActiveModel()
            } catch (e: Exception) {
                val errMsg = e.message?.take(80) ?: "Unknown error"
                _uiState.update { state ->
                    state.copy(
                        apiKeys = state.apiKeys.map {
                            if (it.id == keyId) it.copy(checkedModels = it.checkedModels + (modelId to errMsg), selectedModels = it.selectedModels - modelId) else it
                        },
                        testingSingleModel = null, apiKeyTestResult = "$modelId failed: $errMsg"
                    )
                }
                saveKeys()
                syncActiveModel()
            }
        }
    }

    fun toggleModelForKey(keyId: String, modelId: String) {
        _uiState.update { state ->
            state.copy(apiKeys = state.apiKeys.map { entry ->
                if (entry.id == keyId) {
                    val updated = if (modelId in entry.selectedModels) entry.selectedModels - modelId else entry.selectedModels + modelId
                    entry.copy(selectedModels = updated)
                } else entry
            })
        }
        saveKeys()
        syncActiveModel()
    }

    private fun updateKeyState(keyId: String, isValidated: Boolean, lastError: String?) {
        _uiState.update { state ->
            state.copy(apiKeys = state.apiKeys.map {
                if (it.id == keyId) it.copy(isValidated = isValidated, lastError = lastError) else it
            })
        }
    }

    // ── LiteRT LM Engine (Offline) ──────────────────────────────────

    fun initializeEngine() {
        if (_uiState.value.appMode is AppMode.Online) {
            _uiState.update {
                it.copy(modelStatus = when {
                    it.hasAnyValidatedKey -> ModelStatus.Ready
                    it.apiKeys.isNotEmpty() -> ModelStatus.Error("API key(s) not validated. Test in Settings.")
                    else -> ModelStatus.Error("No API keys. Add one in Settings.")
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
                val catalogModel = ModelCatalog.findByFileName(modelToLoad.fileName)
                val hasVision = catalogModel?.supportsImage == true
                val hasAudio = catalogModel?.supportsAudio == true

                val engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    visionBackend = if (hasVision) Backend.GPU() else null,
                    audioBackend = if (hasAudio) Backend.CPU() else null,
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

                val features = buildList {
                    add("CPU")
                    if (hasVision) add("Vision(GPU)")
                    if (hasAudio) add("Audio")
                }.joinToString(", ")
                _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                addLog(LogLevel.INFO, TAG, "Engine ready: ${modelToLoad.displayName} (LiteRT LM, $features, ${modelMaxTokens} tokens)")

                // Start foreground service to keep model alive in background
                MedhaService.start(application)

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

    // ── Send Message ────────────────────────────────────────────────

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
            it.copy(isGenerating = true, messages = it.messages + userMessage,
                pendingImageUri = null, streamingText = "", streamingThinking = "", isThinking = false)
        }
        addLog(LogLevel.DEBUG, TAG, "User: ${displayText.take(80)}${if (imageUri != null) " [+image]" else ""}")

        when (_uiState.value.appMode) {
            is AppMode.Offline -> sendOfflineMessage(prompt, imageUri)
            is AppMode.Online -> sendOnlineMessage(prompt, imageUri)
        }
    }

    // ── Offline: LiteRT LM Streaming ────────────────────────────────

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

                if (imageUri != null) {
                    try {
                        val imageBytes = encodeImageToPng(Uri.parse(imageUri))
                        if (imageBytes != null) {
                            contentParts.add(Content.ImageBytes(imageBytes))
                            addLog(LogLevel.INFO, TAG, "Image attached (${imageBytes.size / 1024}KB PNG, visionBackend=GPU)")
                        }
                    } catch (e: Exception) {
                        addLog(LogLevel.WARNING, TAG, "Image encode failed: ${e.message}")
                    }
                }

                val gmSystemPrompt = _uiState.value.activeGrandMaster?.systemPrompt
                    ?: _uiState.value.activeCustomGrandMaster?.systemPrompt
                val langInstruction = if (outputLanguage != "Auto") {
                    "IMPORTANT: You MUST respond ONLY in $outputLanguage language, regardless of the input language. "
                } else ""
                val basePrompt = when {
                    imageUri != null && prompt.isBlank() -> "Describe this image in detail."
                    gmSystemPrompt != null -> "$gmSystemPrompt\n\nUser: $prompt\nAssistant:"
                    else -> prompt
                }
                val textPrompt = if (langInstruction.isNotEmpty()) "$langInstruction\n\n$basePrompt" else basePrompt
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

    /**
     * Encode image as PNG for LiteRT LM Gemma 4 vision.
     * - Subsample large images to avoid OOM
     * - Resize to max 512px
     * - PNG format (required by LiteRT LM Content.ImageBytes)
     */
    private fun encodeImageToPng(uri: Uri): ByteArray? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            application.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            val origW = opts.outWidth
            val origH = opts.outHeight
            if (origW <= 0 || origH <= 0) return null

            val targetSize = 512
            var sampleSize = 1
            while (origW / sampleSize > targetSize * 2 || origH / sampleSize > targetSize * 2) {
                sampleSize *= 2
            }

            val loadOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val sampled = application.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, loadOpts)
            } ?: return null

            val longest = maxOf(sampled.width, sampled.height)
            val scaled = if (longest > targetSize) {
                val scale = targetSize.toFloat() / longest
                val newW = (sampled.width * scale).toInt().coerceAtLeast(1)
                val newH = (sampled.height * scale).toInt().coerceAtLeast(1)
                addLog(LogLevel.DEBUG, TAG, "Image: ${origW}x${origH} -> ${newW}x${newH}")
                val result = Bitmap.createScaledBitmap(sampled, newW, newH, true)
                if (result !== sampled) sampled.recycle()
                result
            } else {
                addLog(LogLevel.DEBUG, TAG, "Image: ${origW}x${origH} -> ${sampled.width}x${sampled.height}")
                sampled
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            scaled.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Image encode failed: ${e.message}")
            null
        }
    }

    // ── Online: Gemini API with Failover ────────────────────────────

    private fun sendOnlineMessage(prompt: String, imageUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val validKeys = _uiState.value.validatedKeys
                if (validKeys.isEmpty()) {
                    appendAiMessage("No validated API keys. Go to Settings.", provider = "gemini")
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

                // Build conversation context
                val contents = mutableListOf<GeminiContent>()

                // Inject Grand Master system prompt
                val gmSystemPrompt = _uiState.value.activeGrandMaster?.systemPrompt
                    ?: _uiState.value.activeCustomGrandMaster?.systemPrompt
                val gmTitle = _uiState.value.activeGrandMaster?.title
                    ?: _uiState.value.activeCustomGrandMaster?.title
                if (gmSystemPrompt != null) {
                    contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "System instruction: $gmSystemPrompt"))))
                    contents.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = "Understood. I will act as the $gmTitle as instructed."))))
                }
                // Output language enforcement
                if (outputLanguage != "Auto") {
                    contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "IMPORTANT: For all my future messages, you MUST respond ONLY in $outputLanguage language regardless of what language I write in. This is a strict requirement."))))
                    contents.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = "Understood. I will respond exclusively in $outputLanguage from now on."))))
                }

                val prevMessages = _uiState.value.messages.dropLast(1)
                val isGrandMasterActive = gmSystemPrompt != null
                val messagesToSend = if (isGrandMasterActive && prevMessages.isNotEmpty()) prevMessages.drop(1) else prevMessages
                for (msg in messagesToSend) {
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

                // FAILOVER: try each validated key
                val response = sendWithFailover(api, request, modelName, validKeys)
                val elapsed = System.currentTimeMillis() - startTime

                if (response == null) {
                    // All keys exhausted — already handled in sendWithFailover
                } else if (response.error != null) {
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
                        saveCurrentGrandMasterChat()
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

    private suspend fun sendWithFailover(
        api: GeminiApiService,
        request: GeminiRequest,
        modelName: String,
        validKeys: List<ApiKeyEntry>
    ): GeminiResponse? {
        val startIndex = _uiState.value.activeKeyIndex.coerceIn(0, validKeys.lastIndex)
        val errors = mutableListOf<String>()

        val attempts = mutableListOf<Pair<Int, String>>()
        for (i in validKeys.indices) {
            val idx = (startIndex + i) % validKeys.size
            val entry = validKeys[idx]
            val keyModels = if (entry.selectedModels.isNotEmpty()) {
                val ordered = mutableListOf<String>()
                if (modelName in entry.selectedModels) ordered.add(modelName)
                ordered.addAll(entry.selectedModels.filter { it != modelName })
                ordered
            } else {
                listOf(modelName)
            }
            for (model in keyModels) {
                attempts.add(idx to model)
            }
        }

        for ((idx, tryModel) in attempts) {
            val entry = validKeys[idx]
            try {
                addLog(LogLevel.DEBUG, TAG, "Trying ${entry.label} with $tryModel...")
                val response = api.generateContent(tryModel, entry.key, request)

                if (response.error != null) {
                    val code = response.error.code
                    if (code in listOf(429, 500, 503)) {
                        errors.add("${entry.label}/$tryModel: ${response.error.message}")
                        continue
                    }
                    _uiState.update { it.copy(activeKeyIndex = idx, onlineModelName = tryModel) }
                    return response
                }

                _uiState.update { it.copy(activeKeyIndex = idx, onlineModelName = tryModel) }
                updateKeyState(entry.id, isValidated = true, lastError = null)
                return response
            } catch (e: Exception) {
                errors.add("${entry.label}/$tryModel: ${e.message?.take(60)}")
                continue
            }
        }

        appendAiMessage("All API keys and models exhausted:\n\n${errors.joinToString("\n") { "- $it" }}\n\nAdd more keys or wait for rate limits to reset.", provider = "gemini")
        return null
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val bytes = application.contentResolver.openInputStream(uri)?.readBytes() ?: return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    // ── Model Download ──────────────────────────────────────────────

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

    // ── Grand Master ────────────────────────────────────────────────

    fun showGrandMasterPicker() {
        _uiState.update { it.copy(showGrandMasterPicker = true) }
    }

    fun hideGrandMasterPicker() {
        _uiState.update { it.copy(showGrandMasterPicker = false) }
    }

    fun requestActivateGrandMaster(grandMaster: GrandMaster) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedMessages = settingsRepository.loadChatHistory("gm_${grandMaster.name}")
            if (savedMessages.isNotEmpty()) {
                _uiState.update {
                    it.copy(showGrandMasterPicker = false, showResumeOrResetDialog = true,
                        pendingGrandMaster = grandMaster, pendingCustomGrandMaster = null)
                }
            } else {
                activateGrandMasterFresh(grandMaster)
            }
        }
    }

    fun requestActivateCustomGrandMaster(custom: CustomGrandMaster) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedMessages = settingsRepository.loadChatHistory(custom.chatHistoryKey)
            if (savedMessages.isNotEmpty()) {
                _uiState.update {
                    it.copy(showGrandMasterPicker = false, showResumeOrResetDialog = true,
                        pendingGrandMaster = null, pendingCustomGrandMaster = custom)
                }
            } else {
                activateCustomGrandMasterFresh(custom)
            }
        }
    }

    fun resumeGrandMasterChat() {
        val pendingGM = _uiState.value.pendingGrandMaster
        val pendingCustom = _uiState.value.pendingCustomGrandMaster
        viewModelScope.launch(Dispatchers.IO) {
            if (pendingGM != null) {
                val saved = settingsRepository.loadChatHistory("gm_${pendingGM.name}")
                _uiState.update {
                    it.copy(activeGrandMaster = pendingGM, activeCustomGrandMaster = null,
                        showResumeOrResetDialog = false, showGrandMasterPicker = false,
                        pendingGrandMaster = null, messages = saved)
                }
                addLog(LogLevel.INFO, TAG, "Resumed ${pendingGM.title} chat (${saved.size} messages)")
            } else if (pendingCustom != null) {
                val saved = settingsRepository.loadChatHistory(pendingCustom.chatHistoryKey)
                _uiState.update {
                    it.copy(activeGrandMaster = null, activeCustomGrandMaster = pendingCustom,
                        showResumeOrResetDialog = false, showGrandMasterPicker = false,
                        pendingCustomGrandMaster = null, messages = saved)
                }
                addLog(LogLevel.INFO, TAG, "Resumed custom ${pendingCustom.title} chat (${saved.size} messages)")
            }
        }
    }

    fun resetGrandMasterChat() {
        val pendingGM = _uiState.value.pendingGrandMaster
        val pendingCustom = _uiState.value.pendingCustomGrandMaster
        viewModelScope.launch(Dispatchers.IO) {
            if (pendingGM != null) {
                settingsRepository.clearChatHistory("gm_${pendingGM.name}")
                activateGrandMasterFresh(pendingGM)
            } else if (pendingCustom != null) {
                settingsRepository.clearChatHistory(pendingCustom.chatHistoryKey)
                activateCustomGrandMasterFresh(pendingCustom)
            }
        }
    }

    fun dismissResumeOrResetDialog() {
        _uiState.update { it.copy(showResumeOrResetDialog = false, pendingGrandMaster = null, pendingCustomGrandMaster = null) }
    }

    private fun activateGrandMasterFresh(grandMaster: GrandMaster) {
        _uiState.update {
            it.copy(activeGrandMaster = grandMaster, activeCustomGrandMaster = null,
                showGrandMasterPicker = false, showResumeOrResetDialog = false, pendingGrandMaster = null,
                messages = listOf(Message(text = grandMaster.welcomeMessage, user = User.AI)))
        }
        addLog(LogLevel.INFO, TAG, "Activated Grand Master: ${grandMaster.title}")
    }

    private fun activateCustomGrandMasterFresh(custom: CustomGrandMaster) {
        val welcome = custom.welcomeMessage.ifBlank { "Hello! I'm your ${custom.title}. How can I help you today?" }
        _uiState.update {
            it.copy(activeGrandMaster = null, activeCustomGrandMaster = custom,
                showGrandMasterPicker = false, showResumeOrResetDialog = false, pendingCustomGrandMaster = null,
                messages = listOf(Message(text = welcome, user = User.AI)))
        }
        addLog(LogLevel.INFO, TAG, "Activated custom Grand Master: ${custom.title}")
    }

    fun exitGrandMaster() {
        saveCurrentGrandMasterChat()
        _uiState.update { it.copy(activeGrandMaster = null, activeCustomGrandMaster = null, messages = emptyList()) }
        addLog(LogLevel.INFO, TAG, "Exited Grand Master mode")
    }

    private fun saveCurrentGrandMasterChat() {
        val state = _uiState.value
        val messages = state.messages
        if (messages.isEmpty()) return
        val key = when {
            state.activeGrandMaster != null -> "gm_${state.activeGrandMaster.name}"
            state.activeCustomGrandMaster != null -> state.activeCustomGrandMaster.chatHistoryKey
            else -> return
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveChatHistory(key, messages)
        }
    }

    // ── Custom Grand Master ─────────────────────────────────────────

    fun showCreateGrandMaster() {
        _uiState.update { it.copy(showCreateGrandMaster = true) }
    }

    fun hideCreateGrandMaster() {
        _uiState.update { it.copy(showCreateGrandMaster = false) }
    }

    fun createCustomGrandMaster(icon: String, title: String, subtitle: String, description: String, systemPrompt: String, welcomeMessage: String) {
        if (title.isBlank() || systemPrompt.isBlank()) return
        val custom = CustomGrandMaster(
            icon = icon.ifBlank { "\uD83C\uDF1F" }, title = title.trim(), subtitle = subtitle.trim(),
            description = description.trim(), systemPrompt = systemPrompt.trim(), welcomeMessage = welcomeMessage.trim()
        )
        _uiState.update { it.copy(customGrandMasters = it.customGrandMasters + custom, showCreateGrandMaster = false) }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveCustomGrandMasters(_uiState.value.customGrandMasters)
        }
        addLog(LogLevel.INFO, TAG, "Created custom Grand Master: ${custom.title}")
    }

    fun createCustomGrandMasterFromJson(jsonText: String) {
        try {
            val obj = JSONObject(jsonText)
            createCustomGrandMaster(
                icon = obj.optString("icon", "\uD83C\uDF1F"), title = obj.getString("title"),
                subtitle = obj.optString("subtitle", ""), description = obj.optString("description", ""),
                systemPrompt = obj.getString("systemPrompt"), welcomeMessage = obj.optString("welcomeMessage", "")
            )
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Invalid JSON for Grand Master: ${e.message}")
            _uiState.update { it.copy(apiKeyTestResult = "Invalid JSON: ${e.message?.take(80)}") }
        }
    }

    fun deleteCustomGrandMaster(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val custom = _uiState.value.customGrandMasters.find { it.id == id }
            if (custom != null) settingsRepository.clearChatHistory(custom.chatHistoryKey)
            _uiState.update { it.copy(customGrandMasters = it.customGrandMasters.filter { gm -> gm.id != id }) }
            settingsRepository.saveCustomGrandMasters(_uiState.value.customGrandMasters)
        }
    }

    fun exportCustomGrandMaster(id: String) {
        val custom = _uiState.value.customGrandMasters.find { it.id == id } ?: return
        val json = JSONObject().apply {
            put("icon", custom.icon); put("title", custom.title); put("subtitle", custom.subtitle)
            put("description", custom.description); put("systemPrompt", custom.systemPrompt)
            put("welcomeMessage", custom.welcomeMessage)
        }.toString(2)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "MEDHA Grand Master: ${custom.title}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(Intent.createChooser(intent, "Export Grand Master").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun exportAllCustomGrandMasters() {
        val masters = _uiState.value.customGrandMasters
        if (masters.isEmpty()) return
        val arr = JSONArray()
        for (gm in masters) {
            arr.put(JSONObject().apply {
                put("icon", gm.icon); put("title", gm.title); put("subtitle", gm.subtitle)
                put("description", gm.description); put("systemPrompt", gm.systemPrompt)
                put("welcomeMessage", gm.welcomeMessage)
            })
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, arr.toString(2))
            putExtra(Intent.EXTRA_SUBJECT, "MEDHA Grand Masters (${masters.size})")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(Intent.createChooser(intent, "Export All Grand Masters").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun importGrandMastersFromJson(jsonText: String) {
        try {
            val trimmed = jsonText.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                var imported = 0
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    createCustomGrandMaster(
                        icon = obj.optString("icon", "\uD83C\uDF1F"), title = obj.getString("title"),
                        subtitle = obj.optString("subtitle", ""), description = obj.optString("description", ""),
                        systemPrompt = obj.getString("systemPrompt"), welcomeMessage = obj.optString("welcomeMessage", "")
                    )
                    imported++
                }
                addLog(LogLevel.INFO, TAG, "Imported $imported Grand Masters from JSON array")
            } else {
                createCustomGrandMasterFromJson(trimmed)
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Import failed: ${e.message}")
            _uiState.update { it.copy(apiKeyTestResult = "Import failed: ${e.message?.take(80)}") }
        }
    }

    fun importGrandMastersFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = application.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text.isNullOrBlank()) {
                    addLog(LogLevel.ERROR, TAG, "Import file is empty")
                    return@launch
                }
                importGrandMastersFromJson(text)
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Failed to read import file: ${e.message}")
            }
        }
    }

    // ── Prompt Templates & Image ────────────────────────────────────

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

    fun setPendingAudio(uri: String?) {
        _uiState.update { it.copy(pendingAudioUri = uri) }
        if (uri != null) {
            // If there's a pending template, auto-send with the audio
            val template = _uiState.value.pendingImageTemplate
            if (template != null) {
                _uiState.update { it.copy(pendingImageTemplate = null) }
                addLog(LogLevel.INFO, TAG, "Audio attached, sending with template: ${template.title}")
                sendMessage(template.promptPrefix)
            }
        }
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

    // ── Model Configuration Dialog ──────────────────────────────
    fun showConfigDialog() { _uiState.update { it.copy(showConfigDialog = true) } }
    fun hideConfigDialog() { _uiState.update { it.copy(showConfigDialog = false) } }

    fun applyConfig(newTopK: Int, newTopP: Double, newTemperature: Double, newMaxTokens: Int, useGpu: Boolean, thinking: Boolean, language: String = "Auto") {
        topK = newTopK
        topP = newTopP
        temperature = newTemperature
        maxTokens = newMaxTokens
        enableThinking = thinking
        outputLanguage = language
        _uiState.update { it.copy(showConfigDialog = false) }
        addLog(LogLevel.INFO, TAG, "Config updated: topK=$topK topP=$topP temp=$temperature maxTokens=$maxTokens gpu=$useGpu thinking=$thinking lang=$language")
        // Reinitialize engine with new config
        destroyEngine()
        initializeEngine()
    }
    fun hidePromptTemplates() { _uiState.update { it.copy(showPromptTemplates = false) } }

    // ── Chat History ──────────────────────────────────────────────────

    private fun saveCurrentChatSession() {
        val messages = _uiState.value.messages
        if (messages.isEmpty()) return
        // Don't save Grand Master chats here — they use SettingsRepository
        if (_uiState.value.activeGrandMaster != null || _uiState.value.activeCustomGrandMaster != null) return

        val sessionId = currentSessionId
        val title = messages.firstOrNull { it.user is User.Person }?.text?.take(80) ?: "New Chat"
        val modelName = _uiState.value.selectedModel?.displayName ?: _uiState.value.onlineModelName

        viewModelScope.launch(Dispatchers.IO) {
            chatDb.chatDao().insertSession(
                ChatSessionEntity(
                    id = sessionId, title = title,
                    createdAt = messages.first().timestamp,
                    updatedAt = messages.last().timestamp,
                    messageCount = messages.size, modelUsed = modelName
                )
            )
            chatDb.chatDao().deleteMessages(sessionId)
            chatDb.chatDao().insertMessages(messages.map { msg ->
                ChatMessageEntity(
                    id = msg.id, sessionId = sessionId,
                    role = if (msg.user is User.Person) "user" else "assistant",
                    content = msg.text, timestamp = msg.timestamp,
                    imageUri = msg.imageUri, isError = false,
                    tokenCount = msg.tokenCount, latencyMs = msg.latencyMs,
                    tokensPerSec = msg.tokensPerSec, provider = msg.provider,
                    timeToFirstTokenMs = msg.timeToFirstTokenMs,
                    thinkingText = msg.thinkingText
                )
            })
        }
    }

    fun loadChatSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = chatDb.chatDao().getMessages(sessionId)
            val messages = entities.map { e ->
                Message(
                    id = e.id, text = e.content,
                    user = if (e.role == "user") User.Person else User.AI,
                    timestamp = e.timestamp, imageUri = e.imageUri,
                    tokenCount = e.tokenCount, latencyMs = e.latencyMs,
                    tokensPerSec = e.tokensPerSec, provider = e.provider,
                    timeToFirstTokenMs = e.timeToFirstTokenMs,
                    thinkingText = e.thinkingText
                )
            }
            currentSessionId = sessionId
            _uiState.update { it.copy(messages = messages, activeGrandMaster = null, activeCustomGrandMaster = null) }
            addLog(LogLevel.INFO, TAG, "Loaded session: ${messages.size} messages")
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDb.chatDao().deleteSession(sessionId)
            if (currentSessionId == sessionId) {
                currentSessionId = java.util.UUID.randomUUID().toString()
                _uiState.update { it.copy(messages = emptyList()) }
            }
        }
    }

    fun deleteAllChatSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDb.chatDao().deleteAllSessions()
            currentSessionId = java.util.UUID.randomUUID().toString()
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    fun startNewChat() {
        saveCurrentChatSession()
        currentSessionId = java.util.UUID.randomUUID().toString()
        _uiState.update { it.copy(messages = emptyList(), activeGrandMaster = null, activeCustomGrandMaster = null, streamingText = "", streamingThinking = "") }
        if (_uiState.value.appMode is AppMode.Offline) resetConversation()
        addLog(LogLevel.INFO, TAG, "New chat started")
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun appendAiMessage(text: String, provider: String = "") {
        _uiState.update { it.copy(messages = it.messages + Message(text = text, user = User.AI, provider = provider)) }
        saveCurrentGrandMasterChat()
        saveCurrentChatSession()
    }

    private fun appendAiMessageWithStats(text: String, tokens: Int, latency: Long, tps: Float, ttft: Long, provider: String, thinking: String? = null) {
        _uiState.update {
            it.copy(messages = it.messages + Message(
                text = text, user = User.AI, tokenCount = tokens, latencyMs = latency, tokensPerSec = tps,
                thinkingText = thinking, provider = provider, timeToFirstTokenMs = ttft
            ))
        }
        saveCurrentGrandMasterChat()
        saveCurrentChatSession()
    }

    private fun appendAiMessageWithImages(text: String, images: List<GeneratedImage>) {
        _uiState.update { it.copy(messages = it.messages + Message(text = text, user = User.AI, generatedImages = images)) }
        saveCurrentGrandMasterChat()
        saveCurrentChatSession()
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

    fun clearChat() {
        val state = _uiState.value
        val key = when {
            state.activeGrandMaster != null -> "gm_${state.activeGrandMaster.name}"
            state.activeCustomGrandMaster != null -> state.activeCustomGrandMaster.chatHistoryKey
            else -> null
        }
        if (key != null) {
            viewModelScope.launch(Dispatchers.IO) { settingsRepository.clearChatHistory(key) }
        }
        _uiState.update { it.copy(messages = emptyList(), pendingImageUri = null, streamingText = "", streamingThinking = "",
            activeGrandMaster = null, activeCustomGrandMaster = null) }
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
        MedhaService.stop(application)
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
