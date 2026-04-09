package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.AppMode
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.CustomGrandMaster
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.GrandMaster
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplate
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplates
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.TemplateAction
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.TemplateCategory
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.User
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentCyan
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGold
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGreen
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentPink
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientEnd
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientMid
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientStart
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.StatusError
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.StatusSuccess
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.StatusWarning
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ChatSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToAbout: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState(initial = emptyList())
    var prompt by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.setPendingImage(it.toString()) }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setPendingAudio(it.toString()) }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Prompt templates bottom sheet
    if (uiState.showPromptTemplates) {
        PromptTemplatesSheet(
            onDismiss = { viewModel.hidePromptTemplates() },
            onSelectTemplate = { template ->
                viewModel.hidePromptTemplates()
                when (template.action) {
                    TemplateAction.PICK_IMAGE -> {
                        viewModel.setPendingImageTemplate(template)
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    TemplateAction.PICK_AUDIO -> {
                        viewModel.setPendingImageTemplate(template) // reuse for prompt
                        audioPickerLauncher.launch("audio/*")
                    }
                    TemplateAction.TEXT -> {
                        if (template.requiresInput) prompt = template.promptPrefix
                        else viewModel.sendMessage(template.promptPrefix)
                    }
                }
            },
            supportsImage = uiState.supportsImageInput,
            supportsAudio = uiState.supportsAudioInput
        )
    }

    // Image response style picker (shown after image selected via template)
    if (uiState.showImageResponseStylePicker) {
        ImageResponseStyleSheet(
            templateTitle = uiState.pendingImageTemplate?.title ?: "Image Analysis",
            onDismiss = { viewModel.dismissImageResponseStylePicker() },
            onSelectStyle = { style -> viewModel.sendImageWithStyle(style) }
        )
    }

    // Grand Master picker
    if (uiState.showGrandMasterPicker) {
        GrandMasterPickerSheet(
            onDismiss = { viewModel.hideGrandMasterPicker() },
            onSelect = { grandMaster -> viewModel.requestActivateGrandMaster(grandMaster) },
            onSelectCustom = { custom -> viewModel.requestActivateCustomGrandMaster(custom) },
            onCreateNew = { viewModel.hideGrandMasterPicker(); viewModel.showCreateGrandMaster() },
            onDeleteCustom = { id -> viewModel.deleteCustomGrandMaster(id) },
            onExportCustom = { id -> viewModel.exportCustomGrandMaster(id) },
            onExportAll = { viewModel.exportAllCustomGrandMasters() },
            onImportFromUri = { uri -> viewModel.importGrandMastersFromUri(uri) },
            activeGrandMaster = uiState.activeGrandMaster,
            activeCustomGrandMaster = uiState.activeCustomGrandMaster,
            customGrandMasters = uiState.customGrandMasters
        )
    }

    // Resume or Reset dialog
    if (uiState.showResumeOrResetDialog) {
        ResumeOrResetDialog(
            title = uiState.pendingGrandMaster?.title ?: uiState.pendingCustomGrandMaster?.title ?: "Grand Master",
            onResume = { viewModel.resumeGrandMasterChat() },
            onReset = { viewModel.resetGrandMasterChat() },
            onDismiss = { viewModel.dismissResumeOrResetDialog() }
        )
    }

    // Create custom Grand Master
    if (uiState.showCreateGrandMaster) {
        CreateGrandMasterSheet(
            onDismiss = { viewModel.hideCreateGrandMaster() },
            onCreate = { icon, title, subtitle, desc, prompt, welcome ->
                viewModel.createCustomGrandMaster(icon, title, subtitle, desc, prompt, welcome)
            },
            onCreateFromJson = { json -> viewModel.createCustomGrandMasterFromJson(json) }
        )
    }

    // Chat history bottom sheet
    if (showHistory) {
        ChatHistorySheet(
            sessions = chatSessions,
            onDismiss = { showHistory = false },
            onLoadSession = { sessionId ->
                viewModel.loadChatSession(sessionId)
                showHistory = false
            },
            onDeleteSession = { sessionId -> viewModel.deleteChatSession(sessionId) },
            onDeleteAll = { viewModel.deleteAllChatSessions() }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        if (uiState.activeGrandMaster != null || uiState.activeCustomGrandMaster != null) {
                            IconButton(onClick = { viewModel.exitGrandMaster() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit Grand Master", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(modelStatus = uiState.modelStatus)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val gmActive = uiState.activeGrandMaster != null || uiState.activeCustomGrandMaster != null
                                    val gmDisplayTitle = uiState.activeGrandMaster?.let { "${it.icon} ${it.title}" }
                                        ?: uiState.activeCustomGrandMaster?.let { "${it.icon} ${it.title}" }
                                        ?: "MEDHA"
                                    Text(
                                        gmDisplayTitle,
                                        style = if (gmActive) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (uiState.appMode is AppMode.Online) AccentCyan.copy(alpha = 0.15f) else AccentGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            if (uiState.appMode is AppMode.Online) "ONLINE" else "OFFLINE",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = if (uiState.appMode is AppMode.Online) AccentCyan else AccentGreen
                                        )
                                    }
                                }
                                Text(
                                    getSubtitleText(uiState),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getStatusColor(uiState.modelStatus),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // New chat button
                        if (uiState.messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.startNewChat() }) {
                                Text("\u2795", fontSize = 16.sp)
                            }
                        }
                        // Chat history button
                        IconButton(onClick = { showHistory = true }) {
                            Text("\uD83D\uDCCB", fontSize = 18.sp)
                        }
                        // Grand Master picker button
                        if (uiState.activeGrandMaster == null && uiState.activeCustomGrandMaster == null) {
                            IconButton(onClick = { viewModel.showGrandMasterPicker() }) {
                                Text("\uD83C\uDFC6", fontSize = 20.sp)
                            }
                        }
                        if (uiState.messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearChat() }) {
                                Icon(Icons.Default.Delete, "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        if (uiState.modelStatus is ModelStatus.Error || uiState.modelStatus is ModelStatus.ModelNotFound || uiState.modelStatus is ModelStatus.PermissionRequired) {
                            IconButton(onClick = { viewModel.initializeEngine() }) {
                                Icon(Icons.Default.Refresh, "Retry", tint = StatusWarning)
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = onNavigateToAbout) {
                            Icon(Icons.Default.Info, "About", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        },
        bottomBar = {
            ChatInputBar(
                prompt = prompt,
                onPromptChange = { prompt = it },
                onSend = {
                    viewModel.sendMessage(prompt)
                    prompt = ""
                },
                onShowTemplates = { viewModel.togglePromptTemplates() },
                isEnabled = uiState.modelStatus is ModelStatus.Ready && !uiState.isGenerating,
                isGenerating = uiState.isGenerating,
                pendingImageUri = uiState.pendingImageUri,
                onRemoveImage = { viewModel.setPendingImage(null) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                WelcomeContent(
                    uiState,
                    onRetry = { viewModel.initializeEngine() },
                    onViewLogs = onNavigateToLogs,
                    onOpenSettings = onNavigateToSettings,
                    onSelectGrandMaster = { viewModel.requestActivateGrandMaster(it) }
                )
            } else {
                LazyColumn(
                    state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300))
                        ) {
                            MessageBubble(message = message, viewModel = viewModel, aiName = uiState.activeGrandMaster?.title ?: uiState.activeCustomGrandMaster?.title ?: "Medha")
                        }
                    }
                    if (uiState.isGenerating) {
                        // Show streaming response in real-time
                        if (uiState.streamingText.isNotEmpty() || uiState.streamingThinking.isNotEmpty()) {
                            item { StreamingBubble(streamingText = uiState.streamingText, thinkingText = uiState.streamingThinking, isThinking = uiState.isThinking) }
                        } else {
                            item { TypingIndicator() }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PromptTemplatesSheet(
    onDismiss: () -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    supportsImage: Boolean,
    supportsAudio: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories = TemplateCategory.entries

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Select a template to pre-fill your prompt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                categories.forEach { category ->
                    val templates = PromptTemplates.all.filter { it.category == category }
                    if (templates.isNotEmpty()) {
                        item {
                            val categoryColor = when (category) {
                                TemplateCategory.WRITING -> AccentCyan
                                TemplateCategory.ANALYSIS -> AccentGold
                                TemplateCategory.CODE -> AccentGreen
                                TemplateCategory.CREATIVE -> AccentPink
                                TemplateCategory.UTILITY -> MaterialTheme.colorScheme.primary
                                TemplateCategory.IMAGE -> AccentCyan
                                TemplateCategory.AUDIO -> AccentGold
                            }
                            Text(
                                category.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(templates) { template ->
                            val isImageTemplate = template.category == TemplateCategory.IMAGE
                            val isAudioTemplate = template.category == TemplateCategory.AUDIO
                            val isMediaTemplate = isImageTemplate || isAudioTemplate
                            val isDisabled = (isImageTemplate && !supportsImage) || (isAudioTemplate && !supportsAudio)
                            Surface(
                                onClick = { if (!isDisabled) onSelectTemplate(template) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDisabled)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(template.icon, fontSize = 22.sp, color = if (isDisabled) Color.Unspecified.copy(alpha = 0.4f) else Color.Unspecified)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                template.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isMediaTemplate) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                val badgeLabel = when {
                                                    isImageTemplate && supportsImage -> "VISION"
                                                    isAudioTemplate && supportsAudio -> "AUDIO"
                                                    isImageTemplate -> "NEEDS VISION"
                                                    else -> "NEEDS AUDIO"
                                                }
                                                val badgeOk = (isImageTemplate && supportsImage) || (isAudioTemplate && supportsAudio)
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (badgeOk) AccentCyan.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        badgeLabel,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                        color = if (badgeOk) AccentCyan else StatusWarning
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            if (isDisabled) "Use a compatible model (Gemma 4) or Online mode" else template.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDisabled) StatusWarning.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageResponseStyleSheet(
    templateTitle: String,
    onDismiss: () -> Unit,
    onSelectStyle: (ImageResponseStyle) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(
                templateTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Image attached! How would you like the response?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            ImageResponseStyle.entries.forEach { style ->
                Surface(
                    onClick = { onSelectStyle(style) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(style.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrandMasterPickerSheet(
    onDismiss: () -> Unit,
    onSelect: (GrandMaster) -> Unit,
    onSelectCustom: (CustomGrandMaster) -> Unit,
    onCreateNew: () -> Unit,
    onDeleteCustom: (String) -> Unit,
    onExportCustom: (String) -> Unit,
    onExportAll: () -> Unit,
    onImportFromUri: (Uri) -> Unit,
    activeGrandMaster: GrandMaster?,
    activeCustomGrandMaster: CustomGrandMaster?,
    customGrandMasters: List<CustomGrandMaster>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportFromUri(it) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Grand Masters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Start a specialized AI session with deep expertise",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                // Built-in Grand Masters
                item {
                    Text(
                        "Built-in",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(GrandMaster.entries.toList()) { gm ->
                    val isActive = gm == activeGrandMaster
                    GrandMasterCard(
                        icon = gm.icon,
                        title = gm.title,
                        subtitle = gm.subtitle,
                        description = gm.description,
                        isActive = isActive,
                        onClick = { if (!isActive) onSelect(gm) }
                    )
                }

                // Custom Grand Masters
                if (customGrandMasters.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Your Grand Masters",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(customGrandMasters, key = { it.id }) { custom ->
                        val isActive = custom.id == activeCustomGrandMaster?.id
                        GrandMasterCard(
                            icon = custom.icon,
                            title = custom.title,
                            subtitle = custom.subtitle,
                            description = custom.description,
                            isActive = isActive,
                            isCustom = true,
                            onClick = { if (!isActive) onSelectCustom(custom) },
                            onDelete = { onDeleteCustom(custom.id) },
                            onExport = { onExportCustom(custom.id) }
                        )
                    }
                }

                // Action buttons
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        onClick = onCreateNew,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("+", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Create Your Grand Master",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // Import button
                        Surface(
                            onClick = { importLauncher.launch("application/json") },
                            shape = RoundedCornerShape(12.dp),
                            color = AccentCyan.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("\uD83D\uDCE5", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AccentCyan)
                            }
                        }
                        // Export All button
                        if (customGrandMasters.isNotEmpty()) {
                            Surface(
                                onClick = onExportAll,
                                shape = RoundedCornerShape(12.dp),
                                color = AccentGold.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("\uD83D\uDCE4", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export All", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AccentGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrandMasterCard(
    icon: String,
    title: String,
    subtitle: String,
    description: String,
    isActive: Boolean,
    isCustom: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isActive)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isActive) BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ) else null,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StatusSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "ACTIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = StatusSuccess
                            )
                        }
                    }
                    if (isCustom) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "CUSTOM",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = AccentCyan
                            )
                        }
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            if (isCustom) {
                if (onExport != null) {
                    IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, "Export", modifier = Modifier.size(18.dp), tint = AccentCyan.copy(alpha = 0.7f))
                    }
                }
                if (onDelete != null && !isActive) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = StatusError.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeOrResetDialog(
    title: String,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Text("You have a previous chat session with this Grand Master. Would you like to continue where you left off or start fresh?")
        },
        confirmButton = {
            TextButton(onClick = onResume) {
                Text("Continue Chat", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onReset) {
                Text("Start Fresh", color = StatusWarning)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGrandMasterSheet(
    onDismiss: () -> Unit,
    onCreate: (icon: String, title: String, subtitle: String, description: String, systemPrompt: String, welcomeMessage: String) -> Unit,
    onCreateFromJson: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var useJsonMode by remember { mutableStateOf(false) }

    // Form fields
    var icon by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var welcomeMessage by remember { mutableStateOf("") }
    var jsonText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Create Grand Master",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Define your own AI expert with custom rules and personality",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Toggle between form and JSON mode
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { useJsonMode = false },
                    shape = RoundedCornerShape(8.dp),
                    color = if (!useJsonMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "Form",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (!useJsonMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    onClick = { useJsonMode = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (useJsonMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "JSON",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (useJsonMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (useJsonMode) {
                Text(
                    "Paste your Grand Master JSON configuration:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // JSON example hint
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "{\n  \"icon\": \"\uD83C\uDF1F\",\n  \"title\": \"My Expert\",\n  \"subtitle\": \"Expert in ...\",\n  \"description\": \"Short description\",\n  \"systemPrompt\": \"You are an expert in...\",\n  \"welcomeMessage\": \"Hello! I'm your...\"\n}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("Paste JSON here...") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    maxLines = 15,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    onClick = { if (jsonText.isNotBlank()) onCreateFromJson(jsonText) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (jsonText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Create from JSON",
                        modifier = Modifier.padding(14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (jsonText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                // Form mode
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Icon (emoji)") },
                    placeholder = { Text("e.g. \uD83C\uDF1F \uD83E\uDD16 \uD83C\uDFA8") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g. Fitness Coach") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle") },
                    placeholder = { Text("e.g. Personal Training Expert") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Short description of expertise") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt / Rules *") },
                    placeholder = { Text("Define how this Grand Master should behave, what rules to follow, expertise areas, tone, restrictions...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = welcomeMessage,
                    onValueChange = { welcomeMessage = it },
                    label = { Text("Welcome Message") },
                    placeholder = { Text("First message shown when user starts a session") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                val canCreate = title.isNotBlank() && systemPrompt.isNotBlank()
                Surface(
                    onClick = { if (canCreate) onCreate(icon, title, subtitle, description, systemPrompt, welcomeMessage) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (canCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Create Grand Master",
                        modifier = Modifier.padding(14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (canCreate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(modelStatus: ModelStatus) {
    val color = when (modelStatus) {
        is ModelStatus.Ready -> StatusSuccess
        is ModelStatus.Initializing, is ModelStatus.Loading, is ModelStatus.Downloading -> StatusWarning
        is ModelStatus.Error, is ModelStatus.ModelNotFound, is ModelStatus.PermissionRequired -> StatusError
        is ModelStatus.Idle -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (modelStatus is ModelStatus.Initializing) 0.3f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}

@Composable
private fun WelcomeContent(uiState: ChatState, onRetry: () -> Unit, onViewLogs: () -> Unit, onOpenSettings: () -> Unit, onSelectGrandMaster: (GrandMaster) -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MEDHA", style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
            brush = Brush.linearGradient(listOf(GradientStart, GradientMid, GradientEnd))
        ))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Neural Intelligence Engine", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(modelStatus = uiState.modelStatus)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Engine Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uiState.appMode is AppMode.Online) AccentCyan.copy(alpha = 0.15f) else AccentGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (uiState.appMode is AppMode.Online) "Online" else "Offline",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            color = if (uiState.appMode is AppMode.Online) AccentCyan else AccentGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.appMode is AppMode.Offline && uiState.selectedModel != null) {
                    Text("Model: ${uiState.selectedModel.displayName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AccentGold)
                    Text("${uiState.selectedModel.fileName}  \u2022  ${uiState.selectedModel.sizeInMb} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(6.dp))
                } else if (uiState.appMode is AppMode.Online) {
                    Text("Model: ${uiState.onlineModelName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AccentCyan)
                    val activeKey = uiState.activeKey
                    if (activeKey != null) {
                        Text(
                            "Key: ${activeKey.label} \u2022 ${activeKey.selectedModels.size} model(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(getDetailedStatusText(uiState), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                if (uiState.modelStatus is ModelStatus.ModelNotFound && uiState.appMode is AppMode.Offline) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Quick Setup", style = MaterialTheme.typography.labelLarge, color = AccentGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            SetupStep("1", "Go to Settings \u2192 Model Catalog")
                            SetupStep("2", "Download a Gemma 4 .litertlm model")
                            SetupStep("3", "Tap retry or restart the app")
                        }
                    }
                }
                if (uiState.modelStatus is ModelStatus.Error && uiState.appMode is AppMode.Online && uiState.apiKeys.isEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Online Setup", style = MaterialTheme.typography.labelLarge, color = AccentCyan, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            SetupStep("1", "Get API key from Google AI Studio")
                            SetupStep("2", "Go to Settings and enter your API key")
                            SetupStep("3", "Start chatting with Gemini!")
                        }
                    }
                }

                if (uiState.modelStatus !is ModelStatus.Ready) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (uiState.appMode is AppMode.Offline) {
                            Surface(onClick = onRetry, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                                Text("Retry", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Surface(onClick = onOpenSettings, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("Settings", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                        }
                        Surface(onClick = onViewLogs, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("Logs", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        if (uiState.modelStatus is ModelStatus.Ready) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Type a message or start a Grand Master session", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GrandMaster.entries.forEach { gm ->
                    Surface(
                        onClick = { onSelectGrandMaster(gm) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.width(140.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(gm.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                gm.title.replace("Grand Master", "GM"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                            Text(
                                gm.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStep(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(24.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(number, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}

@Composable
private fun MessageBubble(message: Message, viewModel: ChatViewModel, aiName: String = "Medha") {
    val isUser = message.user == User.Person
    val bubbleShape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val context = LocalContext.current

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeText = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(if (isUser) "You" else aiName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = bubbleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 320.dp).animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // User attached image
                message.imageUri?.let { uri ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(Uri.parse(uri)).crossfade(true).build(),
                        contentDescription = "Attached image",
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Text content
                if (message.text.isNotBlank()) {
                    Text(message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                }

                // AI generated images
                if (message.generatedImages.isNotEmpty()) {
                    if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(10.dp))
                    message.generatedImages.forEach { genImage ->
                        GeneratedImageCard(
                            image = genImage,
                            onSave = { viewModel.saveGeneratedImage(genImage) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Token stats bar — only for AI responses with stats
                if (!isUser && message.tokenCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statColor = textColor.copy(alpha = 0.4f)
                        val dotColor = textColor.copy(alpha = 0.25f)
                        Text("${message.tokenCount} tokens", fontSize = 9.sp, color = statColor)
                        Text("\u00B7", fontSize = 9.sp, color = dotColor)
                        Text("${"%.1f".format(message.tokensPerSec)} tok/s", fontSize = 9.sp, color = statColor)
                        Text("\u00B7", fontSize = 9.sp, color = dotColor)
                        val latencyLabel = if (message.latencyMs >= 1000) {
                            "${"%.1f".format(message.latencyMs / 1000f)}s"
                        } else "${message.latencyMs}ms"
                        Text(latencyLabel, fontSize = 9.sp, color = statColor)
                        if (message.timeToFirstTokenMs > 0) {
                            Text("\u00B7", fontSize = 9.sp, color = dotColor)
                            Text("TTFT ${message.timeToFirstTokenMs}ms", fontSize = 9.sp, color = statColor)
                        }
                        if (message.provider.isNotBlank()) {
                            Text("\u00B7", fontSize = 9.sp, color = dotColor)
                            val provColor = when (message.provider) {
                                "offline" -> Color(0xFF4CAF50)
                                "gemini" -> Color(0xFF4285F4)
                                else -> statColor
                            }
                            Text(message.provider.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = provColor)
                        }
                    }
                }

                // Thinking text (collapsed, expandable)
                if (!isUser && !message.thinkingText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    var showThinking by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { showThinking = !showThinking },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                if (showThinking) "\uD83E\uDDE0 Thinking \u25B2" else "\uD83E\uDDE0 Thinking \u25BC",
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                color = textColor.copy(alpha = 0.5f)
                            )
                            if (showThinking) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(message.thinkingText, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(timeText, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textColor.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun GeneratedImageCard(
    image: com.ashes.dev.works.ai.neural.brain.medha.domain.model.GeneratedImage,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(false) }

    // Decode base64 to bitmap
    val bitmap = remember(image.id) {
        try {
            val bytes = android.util.Base64.decode(image.base64Data, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    Column {
        if (bitmap != null) {
            // Image preview
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(bitmap).crossfade(true).build(),
                    contentDescription = "AI generated image",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Download button
            Surface(
                onClick = {
                    onSave()
                    saved = true
                },
                shape = RoundedCornerShape(8.dp),
                color = if (saved)
                    StatusSuccess.copy(alpha = 0.15f)
                else
                    AccentCyan.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (saved) "\u2705" else "\uD83D\uDCBE",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (saved) "Saved to Gallery" else "Save to Gallery",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (saved) StatusSuccess else AccentCyan
                    )
                }
            }
        } else {
            // Fallback if decode fails
            Text(
                "Failed to load generated image",
                style = MaterialTheme.typography.bodySmall,
                color = StatusError
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(500, delayMillis = index * 150), repeatMode = RepeatMode.Reverse), label = "dot$index")
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentCyan.copy(alpha = alpha)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHistorySheet(
    sessions: List<ChatSessionEntity>,
    onDismiss: () -> Unit,
    onLoadSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chat History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                if (sessions.isNotEmpty()) {
                    TextButton(onClick = onDeleteAll) {
                        Text("Clear All", color = StatusError, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${sessions.size} conversation(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83D\uDCAC", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No conversations yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        Surface(
                            onClick = { onLoadSession(session.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("\uD83D\uDCAC", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        session.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val statColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                        Text(timeFormat.format(Date(session.updatedAt)), fontSize = 10.sp, color = statColor)
                                        Text("\u00B7", fontSize = 10.sp, color = statColor)
                                        Text("${session.messageCount} msgs", fontSize = 10.sp, color = statColor)
                                        if (session.modelUsed != null) {
                                            Text("\u00B7", fontSize = 10.sp, color = statColor)
                                            Text(session.modelUsed, fontSize = 10.sp, color = statColor, maxLines = 1)
                                        }
                                    }
                                }
                                IconButton(onClick = { onDeleteSession(session.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(streamingText: String, thinkingText: String, isThinking: Boolean) {
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Medha", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 320.dp).animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Thinking indicator
                if (isThinking && thinkingText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("\uD83E\uDDE0", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                thinkingText.takeLast(200),
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.5f),
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (streamingText.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                }

                // Streaming response text
                if (streamingText.isNotEmpty()) {
                    Text(streamingText, style = MaterialTheme.typography.bodyLarge, color = textColor)
                } else if (!isThinking) {
                    // Show typing dots if no text yet
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Generating...", fontSize = 13.sp, color = textColor.copy(alpha = 0.5f))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = AccentCyan,
                            strokeWidth = 1.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onShowTemplates: () -> Unit,
    isEnabled: Boolean,
    isGenerating: Boolean,
    pendingImageUri: String?,
    onRemoveImage: () -> Unit
) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
        ) {
            // Image preview bar
            AnimatedVisibility(visible = pendingImageUri != null, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
                pendingImageUri?.let { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(Uri.parse(uri)).crossfade(true).build(),
                            contentDescription = "Selected image",
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Image attached", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AccentCyan)
                            Text(
                                "Will be sent for analysis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = onRemoveImage, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Templates button
                IconButton(
                    onClick = onShowTemplates,
                    enabled = isEnabled,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Menu, "Quick actions", modifier = Modifier.size(22.dp), tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }

                TextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                isGenerating -> "Generating..."
                                !isEnabled -> "Waiting for engine..."
                                else -> "Ask Medha anything..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    enabled = isEnabled,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onSend,
                    enabled = isEnabled && (prompt.isNotBlank() || pendingImageUri != null),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isEnabled && (prompt.isNotBlank() || pendingImageUri != null)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isEnabled && (prompt.isNotBlank() || pendingImageUri != null)) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun getSubtitleText(uiState: ChatState): String {
    val modelName = when (uiState.appMode) {
        is AppMode.Online -> {
            val keyLabel = uiState.activeKey?.label
            if (keyLabel != null) "${uiState.onlineModelName} ($keyLabel)" else uiState.onlineModelName
        }
        is AppMode.Offline -> uiState.selectedModel?.displayName ?: "No model"
    }
    val statusText = when (uiState.modelStatus) {
        is ModelStatus.Idle -> "Idle"
        is ModelStatus.Initializing -> "Loading..."
        is ModelStatus.Loading -> "Loading ${(uiState.modelStatus as ModelStatus.Loading).detail}..."
        is ModelStatus.Ready -> "Ready"
        is ModelStatus.Error -> "Error"
        is ModelStatus.ModelNotFound -> "Model not found"
        is ModelStatus.PermissionRequired -> "Permission needed"
        is ModelStatus.Downloading -> "Downloading..."
    }
    return "$modelName \u2022 $statusText"
}

@Composable
private fun getStatusColor(status: ModelStatus): Color = when (status) {
    is ModelStatus.Ready -> StatusSuccess
    is ModelStatus.Initializing, is ModelStatus.Loading, is ModelStatus.Downloading -> StatusWarning
    is ModelStatus.Error, is ModelStatus.ModelNotFound, is ModelStatus.PermissionRequired -> StatusError
    is ModelStatus.Idle -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
}

private fun getDetailedStatusText(uiState: ChatState): String = when (uiState.modelStatus) {
    is ModelStatus.Idle -> "The AI engine has not started yet."
    is ModelStatus.Initializing -> "Loading model into memory..."
    is ModelStatus.Loading -> "Loading: ${(uiState.modelStatus as ModelStatus.Loading).detail}"
    is ModelStatus.Ready -> if (uiState.appMode is AppMode.Online) "Connected to Gemini API. Start chatting below." else "Offline engine ready (LiteRT LM). Start chatting below."
    is ModelStatus.Error -> "Error: ${uiState.modelStatus.message}"
    is ModelStatus.ModelNotFound -> "No models found. Download one from Settings \u2192 Model Catalog."
    is ModelStatus.PermissionRequired -> "Cannot read model file."
    is ModelStatus.Downloading -> "Downloading model..."
}
