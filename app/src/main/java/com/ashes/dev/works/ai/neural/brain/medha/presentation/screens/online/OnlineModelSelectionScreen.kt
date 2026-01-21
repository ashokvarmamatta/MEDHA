package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.online

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineModelSelectionScreen(
    viewModel: OnlineModelSelectionViewModel = koinViewModel(),
    onNavigateToChat: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Online Model") })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Enter your Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null
            )
            
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { 
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://makersuite.google.com/app/apikey"))
                    context.startActivity(intent)
                }) {
                    Text("Get a Gemini API key")
                }
                Button(onClick = { viewModel.fetchModels(apiKey) }) {
                    Text("Refresh")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Available models", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.models) { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectModel(model) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedModel == model,
                            onClick = { viewModel.selectModel(model) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(model)
                    }
                }
            }

            Button(
                onClick = { 
                    if (uiState.selectedModel != null) {
                        onNavigateToChat(apiKey, uiState.selectedModel!!)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedModel != null
            ) {
                Text("Start Chat")
            }
        }
    }
}