package com.ashes.dev.works.ai.neural.brain.medha.di

import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatViewModel
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.online.OnlineModelSelectionViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { (modelNameOrUri: String?, isOnline: Boolean?, apiKey: String?) ->
        ChatViewModel(
            application = androidApplication(),
            modelNameOrUri = modelNameOrUri,
            isOnline = isOnline,
            apiKey = apiKey
        )
    }
    viewModel { 
        OnlineModelSelectionViewModel()
    }
}