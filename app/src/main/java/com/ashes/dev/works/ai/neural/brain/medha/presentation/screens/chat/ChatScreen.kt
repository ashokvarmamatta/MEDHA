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
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import com.ashes.dev.works.ai.neural.brain.medha.ui.icons.MedhaIcons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.ashes.dev.works.ai.neural.brain.medha.presentation.components.MarkdownText
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SmallFloatingActionButton
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ChatState
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
import com.ashes.dev.works.ai.neural.brain.medha.data.ModelCatalog
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import com.ashes.dev.works.ai.neural.brain.medha.data.local.ChatSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Scroll offset meaning "as far into this item as it goes". LazyColumn clamps it to the real
 * end of the content, which is how we land on the newest token instead of the item's first
 * line. Deliberately not Int.MAX_VALUE — that overflows internal offset arithmetic.
 */
private const val SCROLL_TO_END_OFFSET = 1_000_000

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
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showContextSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Back press handling: don't exit straight from an active chat. The first press
    // clears the conversation back to the empty home state; only a second press
    // (already on the empty home screen) exits the app.
    BackHandler(enabled = uiState.messages.isNotEmpty()) {
        viewModel.startNewChat()
    }

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

    val scope = rememberCoroutineScope()
    val screenContext = LocalContext.current

    // ── Follow-the-stream scrolling ────────────────────────────────────────────
    //
    // Scrolling must stay under the user's control. The previous version decided
    // "am I near the bottom?" from the last VISIBLE ITEM INDEX and then called
    // scrollToItem(lastIndex) — which fails twice over on a streaming reply:
    //
    //   1. A long answer is ONE list item, so the last visible index is that item no
    //      matter where you are inside it. "Near bottom" was therefore always true,
    //      even when the user had scrolled up to read.
    //   2. scrollToItem(index) lands on that item's FIRST line, so every token yanked
    //      the view back to the start of the response.
    //
    // Both are fixed by measuring in pixels rather than item indices: canScrollForward
    // is false only when genuinely at the end of the content, and scrolling to a huge
    // offset within the last item clamps to the true bottom instead of its top.

    /** True while the view is pinned to the bottom; the stream is only followed then. */
    var followStream by remember { mutableStateOf(true) }

    // The user's own gestures decide whether to keep following.
    //
    // Following stops the INSTANT a touch starts, not when the gesture settles. Waiting for
    // it to settle left followStream true for the whole drag, so every arriving token called
    // scrollToItem and fought the finger — the list shoved itself back down while the user
    // was trying to read upwards.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            followStream = if (scrolling) false else !listState.canScrollForward
        }
    }

    LaunchedEffect(
        uiState.messages.size,
        uiState.streamingText.length,
        uiState.streamingThinking.length,
        uiState.isGenerating
    ) {
        if (!followStream) return@LaunchedEffect
        val total = uiState.messages.size + if (uiState.isGenerating) 1 else 0
        if (total == 0) return@LaunchedEffect
        // Large offset = "as far into this item as it goes"; LazyColumn clamps it to the
        // real end, which is the newest token rather than the first line.
        listState.scrollToItem(total - 1, SCROLL_TO_END_OFFSET)
    }

    // Sending a message returns to the bottom — that is an explicit request to see the
    // answer. Keyed on the user's OWN turn only: keying on messages.size alone also fired
    // when the finished reply was appended, dragging the user down at the end of a response
    // they had deliberately scrolled up to read.
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.lastOrNull()?.user is User.Person) followStream = true
    }

    // Surface download errors (storage full, incomplete, etc.) as a toast.
    LaunchedEffect(uiState.downloadError) {
        uiState.downloadError?.let { msg ->
            Toast.makeText(screenContext, msg, Toast.LENGTH_LONG).show()
            viewModel.clearDownloadError()
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

    // Model configuration dialog
    if (uiState.showConfigDialog) {
        ModelConfigDialog(
            currentTopK = viewModel.topK,
            currentTopP = viewModel.topP,
            currentTemperature = viewModel.temperature,
            currentMaxTokens = viewModel.maxTokens,
            currentEnableThinking = viewModel.enableThinking,
            currentLanguage = viewModel.outputLanguage,
            currentPreferGpu = uiState.preferGpu,
            deviceContextCap = uiState.deviceContextCap,
            selectedModel = uiState.selectedModel,
            onDismiss = { viewModel.hideConfigDialog() },
            onApply = { topK, topP, temp, maxTok, gpu, thinking, language ->
                viewModel.applyConfig(topK, topP, temp, maxTok, gpu, thinking, language)
            }
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

    // Context window breakdown sheet
    if (showContextSheet) {
        ContextWindowSheet(uiState = uiState, onDismiss = { showContextSheet = false })
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
                            Column(modifier = Modifier.clickable { showContextSheet = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "MEDHA",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "ON-DEVICE",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = AccentGreen
                                        )
                                    }
                                    // Context-usage % — tap (whole title) opens the full breakdown
                                    val ctxReady = uiState.modelStatus is ModelStatus.Ready && uiState.offlineContextLength > 0
                                    if (ctxReady) {
                                        // Recompute only when the conversation actually changes.
                                        // Without the remember this walks + tokenises every
                                        // message on each streamed token, since streamingText
                                        // recomposes the whole top bar.
                                        val ctxPct = remember(
                                            uiState.messages,
                                            uiState.offlineContextLength
                                        ) { computeContextUsage(uiState).percent }
                                        val pctColor = if (ctxPct >= 85) StatusError else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = pctColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                "$ctxPct%",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = pctColor
                                            )
                                        }
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
                        // Primary: New Chat (only when messages exist)
                        if (uiState.messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.startNewChat() }) {
                                Icon(Icons.Default.Add, "New chat", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Overflow menu for everything else
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("\uD83D\uDCCB  Chat History") },
                                    onClick = { showOverflowMenu = false; showHistory = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("\uD83D\uDCCA  Context window") },
                                    onClick = { showOverflowMenu = false; showContextSheet = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("\u2699\uFE0F  Configurations") },
                                    onClick = { showOverflowMenu = false; viewModel.showConfigDialog() }
                                )
                                if (uiState.messages.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("\uD83D\uDDD1\uFE0F  Clear Chat") },
                                        onClick = { showOverflowMenu = false; viewModel.clearChat() }
                                    )
                                }
                                if (uiState.modelStatus is ModelStatus.Error || uiState.modelStatus is ModelStatus.ModelNotFound) {
                                    DropdownMenuItem(
                                        text = { Text("\uD83D\uDD04  Retry Engine") },
                                        onClick = { showOverflowMenu = false; viewModel.initializeEngine() }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("\u2699\uFE0F  Settings") },
                                    onClick = { showOverflowMenu = false; onNavigateToSettings() }
                                )
                                DropdownMenuItem(
                                    text = { Text("\u2139\uFE0F  About") },
                                    onClick = { showOverflowMenu = false; onNavigateToAbout() }
                                )
                            }
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
                onStop = { viewModel.stopGeneration() },
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
                    onOpenSettings = onNavigateToSettings
                )
            } else {
                LazyColumn(
                    state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(uiState.messages, key = { _, m -> m.id }) { index, message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300))
                        ) {
                            MessageBubble(
                                message = message,
                                viewModel = viewModel,
                                aiName = "Medha",
                                isLast = index == uiState.messages.lastIndex && !uiState.isGenerating
                            )
                        }
                    }
                    if (uiState.isGenerating) {
                        // Show streaming response in real-time
                        if (uiState.streamingText.isNotEmpty() || uiState.streamingThinking.isNotEmpty()) {
                            item {
                                StreamingBubble(
                                    streamingText = uiState.streamingText,
                                    thinkingText = uiState.streamingThinking,
                                    isThinking = uiState.isThinking,
                                    tokenCount = uiState.streamingTokenCount,
                                    tokensPerSec = uiState.streamingTokensPerSec
                                )
                            }
                        } else {
                            item { TypingIndicator() }
                        }
                    }
                }

                // Scroll-to-bottom button. Index-based detection missed the common case —
                // scrolled up INSIDE one long streaming answer, where the last visible item
                // is still the last item — so this asks whether there is any content left
                // below instead.
                val showScrollDown by remember {
                    derivedStateOf {
                        listState.layoutInfo.totalItemsCount > 0 && listState.canScrollForward
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollDown,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            // Jump to the newest token and resume following the stream.
                            followStream = true
                            scope.launch {
                                listState.animateScrollToItem(
                                    (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0),
                                    SCROLL_TO_END_OFFSET
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Scroll to bottom")
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
                        color = AccentGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "On-device",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.selectedModel != null) {
                    Text("Model: ${uiState.selectedModel.displayName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AccentGold)
                    Text("${uiState.selectedModel.fileName}  \u2022  ${uiState.selectedModel.sizeInMb} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(getDetailedStatusText(uiState), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                if (uiState.modelStatus is ModelStatus.ModelNotFound) {
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
                if (uiState.modelStatus !is ModelStatus.Ready) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(onClick = onRetry, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                            Text("Retry", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
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
            Text(
                "Everything runs on this device. Type a message to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
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
private fun MessageBubble(message: Message, viewModel: ChatViewModel, aiName: String = "Medha", isLast: Boolean = false) {
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

                // Text content — assistant replies render as markdown (code blocks, lists,
                // bold, etc.); the user's own text is plain but selectable.
                if (message.text.isNotBlank()) {
                    if (isUser) {
                        SelectionContainer {
                            Text(message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                        }
                    } else {
                        MarkdownText(
                            markdown = message.text,
                            color = textColor,
                            baseStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
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
                            val provColor = if (message.provider == "offline") Color(0xFF4CAF50) else statColor
                            Text(message.provider.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = provColor)
                        }
                    }
                }

                // Thinking text (collapsed, expandable)
                if (!isUser && !message.thinkingText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    var showThinking by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { showThinking = !showThinking },
                        shape = RoundedCornerShape(10.dp),
                        color = AccentGold.copy(alpha = 0.08f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("\uD83E\uDDE0", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Thinking",
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    color = AccentGold.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    if (showThinking) "\u25B2" else "\u25BC",
                                    fontSize = 10.sp, color = textColor.copy(alpha = 0.4f)
                                )
                            }
                            if (showThinking) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    message.thinkingText,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Per-message actions — assistant messages get Copy (+ Regenerate on the last one)
                if (!isUser && message.text.isNotBlank()) {
                    val haptics = LocalHapticFeedback.current
                    val clipboard = LocalClipboardManager.current
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboard.setText(AnnotatedString(message.text))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(MedhaIcons.ContentCopy, "Copy message", modifier = Modifier.size(16.dp), tint = textColor.copy(alpha = 0.6f))
                        }
                        if (isLast) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.regenerateLastResponse()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, "Regenerate", modifier = Modifier.size(16.dp), tint = textColor.copy(alpha = 0.6f))
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

@Composable
private fun ModelConfigDialog(
    currentTopK: Int,
    currentTopP: Double,
    currentTemperature: Double,
    currentMaxTokens: Int,
    currentEnableThinking: Boolean,
    currentLanguage: String,
    currentPreferGpu: Boolean,
    deviceContextCap: Int,
    selectedModel: ModelInfo?,
    onDismiss: () -> Unit,
    onApply: (topK: Int, topP: Double, temperature: Double, maxTokens: Int, useGpu: Boolean, thinking: Boolean, language: String) -> Unit
) {
    val catalogModel = selectedModel?.let { ModelCatalog.findByFileName(it.fileName) }
    val supportsGpu = catalogModel?.supportsGpu ?: false
    val supportsThinking = catalogModel?.supportsThinking ?: false

    var maxTokens by remember { mutableStateOf(currentMaxTokens.toFloat()) }
    var topK by remember { mutableStateOf(currentTopK.toFloat()) }
    var topP by remember { mutableStateOf(currentTopP.toFloat()) }
    var temperature by remember { mutableStateOf(currentTemperature.toFloat()) }
    // Reflects the real, persisted setting. It used to be hardcoded false, so the dialog always
    // opened on "CPU" no matter what — and the value went nowhere when applied.
    var useGpu by remember { mutableStateOf(currentPreferGpu && supportsGpu) }
    var enableThinking by remember { mutableStateOf(currentEnableThinking) }
    var outputLanguage by remember { mutableStateOf(currentLanguage) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Configurations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Max tokens
                Text("Max tokens", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${maxTokens.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                    Slider(
                        value = maxTokens,
                        onValueChange = { maxTokens = it },
                        valueRange = 256f..8192f,
                        steps = 15,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${maxTokens.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                }
                // The device budget is a hard ceiling, not a suggestion — asking for more than
                // the KV cache fits in RAM is what got the app killed. Say so here rather than
                // accepting a number and quietly loading a smaller one.
                if (deviceContextCap in 1 until maxTokens.toInt()) {
                    Text(
                        "This device can allocate $deviceContextCap tokens right now, so that is " +
                            "what will load. Close some apps and reload to raise it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusWarning
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TopK
                Text("TopK", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${topK.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                    Slider(
                        value = topK,
                        onValueChange = { topK = it },
                        valueRange = 1f..128f,
                        steps = 126,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${topK.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TopP
                Text("TopP", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${"%.2f".format(topP)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                    Slider(
                        value = topP,
                        onValueChange = { topP = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${"%.2f".format(topP)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Temperature
                Text("Temperature", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${"%.2f".format(temperature)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0f..2f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${"%.2f".format(temperature)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accelerator
                Text("Accelerator", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = useGpu,
                        onClick = { if (supportsGpu) useGpu = true },
                        label = { Text("GPU") },
                        enabled = supportsGpu,
                        leadingIcon = if (useGpu) { { Text("\u2713", fontSize = 14.sp) } } else null
                    )
                    FilterChip(
                        selected = !useGpu,
                        onClick = { useGpu = false },
                        label = { Text("CPU") },
                        leadingIcon = if (!useGpu) { { Text("\u2713", fontSize = 14.sp) } } else null
                    )
                }
                if (!supportsGpu) {
                    Text("GPU not available for this model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enable thinking
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable thinking", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = enableThinking,
                        onCheckedChange = { if (supportsThinking) enableThinking = it },
                        enabled = supportsThinking
                    )
                }
                if (!supportsThinking) {
                    Text("Thinking not available for this model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Output language picker
                Text("Output language", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    Surface(
                        onClick = { showLanguageMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("\uD83C\uDF10", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(outputLanguage, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                            Text("\u25BC", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        SUPPORTED_LANGUAGES.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    outputLanguage = lang
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }
                Text(
                    if (outputLanguage == "Auto") "AI replies in the input language"
                    else "AI will reply ONLY in $outputLanguage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(topK.toInt(), topP.toDouble(), temperature.toDouble(), maxTokens.toInt(), useGpu, enableThinking, outputLanguage)
            }) { Text("OK") }
        }
    )
}

private val SUPPORTED_LANGUAGES = listOf(
    "Auto",
    "English", "Spanish", "French", "German", "Italian", "Portuguese",
    "Russian", "Chinese (Simplified)", "Chinese (Traditional)", "Japanese", "Korean",
    "Arabic", "Hindi", "Bengali", "Tamil", "Telugu", "Marathi", "Gujarati", "Punjabi", "Urdu",
    "Turkish", "Vietnamese", "Thai", "Indonesian", "Malay", "Filipino",
    "Dutch", "Polish", "Swedish", "Norwegian", "Danish", "Finnish",
    "Greek", "Hebrew", "Czech", "Romanian", "Hungarian", "Ukrainian", "Persian (Farsi)",
    "Swahili", "Catalan", "Bulgarian", "Slovak", "Croatian", "Serbian"
)

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
                                IconButton(onClick = { onDeleteSession(session.id) }, modifier = Modifier.size(48.dp)) {
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
private fun StreamingBubble(
    streamingText: String,
    thinkingText: String,
    isThinking: Boolean,
    tokenCount: Int,
    tokensPerSec: Float
) {
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Medha", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))

        // Thinking bubble (separate, full width)
        if (isThinking && thinkingText.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(0.9f).animateContentSize()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83E\uDDE0", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thinking...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentGold.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        thinkingText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = textColor.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Response bubble
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(0.85f).animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (streamingText.isNotEmpty()) {
                    MarkdownText(markdown = streamingText, color = textColor, baseStyle = MaterialTheme.typography.bodyLarge)
                    // Live decode rate while the answer is still arriving. The finished message
                    // shows the same figures; there is no reason to make the user wait for them.
                    if (tokenCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(10.dp), color = AccentCyan, strokeWidth = 1.dp)
                            Text(
                                "$tokenCount tokens · ${"%.1f".format(tokensPerSec)} tok/s",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentCyan, strokeWidth = 1.5.dp)
                        Text(if (isThinking) "Thinking..." else "Generating...", fontSize = 13.sp, color = textColor.copy(alpha = 0.5f))
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
    onStop: () -> Unit,
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
                        IconButton(onClick = onRemoveImage, modifier = Modifier.size(48.dp)) {
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
                if (isGenerating) {
                    // While a response is streaming, the action button stops it.
                    IconButton(
                        onClick = onStop,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                    ) {
                        Icon(MedhaIcons.Stop, "Stop", modifier = Modifier.size(22.dp))
                    }
                } else {
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
}

private fun getSubtitleText(uiState: ChatState): String {
    val modelName = uiState.selectedModel?.displayName ?: "No model"
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
    val ctxText = if (uiState.modelStatus is ModelStatus.Ready && uiState.offlineContextLength > 0) {
        " \u2022 ${formatContextLength(uiState.offlineContextLength)} ctx"
    } else ""
    // Show MTP (speculative decoding) badge when the engine has it active.
    val mtpText = if (uiState.modelStatus is ModelStatus.Ready && uiState.offlineMtpActive) " \u2022 MTP" else ""
    return "$modelName \u2022 $statusText$ctxText$mtpText"
}

/** Format a token count as a short context label: 32768 -> "32K", 4096 -> "4K", 900 -> "900". */
private fun formatContextLength(tokens: Int): String =
    if (tokens >= 1024) "${tokens / 1024}K" else "$tokens"

/** Rough token estimate when the model hasn't given an exact count (~4 chars per token). */
private fun estimateTokens(text: String): Int =
    if (text.isBlank()) 0 else (text.length / 4).coerceAtLeast(1)

private class ContextUsage(val total: Int, val systemTokens: Int, val messageTokens: Int) {
    val used: Int get() = (systemTokens + messageTokens).coerceAtMost(total)
    val free: Int get() = (total - used).coerceAtLeast(0)
    val percent: Int get() = if (total > 0) (used * 100 / total).coerceIn(0, 100) else 0
}

/** Estimate how much of the context window is in use, against the engine's real
 *  window. Assistant replies use exact counts; user text is estimated. */
private fun computeContextUsage(uiState: ChatState): ContextUsage {
    val total = uiState.offlineContextLength.coerceAtLeast(1)
    val systemTokens = 0
    val messageTokens = uiState.messages.sumOf { m ->
        val body = if (m.tokenCount > 0) m.tokenCount else estimateTokens(m.text)
        body + estimateTokens(m.thinkingText ?: "")
    }
    return ContextUsage(total, systemTokens, messageTokens)
}

private fun fmtTokens(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1000.0)
    else -> "$n"
}

/**
 * Context-window usage breakdown, styled after the Claude desktop panel: a segmented
 * bar plus a legend of Messages / Free space with token counts and %, measured
 * against the on-device engine's real context window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContextWindowSheet(uiState: ChatState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val usage = computeContextUsage(uiState)
    val total = usage.total
    val systemTokens = usage.systemTokens
    val messageTokens = usage.messageTokens
    val used = usage.used
    val free = usage.free
    val pct = usage.percent.toFloat()

    val msgColor = AccentCyan
    val sysColor = MaterialTheme.colorScheme.primary
    val freeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Context window",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${fmtTokens(used)} / ${fmtTokens(total)} (${pct.toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                uiState.selectedModel?.displayName ?: "On-device model",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Segmented usage bar
            Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                if (systemTokens > 0) Box(Modifier.weight(systemTokens.toFloat()).fillMaxHeight().background(sysColor))
                if (messageTokens > 0) Box(Modifier.weight(messageTokens.toFloat()).fillMaxHeight().background(msgColor))
                Box(Modifier.weight(free.toFloat().coerceAtLeast(0.001f)).fillMaxHeight().background(freeColor))
            }
            Spacer(modifier = Modifier.height(16.dp))

            ContextLegendRow(msgColor, "Messages", messageTokens, total)
            if (systemTokens > 0) ContextLegendRow(sysColor, "System prompt", systemTokens, total)
            ContextLegendRow(freeColor, "Free space", free, total)

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Token counts are estimated (~4 chars/token); assistant replies use the model's exact count.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun ContextLegendRow(color: Color, label: String, tokens: Int, total: Int) {
    val pct = if (total > 0) tokens * 100f / total else 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
        Text(fmtTokens(tokens), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(14.dp))
        Text("${pct.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
    }
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
    is ModelStatus.Ready -> "On-device engine ready (LiteRT LM). Start chatting below."
    is ModelStatus.Error -> "Error: ${uiState.modelStatus.message}"
    is ModelStatus.ModelNotFound -> "No models found. Download one from Settings \u2192 Model Catalog."
    is ModelStatus.PermissionRequired -> "Cannot read model file."
    is ModelStatus.Downloading -> "Downloading model..."
}
