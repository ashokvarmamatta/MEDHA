package com.ashes.dev.works.ai.neural.brain.medha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatScreen
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.MEDHATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MEDHATheme {
                ChatScreen()
            }
        }
    }
}
