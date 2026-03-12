package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashes.dev.works.ai.neural.brain.medha.data.remote.GeminiModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ApiKeyEntry
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.AppMode
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatViewModel
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentCyan
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGold
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGreen
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
            // Mode Selection
            SectionHeader("Chat Mode")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    ModeOption(
                        title = "Offline Mode",
                        description = "Run AI locally on your device using MediaPipe. No internet needed.",
                        isSelected = uiState.appMode is AppMode.Offline,
                        onClick = { viewModel.setAppMode(AppMode.Offline) },
                        accentColor = AccentGreen
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    ModeOption(
                        title = "Online Mode",
                        description = "Use Google Gemini API. Requires internet and API key.",
                        isSelected = uiState.appMode is AppMode.Online,
                        onClick = { viewModel.setAppMode(AppMode.Online) },
                        accentColor = AccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Online Settings
            if (uiState.appMode is AppMode.Online) {
                // ===== API Keys Section =====
                SectionHeader("API Keys")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${uiState.apiKeys.size} key(s) added, ${uiState.validatedKeys.size} validated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Add new key form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
                        Text(
                            "Add API Key",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Get your free key from Google AI Studio (aistudio.google.com)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newKeyInput,
                            onValueChange = { newKeyInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste your API key here", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newKeyLabel,
                            onValueChange = { newKeyLabel = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Label (optional, e.g. 'Personal', 'Work')", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            onClick = {
                                if (newKeyInput.isNotBlank()) {
                                    viewModel.addApiKey(newKeyInput.trim(), newKeyLabel.trim())
                                    newKeyInput = ""
                                    newKeyLabel = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (newKeyInput.isNotBlank()) AccentCyan else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isFetchingOnlineModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Fetching models...", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                } else {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (newKeyInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Add Key & Fetch Models",
                                        color = if (newKeyInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Status message
                        uiState.apiKeyTestResult?.let { result ->
                            Spacer(modifier = Modifier.height(10.dp))
                            val isSuccess = result.contains("validated") || result.contains("Found")
                            val isError = result.startsWith("Invalid") || result.startsWith("Error") || result.startsWith("Failed") || result.startsWith("This API key")
                            val color = when {
                                isSuccess -> StatusSuccess
                                isError -> StatusError
                                else -> StatusWarning
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isSuccess) Icons.Default.Check else if (isError) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    result,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ===== Saved Keys List =====
                if (uiState.apiKeys.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Saved Keys",
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            uiState.apiKeys.forEachIndexed { index, entry ->
                                ApiKeyItem(
                                    entry = entry,
                                    isActive = uiState.activeKey?.id == entry.id,
                                    isTesting = uiState.isTestingKeyId == entry.id,
                                    onTest = { viewModel.testApiKey(entry.id) },
                                    onDelete = { viewModel.removeApiKey(entry.id) }
                                )
                                if (index < uiState.apiKeys.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ===== Model Selection =====
                AnimatedVisibility(
                    visible = uiState.onlineAvailableModels.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader("Select Model")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${uiState.onlineAvailableModels.size} models available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                val recommended = uiState.onlineAvailableModels.filter {
                                    it.modelId.contains("flash") || it.modelId.contains("pro")
                                }
                                val others = uiState.onlineAvailableModels - recommended.toSet()

                                if (recommended.isNotEmpty()) {
                                    Text(
                                        "\u2B50 Recommended",
                                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                    recommended.forEachIndexed { index, model ->
                                        OnlineModelOption(
                                            model = model,
                                            isSelected = uiState.onlineModelName == model.modelId,
                                            onClick = { viewModel.selectOnlineModel(model) }
                                        )
                                        if (index < recommended.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                            )
                                        }
                                    }
                                }

                                if (others.isNotEmpty()) {
                                    if (recommended.isNotEmpty()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    }
                                    Text(
                                        "Other Models",
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    others.forEachIndexed { index, model ->
                                        OnlineModelOption(
                                            model = model,
                                            isSelected = uiState.onlineModelName == model.modelId,
                                            onClick = { viewModel.selectOnlineModel(model) }
                                        )
                                        if (index < others.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Test key button (test selected model with first unvalidated or all keys)
                        if (uiState.apiKeys.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Test a key above to validate it for chatting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        if (uiState.hasAnyValidatedKey) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusSuccess,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Ready to chat! Go back and start.",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Offline Model Selection
            if (uiState.appMode is AppMode.Offline) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Offline Models")
                    IconButton(onClick = { viewModel.scanAvailableModels() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.availableModels.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("No models found", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Place .bin model files in your Downloads folder, then tap the refresh button above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
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
                                    onClick = { viewModel.selectModel(model) }
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
                }

                Spacer(modifier = Modifier.height(24.dp))
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
                    StatusRow("Mode", if (uiState.appMode is AppMode.Online) "Online (Gemini API)" else "Offline (On-device)")
                    Spacer(modifier = Modifier.height(6.dp))
                    if (uiState.appMode is AppMode.Online) {
                        StatusRow("Model", uiState.onlineModelName)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusRow("API Keys", "${uiState.validatedKeys.size}/${uiState.apiKeys.size} validated")
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusRow("Active Key", uiState.activeKey?.label ?: "None")
                    } else {
                        StatusRow("Model", uiState.selectedModel?.displayName ?: "None selected")
                        uiState.selectedModel?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            StatusRow("Size", "${it.sizeInMb} MB")
                        }
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

@Composable
private fun ApiKeyItem(
    entry: ApiKeyEntry,
    isActive: Boolean,
    isTesting: Boolean,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .border(
                    width = 2.dp,
                    color = if (entry.isValidated) StatusSuccess else if (entry.lastError != null) StatusError else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Key info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.isValidated) StatusSuccess else MaterialTheme.colorScheme.onSurface
                )
                if (isActive && entry.isValidated) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AccentCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "ACTIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Text(
                entry.maskedKey,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
            entry.lastError?.let { err ->
                Text(
                    err.take(50),
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusError,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Test button
        Surface(
            onClick = { if (!isTesting) onTest() },
            shape = RoundedCornerShape(8.dp),
            color = if (entry.isValidated) StatusSuccess.copy(alpha = 0.1f) else AccentGold.copy(alpha = 0.15f)
        ) {
            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentGold
                    )
                } else {
                    Text(
                        if (entry.isValidated) "Re-test" else "Test",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isValidated) StatusSuccess else AccentGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Delete button
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove key",
                modifier = Modifier.size(18.dp),
                tint = StatusError.copy(alpha = 0.6f)
            )
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

@Composable
private fun ModeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun OnlineModelOption(
    model: GeminiModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderMod = if (isSelected) {
        Modifier.border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderMod)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AccentCyan, unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                model.modelId,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AccentCyan else MaterialTheme.colorScheme.onSurface
            )
            model.displayName?.let { displayName ->
                if (displayName != model.modelId) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row {
                model.inputTokenLimit?.let {
                    Text(
                        "In: ${formatTokenCount(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                model.outputTokenLimit?.let {
                    Text(
                        "Out: ${formatTokenCount(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentCyan, modifier = Modifier.size(20.dp))
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
    onClick: () -> Unit
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
            Text(model.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurface)
            Text("${model.fileName}  \u2022  ${model.sizeInMb} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentGold, modifier = Modifier.size(20.dp))
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
