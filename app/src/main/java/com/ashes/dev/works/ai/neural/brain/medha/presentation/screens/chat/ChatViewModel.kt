package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(private val application: Application) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    private var llmInference: LlmInference? = null

    init {
        initializeEngine()
    }

    fun initializeEngine() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val modelFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "gemma-2b-it-gpu-int4.bin"
                )
                
                if (!modelFile.exists()) {
                    _uiState.update { it.copy(
                        isLoading = false, 
                        error = "Model file not found at: ${modelFile.absolutePath}. Please ensure the file is in your Downloads folder."
                    ) }
                    return@launch
                }

                if (!modelFile.canRead()) {
                    _uiState.update { it.copy(
                        isLoading = false, 
                        error = "Cannot read model file. Please ensure 'All Files Access' permission is granted for the app."
                    ) }
                    return@launch
                }

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1000)
                    .build()
                
                llmInference = LlmInference.createFromOptions(application, options)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Initialization error: ${e.message}", isLoading = false) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        
        if (llmInference == null) {
            _uiState.update { it.copy(error = "Engine not initialized. Check model file and permissions.") }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                messages = it.messages + Message(prompt, User.Person)
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = llmInference?.generateResponse(prompt) ?: ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = it.messages + Message(response, User.AI)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Generation error: ${e.message}", isLoading = false) }
            }
        }
    }
}
