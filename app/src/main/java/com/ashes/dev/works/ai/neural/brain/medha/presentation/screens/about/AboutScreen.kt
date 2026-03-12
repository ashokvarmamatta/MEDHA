package com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatViewModel
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentCyan
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGold
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentGreen
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.AccentPink
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientEnd
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientMid
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.GradientStart
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.StatusWarning

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
                GuideStep("1", "Download Model", "Get a Gemma .bin model file from any source below.")
                GuideStep("2", "Place in Downloads", "Move the model file to your device's Downloads folder.")
                GuideStep("3", "Grant Permission", "Allow 'All Files Access' so the app can read the model.")
                GuideStep("4", "Select Model", "Go to Settings > Offline Models to pick your model.")
                GuideStep("5", "Start Chatting", "The engine loads automatically. Type your first message!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Get Offline Models", Icons.Default.Build, AccentGreen) {
                val uriHandler = LocalUriHandler.current
                Text(
                    "Download AI model files for on-device inference. Place .bin files in your Downloads folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResourceLink("Kaggle \u2014 Google Gemma Models", "Official Gemma 2B/7B models for MediaPipe", "https://kaggle.com/models/google/gemma", AccentGreen, uriHandler)
                ResourceLink("Hugging Face Hub", "Largest open model repository \u2014 GGUF, .bin, safetensors", "https://huggingface.co/models", AccentGold, uriHandler)
                ResourceLink("Ollama Model Library", "Curated models for local inference \u2014 Llama, Gemma, Phi, Mistral", "https://ollama.com/library", AccentCyan, uriHandler)
                ResourceLink("LM Studio", "Desktop app with model browser \u2014 downloads GGUF models", "https://lmstudio.ai", AccentPink, uriHandler)
                ResourceLink("GPT4All", "Curated models optimized for consumer hardware", "https://gpt4all.io", AccentGreen, uriHandler)
                ResourceLink("Mozilla Llamafile", "Single-file executables \u2014 model + runtime bundled", "https://github.com/Mozilla-Ocho/llamafile", AccentGold, uriHandler)
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Online Setup", Icons.Default.Build, AccentCyan) {
                GuideStep("1", "Get API Key", "Get a free API key from any provider below.")
                GuideStep("2", "Open Settings", "Go to Settings and switch to Online Mode.")
                GuideStep("3", "Add API Key", "Paste your key, add a label, and test it.")
                GuideStep("4", "Start Chatting", "Select a model and start chatting!")
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Free API Key Providers", Icons.Default.Star, AccentGreen) {
                val uriHandler = LocalUriHandler.current
                Text(
                    "These providers offer free tiers or free credits. Get started without paying.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResourceLink("Google Gemini \u2014 AI Studio", "Most generous free tier \u2014 Gemini 2.0 Flash, 1.5 Pro. Recommended!", "https://aistudio.google.com/apikey", AccentGreen, uriHandler, badge = "BEST FREE")
                ResourceLink("Groq", "Blazing fast inference \u2014 Llama 3, Mixtral, Gemma. Free with rate limits", "https://console.groq.com/keys", AccentCyan, uriHandler, badge = "FAST")
                ResourceLink("OpenRouter", "Multi-model gateway \u2014 single key for many providers, some models free", "https://openrouter.ai/keys", AccentGold, uriHandler, badge = "MULTI-MODEL")
                ResourceLink("NVIDIA NIM", "1000 free credits \u2014 Llama, Mistral on NVIDIA hardware", "https://build.nvidia.com", AccentGreen, uriHandler)
                ResourceLink("Together AI", "Free credits on signup \u2014 wide selection of open models", "https://api.together.ai/settings/api-keys", AccentCyan, uriHandler)
                ResourceLink("Mistral AI", "Free experiment tier \u2014 Mistral Small, Large, Codestral", "https://console.mistral.ai/api-keys", AccentGold, uriHandler)
                ResourceLink("DeepSeek", "Free credits \u2014 DeepSeek-V3, R1 reasoning, Coder", "https://platform.deepseek.com/api_keys", AccentGreen, uriHandler)
                ResourceLink("Cohere", "Free trial keys \u2014 Command R/R+, good for RAG & embeddings", "https://dashboard.cohere.com/api-keys", AccentCyan, uriHandler)
                ResourceLink("Hugging Face Inference", "Free tier for thousands of models \u2014 rate limited", "https://huggingface.co/settings/tokens", AccentGold, uriHandler)
                ResourceLink("SambaNova Cloud", "Free tier \u2014 fast inference on custom RDU hardware", "https://cloud.sambanova.ai/apis", AccentGreen, uriHandler)
                ResourceLink("Cerebras Inference", "Free tier \u2014 extremely fast inference on wafer-scale chips", "https://cloud.cerebras.ai", AccentCyan, uriHandler)
                ResourceLink("Fireworks AI", "Free credits on signup \u2014 fast open model inference", "https://fireworks.ai/account/api-keys", AccentGold, uriHandler)
                ResourceLink("DeepInfra", "Free credits \u2014 serverless inference for many open models", "https://deepinfra.com/dash/api_keys", AccentGreen, uriHandler)
                ResourceLink("Cloudflare Workers AI", "10,000 free neurons/day \u2014 edge inference", "https://dash.cloudflare.com", AccentCyan, uriHandler)
                ResourceLink("GitHub Models", "Free access to GPT-4o, Llama, Mistral via GitHub", "https://github.com/marketplace/models", AccentGold, uriHandler)
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoSection("Paid API Providers", Icons.Default.Star, StatusWarning) {
                val uriHandler = LocalUriHandler.current
                Text(
                    "Premium providers \u2014 paid only, but offer the most capable models.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResourceLink("OpenAI", "GPT-4o, o1, o3, DALL-E, Whisper \u2014 requires prepaid credits", "https://platform.openai.com/api-keys", StatusWarning, uriHandler, badge = "PAID")
                ResourceLink("Anthropic Claude", "Claude Opus 4, Sonnet 4 \u2014 requires billing setup", "https://console.anthropic.com/settings/keys", StatusWarning, uriHandler, badge = "PAID")
                ResourceLink("Perplexity API", "Search-augmented LLMs with citations \u2014 pay per request", "https://www.perplexity.ai/settings/api", StatusWarning, uriHandler, badge = "PAID")
                ResourceLink("Azure OpenAI", "Enterprise-grade OpenAI models on Azure \u2014 $200 free credits for new accounts", "https://azure.microsoft.com/en-us/products/ai-services/openai-service", StatusWarning, uriHandler, badge = "PAID")
                ResourceLink("AWS Bedrock", "Claude, Llama, Mistral on AWS \u2014 free credits for new accounts", "https://aws.amazon.com/bedrock", StatusWarning, uriHandler, badge = "PAID")
                ResourceLink("Google Vertex AI", "$300 free credits for new GCP accounts \u2014 Gemini, PaLM, Imagen", "https://console.cloud.google.com", StatusWarning, uriHandler, badge = "PAID")
                ResourceLink("Replicate", "Pay-per-second compute \u2014 open models + image/video/audio", "https://replicate.com/account/api-tokens", StatusWarning, uriHandler, badge = "PAID")
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

@Composable
private fun ResourceLink(
    title: String,
    description: String,
    url: String,
    accentColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    badge: String? = null
) {
    Surface(
        onClick = { uriHandler.openUri(url) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
            Text(
                "\u2197",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
