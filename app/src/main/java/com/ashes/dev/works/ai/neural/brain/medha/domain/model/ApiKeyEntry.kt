package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import java.util.UUID

/**
 * Status of a model check: null = not checked, empty = success, non-empty = error message
 */
typealias ModelCheckResult = String?  // null = pass, non-null = error message (empty means not checked yet)

data class ApiKeyEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val label: String = "",
    val isValidated: Boolean = false,
    val lastError: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    // Per-key model check results: modelId -> null (pass) / "error message" (fail)
    val checkedModels: Map<String, ModelCheckResult> = emptyMap(),
    // Models the user has selected for use with this key
    val selectedModels: List<String> = emptyList()
) {
    val maskedKey: String
        get() = if (key.length > 8) "${"*".repeat(key.length - 4)}${key.takeLast(4)}" else "****"

    val workingModels: List<String> get() = checkedModels.filter { it.value == null }.keys.toList()

    fun isModelWorking(modelId: String): Boolean? {
        if (modelId !in checkedModels) return null // not checked
        return checkedModels[modelId] == null // null error = success
    }

    fun modelError(modelId: String): String? = checkedModels[modelId]
}
