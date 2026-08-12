package com.ashes.dev.works.ai.neural.brain.medha.data

data class CatalogModel(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val fileName: String,
    val huggingFaceRepo: String,
    val supportsImage: Boolean = false,
    val supportsAudio: Boolean = false,
    val supportsThinking: Boolean = false,
    val maxContext: Int = 4096,
    val minRamGb: Int = 6,
    val badge: String? = null,
    val defaultMaxTokens: Int = 4096,
    val defaultTopK: Int = 64,
    val defaultTopP: Double = 0.95,
    val defaultTemperature: Double = 1.0,
    val accelerators: List<String> = listOf("cpu"),
    val taskTypes: List<String> = listOf("llm_chat"),
    // Remote file name on Hugging Face, when it differs from the local [fileName]
    // (e.g. a repo that ships a generic "model.litertlm"). Defaults to [fileName].
    val remoteFileName: String? = null,
    // Multi-Token Prediction build: the .litertlm bundles an MTP drafter, so the
    // engine can use speculative decoding for faster generation. Experimental.
    val usesMtp: Boolean = false
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$huggingFaceRepo/resolve/main/${remoteFileName ?: fileName}?download=true"
    val learnMoreUrl: String
        get() = "https://huggingface.co/$huggingFaceRepo"
    val sizeLabel: String get() {
        val mb = sizeBytes / (1024.0 * 1024.0)
        return if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.0f MB".format(mb)
    }
    val featureTags: List<String> get() = buildList {
        if (usesMtp) add("MTP")
        if (supportsThinking) add("Thinking")
        if (supportsImage) add("Vision")
        if (supportsAudio) add("Audio")
        add("${maxContext / 1024}K ctx")
    }
    val supportsGpu: Boolean get() = "gpu" in accelerators
}

object ModelCatalog {
    val models = listOf(
        CatalogModel(
            id = "gemma-4-e2b", name = "Gemma 4 E2B",
            description = "Google's latest. Vision, audio, thinking. 140+ languages. Best all-rounder.",
            // Verified against the Hugging Face Content-Length on 2026-08-12. The download
            // integrity check no longer trusts this number (the server's is authoritative);
            // it is an estimate for the size label and the free-space pre-check.
            sizeBytes = 2_588_147_712L, fileName = "gemma-4-E2B-it.litertlm",
            huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm",
            supportsImage = true, supportsAudio = true, supportsThinking = true,
            maxContext = 32768, minRamGb = 8, badge = "BEST",
            accelerators = listOf("cpu", "gpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab", "llm_ask_image")
        ),
        CatalogModel(
            id = "gemma-4-e4b", name = "Gemma 4 E4B",
            description = "Larger Gemma 4. 140+ languages. Smarter but needs 12GB RAM.",
            sizeBytes = 3_659_530_240L, fileName = "gemma-4-E4B-it.litertlm",
            huggingFaceRepo = "litert-community/gemma-4-E4B-it-litert-lm",
            supportsImage = true, supportsAudio = true, supportsThinking = true,
            maxContext = 32768, minRamGb = 12, badge = "PRO",
            accelerators = listOf("cpu", "gpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab", "llm_ask_image")
        ),
        CatalogModel(
            id = "gemma-4-e2b-mtp", name = "Gemma 4 E2B MTP",
            description = "Experimental. Multi-token prediction (speculative decoding) for faster generation. Community build, text only.",
            sizeBytes = 2_584_805_376L,
            fileName = "gemma-4-E2B-it-128k-mtp.litertlm",
            remoteFileName = "model.litertlm",
            huggingFaceRepo = "metricspace/gemma4-E2B-it-litert-128k-mtp",
            supportsThinking = true, usesMtp = true,
            maxContext = 8192, minRamGb = 8, badge = "MTP",
            accelerators = listOf("cpu", "gpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab")
        ),
        CatalogModel(
            id = "gemma-3n-e2b", name = "Gemma 3n E2B",
            description = "Vision + audio. Reliable previous-gen model.",
            sizeBytes = 3_655_827_456L, fileName = "gemma-3n-E2B-it-int4.litertlm",
            huggingFaceRepo = "google/gemma-3n-E2B-it-litert-lm",
            supportsImage = true, supportsAudio = true, maxContext = 4096, minRamGb = 8,
            accelerators = listOf("cpu", "gpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab", "llm_ask_image")
        ),
        CatalogModel(
            id = "gemma3-1b", name = "Gemma 3 1B",
            description = "Tiny & fast. Text only. Great for low-end phones.",
            sizeBytes = 584_417_280L, fileName = "gemma3-1b-it-int4.litertlm",
            huggingFaceRepo = "litert-community/Gemma3-1B-IT",
            maxContext = 1024, minRamGb = 6, badge = "TINY", defaultMaxTokens = 1024,
            accelerators = listOf("cpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab")
        ),
        CatalogModel(
            id = "deepseek-r1-1.5b", name = "DeepSeek R1 1.5B",
            description = "DeepSeek reasoning model, text only.",
            sizeBytes = 1_833_451_520L,
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
            huggingFaceRepo = "litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
            maxContext = 4096, minRamGb = 6,
            accelerators = listOf("cpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab")
        ),

        // ── Non-Gemma families ──────────────────────────────────────────────
        //
        // Every entry MUST be a .litertlm or .task build. LiteRT-LM opens the model in native
        // code and rejects anything else — a GGUF (llama.cpp) or ONNX file downloads fine and
        // then fails with "INVALID_ARGUMENT: Unsupported or unknown file format", which looks
        // like a corrupt download rather than a format mismatch. Liquid AI's own
        // LiquidAI/LFM2.5-*-GGUF repos are NOT usable here; litert-community republishes the
        // LiteRT builds below.
        //
        // sizeBytes is verified against the Hugging Face API (2026-08-12). It is only an
        // estimate for the UI and the free-space pre-check — the download trusts the server's
        // Content-Length, so a stale value here can no longer fail a good download.
        CatalogModel(
            id = "lfm2-5-1.2b", name = "LFM2.5 1.2B",
            description = "Liquid AI's hybrid edge model. Fast, text only. Good quality for its size.",
            sizeBytes = 736_015_744L,
            fileName = "LFM2.5-1.2B-Instruct_int4.litertlm",
            huggingFaceRepo = "litert-community/LFM2.5-1.2B-Instruct",
            maxContext = 4096, minRamGb = 6, badge = "FAST",
            accelerators = listOf("cpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab")
        ),
        CatalogModel(
            id = "qwen3-1.7b", name = "Qwen3 1.7B",
            description = "Alibaba's Qwen3. Strong multilingual and reasoning, text only.",
            sizeBytes = 977_184_032L,
            fileName = "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
            huggingFaceRepo = "litert-community/Qwen3-1.7B",
            supportsThinking = true,
            maxContext = 4096, minRamGb = 6,
            accelerators = listOf("cpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab")
        ),
        CatalogModel(
            id = "smollm2-360m", name = "SmolLM2 360M",
            description = "Extremely small and quick. Text only. Runs on almost anything.",
            sizeBytes = 373_719_040L,
            fileName = "SmolLM2_360M_instruct.litertlm",
            huggingFaceRepo = "litert-community/SmolLM2-360M-Instruct",
            maxContext = 2048, minRamGb = 4, badge = "TINY", defaultMaxTokens = 1024,
            accelerators = listOf("cpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab")
        ),
        CatalogModel(
            id = "gemma3-270m", name = "Gemma 3 270M",
            description = "Smallest Gemma. Text only. For very low-RAM phones.",
            sizeBytes = 304_005_120L,
            fileName = "gemma3-270m-it-q8.litertlm",
            huggingFaceRepo = "litert-community/gemma-3-270m-it",
            maxContext = 2048, minRamGb = 4, badge = "TINY", defaultMaxTokens = 1024,
            accelerators = listOf("cpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab")
        )
    )

    fun findById(id: String) = models.firstOrNull { it.id == id }
    fun findByFileName(name: String) = models.firstOrNull { it.fileName == name }
}
