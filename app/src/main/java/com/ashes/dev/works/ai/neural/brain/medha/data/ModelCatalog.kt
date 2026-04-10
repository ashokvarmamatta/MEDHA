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
    val taskTypes: List<String> = listOf("llm_chat")
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$huggingFaceRepo/resolve/main/$fileName?download=true"
    val learnMoreUrl: String
        get() = "https://huggingface.co/$huggingFaceRepo"
    val sizeLabel: String get() {
        val mb = sizeBytes / (1024.0 * 1024.0)
        return if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.0f MB".format(mb)
    }
    val featureTags: List<String> get() = buildList {
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
            sizeBytes = 2_583_085_056L, fileName = "gemma-4-E2B-it.litertlm",
            huggingFaceRepo = "litert-community/gemma-4-E2B-it-litert-lm",
            supportsImage = true, supportsAudio = true, supportsThinking = true,
            maxContext = 32768, minRamGb = 8, badge = "BEST",
            accelerators = listOf("cpu", "gpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab", "llm_ask_image")
        ),
        CatalogModel(
            id = "gemma-4-e4b", name = "Gemma 4 E4B",
            description = "Larger Gemma 4. 140+ languages. Smarter but needs 12GB RAM.",
            sizeBytes = 3_654_467_584L, fileName = "gemma-4-E4B-it.litertlm",
            huggingFaceRepo = "litert-community/gemma-4-E4B-it-litert-lm",
            supportsImage = true, supportsAudio = true, supportsThinking = true,
            maxContext = 32768, minRamGb = 12, badge = "PRO",
            accelerators = listOf("cpu", "gpu"),
            taskTypes = listOf("llm_chat", "llm_prompt_lab", "llm_ask_image")
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
        )
    )

    fun findById(id: String) = models.firstOrNull { it.id == id }
    fun findByFileName(name: String) = models.firstOrNull { it.fileName == name }
}
