package com.ashes.dev.works.ai.neural.brain.medha.domain.model

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)