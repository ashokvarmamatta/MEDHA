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
    val apiKey: String = "",
    val onlineModelName: String = "gemini-2.0-flash",
    val pendingImageUri: String? = null,
    val showPromptTemplates: Boolean = false,
    val pendingImageTemplate: PromptTemplate? = null,
    val showImageResponseStylePicker: Boolean = false,
    // Online model selection & validation
    val onlineAvailableModels: List<GeminiModelInfo> = emptyList(),
    val isFetchingOnlineModels: Boolean = false,
    val isTestingApiKey: Boolean = false,
    val apiKeyValidated: Boolean = false,
    val apiKeyTestResult: String? = null
)
