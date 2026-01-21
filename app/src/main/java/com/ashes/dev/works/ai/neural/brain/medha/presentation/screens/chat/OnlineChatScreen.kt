package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OnlineChatScreen(onApiKeyClick: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Enter your Gemini API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Button(onClick = { onApiKeyClick(apiKey) }) {
                    Text("Submit")
                }
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://makersuite.google.com/app/apikey"))
                    context.startActivity(intent)
                }) {
                    Text("Get API Key")
                }
            }
        }
    }
}