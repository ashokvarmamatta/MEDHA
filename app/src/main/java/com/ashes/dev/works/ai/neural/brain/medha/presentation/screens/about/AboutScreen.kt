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
                    text = "MEDHA (Machine-Enhanced Digital Human Assistant) is a dual-mode AI chat application. " +
                            "It supports both Offline mode using Google's Gemma model via MediaPipe (on-device, fully private) " +
                            "and Online mode using the Google Gemini API for more powerful responses when connected to the internet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Key Features", Icons.Default.Star, AccentGold) {
                FeatureItem("Dual Mode", "Switch between Offline (on-device) and Online (Gemini API)")
                FeatureItem("Offline AI", "Run Gemma 2B locally with MediaPipe - no internet needed")
                FeatureItem("Online AI", "Access Google Gemini for powerful cloud-based responses")
                FeatureItem("Model Selection", "Choose from multiple offline models in your Downloads folder")
                FeatureItem("Private & Secure", "Offline mode keeps all data on your device")
                FeatureItem("Detailed Logging", "Monitor engine status, errors, and performance")
                FeatureItem("Premium UI", "Rich, animated chat experience with dark/light theme")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Offline Setup", Icons.Default.Build, AccentGreen) {
                GuideStep("1", "Download Model", "Get a Gemma .bin model file from Kaggle or MediaPipe repository.")
                GuideStep("2", "Place in Downloads", "Move the model file to your device's Downloads folder.")
                GuideStep("3", "Grant Permission", "Allow 'All Files Access' so the app can read the model.")
                GuideStep("4", "Select Model", "Go to Settings > Offline Models to pick your model.")
                GuideStep("5", "Start Chatting", "The engine loads automatically. Type your first message!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Online Setup", Icons.Default.Build, AccentCyan) {
                GuideStep("1", "Get API Key", "Visit aistudio.google.com and create a free Gemini API key.")
                GuideStep("2", "Open Settings", "Go to Settings and switch to Online Mode.")
                GuideStep("3", "Enter API Key", "Paste your API key and tap the checkmark to save.")
                GuideStep("4", "Start Chatting", "You're connected to Gemini - ask anything!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Tips for Best Results", Icons.Default.CheckCircle, AccentGreen) {
                TipItem("Keep prompts clear and concise for better responses")
                TipItem("Online mode provides more accurate and detailed responses")
                TipItem("Offline mode works best with English and simple questions")
                TipItem("First offline load takes 30-60 seconds depending on device")
                TipItem("Use the Logs screen to diagnose any issues")
                TipItem("Offline model uses ~1.5GB RAM when active")
                TipItem("Multiple .bin files in Downloads? Pick the best one in Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Limitations", Icons.Default.Warning, AccentGold) {
                TipItem("Offline: Gemma 2B is compact - may produce inaccurate responses")
                TipItem("Offline: Complex reasoning and math may not be reliable")
                TipItem("Online: Requires active internet connection")
                TipItem("Online: API key is stored locally (not encrypted)")
                TipItem("Chat history is in-memory only - cleared on app restart")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Privacy & Data", Icons.Default.Lock, AccentCyan) {
                Text(
                    text = "In Offline mode, MEDHA processes everything on your device. No data leaves your phone. " +
                            "In Online mode, your prompts are sent to Google's Gemini API servers for processing. " +
                            "Your API key is stored locally on your device. Chat history exists only in memory " +
                            "and is discarded when you close the app.",
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
