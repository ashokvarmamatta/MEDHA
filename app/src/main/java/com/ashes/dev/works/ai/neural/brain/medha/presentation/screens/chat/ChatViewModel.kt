package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.google.ai.client.generativeai.GenerativeModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(
    private val application: Application,
    private val modelNameOrUri: String?,
    private val isOnline: Boolean?,
    private val apiKey: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    private var llmInference: LlmInference? = null
    private var generativeModel: GenerativeModel? = null

    init {
        initializeEngine()
    }

    fun initializeEngine() {
        if (isOnline == true) {
            initializeOnlineEngine()
        } else {
            initializeOfflineEngine()
        }
    }

    private fun initializeOnlineEngine() {
        _uiState.update { it.copy(messages = emptyList()) }
        generativeModel = GenerativeModel(
            modelName = modelNameOrUri ?: "gemini-1.5-flash-latest",
            apiKey = apiKey ?: ""
        )
    }

    private fun initializeOfflineEngine() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, messages = emptyList()) }

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setMaxTokens(1000)

                if (modelNameOrUri?.startsWith("content://") == true) {
                    options.setModelPath(modelNameOrUri)
                } else {
                    val modelFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        modelNameOrUri ?: "gemma-2b-it-gpu-int4.bin"
                    )
                    options.setModelPath(modelFile.absolutePath)
                }
                
                llmInference = LlmInference.createFromOptions(application, options.build())
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Initialization error: ${e.message}", isLoading = false) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        _uiState.update {
            it.copy(
                isLoading = true,
                messages = it.messages + Message(prompt, User.Person)
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = if (isOnline == true) {
                    generativeModel?.generateContent(prompt)?.text ?: ""
                } else {
                    llmInference?.generateResponse(prompt) ?: ""
                }
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