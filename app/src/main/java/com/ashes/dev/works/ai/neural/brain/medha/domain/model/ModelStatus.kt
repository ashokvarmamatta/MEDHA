package com.ashes.dev.works.ai.neural.brain.medha.domain.model

sealed class ModelStatus {
    data object Idle : ModelStatus()
    data object Initializing : ModelStatus()
    data class Loading(val progress: Float = 0f, val detail: String = "") : ModelStatus()
    data object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
    data object ModelNotFound : ModelStatus()
    data object PermissionRequired : ModelStatus()
    // Download states for catalog models
    data class Downloading(val progress: Float = 0f, val speedMbps: Float = 0f) : ModelStatus()
}
