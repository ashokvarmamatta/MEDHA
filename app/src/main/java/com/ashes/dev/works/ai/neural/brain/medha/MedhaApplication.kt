package com.ashes.dev.works.ai.neural.brain.medha

import android.app.Application
import com.ashes.dev.works.ai.neural.brain.medha.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MedhaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MedhaApplication)
            modules(appModule)
        }
    }
}