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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.AppMode
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.Message
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelStatus
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplate
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.PromptTemplates
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
    var prompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.setPendingImage(it.toString()) }
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
                if (template.category == TemplateCategory.IMAGE) {
                    // Image template: store template, open picker → after pick, style picker shows
                    viewModel.setPendingImageTemplate(template)
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } else if (template.requiresInput) {
                    prompt = template.promptPrefix
                } else {
                    viewModel.sendMessage(template.promptPrefix)
                }
            },
            isOnlineMode = uiState.appMode is AppMode.Online
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(modelStatus = uiState.modelStatus)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "MEDHA",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
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
                onAttachImage = {
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onShowTemplates = { viewModel.togglePromptTemplates() },
                isEnabled = uiState.modelStatus is ModelStatus.Ready && !uiState.isGenerating,
                isGenerating = uiState.isGenerating,
                appMode = uiState.appMode,
                pendingImageUri = uiState.pendingImageUri,
                onRemoveImage = { viewModel.setPendingImage(null) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                WelcomeContent(uiState, onRetry = { viewModel.initializeEngine() }, onViewLogs = onNavigateToLogs, onOpenSettings = onNavigateToSettings)
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
                            MessageBubble(message = message, viewModel = viewModel)
                        }
                    }
                    if (uiState.isGenerating) {
                        item { TypingIndicator() }
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
    isOnlineMode: Boolean
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
                            val isDisabled = isImageTemplate && !isOnlineMode
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
                                            if (isImageTemplate) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isOnlineMode) AccentCyan.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        if (isOnlineMode) "ONLINE" else "NEEDS ONLINE",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                        color = if (isOnlineMode) AccentCyan else StatusWarning
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            if (isDisabled) "Switch to Online mode in Settings" else template.description,
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
private fun WelcomeContent(uiState: ChatState, onRetry: () -> Unit, onViewLogs: () -> Unit, onOpenSettings: () -> Unit) {
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
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(getDetailedStatusText(uiState), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                if (uiState.modelStatus is ModelStatus.ModelNotFound && uiState.appMode is AppMode.Offline) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Quick Setup", style = MaterialTheme.typography.labelLarge, color = AccentGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            SetupStep("1", "Download a Gemma .bin model file")
                            SetupStep("2", "Place it in your Downloads folder")
                            SetupStep("3", "Tap retry or go to Settings")
                        }
                    }
                }
                if (uiState.modelStatus is ModelStatus.Error && uiState.appMode is AppMode.Online && uiState.apiKey.isBlank()) {
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
            Text("Type a message or tap the menu for quick actions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
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
private fun MessageBubble(message: Message, viewModel: ChatViewModel) {
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
        Text(if (isUser) "You" else "Medha", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))

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

@Composable
private fun ChatInputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    onShowTemplates: () -> Unit,
    isEnabled: Boolean,
    isGenerating: Boolean,
    appMode: AppMode,
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
                                if (appMode is AppMode.Online) "Will be sent for analysis" else "Image analysis requires Online mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (appMode is AppMode.Online) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else StatusError.copy(alpha = 0.7f)
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

                // Image attach button
                if (appMode is AppMode.Online) {
                    IconButton(
                        onClick = onAttachImage,
                        enabled = isEnabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("\uD83D\uDCF7", fontSize = 18.sp)
                    }
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
                                appMode is AppMode.Online -> "Ask Gemini anything..."
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
        is AppMode.Online -> uiState.onlineModelName
        is AppMode.Offline -> uiState.selectedModel?.displayName ?: "No model"
    }
    val statusText = when (uiState.modelStatus) {
        is ModelStatus.Idle -> "Idle"
        is ModelStatus.Initializing -> "Loading..."
        is ModelStatus.Loading -> "Loading ${(uiState.modelStatus as ModelStatus.Loading).detail}"
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
    is ModelStatus.Loading -> (uiState.modelStatus as ModelStatus.Loading).let { "Loading model... ${(it.progress * 100).toInt()}% ${it.detail}" }
    is ModelStatus.Ready -> if (uiState.appMode is AppMode.Online) "Connected to Gemini API. Start chatting below." else "Offline engine ready (LiteRT LM). Start chatting below."
    is ModelStatus.Error -> "Error: ${uiState.modelStatus.message}"
    is ModelStatus.ModelNotFound -> "No model found. Download one from Settings → Model Catalog."
    is ModelStatus.PermissionRequired -> "Grant storage permission in device settings."
    is ModelStatus.Downloading -> "Downloading model..."
}
