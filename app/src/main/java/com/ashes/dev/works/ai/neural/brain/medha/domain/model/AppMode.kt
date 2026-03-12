package com.ashes.dev.works.ai.neural.brain.medha.domain.model

sealed class AppMode {
    data object Offline : AppMode()
    data object Online : AppMode()
}
