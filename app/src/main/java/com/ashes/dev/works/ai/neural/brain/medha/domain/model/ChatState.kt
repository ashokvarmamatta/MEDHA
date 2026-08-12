package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import com.ashes.dev.works.ai.neural.brain.medha.data.local.ModelLocation

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isGenerating: Boolean = false,
    val modelStatus: ModelStatus = ModelStatus.Idle,
    val logs: List<LogEntry> = emptyList(),
    val availableModels: List<ModelInfo> = emptyList(),
    val selectedModel: ModelInfo? = null,
    val pendingImageUri: String? = null,
    val pendingAudioUri: String? = null,
    val showPromptTemplates: Boolean = false,
    val pendingImageTemplate: PromptTemplate? = null,
    val showImageResponseStylePicker: Boolean = false,
    // Streaming state (LiteRT LM)
    val streamingText: String = "",
    val streamingThinking: String = "",
    val isThinking: Boolean = false,
    // Live decode stats, updated as tokens arrive — the same numbers the finished message
    // shows, but visible while you are waiting rather than only afterwards.
    val streamingTokenCount: Int = 0,
    val streamingTokensPerSec: Float = 0f,
    // Model catalog download progress (modelId -> progress 0-1)
    val catalogDownloadProgress: Map<String, Float> = emptyMap(),
    // One-shot user-facing download error (storage full, incomplete, etc.); null = none.
    val downloadError: String? = null,
    // Shared model folder (/sdcard/AIModels) — one copy of the weights across the user's apps.
    val modelLocation: ModelLocation = ModelLocation.App,
    // Display name of the granted folder (e.g. "AIModels"); blank when none is picked.
    val sharedFolderPath: String = "",
    // /sdcard/AIModels — shown as a hint so the user points the picker where Koeyomi downloads.
    val suggestedFolderPath: String = "",
    val hasSharedStorageAccess: Boolean = false,
    // Prefer the GPU for inference. Drives the GPU/CPU chips in the Configurations dialog, which
    // until now were purely decorative.
    val preferGpu: Boolean = true,
    // Largest context this device can afford right now. The token slider cannot exceed it, so
    // the Configurations dialog shows it rather than silently ignoring a higher setting.
    val deviceContextCap: Int = 0,
    // Copying a shared-folder model into app storage (fileName -> 0..1). The native loader
    // cannot open a SAF document, so a shared model has to be copied in before it can run.
    val modelCopyProgress: Map<String, Float> = emptyMap(),
    // Model config dialog
    val showConfigDialog: Boolean = false,
    // Whether the loaded offline engine actually brought up its vision/audio
    // encoder. False when the model claims support but the runtime fell back to
    // text-only (e.g. an incompatible vision encoder). Defaults true until proven otherwise.
    val offlineVisionAvailable: Boolean = true,
    val offlineAudioAvailable: Boolean = true,
    // Token context window the offline engine actually allocated (maxNumTokens).
    // 0 = unknown / not an offline model. Used to show the context length in chat.
    val offlineContextLength: Int = 0,
    // Whether the loaded offline engine has Multi-Token Prediction (speculative
    // decoding) active — only true for MTP model builds that pass the capability probe.
    val offlineMtpActive: Boolean = false
) {
    /** Image input: on-device vision models only (needs visionBackend=GPU + the engine's vision encoder actually loaded) */
    val supportsImageInput: Boolean get() = selectedModel?.supportsImage == true && offlineVisionAvailable
    /** Audio input: on-device audio-capable models whose audio encoder actually loaded */
    val supportsAudioInput: Boolean get() = selectedModel?.supportsAudio == true && offlineAudioAvailable
}
