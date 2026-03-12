package com.ashes.dev.works.ai.neural.brain.medha.domain.model

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}
