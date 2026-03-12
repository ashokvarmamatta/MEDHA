package com.ashes.dev.works.ai.neural.brain.medha.domain.model

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
    val showPromptTemplates: Boolean = false
)
