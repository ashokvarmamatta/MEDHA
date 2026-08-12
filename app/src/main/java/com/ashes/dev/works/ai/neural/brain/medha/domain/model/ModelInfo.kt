package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import com.ashes.dev.works.ai.neural.brain.medha.data.ModelCatalog

data class ModelInfo(
    val fileName: String,
    val filePath: String,
    val sizeInMb: Long,
    val displayName: String,
    val isLiteRtFormat: Boolean = false,
    val supportsImage: Boolean = false,
    val supportsAudio: Boolean = false,
    /**
     * True when the file lives in the shared `/sdcard/AIModels` folder rather than in MEDHA's
     * private `filesDir/medha_models`. Either way [filePath] is a real path the engine opens
     * directly — this only drives the "Shared" badge and delete confirmation in the UI.
     */
    val isShared: Boolean = false
) {

    companion object {
        val SUPPORTED_EXTENSIONS = listOf(".bin", ".litertlm", ".task")

        /**
         * Runtime artifacts LiteRT-LM writes NEXT TO a model on first load — e.g.
         * `gemma-4-E2B-it.litertlm.vision_encoder_1780901186_2588147712_mldrift_weight_cache.bin`.
         * They end in `.bin`, so a plain extension check lists them as selectable models that can
         * never load. Observed on device 2026-08-12.
         */
        private val COMPANION_MARKERS = listOf(
            "_weight_cache", "_mldrift", ".vision_encoder", ".audio_encoder", ".cache"
        )

        /**
         * MEDHA is a CHAT app. A shared folder holds other apps' AI assets too — Koeyomi keeps
         * its Kokoro voices and Whisper speech models in the same place — and a text-to-speech
         * voice pack or a speech-recognition model is not something this app can chat with.
         * Names carrying any of these are skipped so the picker only ever offers chat models.
         */
        private val NON_CHAT_MARKERS = listOf(
            "kokoro", "voices", "voice", "tts", "piper", "espeak",
            "whisper", "ggml", "vosk", "sherpa", "encodec", "vocoder"
        )

        /**
         * Extensions the LiteRT-LM chat engine actually loads. `.bin` is deliberately absent:
         * it is far too generic to trust inside a folder shared with other apps (Whisper ships
         * `ggml-*.bin`, Kokoro ships `voices.bin`), and every real chat model this app supports
         * is `.litertlm` or `.task`. MEDHA's own private folder still accepts `.bin` for a model
         * the user imported deliberately.
         */
        private val SHARED_FOLDER_EXTENSIONS = listOf(".litertlm", ".task")

        /**
         * Worth descending into when scanning the shared folder? A voice or speech-recognition
         * pack never holds a chat model, and Koeyomi's `kokoro-en-v0_19/` in particular contains
         * `espeak-ng-data/` with hundreds of files — querying that over SAF is slow enough to
         * hold up engine start for nothing.
         */
        fun isPlausibleModelDir(dirName: String): Boolean {
            val lower = dirName.lowercase()
            return NON_CHAT_MARKERS.none { it in lower }
        }

        /**
         * `.bin` is far too generic to trust on its own. A shared folder holds other apps' data
         * too — Koeyomi's Kokoro TTS pack ships a 5.7 MB `voices.bin` that is a voice table, not a
         * language model. Real LiteRT weights are hundreds of MB at minimum, so size disambiguates
         * where the extension cannot. `.litertlm` and `.task` are unambiguous and skip this.
         */
        private const val MIN_BIN_MODEL_BYTES = 50L * 1024 * 1024

        /**
         * Is this a model the engine could actually load, rather than a sidecar the runtime
         * generated or another app's data? Used for both the private folder and the shared one,
         * so the two lists agree.
         *
         * Pass [sizeBytes] when known; -1 skips the size heuristic. Set [sharedFolder] for files
         * coming from the user's own folder, where other apps' AI assets live alongside ours and
         * the rules have to be stricter.
         */
        fun isModelFile(
            fileName: String,
            sizeBytes: Long = -1L,
            sharedFolder: Boolean = false
        ): Boolean {
            val lower = fileName.lowercase()
            val allowed = if (sharedFolder) SHARED_FOLDER_EXTENSIONS else SUPPORTED_EXTENSIONS
            if (allowed.none { lower.endsWith(it) }) return false
            if (COMPANION_MARKERS.any { it in lower }) return false
            // Only screen out other apps' voice/speech assets in the shared folder — a private
            // model the user imported and named "voices.litertlm" is still theirs to load.
            if (sharedFolder && NON_CHAT_MARKERS.any { it in lower }) return false
            // A real model carries exactly ONE recognised extension, at the end. A name like
            // "model.litertlm.something.bin" still has ".litertlm" in its stem, which means it
            // was derived from a model rather than being one.
            val stem = lower.substringBeforeLast('.')
            if (SUPPORTED_EXTENSIONS.any { ext -> stem.contains(ext) }) return false
            if (lower.endsWith(".bin") && sizeBytes in 0 until MIN_BIN_MODEL_BYTES) return false
            return true
        }

        fun fromFileName(
            fileName: String,
            filePath: String,
            sizeBytes: Long,
            isShared: Boolean = false
        ): ModelInfo {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val isLiteRt = ext == "litertlm" || ext == "task"

            // Match against catalog for capabilities
            val catalogMatch = ModelCatalog.findByFileName(fileName)

            val displayName = catalogMatch?.name ?: fileName
                .removeSuffix(".bin")
                .removeSuffix(".litertlm")
                .removeSuffix(".task")
                .replace("-", " ")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercaseChar() }
                }
            return ModelInfo(
                fileName = fileName,
                filePath = filePath,
                sizeInMb = sizeBytes / (1024 * 1024),
                displayName = displayName,
                isLiteRtFormat = isLiteRt,
                supportsImage = catalogMatch?.supportsImage ?: false,
                supportsAudio = catalogMatch?.supportsAudio ?: false,
                isShared = isShared
            )
        }
    }
}
