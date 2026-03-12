package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.about

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatViewModel
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentCyan
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGold
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGreen
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientEnd
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientMid
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        Text("About MEDHA", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MEDHA",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    brush = Brush.linearGradient(colors = listOf(GradientStart, GradientMid, GradientEnd))
                )
            )
            Text("Neural Intelligence Engine", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Text("Version ${ChatViewModel.APP_VERSION}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(24.dp))

            InfoSection("What is MEDHA?", Icons.Default.Info, AccentCyan) {
                Text(
                    text = "MEDHA (Mobile Edge Device Hybrid AI) is an offline-first AI chat application. " +
                            "It runs Gemma models on-device via MediaPipe for fully private conversations. " +
                            "For enhanced capabilities, connect to online APIs like Google Gemini with multi-key failover.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Key Features", Icons.Default.Star, AccentGold) {
                FeatureItem("Offline First", "Run Gemma 2B locally with MediaPipe - no internet needed")
                FeatureItem("Online Mode", "Connect to Gemini API with multi-key failover")
                FeatureItem("Grand Masters", "Specialized AI personas - Chess, Health, Code, Career + create your own")
                FeatureItem("Chat Persistence", "Grand Master chats are saved - resume or start fresh anytime")
                FeatureItem("Custom Grand Masters", "Create your own AI expert via form or JSON configuration")
                FeatureItem("Image Analysis", "Upload images for AI-powered analysis, OCR, and descriptions")
                FeatureItem("22 Prompt Templates", "One-tap templates across 6 categories")
                FeatureItem("Private & Secure", "Offline mode keeps all data on your device")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Offline Setup", Icons.Default.Build, AccentGreen) {
                GuideStep("1", "Download Model", "Get a Gemma .bin model file (see Settings for download links).")
                GuideStep("2", "Place in Downloads", "Move the model file to your device's Downloads folder.")
                GuideStep("3", "Grant Permission", "Allow 'All Files Access' so the app can read the model.")
                GuideStep("4", "Select Model", "Go to Settings > Offline Models to pick your model.")
                GuideStep("5", "Start Chatting", "The engine loads automatically. Type your first message!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Online Setup", Icons.Default.Build, AccentCyan) {
                GuideStep("1", "Get API Key", "Go to Settings for a list of free & paid API key providers.")
                GuideStep("2", "Switch Mode", "Go to Settings and switch to Online Mode.")
                GuideStep("3", "Add API Key", "Paste your key, add a label, and test it.")
                GuideStep("4", "Start Chatting", "Select a model and start chatting!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Tips for Best Results", Icons.Default.CheckCircle, AccentGreen) {
                TipItem("Keep prompts clear and concise for better responses")
                TipItem("Online mode provides more accurate and detailed responses")
                TipItem("Offline mode works best with English and simple questions")
                TipItem("First offline load takes 30-60 seconds depending on device")
                TipItem("Use Grand Masters for focused, expert-level conversations")
                TipItem("Go to Settings for model download links and API key providers")
                TipItem("Use the Logs screen to diagnose any issues")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Limitations", Icons.Default.Warning, AccentGold) {
                TipItem("Offline: Gemma 2B is compact - may produce inaccurate responses")
                TipItem("Offline: Complex reasoning and math may not be reliable")
                TipItem("Online: Requires active internet connection")
                TipItem("Online: API keys are stored locally (not encrypted)")
                TipItem("Currently supports Gemini API format - other providers coming soon")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Privacy & Data", Icons.Default.Lock, AccentCyan) {
                Text(
                    text = "In Offline mode, MEDHA processes everything on your device. No data leaves your phone. " +
                            "In Online mode, your prompts are sent to the API provider's servers for processing. " +
                            "API keys and Grand Master chat history are stored locally on your device via DataStore. " +
                            "Custom Grand Master configurations are also stored locally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Built with Jetpack Compose & MediaPipe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), textAlign = TextAlign.Center)
            Text("Gemma 2B by Google \u2022 Gemini API by Google", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoSection(title: String, icon: ImageVector, iconColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text("\u2022", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun GuideStep(number: String, title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(28.dp)) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun TipItem(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text("\u2022", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}
