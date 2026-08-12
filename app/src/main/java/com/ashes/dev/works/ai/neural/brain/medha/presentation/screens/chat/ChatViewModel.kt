package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.ashes.dev.works.ai.neural.brain.medha.data.local.EnginePrefs
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ExternalModelStore
import com.ashes.dev.works.ai.neural.brain.medha.data.local.FileDownloadSink
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ModelDownloadSink
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ModelStorage
import com.ashes.dev.works.ai.neural.brain.medha.data.local.SafDownloadSink
import com.ashes.dev.works.ai.neural.brain.medha.data.ModelCatalog
import com.ashes.dev.works.ai.neural.brain.medha.data.repository.SettingsRepository
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GeneratedImage
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.LogLevel
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplate
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.ashes.dev.works.ai.neural.brain.medha.service.MedhaService
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Capabilities
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatViewModel(
    private val application: Application,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MedhaEngine"
        const val APP_VERSION = "2.0.0"
        private const val MODELS_DIR = "medha_models"
        private const val TMP_EXT = ".medhatmp"
        /** Room left for the rest of the app (UI, bitmaps, Room) on top of weights + KV cache. */
        private const val SAFETY_MARGIN_MB = 512L
        /**
         * KV cache cost per token, in KB. Derived from a measured failure rather than a spec:
         * a 32768-token window took RSS from 1.0 GB to 7.22 GB, ≈190 KB/token for Gemma 4 E2B.
         * Rounded up for headroom. LiteRT-LM does not expose the real figure and it varies by
         * architecture, so this is an estimate — deliberately pessimistic.
         */
        private const val KV_KB_PER_TOKEN = 200L
    }

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    // LiteRT LM engine state
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    /**
     * Keeps the shared-folder file descriptor open while the engine holds its mmap. Closed only
     * in [destroyEngine], AFTER the engine — closing it first would pull the mapping out from
     * under native code.
     */
    private var modelHandle: ExternalModelStore.EngineHandle? = null
    // In-flight generation coroutine, so the user can stop a long response.
    private var generationJob: Job? = null

    /** GPU preference + the native-GPU-crash guard. Read synchronously during engine init. */
    private val enginePrefs = EnginePrefs(application)

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
        // A GPU attempt still marked in-flight means last launch died inside the native GPU
        // backend. Record it so this launch quietly uses CPU instead of crashing again.
        enginePrefs.migrateUnsafeGpuPreference()
        if (enginePrefs.consumeMigrationNotice()) {
            addLog(LogLevel.WARNING, TAG,
                "GPU turned off: it was enabled by a build that could not catch a GPU crash on " +
                    "this device (SIGSEGV on RenderThread). Re-enable it in Configurations to retry.")
        }
        enginePrefs.reconcileGpuCrash()?.let { victim ->
            addLog(LogLevel.WARNING, TAG, "Previous GPU load of $victim crashed — falling back to CPU")
        }
        _uiState.update { it.copy(preferGpu = enginePrefs.preferGpu) }
        refreshStorageState()
        loadSavedSettings()
        viewModelScope.launch(Dispatchers.IO) { sweepTempDownloads() }
    }

    // ── Persistence ─────────────────────────────────────────────────

    private fun loadSavedSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedModel = settingsRepository.selectedModelFlow.first()
                addLog(LogLevel.INFO, TAG, "Last used on-device model: ${savedModel.ifBlank { "none" }}")
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Failed to load settings: ${e.message}")
            }
            // Inference is on-device only, so there is one startup path: find the
            // weights on disk and bring the engine up.
            scanAvailableModels()
            initializeEngine()
        }
    }

    private fun saveModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.selectedModel?.fileName?.let { settingsRepository.saveSelectedModel(it) }
        }
    }

    // ── Model Scanning ──────────────────────────────────────────────

    /** Where a NEW download goes. Always MEDHA's own folder — see [ModelStorage.downloadRoot]. */
    private fun modelsDir(): File = ModelStorage.downloadRoot(application)

    fun scanAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) { scanModelsNow() }
    }

    /**
     * The scan itself, awaitable.
     *
     * Engine init used to fire [scanAvailableModels] and then `delay(500)`, hoping the scan had
     * finished. It often had not — a SAF query over a shared folder is much slower than a
     * `File.listFiles()` — so `selectedModel` was still null and the engine reported
     * "Model not found" for a model that was sitting right there. Awaiting the real work removes
     * the guess.
     */
    private suspend fun scanModelsNow() {
        withContext(Dispatchers.IO) {
            try {
                // MEDHA's own models, plus anything in the shared folder the user granted. Both
                // are always listed, so a model another app already downloaded is used rather
                // than downloaded again — whichever way the storage setting happens to point.
                val local = (modelsDir().listFiles()?.toList() ?: emptyList())
                    .filter { f -> f.isFile && f.length() > 0 && ModelInfo.isModelFile(f.name, f.length()) }
                    .map { ModelInfo.fromFileName(it.name, it.absolutePath, it.length()) }

                // Anything in the granted folder (one level of subfolders included), plus any
                // model files the user handed over individually.
                val fromFolder = ModelStorage.sharedTreeUri(application)
                    ?.let { ExternalModelStore.listModels(application, it) }
                    ?: emptyList()
                val fromFiles = ModelStorage.sharedFileUris(application)
                    .mapNotNull { ExternalModelStore.describeFile(application, it) }
                val sharedSeen = mutableSetOf<String>()
                val shared = (fromFolder + fromFiles).filter { sharedSeen.add(it.filePath) }

                // A file name present in both places is the same weights twice; prefer the local
                // copy, which needs no fd bridge to open.
                val localNames = local.map { it.fileName.lowercase() }.toSet()
                val models = local + shared.filterNot { it.fileName.lowercase() in localNames }

                _uiState.update { it.copy(availableModels = models) }
                refreshStorageState()

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
        // A shared-folder model is the user's own file and may be in use by their other apps —
        // MEDHA does not own it and must not delete it. "Remove folder" is the way out.
        if (model.isShared) {
            addLog(LogLevel.WARNING, TAG, "${model.fileName} lives in the shared folder — not deleting it")
            return
        }
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
        val sameModel = _uiState.value.selectedModel?.filePath == model.filePath
        val currentStatus = _uiState.value.modelStatus
        // Re-init even if same model when status indicates not loaded (e.g. ModelNotFound after download)
        val needsReinit = !sameModel || currentStatus is ModelStatus.ModelNotFound || currentStatus is ModelStatus.Error || currentStatus is ModelStatus.Idle
        if (sameModel && !needsReinit) return
        addLog(LogLevel.INFO, TAG, "Switching to: ${model.fileName}")
        _uiState.update { it.copy(selectedModel = model) }
        saveModel()
        viewModelScope.launch(Dispatchers.IO) {
            cancelActiveGeneration()
            destroyEngine()
            initializeEngine()
        }
    }

    // ── Load / Unload ───────────────────────────────────────────────

    /**
     * Drop the model out of RAM without forgetting which one is selected.
     *
     * Worth having explicitly: a loaded Gemma 4 holds ~2.5 GB of mapped weights plus its KV
     * cache, and until now the only ways to release that were switching mode or killing the app.
     * The selection survives, so [loadModel] brings the same model back.
     */
    fun unloadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            cancelActiveGeneration()
            destroyEngine()
            try { MedhaService.stop(application) } catch (_: Exception) {}
            _uiState.update {
                it.copy(
                    modelStatus = ModelStatus.Idle,
                    offlineContextLength = 0,
                    offlineMtpActive = false
                )
            }
            addLog(LogLevel.INFO, TAG, "Model unloaded — memory released")
        }
    }

    /** Load the currently selected model. No-op while one is already loaded or loading. */
    fun loadModel() {
        val status = _uiState.value.modelStatus
        if (status is ModelStatus.Ready || status is ModelStatus.Loading || status is ModelStatus.Initializing) return
        addLog(LogLevel.INFO, TAG, "Loading model on request")
        initializeEngine()
    }

    /** True when a model is selected but not currently resident — drives the Load/Unload button. */
    val isModelLoaded: Boolean get() = engine != null

    // ── LiteRT LM Engine (on-device) ────────────────────────────────

    @OptIn(ExperimentalApi::class)
    fun initializeEngine() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update {
                    it.copy(
                        modelStatus = ModelStatus.Initializing,
                        offlineVisionAvailable = true,
                        offlineAudioAvailable = true,
                        offlineContextLength = 0,
                        offlineMtpActive = false
                    )
                }

                // Wait for the real scan rather than guessing at a delay — see [scanModelsNow].
                if (_uiState.value.selectedModel == null) scanModelsNow()

                val modelToLoad = _uiState.value.selectedModel
                if (modelToLoad == null) {
                    addLog(LogLevel.INFO, TAG, "No model found. Download from catalog.")
                    _uiState.update { it.copy(modelStatus = ModelStatus.ModelNotFound) }
                    return@launch
                }

                // A shared model lives in the user's own folder and is reached through SAF, so
                // its "filePath" is a document URI. openForEngine turns that into a path the
                // native loader can open — a direct path when the file is plainly readable,
                // otherwise a /proc/self/fd alias whose descriptor must outlive the engine.
                val handle = if (modelToLoad.isShared) {
                    ExternalModelStore.openForEngine(application, modelToLoad.filePath).also {
                        if (it == null) {
                            addLog(LogLevel.INFO, TAG,
                                "${modelToLoad.fileName} is in the shared folder — it must be copied in before it can load")
                            _uiState.update { s ->
                                s.copy(modelStatus = ModelStatus.Error(
                                    "Shared models must be copied into MEDHA before they can run. " +
                                        "Tap \"Copy into MEDHA\" next to the model in Settings."
                                ))
                            }
                        }
                    } ?: return@launch
                } else {
                    val f = File(modelToLoad.filePath)
                    if (!f.exists()) {
                        _uiState.update { it.copy(modelStatus = ModelStatus.ModelNotFound) }
                        return@launch
                    }
                    ExternalModelStore.EngineHandle(f.absolutePath, null)
                }
                modelHandle?.close()
                modelHandle = handle
                val modelPath = handle.path

                addLog(LogLevel.INFO, TAG,
                    "Loading ${modelToLoad.fileName} (${modelToLoad.sizeInMb}MB)" +
                        if (modelToLoad.isShared) " from shared folder${if (handle.isDirectPath) "" else " (fd)"}" else "")
                _uiState.update { it.copy(modelStatus = ModelStatus.Loading(0f, "Preparing engine...")) }

                val catalogModel = ModelCatalog.findByFileName(modelToLoad.fileName)
                val hasVision = catalogModel?.supportsImage == true
                val hasAudio = catalogModel?.supportsAudio == true
                // Context window = the KV cache, and the KV cache is what actually kills the app.
                //
                // This used to allocate the model's ADVERTISED maximum (Gemma 4 = 32K), ignoring
                // both the user's token budget and the device. Measured on a 7.5 GB phone
                // 2026-08-12: 2.4 GB of weights plus a 32K cache took MEDHA's RSS to 3.84 GB,
                // Android's lowmemorykiller started reclaiming apps across the system
                // ("critical pressure and device is low on memory") and killed MEDHA too.
                //
                // A SIGKILL throws nothing, so the out-of-memory rung of the retry ladder below
                // can never run — the only defence is not to ask for too much in the first place.
                // Koeyomi asks for 1024 and never sees this.
                val deviceCap = contextCapForDevice(modelToLoad.sizeInMb)
                _uiState.update { it.copy(deviceContextCap = deviceCap) }
                val advertised = catalogModel?.maxContext ?: maxTokens
                val fullContext = if (modelToLoad.isLiteRtFormat) {
                    minOf(advertised, maxTokens.coerceAtLeast(1024), deviceCap)
                } else 1024
                val minContext = 1024
                if (fullContext < advertised) {
                    addLog(LogLevel.INFO, TAG,
                        "Context capped at $fullContext tokens (model advertises $advertised, " +
                            "device budget $deviceCap, your setting $maxTokens)")
                }

                // Multi-Token Prediction (speculative decoding) is a GLOBAL runtime flag.
                // Gemma 4's capability probe returns true even for the standard build, but
                // turning it on saturates the mobile CPU (drafter + verify with no spare
                // parallel compute) and tanks throughput to ~0.2 tok/s. So enable it ONLY for
                // models explicitly marked as MTP in the catalog, and only if the build
                // actually carries the drafter. Standard models always run with it OFF.
                val mtpActive = if (modelToLoad.isLiteRtFormat && catalogModel?.usesMtp == true) {
                    try {
                        Capabilities(modelPath).use { it.hasSpeculativeDecodingSupport() }
                    } catch (e: Exception) {
                        addLog(LogLevel.WARNING, TAG, "MTP capability probe failed: ${e.message}")
                        false
                    }
                } else false
                ExperimentalFlags.enableSpeculativeDecoding = mtpActive
                addLog(LogLevel.INFO, TAG,
                    if (mtpActive) "Multi-Token Prediction (speculative decoding) ENABLED"
                    else "Speculative decoding OFF (standard generation)")

                _uiState.update { it.copy(modelStatus = ModelStatus.Loading(0.3f, "Loading weights...")) }

                // Build the engine with graceful degradation. Two things can fail at create time:
                //  1) A multimodal (vision/audio) encoder the runtime can't load — e.g. LiteRT-LM
                //     rejecting a multi-signature vision encoder. → retry text-only.
                //  2) Not enough memory to allocate the full context KV-cache on this device.
                //     → retry with a halved context until it fits (down to minContext).
                var visionEnabled = hasVision
                var audioEnabled = hasAudio
                var contextTokens = fullContext

                // GPU FIRST — for text as well as vision. The main backend was hardcoded to CPU
                // before, so the GPU chip in Configurations changed nothing at all.
                //
                // Both fall back to CPU on any create failure we can catch. The one we CANNOT
                // catch is a native GPU crash, which kills the process before any `catch` runs —
                // EnginePrefs' in-flight flag turns that into a recorded crash on the next launch
                // instead of a boot loop.
                val modelSupportsGpu = catalogModel?.supportsGpu ?: true
                val gpuCrashedBefore = enginePrefs.gpuCrashedFor == modelToLoad.fileName
                var mainOnGpu = enginePrefs.preferGpu && modelSupportsGpu && !gpuCrashedBefore
                var visionOnGpu = mainOnGpu
                if (gpuCrashedBefore) {
                    addLog(LogLevel.WARNING, TAG,
                        "GPU previously crashed loading ${modelToLoad.fileName} — using CPU. " +
                            "Re-enable GPU in Configurations to try again.")
                }

                // The GPU marker is NOT cleared here. Engine create succeeds on GPU and the
                // process dies later, on RenderThread — clearing on create meant the crash was
                // never recorded and every relaunch repeated it. It is cleared in
                // sendOfflineMessage once a generation completes, which is the first point the
                // GPU has actually proved itself.
                fun buildEngine(vision: Boolean, audio: Boolean, ctx: Int, mainGpu: Boolean, visGpu: Boolean): Engine {
                    if (mainGpu || (vision && visGpu)) enginePrefs.markGpuAttempt(modelToLoad.fileName)
                    return Engine(
                        EngineConfig(
                            modelPath = modelPath,
                            backend = if (mainGpu) Backend.GPU() else Backend.CPU(),
                            visionBackend = if (vision) (if (visGpu) Backend.GPU() else Backend.CPU()) else null,
                            audioBackend = if (audio) Backend.CPU() else null,
                            maxNumTokens = ctx
                        )
                    ).also { it.initialize() }
                }

                var built: Engine? = null
                var lastError: Exception? = null
                while (built == null) {
                    try {
                        built = buildEngine(visionEnabled, audioEnabled, contextTokens, mainOnGpu, visionOnGpu)
                    } catch (e: Exception) {
                        lastError = e
                        when {
                            isOutOfMemoryError(e) && contextTokens > minContext -> {
                                val reduced = (contextTokens / 2).coerceAtLeast(minContext)
                                addLog(LogLevel.WARNING, TAG,
                                    "Not enough memory for ${contextTokens}-token context; retrying at $reduced")
                                contextTokens = reduced
                            }
                            visionEnabled && visionOnGpu -> {
                                addLog(LogLevel.WARNING, TAG,
                                    "Engine create failed on GPU vision (${e.message}); retrying with CPU vision")
                                visionOnGpu = false
                            }
                            mainOnGpu -> {
                                addLog(LogLevel.WARNING, TAG,
                                    "Engine create failed on GPU (${e.message}); retrying on CPU")
                                mainOnGpu = false
                            }
                            // Any remaining create failure, not just ones whose message happens to
                            // mention an encoder: the old gate looked for "vision"/"audio"/
                            // "signature", so a multimodal model that failed with
                            // "Unsupported or unknown file format" never got its text-only retry
                            // and the whole load was reported as broken.
                            visionEnabled || audioEnabled -> {
                                addLog(LogLevel.WARNING, TAG,
                                    "Engine create failed (${e.message}); retrying text-only")
                                visionEnabled = false
                                audioEnabled = false
                                visionOnGpu = true
                            }
                            // Last resort: a context smaller than the catalog claims. Some model
                            // builds advertise a window the runtime will not actually allocate.
                            contextTokens > minContext -> {
                                val reduced = (contextTokens / 2).coerceAtLeast(minContext)
                                addLog(LogLevel.WARNING, TAG,
                                    "Engine create failed at ${contextTokens} tokens; retrying at $reduced")
                                contextTokens = reduced
                            }
                            else -> throw lastError
                        }
                    }
                }
                engine = built

                _uiState.update { it.copy(modelStatus = ModelStatus.Loading(0.8f, "Creating conversation...")) }
                val samplerConfig = SamplerConfig(topK = topK, topP = topP, temperature = temperature)
                conversation = engine!!.createConversation(
                    ConversationConfig(samplerConfig = samplerConfig)
                )

                val features = buildList {
                    add(if (mainOnGpu) "GPU" else "CPU")
                    if (visionEnabled) add(if (visionOnGpu) "Vision(GPU)" else "Vision(CPU)")
                    if (audioEnabled) add("Audio")
                }.joinToString(", ")
                _uiState.update {
                    it.copy(
                        modelStatus = ModelStatus.Ready,
                        offlineVisionAvailable = visionEnabled,
                        offlineAudioAvailable = audioEnabled,
                        offlineContextLength = contextTokens,
                        offlineMtpActive = mtpActive
                    )
                }
                addLog(LogLevel.INFO, TAG, "Engine ready: ${modelToLoad.displayName} (LiteRT LM, $features, ${contextTokens} tokens)")

                // Start foreground service to keep model alive in background.
                // MUST NOT be inside the engine-init try: if the app happens to be in the
                // background when loading finishes, startForegroundService throws
                // ForegroundServiceStartNotAllowedException (API 31+) — and the outer catch
                // would then mark a perfectly good engine as failed and destroy it.
                try {
                    MedhaService.start(application)
                } catch (e: Exception) {
                    addLog(LogLevel.WARNING, TAG, "Background service not started (app not in foreground): ${e.message}")
                }

            } catch (e: Exception) {
                addLog(LogLevel.ERROR, TAG, "Init failed: ${e.message}")
                _uiState.update { it.copy(modelStatus = ModelStatus.Error(friendlyEngineError(e)), offlineContextLength = 0, offlineMtpActive = false) }
                destroyEngine()
            }
        }
    }

    /**
     * How large a context this device can afford for a model of [modelSizeMb], in tokens.
     *
     * Android reclaims an app long before it can OOM gracefully, so this is a budget, not a
     * limit to be discovered by failing. The rule: the model's weights plus its KV cache should
     * stay under roughly half of total RAM, because crossing that on a 7.5 GB phone is what
     * triggered `lowmemorykiller: critical pressure and device is low on memory` and took MEDHA
     * (and several unrelated apps) down with it.
     *
     * The per-token cost is approximated rather than derived — LiteRT-LM does not expose it, and
     * it varies by architecture — so the tiers below are deliberately conservative. The user can
     * still raise the ceiling with the token slider in Configurations; this only decides the
     * DEFAULT, and a default that kills the app is not a useful default.
     */
    private fun contextCapForDevice(modelSizeMb: Long): Int {
        val am = application.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val info = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalMb = info.totalMem / (1024 * 1024)
        val availableMb = info.availMem / (1024 * 1024)

        // Do NOT subtract the model size. Weights are mmap'd from a file, so those pages are
        // file-backed and evictable under pressure — measured on device, a 2468 MB model sat at
        // ~1000 MB RSS after loading, not 2468 MB. Subtracting the full size (an earlier version
        // of this function did) made the budget negative and pinned every device to 1024 tokens
        // no matter how much memory it had.
        //
        // What actually costs anonymous, non-evictable memory is the KV cache. Its size is
        // measurable: the 2026-08-12 crash grew RSS from 1.0 GB to 7.22 GB for a 32768-token
        // window — about 190 KB per token for Gemma 4 E2B. Budget against that, conservatively.
        val kvBudgetMb = minOf(availableMb / 2, (totalMb * 35) / 100) - SAFETY_MARGIN_MB
        val affordableTokens = if (kvBudgetMb <= 0) 0L else (kvBudgetMb * 1024) / KV_KB_PER_TOKEN

        // Round down to a sensible window rather than an arbitrary number.
        val cap = when {
            affordableTokens >= 8192 -> 8192
            affordableTokens >= 4096 -> 4096
            affordableTokens >= 2048 -> 2048
            else -> 1024
        }
        addLog(LogLevel.DEBUG, TAG,
            "Device RAM ${totalMb}MB total / ${availableMb}MB free → KV budget ${kvBudgetMb}MB " +
                "(~$affordableTokens tokens at ${KV_KB_PER_TOKEN}KB each) → context cap $cap")
        return cap
    }

    /**
     * Heuristic: does this engine-init failure look like a vision/audio encoder
     * load problem (vs. a genuine model-wide failure)? If so we can safely retry
     * text-only. Matches the LiteRT-LM multi-signature vision encoder rejection
     * and related multimodal executor failures.
     */
    private fun isMultimodalLoadError(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return "vision" in msg ||
            "signature" in msg ||
            "audio" in msg ||
            "encoder" in msg
    }

    /** Does this generation failure look like the conversation outgrew the context window? */
    private fun isContextOverflowError(e: Throwable): Boolean {
        val m = (e.message ?: "").lowercase()
        val mentionsContext = "context" in m || "token" in m || "kv" in m || "sequence" in m
        val mentionsLimit = "exceed" in m || "too long" in m || "overflow" in m ||
            "out of range" in m || "maximum" in m || "max num" in m || "full" in m
        return mentionsContext && mentionsLimit
    }

    /** Does this failure look like the device ran out of memory for the requested context? */
    private fun isOutOfMemoryError(e: Throwable): Boolean {
        if (e is OutOfMemoryError) return true
        val msg = (e.message ?: "").lowercase()
        return "out of memory" in msg ||
            "oom" in msg ||
            "resource_exhausted" in msg ||
            "failed to allocate" in msg ||
            "cannot allocate" in msg ||
            "bad_alloc" in msg
    }

    /**
     * Map a raw engine-create failure to a user-readable message. Notably, web-only model
     * builds (e.g. the "-web" LiteRT variants) lack the Android prefill/decode signatures and
     * fail with "NOT_FOUND: TF_LITE_PREFILL_DECODE not found in the model" — tell the user to
     * pick a different model rather than showing the raw runtime string.
     */
    private fun friendlyEngineError(e: Throwable): String {
        val msg = e.message ?: "Unknown error"
        return when {
            "TF_LITE_PREFILL_DECODE" in msg ||
                ("NOT_FOUND" in msg && "not found in the model" in msg) ->
                "This model build isn't compatible with the on-device runtime " +
                    "(missing prefill/decode). Pick a different model in Settings."
            else -> msg
        }
    }

    private fun destroyEngine() {
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        conversation = null
        engine = null
        // Strictly after the engine: the fd backs its mmap.
        try { modelHandle?.close() } catch (_: Exception) {}
        modelHandle = null
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
                pendingImageUri = null, streamingText = "", streamingThinking = "", isThinking = false,
                // Zero the live counters so the previous turn's rate isn't shown during prefill.
                streamingTokenCount = 0, streamingTokensPerSec = 0f)
        }
        addLog(LogLevel.DEBUG, TAG, "User: ${displayText.take(80)}${if (imageUri != null) " [+image]" else ""}")

        sendOfflineMessage(prompt, imageUri)
    }

    /**
     * Regenerate the most recent answer: drop the last user turn and everything after it,
     * then resend that same prompt (and image, if any).
     */
    fun regenerateLastResponse() {
        if (_uiState.value.isGenerating) return
        val msgs = _uiState.value.messages
        val lastUserIdx = msgs.indexOfLast { it.user is User.Person }
        if (lastUserIdx < 0) return
        val userMsg = msgs[lastUserIdx]
        viewModelScope.launch(Dispatchers.IO) {
            cancelActiveGeneration()
            _uiState.update { it.copy(messages = it.messages.take(lastUserIdx)) }
            withContext(Dispatchers.Main) {
                if (userMsg.imageUri != null) _uiState.update { it.copy(pendingImageUri = userMsg.imageUri) }
                sendMessage(userMsg.text)
            }
        }
    }

    /**
     * Stop an in-flight response. Asks the native engine to halt first (offline),
     * then cancels the coroutine and keeps whatever was already streamed so the
     * partial answer isn't lost.
     */
    fun stopGeneration() {
        if (!_uiState.value.isGenerating) return
        Log.e(TAG, "stopGeneration() called by user — cancelling in-flight response")
        addLog(LogLevel.WARNING, TAG, "Generation stopped by user")
        try {
            conversation?.cancelProcess()
        } catch (e: Exception) {
            Log.e(TAG, "cancelProcess() failed", e)
            addLog(LogLevel.ERROR, TAG, "cancelProcess failed: ${e.message}")
        }
        generationJob?.cancel()
        generationJob = null

        val partialText = _uiState.value.streamingText.trim()
        val partialThinking = _uiState.value.streamingThinking.trim().ifEmpty { null }
        _uiState.update { s ->
            val msgs = if (partialText.isNotEmpty()) {
                s.messages + Message(
                    text = partialText,
                    user = User.AI,
                    thinkingText = partialThinking,
                    provider = "offline"
                )
            } else s.messages
            s.copy(messages = msgs, isGenerating = false, streamingText = "", streamingThinking = "", isThinking = false)
        }
        if (partialText.isNotEmpty()) {
            saveCurrentChatSession()
        }
    }

    /**
     * Cancel any in-flight generation and WAIT for it to fully stop before the caller
     * mutates the engine/conversation/session. This is what prevents the native
     * use-after-close race when switching model/mode/config or loading a session while
     * a response is streaming.
     */
    private suspend fun cancelActiveGeneration() {
        val job = generationJob
        if (job != null && job.isActive) {
            try { conversation?.cancelProcess() } catch (_: Exception) {}
            try { job.cancelAndJoin() } catch (_: Exception) {}
        }
        generationJob = null
        if (_uiState.value.isGenerating) {
            _uiState.update { it.copy(isGenerating = false, streamingText = "", streamingThinking = "", isThinking = false) }
        }
    }

    // ── Offline: LiteRT LM Streaming ────────────────────────────────

    private fun sendOfflineMessage(prompt: String, imageUri: String?) {
        generationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                try { MedhaService.inferenceOn(application) } catch (_: Exception) {}
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

                val langInstruction = if (outputLanguage != "Auto") {
                    "IMPORTANT: You MUST respond ONLY in $outputLanguage language, regardless of the input language. "
                } else ""
                val basePrompt = when {
                    imageUri != null && prompt.isBlank() -> "Describe this image in detail."
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
                                // Live decode rate. Measured from the FIRST token, not from the
                                // request, so the prefill wait doesn't drag the figure down and
                                // it matches the tok/s reported on the finished message.
                                val sinceFirst = System.currentTimeMillis() - firstTokenTime
                                val liveTps = if (sinceFirst > 0) (tokenCount * 1000f) / sinceFirst else 0f
                                _uiState.update {
                                    it.copy(
                                        streamingText = fullResponse.toString(),
                                        isThinking = false,
                                        streamingTokenCount = tokenCount,
                                        streamingTokensPerSec = liveTps
                                    )
                                }
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

                // A generation finished without taking the process down, so whatever backend is
                // loaded is safe on this device. This is the earliest honest point to clear the
                // GPU crash marker — engine create returning is not proof of anything.
                enginePrefs.clearGpuAttempt()

                if (clean.isEmpty()) {
                    appendAiMessage("Empty response. Try rephrasing.", provider = "offline")
                } else {
                    val thinking = fullThinking.toString().trim().ifEmpty { null }
                    appendAiMessageWithStats(clean, tokenCount, elapsed, tps, ttft, "offline", thinking)
                    addLog(LogLevel.INFO, TAG, "Response: ${tokenCount} tokens, ${"%.1f".format(tps)} tok/s, ${elapsed}ms")
                }

                _uiState.update { it.copy(isGenerating = false, streamingText = "", streamingThinking = "", isThinking = false) }

            } catch (ce: kotlinx.coroutines.CancellationException) {
                // User pressed Stop — stopGeneration() already handled UI/partial text.
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Offline generation error", e)
                addLog(LogLevel.ERROR, TAG, "Offline error: ${e.message}")
                val msg = if (isContextOverflowError(e))
                    "This chat has reached the model's context limit. Tap New Chat (＋) to keep going."
                else "Error: ${e.message}"
                appendAiMessage(msg, provider = "offline")
                _uiState.update { it.copy(isGenerating = false, streamingText = "", streamingThinking = "", isThinking = false) }
            } finally {
                try { MedhaService.inferenceOff(application) } catch (_: Exception) {}
                if (generationJob === coroutineContext[Job]) generationJob = null
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

    // ── Model Download ──────────────────────────────────────────────

    fun downloadCatalogModel(catalogModel: CatalogModel) {
        viewModelScope.launch(Dispatchers.IO) {
            // Start foreground service so the OS doesn't kill the download when app is minimized
            try { MedhaService.start(application) } catch (_: Exception) {}
            try {
                _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress + (catalogModel.id to 0f)) }
                addLog(LogLevel.INFO, TAG, "Downloading ${catalogModel.name} (${catalogModel.sizeLabel})...")

                // Download INTO the shared folder when the user granted one — the whole point is
                // that the next app finds the weights already there. SAF's tree grant covers
                // creating documents, so this needs no storage permission.
                val sharedTree = ModelStorage.sharedTreeUri(application)
                    ?.takeIf { ExternalModelStore.canWrite(application, it) }
                val sink: ModelDownloadSink = if (sharedTree != null) {
                    SafDownloadSink(application, sharedTree, catalogModel.fileName, TMP_EXT)
                } else {
                    FileDownloadSink(modelsDir(), catalogModel.fileName, TMP_EXT)
                }

                // If the model is already present anywhere MEDHA can see — the shared folder,
                // because another app downloaded it or it was copied in from a PC — there is
                // nothing to do. That is the entire point of the shared folder.
                val existing = findAvailable(catalogModel.fileName)
                if (existing != null) {
                    addLog(LogLevel.INFO, TAG,
                        "${catalogModel.fileName} already present" +
                            if (existing.isShared) " in the shared folder" else "")
                    _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress - catalogModel.id) }
                    scanAvailableModels()
                    return@launch
                }
                addLog(LogLevel.INFO, TAG, "Saving to ${sink.label}")

                // Storage pre-check: refuse to start if there isn't room (+50MB headroom),
                // accounting for any partially-downloaded temp file we can resume. A sink that
                // cannot report free space (-1) is not second-guessed.
                val alreadyHave = sink.partialBytes()
                val remainingBytes = (catalogModel.sizeBytes - alreadyHave).coerceAtLeast(0L)
                val headroom = 50L * 1024 * 1024
                val free = sink.usableSpace()
                if (catalogModel.sizeBytes > 0 && free >= 0 && free < remainingBytes + headroom) {
                    val needMb = remainingBytes / (1024 * 1024)
                    val freeMb = free / (1024 * 1024)
                    addLog(LogLevel.ERROR, TAG, "Not enough storage for ${catalogModel.name}: need ~${needMb}MB free, only ${freeMb}MB available")
                    _uiState.update {
                        it.copy(
                            catalogDownloadProgress = it.catalogDownloadProgress - catalogModel.id,
                            downloadError = "Not enough storage for ${catalogModel.name} — free up ~${needMb}MB and try again."
                        )
                    }
                    return@launch
                }

                val maxRetries = 5
                var attempt = 0
                while (attempt < maxRetries) {
                    attempt++
                    try {
                        var startByte = sink.partialBytes()
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
                        // Server ignored the Range header and is sending the whole file again.
                        if (conn.responseCode == 200 && startByte > 0) { startByte = 0; sink.discardPartial() }

                        // THE SERVER decides how big the file is, not the catalog.
                        // A hardcoded sizeBytes goes stale the moment upstream re-publishes a
                        // model, and then a perfectly good download is judged "incomplete",
                        // deleted, retried five times and reported as a failure. That is exactly
                        // what happened to Gemma 4 E2B/E4B: the catalog was ~5MB short of the
                        // real file, so the flagship model could never be installed. The catalog
                        // size is now only an estimate for the UI and the free-space pre-check.
                        val expectedTotal = when {
                            conn.responseCode == 206 -> {
                                // "Content-Range: bytes 100-999/1000" — the part after '/'.
                                conn.getHeaderField("Content-Range")
                                    ?.substringAfterLast('/', "")
                                    ?.trim()?.toLongOrNull()
                                    ?: (startByte + conn.contentLengthLong).takeIf { conn.contentLengthLong > 0 }
                            }
                            else -> conn.contentLengthLong.takeIf { it > 0 }
                        } ?: catalogModel.sizeBytes
                        if (expectedTotal > 0 && expectedTotal != catalogModel.sizeBytes) {
                            addLog(LogLevel.INFO, TAG,
                                "Catalog size for ${catalogModel.name} is stale " +
                                    "(${catalogModel.sizeBytes}); server says $expectedTotal")
                        }

                        conn.inputStream.use { input ->
                            sink.openAt(startByte).use { output ->
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
                                        val p = if (expectedTotal > 0) (received.toFloat() / expectedTotal).coerceIn(0f, 1f) else 0f
                                        _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress + (catalogModel.id to p)) }
                                        lastUpdate = now
                                    }
                                }
                                output.flush()
                            }
                        }
                        conn.disconnect()

                        // Integrity check: a stream that closes early (CDN hiccup) leaves a
                        // truncated file. Never accept a size mismatch as a valid model —
                        // resume on the next attempt, or fail & delete on the last.
                        val written = sink.partialBytes()
                        if (expectedTotal > 0 && written != expectedTotal) {
                            addLog(LogLevel.WARNING, TAG, "Incomplete download ($written/$expectedTotal bytes)")
                            if (attempt < maxRetries) { kotlinx.coroutines.delay(attempt * 2000L); continue }
                            sink.discardPartial()
                            throw Exception("Download incomplete: $written/$expectedTotal bytes")
                        }

                        if (!sink.finish()) throw Exception("Could not finalise the downloaded file")
                        addLog(LogLevel.INFO, TAG, "${catalogModel.name} downloaded to ${sink.label}")
                        _uiState.update { it.copy(catalogDownloadProgress = it.catalogDownloadProgress - catalogModel.id) }
                        scanAvailableModels()
                        return@launch

                    } catch (e: Exception) {
                        val saved = sink.partialBytes() / (1024 * 1024)
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
                _uiState.update {
                    it.copy(
                        catalogDownloadProgress = it.catalogDownloadProgress - catalogModel.id,
                        downloadError = "Download failed for ${catalogModel.name}: ${e.message}"
                    )
                }
            } finally {
                // Release the foreground service if it was only kept alive for this download
                // (engine init starts its own; this just covers the idle case).
                if (engine == null) {
                    try { MedhaService.stop(application) } catch (_: Exception) {}
                }
            }
        }
    }

    fun clearDownloadError() {
        _uiState.update { it.copy(downloadError = null) }
    }

    // ── Shared model folder ─────────────────────────────────────────

    /**
     * Re-read the storage setting and whether All-files access is currently granted. Called on
     * every scan and whenever the Settings screen resumes — the permission is toggled on a system
     * screen, so there is no result to await, only a state to re-check.
     */
    fun refreshStorageState() {
        val tree = ModelStorage.sharedTreeUri(application)
        _uiState.update {
            it.copy(
                modelLocation = ModelStorage.location(application),
                sharedFolderPath = tree?.let { u -> ExternalModelStore.folderLabel(u) } ?: "",
                suggestedFolderPath = ModelStorage.suggestedPath(),
                hasSharedStorageAccess = tree != null
            )
        }
    }

    /** The intent the Settings screen launches for the system folder picker. */
    fun modelFolderPickerIntent() = ExternalModelStore.pickFolderIntent()

    /**
     * Remember the folder the user just granted. Existing models are never moved — MEDHA's own
     * folder stays in the list, so this can't orphan a model that is already on disk.
     */
    fun setSharedModelFolder(treeUri: Uri) {
        if (ModelStorage.setSharedTree(application, treeUri)) {
            addLog(LogLevel.INFO, TAG, "Model folder set to ${ModelStorage.describe(application)}")
        } else {
            addLog(LogLevel.ERROR, TAG, "Could not keep access to that folder")
            _uiState.update { it.copy(downloadError = "Could not keep access to that folder. Try picking it again.") }
        }
        refreshStorageState()
        scanAvailableModels()
    }

    /**
     * Add ONE model file the user picked, used in place rather than copied. The alternative,
     * "Import", duplicates the weights — pointless for a 2.4 GB file that is already on the device.
     */
    fun addSharedModelFile(uri: Uri) {
        if (ModelStorage.addSharedFile(application, uri)) {
            addLog(LogLevel.INFO, TAG, "Added model file from ${uri.lastPathSegment}")
        } else {
            addLog(LogLevel.ERROR, TAG, "Could not keep access to that file")
            _uiState.update { it.copy(downloadError = "Could not keep access to that file. Try picking it again.") }
        }
        refreshStorageState()
        scanAvailableModels()
    }

    /**
     * Copy a shared-folder model into MEDHA's own storage so the engine can open it by real path.
     *
     * Unavoidable: LiteRT-LM opens the model path in native code, and neither a `content://` URI
     * nor a `/proc/self/fd` alias survives that (the alias re-opens the file and fails a fresh
     * permission check). Reading through SAF to make the copy is fine — it is only the loader's
     * re-open that is refused.
     */
    fun copySharedModelIn(model: ModelInfo) {
        if (!model.isShared) return
        val key = model.fileName
        if (_uiState.value.modelCopyProgress.containsKey(key)) return

        viewModelScope.launch(Dispatchers.IO) {
            val needed = ExternalModelStore.sizeOf(application, model.filePath)
            val destDir = ModelStorage.appRoot(application)
            val free = runCatching { destDir.usableSpace }.getOrDefault(-1L)
            val headroom = 50L * 1024 * 1024
            if (needed > 0 && free in 0 until (needed + headroom)) {
                val needMb = needed / (1024 * 1024)
                val freeMb = free / (1024 * 1024)
                addLog(LogLevel.ERROR, TAG, "Not enough storage to copy ${model.fileName}: need ~${needMb}MB, have ${freeMb}MB")
                _uiState.update {
                    it.copy(downloadError = "Not enough space to copy ${model.displayName} — needs ~${needMb}MB, ${freeMb}MB free.")
                }
                return@launch
            }

            addLog(LogLevel.INFO, TAG, "Copying ${model.fileName} into app storage (${needed / (1024 * 1024)}MB)...")
            _uiState.update { it.copy(modelCopyProgress = it.modelCopyProgress + (key to 0f)) }

            // Name it exactly as in the shared folder, minus any "subfolder/" prefix the scan
            // added for display, so the catalog can still match it by file name.
            val destName = model.fileName.substringAfterLast('/')
            val dest = File(destDir, destName)
            val ok = ExternalModelStore.copyIn(application, model.filePath, dest) { p ->
                _uiState.update { it.copy(modelCopyProgress = it.modelCopyProgress + (key to p)) }
            }

            _uiState.update { it.copy(modelCopyProgress = it.modelCopyProgress - key) }
            if (!ok) {
                addLog(LogLevel.ERROR, TAG, "Copy failed for ${model.fileName}")
                _uiState.update { it.copy(downloadError = "Could not copy ${model.displayName} into MEDHA.") }
                return@launch
            }

            addLog(LogLevel.INFO, TAG, "${model.displayName} copied — loading")
            scanModelsNow()
            val local = _uiState.value.availableModels.firstOrNull {
                !it.isShared && it.fileName.equals(destName, ignoreCase = true)
            }
            if (local != null) {
                cancelActiveGeneration()
                destroyEngine()
                _uiState.update { it.copy(selectedModel = local, modelStatus = ModelStatus.Initializing) }
                initializeEngine()
            }
        }
    }

    /** Forget the shared folder and fall back to MEDHA's own models. */
    fun clearSharedModelFolder() {
        val active = _uiState.value.selectedModel
        ModelStorage.clearSharedTree(application)
        addLog(LogLevel.INFO, TAG, "Shared model folder removed")
        // Loading a model out of a folder we no longer have is impossible — drop the engine
        // rather than leave a Ready status pointing at an unreachable file.
        if (active?.isShared == true) {
            viewModelScope.launch(Dispatchers.IO) {
                cancelActiveGeneration()
                destroyEngine()
                _uiState.update { it.copy(selectedModel = null, modelStatus = ModelStatus.ModelNotFound) }
                refreshStorageState()
                scanAvailableModels()
            }
        } else {
            refreshStorageState()
            scanAvailableModels()
        }
    }

    /**
     * Delete only *orphan* *.medhatmp partials — ones whose base file name has no catalog
     * entry, so they can never be resumed through the UI. Catalog-matching partials are kept
     * so an interrupted download can still resume. Safe to call on startup.
     */
    private fun sweepTempDownloads() {
        try {
            modelsDir().listFiles { f -> f.name.endsWith(TMP_EXT) }?.forEach { stale ->
                val baseName = stale.name.removeSuffix(TMP_EXT)
                if (ModelCatalog.findByFileName(baseName) == null) stale.delete()
            }
        } catch (_: Exception) {}
    }

    /**
     * A catalog model counts as present if MEDHA downloaded it OR the shared folder already has
     * it — the whole point of the shared folder is that a model another app fetched is not
     * fetched again. The shared side is read from the last scan rather than re-queried, so this
     * stays cheap enough to call from the catalog list.
     */
    private fun findAvailable(fileName: String): ModelInfo? {
        val local = File(modelsDir(), fileName)
        if (local.isFile && local.length() > 0) {
            return ModelInfo.fromFileName(local.name, local.absolutePath, local.length())
        }
        return _uiState.value.availableModels.firstOrNull { it.fileName.equals(fileName, true) }
    }

    fun isModelDownloaded(catalogModel: CatalogModel): Boolean =
        findAvailable(catalogModel.fileName) != null

    fun activateCatalogModel(catalogModel: CatalogModel) {
        val info = findAvailable(catalogModel.fileName) ?: return
        // Refresh available models list and force engine reload
        scanAvailableModels()
        _uiState.update { it.copy(selectedModel = info, modelStatus = ModelStatus.Initializing) }
        saveModel()
        destroyEngine()
        initializeEngine()
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
        // useGpu used to be logged and thrown away, so the GPU/CPU chips did nothing at all.
        // Now it is persisted and drives the backend choice on the next load. Choosing GPU
        // explicitly also forgives a previously recorded GPU crash — the user is asking to retry.
        if (useGpu) enginePrefs.clearGpuCrash()
        enginePrefs.preferGpu = useGpu
        _uiState.update { it.copy(showConfigDialog = false, preferGpu = useGpu) }
        addLog(LogLevel.INFO, TAG, "Config updated: topK=$topK topP=$topP temp=$temperature maxTokens=$maxTokens gpu=$useGpu thinking=$thinking lang=$language")
        // Reinitialize engine with new config (after any in-flight generation has stopped)
        viewModelScope.launch(Dispatchers.IO) {
            cancelActiveGeneration()
            destroyEngine()
            initializeEngine()
        }
    }
    fun hidePromptTemplates() { _uiState.update { it.copy(showPromptTemplates = false) } }

    // ── Chat History ──────────────────────────────────────────────────

    private fun saveCurrentChatSession() {
        val messages = _uiState.value.messages
        if (messages.isEmpty()) return

        val sessionId = currentSessionId
        val title = messages.firstOrNull { it.user is User.Person }?.text?.take(80) ?: "New Chat"
        val modelName = _uiState.value.selectedModel?.displayName ?: "on-device"

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
            cancelActiveGeneration()
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
            _uiState.update { it.copy(messages = messages) }
            addLog(LogLevel.INFO, TAG, "Loaded session: ${messages.size} messages")
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentSessionId == sessionId) cancelActiveGeneration()
            chatDb.chatDao().deleteSession(sessionId)
            if (currentSessionId == sessionId) {
                currentSessionId = java.util.UUID.randomUUID().toString()
                _uiState.update { it.copy(messages = emptyList()) }
            }
        }
    }

    fun deleteAllChatSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            cancelActiveGeneration()
            chatDb.chatDao().deleteAllSessions()
            currentSessionId = java.util.UUID.randomUUID().toString()
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    fun startNewChat() {
        viewModelScope.launch(Dispatchers.IO) {
            cancelActiveGeneration()
            saveCurrentChatSession()
            currentSessionId = java.util.UUID.randomUUID().toString()
            _uiState.update { it.copy(messages = emptyList(), streamingText = "", streamingThinking = "") }
            resetConversation()
            addLog(LogLevel.INFO, TAG, "New chat started")
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun appendAiMessage(text: String, provider: String = "") {
        _uiState.update { it.copy(messages = it.messages + Message(text = text, user = User.AI, provider = provider)) }
        saveCurrentChatSession()
    }

    private fun appendAiMessageWithStats(text: String, tokens: Int, latency: Long, tps: Float, ttft: Long, provider: String, thinking: String? = null) {
        _uiState.update {
            it.copy(messages = it.messages + Message(
                text = text, user = User.AI, tokenCount = tokens, latencyMs = latency, tokensPerSec = tps,
                thinkingText = thinking, provider = provider, timeToFirstTokenMs = ttft
            ))
        }
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
        _uiState.update { it.copy(messages = emptyList(), pendingImageUri = null, streamingText = "", streamingThinking = "") }
        resetConversation()
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
            // minSdk is 31, so scoped storage (RELATIVE_PATH + IS_PENDING) always applies.
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, image.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MEDHA")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = application.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            application.contentResolver.openOutputStream(uri)?.use { bitmap.compress(format, 95, it) }
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            application.contentResolver.update(uri, values, null, null)
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
