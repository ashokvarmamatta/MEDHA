package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MenuScreen(onOfflineModeClick: () -> Unit, onOnlineModeClick: () -> Unit) {
    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onOfflineModeClick) {
                Text("Offline Mode")
            }
            Button(onClick = onOnlineModeClick) {
                Text("Online Mode")
            }
        }
    }
}