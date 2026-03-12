package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val user: User,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val generatedImages: List<GeneratedImage> = emptyList()
)

data class GeneratedImage(
    val id: String = UUID.randomUUID().toString(),
    val base64Data: String,
    val mimeType: String,
    val savedPath: String? = null
)
