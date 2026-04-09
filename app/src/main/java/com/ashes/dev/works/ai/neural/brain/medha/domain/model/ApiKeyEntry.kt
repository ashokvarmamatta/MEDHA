package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import java.util.UUID

/**
 * Status of a model check: null = pass, non-null = error message
 */
typealias ModelCheckResult = String?

data class ApiKeyEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val label: String = "",
    val baseUrl: String = "",
    val isValidated: Boolean = false,
    val isEnabled: Boolean = true,
    val lastError: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val checkedModels: Map<String, ModelCheckResult> = emptyMap(),
    val selectedModels: List<String> = emptyList()
) {
    val maskedKey: String
        get() = if (key.length > 8) "${"*".repeat(key.length - 4)}${key.takeLast(4)}" else "****"

    val workingModels: List<String> get() = checkedModels.filter { it.value == null }.keys.toList()

    fun isModelWorking(modelId: String): Boolean? {
        if (modelId !in checkedModels) return null
        return checkedModels[modelId] == null
    }

    fun modelError(modelId: String): String? = checkedModels[modelId]
}
