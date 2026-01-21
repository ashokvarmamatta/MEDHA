package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.model

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ModelListScreen(onModelClick: (String) -> Unit, onImportClick: () -> Unit) {
    var showBinFiles by remember { mutableStateOf(true) }
    val models by remember(showBinFiles) {
        derivedStateOf {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val extension = if (showBinFiles) ".bin" else ".tflite"
            downloads.listFiles { _, name -> name.endsWith(extension) }?.map { it.name } ?: emptyList()
        }
    }
    val context = LocalContext.current

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { showBinFiles = true }) {
                    Text(".bin files")
                }
                Button(onClick = { showBinFiles = false }) {
                    Text(".tflite files")
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(models) { model ->
                    Text(
                        text = model,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelClick(model) }
                            .padding(16.dp)
                    )
                }
            }
            Button(
                onClick = onImportClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            ) {
                Text("Import Model")
            }
            Button(
                onClick = {
                    val url = if (showBinFiles) {
                        "https://www.kaggle.com/models/google/gemma/tfLite"
                    } else {
                        "https://huggingface.co/models?library=tflite&p=1&sort=trending"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("Download Models")
            }
        }
    }
}