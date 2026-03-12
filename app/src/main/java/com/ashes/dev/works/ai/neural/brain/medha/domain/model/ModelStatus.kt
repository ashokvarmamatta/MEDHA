package com.ashes.dev.works.ai.neural.brain.medha.domain.model

sealed class ModelStatus {
    data object Idle : ModelStatus()
    data object Initializing : ModelStatus()
    data object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
    data object ModelNotFound : ModelStatus()
    data object PermissionRequired : ModelStatus()
}
