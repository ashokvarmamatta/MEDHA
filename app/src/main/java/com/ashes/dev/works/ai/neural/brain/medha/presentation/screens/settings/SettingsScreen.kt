package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import com.ashes.dev.works.ai.neural.brain.medha.ui.icons.MedhaIcons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatViewModel
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentCyan
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGold
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGreen
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentPink
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.StatusError
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.StatusSuccess
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var newKeyInput by remember { mutableStateOf("") }
    var newKeyLabel by remember { mutableStateOf("") }
    var newKeyBaseUrl by remember { mutableStateOf("") }
    var curlMode by remember { mutableStateOf(false) }
    var curlText by remember { mutableStateOf("") }
    var parsedCurlToken by remember { mutableStateOf("") }
    var parsedCurlBase by remember { mutableStateOf("") }
    var parsedCurlModel by remember { mutableStateOf("") }
    var showDeleteModel by remember { mutableStateOf<com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo?>(null) }

    val context = LocalContext.current

    // All-files access is granted on a system Settings screen, so there is no result to await —
    // re-check whenever this screen comes back to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStorageState()
                viewModel.scanAvailableModels()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // SAF file picker for importing offline models — no permission needed
    val modelPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "model.litertlm"
        viewModel.importModelFromUri(uri, fileName)
    }

    // System folder picker for the shared model folder. OpenDocumentTree needs no permission —
    // the user grants exactly one folder and MEDHA persists that grant across reboots.
    val modelFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) viewModel.setSharedModelFolder(treeUri)
    }

    // Same idea for one file at a time — used in place, unlike "Import" which copies.
    val singleModelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.addSharedModelFile(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Text("Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // On-device models
            SectionHeader("Shared model folder")
            Spacer(modifier = Modifier.height(8.dp))
            SharedModelFolderCard(
                folderName = uiState.sharedFolderPath,
                suggestedPath = uiState.suggestedFolderPath,
                onPick = { runCatching { modelFolderPicker.launch(null) } },
                onPickFile = { runCatching { singleModelPicker.launch(arrayOf("*/*")) } },
                onClear = { viewModel.clearSharedModelFolder() }
            )

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Models")
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.availableModels.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        uiState.availableModels.forEachIndexed { index, model ->
                            ModelOption(
                                model = model,
                                isSelected = uiState.selectedModel?.filePath == model.filePath,
                                copyProgress = uiState.modelCopyProgress[model.fileName],
                                // A shared model cannot be loaded in place — the native
                                // loader can't open a SAF document — so tapping it starts
                                // the copy instead of a doomed load.
                                onClick = {
                                    if (model.isShared) viewModel.copySharedModelIn(model)
                                    else viewModel.selectModel(model)
                                },
                                // No delete for shared-folder models: the file belongs to the
                                // user and their other apps may be using it.
                                onDelete = if (model.isShared) null else ({ showDeleteModel = model })
                            )
                            if (index < uiState.availableModels.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Explicit load/unload. A resident Gemma 4 holds ~2.5GB; being able to
                // release it without killing the app matters on a full device.
                ModelLoadControl(
                    status = uiState.modelStatus,
                    modelName = uiState.selectedModel?.displayName,
                    onLoad = { viewModel.loadModel() },
                    onUnload = { viewModel.unloadModel() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Import model button
            Surface(
                onClick = { modelPickerLauncher.launch(arrayOf("application/octet-stream", "*/*")) },
                shape = RoundedCornerShape(12.dp),
                color = AccentCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (uiState.availableModels.isEmpty()) "Import Model File (.litertlm / .bin)" else "Import Another Model",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState.availableModels.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Download a .litertlm model from Model Catalog, or import one manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Where to get models (shown under offline mode)
            Spacer(modifier = Modifier.height(12.dp))
            OfflineModelSourcesSection()

            // Model Catalog — download LiteRT LM models directly
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Model Catalog")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Download optimized LiteRT LM models directly. Supports resume.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            com.ashes.dev.works.ai.neural.brain.medha.data.ModelCatalog.models.forEach { catalogModel ->
                val isDownloaded = viewModel.isModelDownloaded(catalogModel)
                val progress = uiState.catalogDownloadProgress[catalogModel.id]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(catalogModel.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (catalogModel.badge != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            catalogModel.badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(AccentGold, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(catalogModel.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${catalogModel.sizeLabel} \u2022 ${catalogModel.featureTags.joinToString(", ")} \u2022 Min ${catalogModel.minRamGb}GB RAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isDownloaded) {
                                androidx.compose.material3.TextButton(onClick = { viewModel.activateCatalogModel(catalogModel) }) {
                                    Text("USE", fontWeight = FontWeight.Bold, color = AccentGreen)
                                }
                            } else if (progress != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = AccentCyan
                                    )
                                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                                }
                            } else {
                                androidx.compose.material3.TextButton(onClick = { viewModel.downloadCatalogModel(catalogModel) }) {
                                    Text("DOWNLOAD", fontWeight = FontWeight.Bold, color = AccentCyan)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete model confirmation dialog
            showDeleteModel?.let { model ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showDeleteModel = null },
                    icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Delete Model?", fontWeight = FontWeight.Bold) },
                    text = { Text("Delete ${model.fileName} (${model.sizeInMb} MB) from app storage?") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            viewModel.deleteModel(model)
                            showDeleteModel = null
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { showDeleteModel = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Current Status Info
            SectionHeader("Current Status")
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow("Mode", "On-device")
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow("Model", uiState.selectedModel?.displayName ?: "None selected")
                    uiState.selectedModel?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusRow("Size", "${it.sizeInMb} MB")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow("Engine", uiState.modelStatus.toString().substringAfterLast("$").substringBefore("("))
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow("Messages", "${uiState.messages.size}")
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow("Version", ChatViewModel.APP_VERSION)
                }
            }
        }
    }
}

/**
 * Load / unload the selected model, showing what the engine is actually doing.
 *
 * The button is the single control for engine residency: it reads "Unload" only while the model
 * is genuinely in memory, and "Retry" after a failure, so it never invites a tap that does
 * nothing.
 */
@Composable
private fun ModelLoadControl(
    status: ModelStatus,
    modelName: String?,
    onLoad: () -> Unit,
    onUnload: () -> Unit
) {
    val loaded = status is ModelStatus.Ready
    val busy = status is ModelStatus.Loading || status is ModelStatus.Initializing

    val (label, tint) = when {
        busy -> "Loading…" to AccentGold
        loaded -> "Unload model" to StatusWarning
        status is ModelStatus.Error -> "Retry load" to StatusError
        else -> "Load model" to AccentGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            when {
                                loaded -> StatusSuccess
                                busy -> AccentGold
                                status is ModelStatus.Error -> StatusError
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when {
                            loaded -> "Model loaded"
                            busy -> "Loading model…"
                            status is ModelStatus.Error -> "Model failed to load"
                            else -> "Model not loaded"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        when {
                            loaded -> "${modelName ?: "Model"} is in memory and ready to chat."
                            busy -> "Mapping weights — this takes a few seconds."
                            status is ModelStatus.Error -> (status as ModelStatus.Error).message
                            else -> "Frees roughly the model's size in RAM while unloaded."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = { if (!busy) { if (loaded) onUnload() else onLoad() } },
                enabled = !busy,
                shape = RoundedCornerShape(10.dp),
                color = tint.copy(alpha = if (busy) 0.08f else 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tint.copy(alpha = if (busy) 0.6f else 1f)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Points MEDHA at a folder of models the user owns, so a 2.4 GB `.litertlm` downloaded by another
 * app is used in place instead of downloaded a second time.
 *
 * Deliberately a folder PICKER rather than a permission toggle: `ACTION_OPEN_DOCUMENT_TREE` grants
 * exactly one folder and needs no manifest permission, where all-files access would be a
 * Play-restricted permission for the same result.
 */
@Composable
private fun SharedModelFolderCard(
    folderName: String,
    suggestedPath: String,
    onPick: () -> Unit,
    onPickFile: () -> Unit,
    onClear: () -> Unit
) {
    val picked = folderName.isNotBlank()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    MedhaIcons.Folder,
                    contentDescription = null,
                    tint = if (picked) AccentCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (picked) folderName else "No folder selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (picked) AccentCyan else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (picked) {
                            "Models here and in its subfolders are listed below and loaded in " +
                                "place — no second copy. New downloads are saved here too."
                        } else {
                            "Pick a folder — e.g. $suggestedPath — to use models your other apps " +
                                "already downloaded, or that you copied over from a PC."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Surface(
                    onClick = onPick,
                    shape = RoundedCornerShape(10.dp),
                    color = AccentCyan.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (picked) "Change folder" else "Choose folder",
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCyan
                    )
                }
                if (picked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onClear,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Remove",
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onPickFile,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Or add a single model file",
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                "Loaded from where it already is — unlike Import below, which makes a second copy.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

private fun formatTokenCount(tokens: Int): String {
    return when {
        tokens >= 1_000_000 -> "${tokens / 1_000_000}M"
        tokens >= 1_000 -> "${tokens / 1_000}K"
        else -> "$tokens"
    }
}

@Composable
private fun ModelOption(
    model: ModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    copyProgress: Float? = null
) {
    val borderMod = if (isSelected) {
        Modifier.border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Row(
        modifier = Modifier.fillMaxWidth().then(borderMod).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AccentGold, unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(model.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurface)
                // Tells the user this file is the shared copy, so deleting it affects other apps.
                if (model.isShared) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = AccentCyan.copy(alpha = 0.15f)) {
                        Text(
                            "SHARED",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = AccentCyan
                        )
                    }
                }
            }
            Text("${model.fileName}  \u2022  ${model.sizeInMb} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            // A shared model has to be copied in before the engine can open it; say so on the
            // row rather than letting the user tap it and get an error.
            if (copyProgress != null) {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { copyProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentCyan
                )
                Text(
                    "Copying into MEDHA\u2026 ${(copyProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            } else if (model.isShared) {
                Text(
                    "Tap to copy into MEDHA (${model.sizeInMb} MB) \u2014 needed before it can run",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan.copy(alpha = 0.8f)
                )
            }
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentGold, modifier = Modifier.size(20.dp))
        }
        onDelete?.let {
            IconButton(onClick = it, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ==================== RESOURCE SECTIONS ====================

@Composable
private fun OfflineModelSourcesSection() {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Surface(
                onClick = { expanded = !expanded },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDCE5", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Where to Get Models", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AccentGreen)
                            Text("Download .litertlm model files for offline use", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    Text(if (expanded) "\u25B2" else "\u25BC", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    ResourceLink("LiteRT Community", "Official LiteRT LM models \u2014 Gemma 4, DeepSeek", AccentGreen, "https://huggingface.co/litert-community", uriHandler, badge = "RECOMMENDED")
                    ResourceLink("Hugging Face Hub", "Largest model repository \u2014 LiteRT LM, GGUF, safetensors", AccentGold, "https://huggingface.co/models", uriHandler)
                    ResourceLink("Ollama Library", "Curated models \u2014 Llama, Gemma, Phi, Mistral", AccentCyan, "https://ollama.com/library", uriHandler)
                    ResourceLink("LM Studio", "Desktop app with model browser \u2014 GGUF models", AccentPink, "https://lmstudio.ai", uriHandler)
                    ResourceLink("GPT4All", "Models optimized for consumer hardware", AccentGreen, "https://gpt4all.io", uriHandler)
                    ResourceLink("Mozilla Llamafile", "Single-file executables \u2014 model + runtime", AccentGold, "https://github.com/Mozilla-Ocho/llamafile", uriHandler)
                }
            }
        }
    }
}

@Composable
private fun ResourceLink(
    title: String,
    description: String,
    accentColor: Color,
    url: String,
    uriHandler: UriHandler,
    badge: String? = null
) {
    Surface(
        onClick = { uriHandler.openUri(url) },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                badge,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text("\u2197", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}
