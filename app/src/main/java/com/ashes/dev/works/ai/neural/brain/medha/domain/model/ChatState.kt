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
    val showPromptTemplates: Boolean = false,
    val pendingImageTemplate: PromptTemplate? = null,
    val showImageResponseStylePicker: Boolean = false,
    // Online model selection & validation
    val onlineAvailableModels: List<GeminiModelInfo> = emptyList(),
    val isFetchingOnlineModels: Boolean = false,
    val isTestingKeyId: String? = null,
    val apiKeyTestResult: String? = null,
    // Grand Master mode
    val activeGrandMaster: GrandMaster? = null,
    val showGrandMasterPicker: Boolean = false
) {
    val hasAnyValidatedKey: Boolean get() = apiKeys.any { it.isValidated }
    val validatedKeys: List<ApiKeyEntry> get() = apiKeys.filter { it.isValidated }
    val activeKey: ApiKeyEntry? get() = validatedKeys.getOrNull(activeKeyIndex.coerceIn(0, (validatedKeys.size - 1).coerceAtLeast(0)))
}
