package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.online

import androidx.lifecycle.ViewModel
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnlineModelSelectionState(
    val isLoading: Boolean = false,
    val models: List<String> = emptyList(),
    val error: String? = null,
    val selectedModel: String? = null
)

class OnlineModelSelectionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineModelSelectionState())
    val uiState = _uiState.asStateFlow()

    private val availableModels = listOf(
        "gemini-pro-latest",
        "gemini-flash-lite-latest",
        "gemini-3-flash-preview",
        "gemini-3-pro-preview",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash-lite-preview-sep-2025",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite"
    )

    fun fetchModels(apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(error = "API Key cannot be empty.") }
            return
        }
        
        _uiState.update {
            it.copy(
                models = availableModels,
                selectedModel = "gemini-2.5-flash"
            )
        }
    }

    fun selectModel(modelName: String) {
        _uiState.update { it.copy(selectedModel = modelName) }
    }
}
