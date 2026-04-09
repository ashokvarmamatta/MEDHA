package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiModelInfo

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isGenerating: Boolean = false,
    val modelStatus: ModelStatus = ModelStatus.Idle,
    val logs: List<LogEntry> = emptyList(),
    val appMode: AppMode = AppMode.Offline,
    val availableModels: List<ModelInfo> = emptyList(),
    val selectedModel: ModelInfo? = null,
    // Multi API key support
    val apiKeys: List<ApiKeyEntry> = emptyList(),
    val activeKeyIndex: Int = 0,
    val onlineModelName: String = "gemini-2.0-flash",
    val pendingImageUri: String? = null,
    val pendingAudioUri: String? = null,
    val showPromptTemplates: Boolean = false,
    val pendingImageTemplate: PromptTemplate? = null,
    val showImageResponseStylePicker: Boolean = false,
    // Online model selection & validation
    val onlineAvailableModels: List<GeminiModelInfo> = emptyList(),
    val isFetchingOnlineModels: Boolean = false,
    val isTestingKeyId: String? = null,
    val apiKeyTestResult: String? = null,
    // "Check All Models" progress per key
    val checkingAllModelsKeyId: String? = null,
    val modelCheckProgress: Map<String, String?> = emptyMap(),
    // Single model test in progress: "keyId:modelId"
    val testingSingleModel: String? = null,
    // Streaming state (LiteRT LM)
    val streamingText: String = "",
    val streamingThinking: String = "",
    val isThinking: Boolean = false,
    // Model catalog download progress (modelId -> progress 0-1)
    val catalogDownloadProgress: Map<String, Float> = emptyMap(),
    // Grand Master mode
    val activeGrandMaster: GrandMaster? = null,
    val activeCustomGrandMaster: CustomGrandMaster? = null,
    val showGrandMasterPicker: Boolean = false,
    val showResumeOrResetDialog: Boolean = false,
    val pendingGrandMaster: GrandMaster? = null,
    val pendingCustomGrandMaster: CustomGrandMaster? = null,
    val customGrandMasters: List<CustomGrandMaster> = emptyList(),
    val showCreateGrandMaster: Boolean = false
) {
    val hasAnyValidatedKey: Boolean get() = apiKeys.any { it.isValidated && it.isEnabled }
    val validatedKeys: List<ApiKeyEntry> get() = apiKeys.filter { it.isValidated && it.isEnabled }
    val activeKey: ApiKeyEntry? get() = validatedKeys.getOrNull(activeKeyIndex.coerceIn(0, (validatedKeys.size - 1).coerceAtLeast(0)))

    /** Image input: online always, offline with Gemma 4 vision models (needs visionBackend=GPU) */
    val supportsImageInput: Boolean get() = appMode is AppMode.Online || (selectedModel?.supportsImage == true)
    /** Whether current mode supports audio input (online only for now) */
    val supportsAudioInput: Boolean get() = appMode is AppMode.Online
}
