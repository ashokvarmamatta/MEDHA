package com.ashes.dev.works.ai.neural.brain.medha.domain.model

data class ModelInfo(
    val fileName: String,
    val filePath: String,
    val sizeInMb: Long,
    val displayName: String
) {
    companion object {
        fun fromFileName(fileName: String, filePath: String, sizeBytes: Long): ModelInfo {
            val displayName = fileName
                .removeSuffix(".bin")
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
                displayName = displayName
            )
        }
    }
}
