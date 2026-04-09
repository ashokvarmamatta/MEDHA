package com.ashes.dev.works.ai.neural.brain.medha.domain.model

import java.util.UUID

data class CustomGrandMaster(
    val id: String = UUID.randomUUID().toString(),
    val icon: String = "\uD83C\uDF1F",
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val systemPrompt: String,
    val welcomeMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Key used for saving chat history, matches GrandMaster enum usage */
    val chatHistoryKey: String get() = "custom_$id"
}
