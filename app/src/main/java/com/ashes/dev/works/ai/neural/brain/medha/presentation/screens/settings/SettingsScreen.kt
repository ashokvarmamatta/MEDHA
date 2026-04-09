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
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
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
                        description = "Run AI locally on your device using LiteRT LM. No internet needed.",
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
                        // ── Title + cURL mode toggle ─────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (curlMode) "Add via cURL" else "Add API Key",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                onClick = { curlMode = !curlMode },
                                shape = RoundedCornerShape(16.dp),
                                color = if (curlMode) AccentCyan else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("cURL", style = MaterialTheme.typography.labelSmall,
                                        color = if (curlMode) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (curlMode) "Paste a cURL command — key, base URL & model are extracted automatically"
                            else "Get your free key from Google AI Studio (aistudio.google.com)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (curlMode) {
                            // ── cURL MODE ────────────────────────────────────
                            OutlinedTextField(
                                value = curlText,
                                onValueChange = {
                                    curlText = it
                                    // Parse cURL
                                    val bearerRegex = Regex("""Authorization:\s*Bearer\s+([^\s"'\\]+)""", RegexOption.IGNORE_CASE)
                                    parsedCurlToken = bearerRegex.find(it)?.groupValues?.get(1) ?: ""
                                    val urlRegex = Regex("""https?://[^\s"'\\]+""")
                                    val rawUrl = urlRegex.find(it)?.value ?: ""
                                    parsedCurlBase = rawUrl.replace(Regex("""/chat/completions.*"""), "")
                                        .replace(Regex("""/v1/.*"""), "/v1")
                                    val modelRegex = Regex(""""model"\s*:\s*"([^"]+)"""")
                                    parsedCurlModel = modelRegex.find(it)?.groupValues?.get(1) ?: ""
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                                placeholder = { Text("curl -X POST \"https://...\" -H \"Authorization: Bearer sk-...\" -d '{\"model\":\"...\"}'", style = MaterialTheme.typography.bodySmall) },
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 6,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            // Parsed preview
                            if (parsedCurlToken.isNotBlank() || parsedCurlBase.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("Parsed:", style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (parsedCurlBase.isNotBlank())
                                            Text("🌐 $parsedCurlBase", style = MaterialTheme.typography.bodySmall)
                                        if (parsedCurlToken.isNotBlank())
                                            Text("🔑 ${if (parsedCurlToken.length > 8) "${parsedCurlToken.take(6)}••••${parsedCurlToken.takeLast(4)}" else "••••"}",
                                                style = MaterialTheme.typography.bodySmall)
                                        if (parsedCurlModel.isNotBlank())
                                            Text("🤖 $parsedCurlModel", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Label for cURL
                            OutlinedTextField(
                                value = newKeyLabel,
                                onValueChange = { newKeyLabel = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Label (optional)", style = MaterialTheme.typography.bodyMedium) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Save cURL button
                            Surface(
                                onClick = {
                                    if (parsedCurlToken.isNotBlank()) {
                                        val lbl = newKeyLabel.trim().ifBlank {
                                            parsedCurlModel.ifBlank { parsedCurlBase.substringAfterLast("/").take(20) }
                                        }
                                        viewModel.addApiKey(parsedCurlToken, lbl, parsedCurlBase)
                                        curlText = ""
                                        parsedCurlToken = ""
                                        parsedCurlBase = ""
                                        parsedCurlModel = ""
                                        newKeyLabel = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (parsedCurlToken.isNotBlank()) AccentCyan else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (parsedCurlToken.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Save from cURL",
                                        color = if (parsedCurlToken.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                        } else {
                            // ── NORMAL KEY MODE ──────────────────────────────
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Base URL field (optional)
                            OutlinedTextField(
                                value = newKeyBaseUrl,
                                onValueChange = { newKeyBaseUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Base URL (optional, for custom endpoints)", style = MaterialTheme.typography.bodyMedium) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                supportingText = {
                                    Text("Leave blank for default Gemini API. Set for OpenAI-compatible proxies.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                onClick = {
                                    if (newKeyInput.isNotBlank()) {
                                        viewModel.addApiKey(newKeyInput.trim(), newKeyLabel.trim(), newKeyBaseUrl.trim())
                                        newKeyInput = ""
                                        newKeyLabel = ""
                                        newKeyBaseUrl = ""
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
                                    index = index,
                                    totalKeys = uiState.apiKeys.size,
                                    isActive = uiState.activeKey?.id == entry.id,
                                    isTesting = uiState.isTestingKeyId == entry.id,
                                    isCheckingAll = uiState.checkingAllModelsKeyId == entry.id,
                                    modelCheckProgress = if (uiState.checkingAllModelsKeyId == entry.id) uiState.modelCheckProgress else emptyMap(),
                                    hasModelsLoaded = uiState.onlineAvailableModels.isNotEmpty(),
                                    onTest = { viewModel.testApiKey(entry.id) },
                                    onCheckAll = { viewModel.checkAllModels(entry.id) },
                                    onToggleModel = { modelId -> viewModel.toggleModelForKey(entry.id, modelId) },
                                    onMoveUp = { viewModel.moveApiKey(entry.id, -1) },
                                    onMoveDown = { viewModel.moveApiKey(entry.id, 1) },
                                    onToggleEnabled = { viewModel.toggleApiKeyEnabled(entry.id) },
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

                        // Gather per-key status for each model across all keys
                        val allKeyStatuses = uiState.apiKeys.associate { key ->
                            key.id to key
                        }

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
                                            apiKeys = uiState.apiKeys,
                                            testingSingleModel = uiState.testingSingleModel,
                                            onClick = { viewModel.selectOnlineModel(model) },
                                            onTestForKey = { keyId -> viewModel.testSingleModel(keyId, model.modelId) }
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
                                            apiKeys = uiState.apiKeys,
                                            testingSingleModel = uiState.testingSingleModel,
                                            onClick = { viewModel.selectOnlineModel(model) },
                                            onTestForKey = { keyId -> viewModel.testSingleModel(keyId, model.modelId) }
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
                                "Select a model and use 'Test' on each key, or 'Check All Models' per key.",
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

                // Where to get API keys
                Spacer(modifier = Modifier.height(16.dp))
                ApiKeyProvidersSection()

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Offline Model Selection
            if (uiState.appMode is AppMode.Offline) {
                SectionHeader("Offline Models")
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
                                    onClick = { viewModel.selectModel(model) },
                                    onDelete = { showDeleteModel = model }
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
            }

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
    index: Int,
    totalKeys: Int,
    isActive: Boolean,
    isTesting: Boolean,
    isCheckingAll: Boolean,
    modelCheckProgress: Map<String, String?>,
    hasModelsLoaded: Boolean,
    onTest: () -> Unit,
    onCheckAll: () -> Unit,
    onToggleModel: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    var showModels by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority reorder arrows (only show when multiple keys)
            if (totalKeys > 1) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move up",
                            modifier = Modifier.size(18.dp),
                            tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 9.sp
                    )
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalKeys - 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move down",
                            modifier = Modifier.size(18.dp),
                            tint = if (index < totalKeys - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

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
                if (entry.selectedModels.isNotEmpty()) {
                    Text(
                        "${entry.selectedModels.size} model(s) selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan.copy(alpha = if (entry.isEnabled) 0.7f else 0.3f)
                    )
                }
                if (!entry.isEnabled) {
                    Text(
                        "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }

            // Enable/Disable toggle
            Switch(
                checked = entry.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                modifier = Modifier.height(24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StatusSuccess,
                    checkedTrackColor = StatusSuccess.copy(alpha = 0.3f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Test button
            Surface(
                onClick = { if (!isTesting && entry.isEnabled) onTest() },
                shape = RoundedCornerShape(8.dp),
                color = if (!entry.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else if (entry.isValidated) StatusSuccess.copy(alpha = 0.1f)
                    else AccentGold.copy(alpha = 0.15f)
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
                            color = if (!entry.isEnabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else if (entry.isValidated) StatusSuccess else AccentGold
                        )
                    }
                }
            }

            // Models select/deselect button (only if checked models exist)
            if (entry.checkedModels.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    onClick = { showModels = !showModels },
                    shape = RoundedCornerShape(8.dp),
                    color = AccentCyan.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (showModels) "Hide" else "Models",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
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

        // Check All Models button
        if (hasModelsLoaded) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { if (!isCheckingAll) onCheckAll() },
                    shape = RoundedCornerShape(8.dp),
                    color = AccentCyan.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCheckingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val done = modelCheckProgress.count { it.value != null }
                            val total = modelCheckProgress.size
                            Text(
                                "Checking... $done/$total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Check All Models",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                    }
                }

                // Show/hide models toggle (only if checked models exist)
                if (entry.checkedModels.isNotEmpty()) {
                    Surface(
                        onClick = { showModels = !showModels },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            if (showModels) "Hide" else "${entry.workingModels.size} OK",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Live progress during checking
        if (isCheckingAll && modelCheckProgress.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                modelCheckProgress.forEach { (modelId, result) ->
                    // result: null = pending, "" = pass, non-empty string = error message
                    val isPass = result?.isEmpty() == true
                    val isFail = result?.isNotEmpty() == true
                    // isPending when result is null (entry exists but value is null)
                    val isPending = result == null

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isPending -> CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            isPass -> Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = StatusSuccess
                            )
                            else -> Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = StatusError
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                modelId,
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isPass -> StatusSuccess
                                    isFail -> StatusError.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isFail) {
                                Text(
                                    (result ?: "").take(60),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusError.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Checked models list (expandable) — user can select/deselect
        if (showModels && entry.checkedModels.isNotEmpty() && !isCheckingAll) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "Select models for this key:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    entry.checkedModels.toSortedMap().forEach { (modelId, errorMsg) ->
                        val works = errorMsg == null
                        val isModelSelected = modelId in entry.selectedModels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = works) { onToggleModel(modelId) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isModelSelected,
                                onCheckedChange = { if (works) onToggleModel(modelId) },
                                enabled = works,
                                modifier = Modifier.size(20.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentCyan,
                                    uncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    modelId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (works) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (errorMsg != null) {
                                    Text(
                                        errorMsg.take(60),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusError.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (works) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (works) "OK" else "FAIL",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (works) StatusSuccess else StatusError,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
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
    apiKeys: List<ApiKeyEntry>,
    testingSingleModel: String?,
    onClick: () -> Unit,
    onTestForKey: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val borderMod = if (isSelected) {
        Modifier.border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderMod)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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

                // Per-key status summary row
                if (apiKeys.any { it.checkedModels.containsKey(model.modelId) }) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        apiKeys.forEach { key ->
                            val status = key.isModelWorking(model.modelId)
                            if (status != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (status) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${key.label}: ${if (status) "OK" else "FAIL"}",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (status) StatusSuccess else StatusError
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Show expand arrow if there are keys to test
            if (apiKeys.isNotEmpty()) {
                Surface(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        if (expanded) "Less" else "Test",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontSize = 10.sp
                    )
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = AccentCyan, modifier = Modifier.size(20.dp))
            }
        }

        // Expanded: per-key test status + test button
        if (expanded && apiKeys.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 12.dp, bottom = 8.dp)
            ) {
                apiKeys.forEach { key ->
                    val status = key.isModelWorking(model.modelId)
                    val error = key.modelError(model.modelId)
                    val isTesting = testingSingleModel == "${key.id}:${model.modelId}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status icon
                        when (status) {
                            true -> Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusSuccess)
                            false -> Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusError)
                            null -> Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(50))
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                key.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (status) {
                                    true -> StatusSuccess
                                    false -> StatusError
                                    null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                            if (error != null) {
                                Text(
                                    error.take(60),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusError.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Test button per key
                        Surface(
                            onClick = { if (!isTesting) onTestForKey(key.id) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (status == true) StatusSuccess.copy(alpha = 0.1f) else AccentGold.copy(alpha = 0.12f)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = AccentGold
                                    )
                                } else {
                                    Text(
                                        if (status != null) "Re-test" else "Test",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = if (status == true) StatusSuccess else AccentGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
    onDelete: (() -> Unit)? = null
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
        onDelete?.let {
            IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
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
private fun ApiKeyProvidersSection() {
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
                        Text("\uD83D\uDD11", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Where to Get API Keys", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = AccentCyan)
                            Text("Free & paid providers \u2014 tap to expand", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    Text(if (expanded) "\u25B2" else "\u25BC", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Free tier providers
                    Text("Free Tier", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentGreen, modifier = Modifier.padding(bottom = 6.dp))
                    ResourceLink("Google Gemini \u2014 AI Studio", "Most generous free tier \u2014 Gemini 2.0 Flash, 1.5 Pro", AccentGreen, "https://aistudio.google.com/apikey", uriHandler, badge = "BEST FREE")
                    ResourceLink("Groq", "Blazing fast \u2014 Llama 3, Mixtral, Gemma", AccentCyan, "https://console.groq.com/keys", uriHandler, badge = "FAST")
                    ResourceLink("OpenRouter", "Multi-model gateway \u2014 single key, many providers", AccentGold, "https://openrouter.ai/keys", uriHandler, badge = "MULTI")
                    ResourceLink("NVIDIA NIM", "1000 free credits \u2014 Llama, Mistral", AccentGreen, "https://build.nvidia.com", uriHandler)
                    ResourceLink("Together AI", "Free credits on signup \u2014 wide model selection", AccentCyan, "https://api.together.ai/settings/api-keys", uriHandler)
                    ResourceLink("Mistral AI", "Free experiment tier \u2014 Mistral Small, Large", AccentGold, "https://console.mistral.ai/api-keys", uriHandler)
                    ResourceLink("DeepSeek", "Free credits \u2014 V3, R1 reasoning, Coder", AccentGreen, "https://platform.deepseek.com/api_keys", uriHandler)
                    ResourceLink("Cohere", "Free trial \u2014 Command R/R+, RAG, embeddings", AccentCyan, "https://dashboard.cohere.com/api-keys", uriHandler)
                    ResourceLink("Hugging Face", "Free tier for thousands of models", AccentGold, "https://huggingface.co/settings/tokens", uriHandler)
                    ResourceLink("SambaNova Cloud", "Free tier \u2014 fast custom hardware", AccentGreen, "https://cloud.sambanova.ai/apis", uriHandler)
                    ResourceLink("Cerebras", "Free tier \u2014 extremely fast inference", AccentCyan, "https://cloud.cerebras.ai", uriHandler)
                    ResourceLink("Fireworks AI", "Free credits \u2014 fast open models", AccentGold, "https://fireworks.ai/account/api-keys", uriHandler)
                    ResourceLink("DeepInfra", "Free credits \u2014 serverless inference", AccentGreen, "https://deepinfra.com/dash/api_keys", uriHandler)
                    ResourceLink("Cloudflare Workers AI", "10K free neurons/day \u2014 edge inference", AccentCyan, "https://dash.cloudflare.com", uriHandler)
                    ResourceLink("GitHub Models", "Free \u2014 GPT-4o, Llama, Mistral via GitHub", AccentGold, "https://github.com/marketplace/models", uriHandler)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Paid providers
                    Text("Paid", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = StatusWarning, modifier = Modifier.padding(bottom = 6.dp))
                    ResourceLink("OpenAI", "GPT-4o, o1, o3, DALL-E \u2014 prepaid credits", StatusWarning, "https://platform.openai.com/api-keys", uriHandler, badge = "PAID")
                    ResourceLink("Anthropic Claude", "Claude Opus 4, Sonnet 4 \u2014 billing required", StatusWarning, "https://console.anthropic.com/settings/keys", uriHandler, badge = "PAID")
                    ResourceLink("Perplexity API", "Search-augmented LLMs with citations", StatusWarning, "https://www.perplexity.ai/settings/api", uriHandler, badge = "PAID")
                    ResourceLink("Azure OpenAI", "Enterprise OpenAI on Azure \u2014 $200 new account credits", StatusWarning, "https://azure.microsoft.com/en-us/products/ai-services/openai-service", uriHandler, badge = "PAID")
                    ResourceLink("AWS Bedrock", "Claude, Llama, Mistral on AWS", StatusWarning, "https://aws.amazon.com/bedrock", uriHandler, badge = "PAID")
                    ResourceLink("Google Vertex AI", "$300 free GCP credits \u2014 Gemini, PaLM, Imagen", StatusWarning, "https://console.cloud.google.com", uriHandler, badge = "PAID")
                    ResourceLink("Replicate", "Pay-per-second \u2014 open models + image/video/audio", StatusWarning, "https://replicate.com/account/api-tokens", uriHandler, badge = "PAID")
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
