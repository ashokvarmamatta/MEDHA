package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.content.ContentValues
import android.content.Intent
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
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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
        private const val MODELS_DIR = "offline_models"
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

                val customGMs = settingsRepository.customGrandMastersFlow.first()

                _uiState.update {
                    it.copy(
                        apiKeys = savedKeys,
                        onlineModelName = savedModel,
                        appMode = appMode,
                        customGrandMasters = customGMs
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

    private fun modelsDir(): java.io.File {
        val dir = java.io.File(application.filesDir, MODELS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun scanAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = modelsDir()
                val models = dir.listFiles()
                    ?.filter { file ->
                        file.isFile && SUPPORTED_EXTENSIONS.any { file.name.endsWith(it, ignoreCase = true) }
                    }
                    ?.map { file -> ModelInfo.fromFileName(file.name, file.absolutePath, file.length()) }
                    ?: emptyList()

                _uiState.update { it.copy(availableModels = models) }

                if (models.isNotEmpty()) {
                    addLog(LogLevel.INFO, TAG, "Found ${models.size} model(s) in app storage")
                    if (_uiState.value.selectedModel == null) {
                        _uiState.update { it.copy(selectedModel = models.first()) }
                    }
                } else {
                    addLog(LogLevel.INFO, TAG, "No models imported yet — use Import Model in Settings")
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
                val destFile = java.io.File(modelsDir(), fileName)
                application.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
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
                val file = java.io.File(model.filePath)
                if (file.delete()) {
                    addLog(LogLevel.INFO, TAG, "Deleted model: ${model.fileName}")
                    if (_uiState.value.selectedModel?.filePath == model.filePath) {
                        llmInference?.close()
                        llmInference = null
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

    fun addApiKey(key: String, label: String = "", baseUrl: String = "") {
        if (key.isBlank()) return
        // Don't add duplicate keys
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
        // Fetch models using this key
        fetchOnlineModels(key)
    }

    fun toggleApiKeyEnabled(id: String) {
        _uiState.update { state ->
            state.copy(
                apiKeys = state.apiKeys.map {
                    if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
                }
            )
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
                // Clear models when all keys are removed
                onlineAvailableModels = if (updated.isEmpty()) emptyList() else it.onlineAvailableModels
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

    fun moveApiKey(id: String, direction: Int) {
        _uiState.update { state ->
            val keys = state.apiKeys.toMutableList()
            val fromIndex = keys.indexOfFirst { it.id == id }
            if (fromIndex < 0) return@update state
            val toIndex = fromIndex + direction
            if (toIndex < 0 || toIndex >= keys.size) return@update state
            // Swap
            val temp = keys[fromIndex]
            keys[fromIndex] = keys[toIndex]
            keys[toIndex] = temp
            state.copy(apiKeys = keys)
        }
        saveKeys()
        syncActiveModel()
    }

    /** Update onlineModelName to reflect the first selected model of the highest-priority validated key */
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
        // Prevent testing if this key is already being tested
        if (_uiState.value.isTestingKeyId == keyId) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isTestingKeyId = keyId, apiKeyTestResult = "Testing ${entry.label}...") }

                // Use a default model if none selected yet
                val modelName = _uiState.value.onlineModelName.ifBlank { "gemini-2.0-flash" }

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

            // Initialize progress: all models pending (null = pending)
            val initialProgress = models.associate { it.modelId to null as String? }
            _uiState.update { it.copy(checkingAllModelsKeyId = keyId, modelCheckProgress = initialProgress) }
            addLog(LogLevel.INFO, TAG, "Checking ${models.size} models for ${entry.label}...")

            // checkedResults: null = pass, non-null string = error message
            val checkedResults = mutableMapOf<String, String?>()
            val passedModels = mutableListOf<String>()

            for (model in models) {
                try {
                    val request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Hi")))
                        ),
                        generationConfig = GenerationConfig(maxOutputTokens = 5, temperature = 0.1f)
                    )
                    val response = api.generateContent(model.modelId, entry.key, request)

                    if (response.error != null) {
                        val errMsg = response.error.message ?: "Error code: ${response.error.code}"
                        checkedResults[model.modelId] = errMsg
                        _uiState.update {
                            it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to errMsg))
                        }
                        addLog(LogLevel.WARNING, TAG, "${model.modelId}: FAIL - $errMsg")
                    } else if (response.candidates.isNullOrEmpty() ||
                        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text == null) {
                        val errMsg = "Empty response"
                        checkedResults[model.modelId] = errMsg
                        _uiState.update {
                            it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to errMsg))
                        }
                        addLog(LogLevel.WARNING, TAG, "${model.modelId}: FAIL - Empty response")
                    } else {
                        checkedResults[model.modelId] = null // null = success
                        passedModels.add(model.modelId)
                        _uiState.update {
                            it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to ""))  // empty = pass in progress
                        }
                        addLog(LogLevel.INFO, TAG, "${model.modelId}: PASS")
                    }
                } catch (e: Exception) {
                    val errMsg = e.message?.take(80) ?: "Unknown error"
                    checkedResults[model.modelId] = errMsg
                    _uiState.update {
                        it.copy(modelCheckProgress = it.modelCheckProgress + (model.modelId to errMsg))
                    }
                    addLog(LogLevel.WARNING, TAG, "${model.modelId}: FAIL - ${e.message?.take(50)}")
                }
            }

            // Save results — auto-select all passing models
            _uiState.update { state ->
                state.copy(
                    apiKeys = state.apiKeys.map {
                        if (it.id == keyId) it.copy(
                            checkedModels = checkedResults,
                            selectedModels = passedModels,
                            isValidated = passedModels.isNotEmpty(),
                            lastError = if (passedModels.isEmpty()) "No models passed" else null
                        ) else it
                    },
                    checkingAllModelsKeyId = null,
                    modelCheckProgress = emptyMap(),
                    apiKeyTestResult = "${passedModels.size}/${models.size} models working for ${entry.label}",
                    modelStatus = if (passedModels.isNotEmpty()) ModelStatus.Ready else _uiState.value.modelStatus
                )
            }
            saveKeys()
            syncActiveModel()
            addLog(LogLevel.INFO, TAG, "Check complete: ${passedModels.size}/${models.size} passed for ${entry.label}")
        }
    }

    fun testSingleModel(keyId: String, modelId: String) {
        val entry = _uiState.value.apiKeys.find { it.id == keyId } ?: return
        val testingKey = "$keyId:$modelId"
        if (_uiState.value.testingSingleModel == testingKey) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(testingSingleModel = testingKey) }
                addLog(LogLevel.INFO, TAG, "Testing $modelId for ${entry.label}...")

                val api = geminiApi ?: run {
                    _uiState.update { it.copy(testingSingleModel = null, apiKeyTestResult = "API client error") }
                    return@launch
                }

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Hi")))
                    ),
                    generationConfig = GenerationConfig(maxOutputTokens = 5, temperature = 0.1f)
                )

                val response = api.generateContent(modelId, entry.key, request)

                val errorMsg: String? = when {
                    response.error != null -> response.error.message ?: "Error code: ${response.error.code}"
                    response.candidates.isNullOrEmpty() -> "Empty response"
                    response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text == null -> "Empty response"
                    else -> null // success
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
                                it.copy(
                                    checkedModels = updatedChecked,
                                    selectedModels = updatedSelected,
                                    isValidated = updatedSelected.isNotEmpty() || it.isValidated
                                )
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
                            if (it.id == keyId) {
                                it.copy(
                                    checkedModels = it.checkedModels + (modelId to errMsg),
                                    selectedModels = it.selectedModels - modelId
                                )
                            } else it
                        },
                        testingSingleModel = null,
                        apiKeyTestResult = "$modelId failed: $errMsg"
                    )
                }
                saveKeys()
                syncActiveModel()
            }
        }
    }

    fun toggleModelForKey(keyId: String, modelId: String) {
        _uiState.update { state ->
            state.copy(
                apiKeys = state.apiKeys.map { entry ->
                    if (entry.id == keyId) {
                        val updated = if (modelId in entry.selectedModels) {
                            entry.selectedModels - modelId
                        } else {
                            entry.selectedModels + modelId
                        }
                        entry.copy(selectedModels = updated)
                    } else entry
                }
            )
        }
        saveKeys()
        syncActiveModel()
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

    /** Called when user taps a built-in Grand Master — checks for saved chat first */
    fun requestActivateGrandMaster(grandMaster: GrandMaster) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedMessages = settingsRepository.loadChatHistory("gm_${grandMaster.name}")
            if (savedMessages.isNotEmpty()) {
                // Previous chat exists — ask user to resume or reset
                _uiState.update {
                    it.copy(
                        showGrandMasterPicker = false,
                        showResumeOrResetDialog = true,
                        pendingGrandMaster = grandMaster,
                        pendingCustomGrandMaster = null
                    )
                }
            } else {
                // No saved chat — start fresh
                activateGrandMasterFresh(grandMaster)
            }
        }
    }

    /** Called when user taps a custom Grand Master — checks for saved chat first */
    fun requestActivateCustomGrandMaster(custom: CustomGrandMaster) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedMessages = settingsRepository.loadChatHistory(custom.chatHistoryKey)
            if (savedMessages.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        showGrandMasterPicker = false,
                        showResumeOrResetDialog = true,
                        pendingGrandMaster = null,
                        pendingCustomGrandMaster = custom
                    )
                }
            } else {
                activateCustomGrandMasterFresh(custom)
            }
        }
    }

    /** Resume previous chat for the pending Grand Master */
    fun resumeGrandMasterChat() {
        val pendingGM = _uiState.value.pendingGrandMaster
        val pendingCustom = _uiState.value.pendingCustomGrandMaster
        viewModelScope.launch(Dispatchers.IO) {
            if (pendingGM != null) {
                val saved = settingsRepository.loadChatHistory("gm_${pendingGM.name}")
                _uiState.update {
                    it.copy(
                        activeGrandMaster = pendingGM,
                        activeCustomGrandMaster = null,
                        showResumeOrResetDialog = false,
                        showGrandMasterPicker = false,
                        pendingGrandMaster = null,
                        messages = saved
                    )
                }
                addLog(LogLevel.INFO, TAG, "Resumed ${pendingGM.title} chat (${saved.size} messages)")
            } else if (pendingCustom != null) {
                val saved = settingsRepository.loadChatHistory(pendingCustom.chatHistoryKey)
                _uiState.update {
                    it.copy(
                        activeGrandMaster = null,
                        activeCustomGrandMaster = pendingCustom,
                        showResumeOrResetDialog = false,
                        showGrandMasterPicker = false,
                        pendingCustomGrandMaster = null,
                        messages = saved
                    )
                }
                addLog(LogLevel.INFO, TAG, "Resumed custom ${pendingCustom.title} chat (${saved.size} messages)")
            }
        }
    }

    /** Reset and start fresh for the pending Grand Master */
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
        _uiState.update {
            it.copy(showResumeOrResetDialog = false, pendingGrandMaster = null, pendingCustomGrandMaster = null)
        }
    }

    private fun activateGrandMasterFresh(grandMaster: GrandMaster) {
        _uiState.update {
            it.copy(
                activeGrandMaster = grandMaster,
                activeCustomGrandMaster = null,
                showGrandMasterPicker = false,
                showResumeOrResetDialog = false,
                pendingGrandMaster = null,
                messages = listOf(
                    Message(text = grandMaster.welcomeMessage, user = User.AI)
                )
            )
        }
        addLog(LogLevel.INFO, TAG, "Activated Grand Master: ${grandMaster.title}")
    }

    private fun activateCustomGrandMasterFresh(custom: CustomGrandMaster) {
        val welcome = custom.welcomeMessage.ifBlank { "Hello! I'm your ${custom.title}. How can I help you today?" }
        _uiState.update {
            it.copy(
                activeGrandMaster = null,
                activeCustomGrandMaster = custom,
                showGrandMasterPicker = false,
                showResumeOrResetDialog = false,
                pendingCustomGrandMaster = null,
                messages = listOf(
                    Message(text = welcome, user = User.AI)
                )
            )
        }
        addLog(LogLevel.INFO, TAG, "Activated custom Grand Master: ${custom.title}")
    }

    fun exitGrandMaster() {
        // Save chat before exiting
        saveCurrentGrandMasterChat()
        _uiState.update {
            it.copy(activeGrandMaster = null, activeCustomGrandMaster = null, messages = emptyList())
        }
        addLog(LogLevel.INFO, TAG, "Exited Grand Master mode")
    }

    /** Save current Grand Master chat to persistence */
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

    // ==================== CUSTOM GRAND MASTER ====================

    fun showCreateGrandMaster() {
        _uiState.update { it.copy(showCreateGrandMaster = true) }
    }

    fun hideCreateGrandMaster() {
        _uiState.update { it.copy(showCreateGrandMaster = false) }
    }

    fun createCustomGrandMaster(
        icon: String,
        title: String,
        subtitle: String,
        description: String,
        systemPrompt: String,
        welcomeMessage: String
    ) {
        if (title.isBlank() || systemPrompt.isBlank()) return
        val custom = CustomGrandMaster(
            icon = icon.ifBlank { "\uD83C\uDF1F" },
            title = title.trim(),
            subtitle = subtitle.trim(),
            description = description.trim(),
            systemPrompt = systemPrompt.trim(),
            welcomeMessage = welcomeMessage.trim()
        )
        _uiState.update {
            it.copy(
                customGrandMasters = it.customGrandMasters + custom,
                showCreateGrandMaster = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveCustomGrandMasters(_uiState.value.customGrandMasters)
        }
        addLog(LogLevel.INFO, TAG, "Created custom Grand Master: ${custom.title}")
    }

    /** Parse JSON text to create a custom Grand Master */
    fun createCustomGrandMasterFromJson(jsonText: String) {
        try {
            val obj = JSONObject(jsonText)
            createCustomGrandMaster(
                icon = obj.optString("icon", "\uD83C\uDF1F"),
                title = obj.getString("title"),
                subtitle = obj.optString("subtitle", ""),
                description = obj.optString("description", ""),
                systemPrompt = obj.getString("systemPrompt"),
                welcomeMessage = obj.optString("welcomeMessage", "")
            )
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Invalid JSON for Grand Master: ${e.message}")
            _uiState.update { it.copy(apiKeyTestResult = "Invalid JSON: ${e.message?.take(80)}") }
        }
    }

    fun deleteCustomGrandMaster(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val custom = _uiState.value.customGrandMasters.find { it.id == id }
            if (custom != null) {
                settingsRepository.clearChatHistory(custom.chatHistoryKey)
            }
            _uiState.update {
                it.copy(customGrandMasters = it.customGrandMasters.filter { gm -> gm.id != id })
            }
            settingsRepository.saveCustomGrandMasters(_uiState.value.customGrandMasters)
            addLog(LogLevel.INFO, TAG, "Deleted custom Grand Master: ${custom?.title}")
        }
    }

    /** Export a single custom Grand Master as JSON via share intent */
    fun exportCustomGrandMaster(id: String) {
        val custom = _uiState.value.customGrandMasters.find { it.id == id } ?: return
        val json = JSONObject().apply {
            put("icon", custom.icon)
            put("title", custom.title)
            put("subtitle", custom.subtitle)
            put("description", custom.description)
            put("systemPrompt", custom.systemPrompt)
            put("welcomeMessage", custom.welcomeMessage)
        }.toString(2)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "MEDHA Grand Master: ${custom.title}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(Intent.createChooser(intent, "Export Grand Master").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        addLog(LogLevel.INFO, TAG, "Exported Grand Master: ${custom.title}")
    }

    /** Export all custom Grand Masters as a JSON array via share intent */
    fun exportAllCustomGrandMasters() {
        val masters = _uiState.value.customGrandMasters
        if (masters.isEmpty()) return
        val arr = JSONArray()
        for (gm in masters) {
            arr.put(JSONObject().apply {
                put("icon", gm.icon)
                put("title", gm.title)
                put("subtitle", gm.subtitle)
                put("description", gm.description)
                put("systemPrompt", gm.systemPrompt)
                put("welcomeMessage", gm.welcomeMessage)
            })
        }
        val json = arr.toString(2)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "MEDHA Grand Masters (${masters.size})")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(Intent.createChooser(intent, "Export All Grand Masters").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        addLog(LogLevel.INFO, TAG, "Exported ${masters.size} Grand Masters")
    }

    /** Import Grand Masters from JSON text - supports single object or array */
    fun importGrandMastersFromJson(jsonText: String) {
        try {
            val trimmed = jsonText.trim()
            if (trimmed.startsWith("[")) {
                // Array of Grand Masters
                val arr = JSONArray(trimmed)
                var imported = 0
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    createCustomGrandMaster(
                        icon = obj.optString("icon", "\uD83C\uDF1F"),
                        title = obj.getString("title"),
                        subtitle = obj.optString("subtitle", ""),
                        description = obj.optString("description", ""),
                        systemPrompt = obj.getString("systemPrompt"),
                        welcomeMessage = obj.optString("welcomeMessage", "")
                    )
                    imported++
                }
                addLog(LogLevel.INFO, TAG, "Imported $imported Grand Masters from JSON array")
            } else {
                // Single Grand Master
                createCustomGrandMasterFromJson(trimmed)
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, TAG, "Import failed: ${e.message}")
            _uiState.update { it.copy(apiKeyTestResult = "Import failed: ${e.message?.take(80)}") }
        }
    }

    /** Import Grand Masters from a file URI */
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
                val systemPrompt = _uiState.value.activeGrandMaster?.systemPrompt
                    ?: _uiState.value.activeCustomGrandMaster?.systemPrompt
                val fullPrompt = if (systemPrompt != null) {
                    val recentMessages = _uiState.value.messages
                        .drop(1) // skip welcome message
                        .takeLast(6) // keep last 3 exchanges to fit in context
                        .dropLast(1) // drop the user message we just added
                    val historyText = if (recentMessages.isNotEmpty()) {
                        recentMessages.joinToString("\n") { msg ->
                            if (msg.user is User.Person) "User: ${msg.text}" else "Assistant: ${msg.text}"
                        } + "\n"
                    } else ""
                    "$systemPrompt\n\n${historyText}User: $prompt\nAssistant:"
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
                val gmSystemPrompt = _uiState.value.activeGrandMaster?.systemPrompt
                    ?: _uiState.value.activeCustomGrandMaster?.systemPrompt
                val gmTitle = _uiState.value.activeGrandMaster?.title
                    ?: _uiState.value.activeCustomGrandMaster?.title
                if (gmSystemPrompt != null) {
                    contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = "System instruction: $gmSystemPrompt"))))
                    contents.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = "Understood. I will act as the $gmTitle as instructed."))))
                }

                val prevMessages = _uiState.value.messages.dropLast(1)
                // Skip the welcome message (first AI message from Grand Master)
                val isGrandMasterActive = gmSystemPrompt != null
                val messagesToSend = if (isGrandMasterActive && prevMessages.isNotEmpty()) prevMessages.drop(1) else prevMessages
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

        // Build list of (keyIndex, modelId) pairs to try
        // For each key: try its selectedModels (starting with the requested modelName), then fall back
        val attempts = mutableListOf<Pair<Int, String>>()
        for (i in validKeys.indices) {
            val idx = (startIndex + i) % validKeys.size
            val entry = validKeys[idx]
            val keyModels = if (entry.selectedModels.isNotEmpty()) {
                // Put the requested model first if it's in selectedModels, then others
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
                    // Retryable errors: rate limit, server errors
                    if (code in listOf(429, 500, 503)) {
                        errors.add("${entry.label}/$tryModel: ${response.error.message}")
                        addLog(LogLevel.WARNING, TAG, "${entry.label}/$tryModel failed ($code), trying next...")
                        continue
                    }
                    // Non-retryable (400, 403, etc) — return as-is
                    _uiState.update { it.copy(activeKeyIndex = idx, onlineModelName = tryModel) }
                    return response
                }

                // Success
                _uiState.update { it.copy(activeKeyIndex = idx, onlineModelName = tryModel) }
                updateKeyState(entry.id, isValidated = true, lastError = null)
                return response

            } catch (e: Exception) {
                errors.add("${entry.label}/$tryModel: ${e.message?.take(60)}")
                addLog(LogLevel.WARNING, TAG, "${entry.label}/$tryModel exception: ${e.message}, trying next...")
                continue
            }
        }

        // All keys & models exhausted
        addLog(LogLevel.ERROR, TAG, "All API keys and models exhausted")
        appendAiMessage("All API keys and models exhausted:\n\n${errors.joinToString("\n") { "- $it" }}\n\nAdd more keys or wait for rate limits to reset.")
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
        saveCurrentGrandMasterChat()
    }

    private fun appendAiMessageWithImages(text: String, images: List<GeneratedImage>) {
        _uiState.update {
            it.copy(messages = it.messages + Message(text = text, user = User.AI, generatedImages = images))
        }
        saveCurrentGrandMasterChat()
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
        // Clear saved chat for current Grand Master
        val state = _uiState.value
        val key = when {
            state.activeGrandMaster != null -> "gm_${state.activeGrandMaster.name}"
            state.activeCustomGrandMaster != null -> state.activeCustomGrandMaster.chatHistoryKey
            else -> null
        }
        if (key != null) {
            viewModelScope.launch(Dispatchers.IO) {
                settingsRepository.clearChatHistory(key)
            }
        }
        _uiState.update { it.copy(messages = emptyList(), pendingImageUri = null, activeGrandMaster = null, activeCustomGrandMaster = null) }
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
