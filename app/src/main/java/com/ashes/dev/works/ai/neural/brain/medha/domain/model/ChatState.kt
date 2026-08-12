package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import com.ashes.dev.works.ai.neural.brain.medha.data.local.ModelLocation
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
    // Grand Master mode
    val activeGrandMaster: GrandMaster? = null,
    val activeCustomGrandMaster: CustomGrandMaster? = null,
    val showGrandMasterPicker: Boolean = false,
    val showResumeOrResetDialog: Boolean = false,
    val pendingGrandMaster: GrandMaster? = null,
    val pendingCustomGrandMaster: CustomGrandMaster? = null,
    val customGrandMasters: List<CustomGrandMaster> = emptyList(),
    val showCreateGrandMaster: Boolean = false,
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
    val hasAnyValidatedKey: Boolean get() = apiKeys.any { it.isValidated && it.isEnabled }
    val validatedKeys: List<ApiKeyEntry> get() = apiKeys.filter { it.isValidated && it.isEnabled }
    val activeKey: ApiKeyEntry? get() = validatedKeys.getOrNull(activeKeyIndex.coerceIn(0, (validatedKeys.size - 1).coerceAtLeast(0)))

    /** Image input: online always, offline with Gemma 4 vision models (needs visionBackend=GPU + the engine's vision encoder actually loaded) */
    val supportsImageInput: Boolean get() = appMode is AppMode.Online || (selectedModel?.supportsImage == true && offlineVisionAvailable)
    /** Whether current mode supports audio input (online, or offline with audio-capable models whose audio encoder loaded) */
    val supportsAudioInput: Boolean get() = appMode is AppMode.Online || (selectedModel?.supportsAudio == true && offlineAudioAvailable)
}
