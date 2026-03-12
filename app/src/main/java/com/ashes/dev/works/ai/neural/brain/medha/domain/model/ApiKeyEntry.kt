package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import java.util.UUID

data class ApiKeyEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val label: String = "",
    val isValidated: Boolean = false,
    val lastError: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    val maskedKey: String
        get() = if (key.length > 8) "${"*".repeat(key.length - 4)}${key.takeLast(4)}" else "****"
}
