package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnlineChatViewModel(private val apiKey: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(prompt: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                messages = it.messages + Message(prompt, User.Person)
            )
        }

        viewModelScope.launch {
            // TODO: Implement Gemini API call
        }
    }
}