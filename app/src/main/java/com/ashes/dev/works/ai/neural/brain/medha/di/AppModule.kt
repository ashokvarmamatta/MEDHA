package com.ashes.dev.works.ai.neural.brain.medha.di

import com.ashes.dev.works.ai.neural.brain.medha.data.repository.SettingsRepository
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SettingsRepository(androidContext()) }
    viewModel { ChatViewModel(androidApplication(), get()) }
}
