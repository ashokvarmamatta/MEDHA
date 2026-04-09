package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import com.ashes.dev.works.ai.neural.brain.medha.data.ModelCatalog

data class ModelInfo(
    val fileName: String,
    val filePath: String,
    val sizeInMb: Long,
    val displayName: String,
    val isLiteRtFormat: Boolean = false,
    val supportsImage: Boolean = false,
    val supportsAudio: Boolean = false
) {
    companion object {
        val SUPPORTED_EXTENSIONS = listOf(".bin", ".litertlm", ".task")

        fun fromFileName(fileName: String, filePath: String, sizeBytes: Long): ModelInfo {
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
                supportsAudio = catalogMatch?.supportsAudio ?: false
            )
        }
    }
}
