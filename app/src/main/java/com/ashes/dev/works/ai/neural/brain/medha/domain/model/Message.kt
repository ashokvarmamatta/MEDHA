package com.ashes.dev.works.ai.neural.brain.medha.domain.model

data class Message(
    val text: String,
    val user: User,
    val timestamp: Long = System.currentTimeMillis()
)