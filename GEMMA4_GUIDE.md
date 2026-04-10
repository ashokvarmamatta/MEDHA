Run Gemma 4 On Your Android Phone — Complete Beginner's Guide (LiteRT LM SDK, Kotlin, on-device AI)

<div align="center">

# 🧠 Run Gemma 4 On Your Android Phone

### The Complete Beginner's Guide — From Zero to On-Device AI

<img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&weight=600&size=20&pause=1000&color=4285F4&center=true&vCenter=true&width=600&lines=No+internet+needed+after+download;32K+context+window;Streaming+token-by-token;Thinking+mode+(chain-of-thought);Vision+%2B+Audio+support;140%2B+languages+supported;100%25+private+%E2%80%94+nothing+leaves+your+phone" alt="Typing SVG" />

<br/>

[![LiteRT LM](https://img.shields.io/badge/Engine-LiteRT_LM_0.10.0-FF6F00?style=for-the-badge)](https://ai.google.dev/edge/litert)
[![Gemma 4](https://img.shields.io/badge/Model-Gemma_4-4285F4?style=for-the-badge)](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)
[![Android](https://img.shields.io/badge/Android_12+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin_2.2-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Privacy](https://img.shields.io/badge/100%25_On--Device-4CAF50?style=for-the-badge)](.)

</div>

---

## 📖 What This Guide Covers

```
You want to run an AI model ON YOUR PHONE.
No cloud. No API key. No internet (after download).
This guide shows you EXACTLY how — from scratch.

         ┌──────────────────────────────┐
         │  1. Pick a model             │  ← Which Gemma to download?
         │  2. Add the SDK             │  ← One Gradle line
         │  3. Download the model       │  ← 2.6 GB, one-time
         │  4. Load it                  │  ← Engine + Conversation
         │  5. Chat with it             │  ← Send text, get response
         │  6. Advanced stuff           │  ← Images, audio, thinking
         └──────────────────────────────┘
```

---

## 🤔 Wait, What Does "On-Device AI" Mean?

```
NORMAL AI (Cloud)                    ON-DEVICE AI (This Guide)
─────────────────                    ────────────────────────
Your phone                           Your phone
    │                                     │
    │  "What is gravity?"                 │  "What is gravity?"
    │                                     │
    ▼                                     ▼
Internet ──► OpenAI/Google server    THE MODEL RUNS HERE
    │            │                   ON YOUR PHONE'S CPU/GPU
    │            │ (processes)            │
    │            ▼                        │ (processes locally)
    │        Response                     ▼
    ◄────────────┘                   Response
    │                                     │
    ▼                                     ▼
You see the reply                    You see the reply

❌ Needs internet                    ✅ Works in airplane mode
❌ Data goes to cloud                ✅ Data never leaves phone
❌ Costs money (API fees)            ✅ 100% free after download
❌ Server can be down                ✅ Always available
```

---

## 📦 Step 1 — Pick Your Model

| Model | Size | RAM Needed | What It Can Do | Best For |
|-------|------|-----------|----------------|----------|
| 🥇 **Gemma 4 E2B** | **2.6 GB** | 8 GB | Text + Vision + Audio + Thinking + 140 languages | **Most phones. Start here.** |
| 🥈 **Gemma 4 E4B** | 3.7 GB | 12 GB | Same but smarter, 140 languages | Flagship phones (Pixel 9, S25 Ultra) |
| 🥉 **Gemma 3n E2B** | 3.7 GB | 8 GB | Text + Vision + Audio (no thinking) | Previous gen, still solid |
| ⚡ **Gemma 3 1B** | 584 MB | 6 GB | Text only | Low-end phones, fast responses |
| 🧪 **DeepSeek R1 1.5B** | 1.8 GB | 6 GB | Text only (reasoning) | Logical/math tasks |

> 💡 **Don't know which to pick?** → **Gemma 4 E2B**. It's the newest, smallest for its power, and works on most modern phones.

### Where to download

Every model is on HuggingFace. Direct links:

| Model | Download Link |
|-------|--------------|
| Gemma 4 E2B | https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm |
| Gemma 4 E4B | https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm |
| Gemma 3n E2B | https://huggingface.co/google/gemma-3n-E2B-it-litert-lm |
| Gemma 3 1B | https://huggingface.co/litert-community/Gemma3-1B-IT |
| DeepSeek R1 | https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B |

---

## 🔧 Step 2 — Add the SDK to Your Android Project

You need **one library**: LiteRT LM (Google's on-device LLM engine).

### 2a. Version catalog (`gradle/libs.versions.toml`)

```toml
[versions]
kotlin = "2.2.0"          # Must be 2.2.0+ (LiteRT LM requires it)
litertlm = "0.10.0"
ksp = "2.2.0-2.0.2"       # If you use Room, switch from kapt to KSP

[libraries]
litertlm = { group = "com.google.ai.edge.litertlm", name = "litertlm-android", version.ref = "litertlm" }

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 2b. App build file (`app/build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)   // ← Required for Kotlin 2.0+
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 31    // Android 12+ required
    }
    // ⚠️ Remove composeOptions { kotlinCompilerExtensionVersion = "..." }
    //    The kotlin-compose plugin handles this now
}

dependencies {
    implementation(libs.litertlm)   // ← This is the only new dependency
}
```

### 2c. Sync Gradle

Click **"Sync Now"** in Android Studio. If you see errors:

| Error | Fix |
|-------|-----|
| `Metadata version 2.3.0, expected 1.9.0` | Upgrade Kotlin to 2.2.0 |
| `kapt` fails with Room | Switch Room from `kapt` to `ksp` |
| `composeOptions` error | Remove `composeOptions` block, add `kotlin-compose` plugin |

> ⚠️ **Big gotcha**: LiteRT LM uses Kotlin 2.3 metadata. Your project MUST use Kotlin 2.2.0+. This cascades: Room needs 2.7+, kapt→KSP, old Compose compiler plugin replaced by `kotlin-compose`. See [BUG-35 in ZeroClaw](https://github.com/ashokvarmamatta/ZeroClawAndroid/blob/main/BUGS.md) for the full story.

---

## 📥 Step 3 — Download the Model to the Phone

Two approaches: **in-app download** or **manual push via ADB**.

### Option A: Download in your app (recommended)

```kotlin
// Use WorkManager for reliable background download with resume support
// Full example: ModelDownloadWorker.kt in ZeroClawAndroid

val url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"
val destDir = File(context.filesDir, "models")
destDir.mkdirs()
val destFile = File(destDir, "gemma-4-E2B-it.litertlm")

// Simple download (for testing — use WorkManager for production)
withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connect()
    conn.inputStream.use { input ->
        FileOutputStream(destFile).use { output ->
            input.copyTo(output)
        }
    }
}
// destFile.absolutePath is your model path
```

### Option B: Push via ADB (for development)

```bash
# Download the model file to your computer first, then:
adb push gemma-4-E2B-it.litertlm /data/local/tmp/

# Or push to app's files directory:
adb push gemma-4-E2B-it.litertlm /storage/emulated/0/Android/data/YOUR.PACKAGE.NAME/files/models/
```

### Resume support (for large downloads)

```kotlin
// If download fails mid-way, resume from where it stopped:
val startByte = if (tmpFile.exists()) tmpFile.length() else 0L
val conn = URL(url).openConnection() as HttpURLConnection
if (startByte > 0) {
    conn.setRequestProperty("Range", "bytes=$startByte-")
}
// HTTP 206 = resumed, 200 = started over
```

---

## 🚀 Step 4 — Load the Model (Engine + Conversation)

This is where the magic happens. The LiteRT LM SDK has two main objects:

```
Engine          = the brain (loads model weights into memory)
Conversation    = the chat session (sends messages, gets replies)

You create ONE Engine, then create Conversations from it.
Engine is heavy (5-30 sec to load). Conversation is light (instant).
```

### Complete loading code

```kotlin
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig

// ── Step 1: Configure the engine ────────────────────────

val modelPath = "/path/to/gemma-4-E2B-it.litertlm"

val engineConfig = EngineConfig(
    modelPath = modelPath,
    backend = Backend.CPU(),          // ← text inference on CPU (safe default)
    visionBackend = Backend.GPU(),    // ← REQUIRED for image input! (null = no vision)
    audioBackend = Backend.CPU(),     // ← for audio input (null = no audio)
    maxNumTokens = 4096               // ← max tokens for input + output combined
)

// ── Step 2: Create and initialize the engine ────────────
//    This loads the model into memory. Takes 5-30 seconds.
//    Do this on a background thread!

val engine = Engine(engineConfig)
engine.initialize()    // ← blocking call, run on Dispatchers.IO

// ── Step 3: Create a conversation ───────────────────────

val samplerConfig = SamplerConfig(
    topK = 64,            // consider top 64 tokens at each step
    topP = 0.95,          // nucleus sampling: keep tokens until 95% probability
    temperature = 1.0     // 1.0 = balanced, 0.0 = deterministic, 2.0 = creative
)

val conversation = engine.createConversation(
    ConversationConfig(
        samplerConfig = samplerConfig,
        // Optional: set a system prompt
        systemInstruction = Contents.of(listOf(
            Content.Text("You are a helpful assistant. Be concise.")
        ))
    )
)

println("✅ Model loaded and ready!")
```

### What each parameter does

```
┌─────────────────────────────────────────────────────────────┐
│                    EngineConfig                               │
│                                                               │
│  modelPath     = where the .litertlm file is on disk         │
│  backend       = CPU or GPU (see section below)              │
│  maxNumTokens  = total budget for input + output tokens      │
│                  4096 = good default                          │
│                  32768 = max for Gemma 4 (uses more RAM)     │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│                    SamplerConfig                              │
│                                                               │
│  temperature   = randomness of output                        │
│                  0.0 = always picks most likely word          │
│                  1.0 = balanced (default, good for chat)     │
│                  2.0 = very creative/random                  │
│                                                               │
│  topK          = only consider the top K most likely tokens  │
│                  64 = good default                            │
│                  1 = greedy (always pick the best)            │
│                                                               │
│  topP          = nucleus sampling threshold                  │
│                  0.95 = consider tokens until 95% cumulative │
│                  1.0 = consider all tokens                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 💬 Step 5 — Chat With the Model

### 5a. Simple blocking call

```kotlin
// Send a message and get the complete response
val input = Contents.of(listOf(Content.Text("What is photosynthesis?")))

// This blocks until the full response is generated
val response = conversation.generateResponse(input)
println(response)
// Output: "Photosynthesis is the process by which green plants..."
```

### 5b. Streaming (token-by-token) ⭐ Recommended

```kotlin
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback

val input = Contents.of(listOf(Content.Text("Explain gravity in simple terms")))

conversation.sendMessageAsync(
    input,
    object : MessageCallback {
        override fun onMessage(message: Message) {
            // Called for EACH token as it's generated
            val token = message.toString()
            print(token)  // prints word-by-word: "Gravity" "is" "a" "force" ...
            
            // Check for thinking content (Gemma 4 only)
            val thinking = message.channels["thought"]?.toString()
            if (!thinking.isNullOrEmpty()) {
                println("[THINKING] $thinking")
            }
        }

        override fun onDone() {
            println("\n✅ Generation complete!")
        }

        override fun onError(throwable: Throwable) {
            println("❌ Error: ${throwable.message}")
        }
    },
    emptyMap()  // extra context (pass mapOf("enable_thinking" to "true") for thinking mode)
)
```

### 5c. Multi-turn conversation (the Conversation remembers!)

```kotlin
// Turn 1
conversation.sendMessageAsync(
    Contents.of(listOf(Content.Text("My name is Alex"))),
    callback, emptyMap()
)
// AI: "Nice to meet you, Alex!"

// Turn 2 — the AI remembers turn 1!
conversation.sendMessageAsync(
    Contents.of(listOf(Content.Text("What's my name?"))),
    callback, emptyMap()
)
// AI: "Your name is Alex!"

// No manual history management needed — the Conversation object handles it.
```

### 5d. Reset conversation (clear history)

```kotlin
// Close old conversation, create new one on same engine
conversation.close()
val newConversation = engine.createConversation(
    ConversationConfig(samplerConfig = samplerConfig)
)
// New conversation has no memory of previous messages
```

---

## 🖼️ Step 6 — Send Images (Vision)

Gemma 4 can understand images! Send a photo and ask about it.

```kotlin
// Load image as PNG byte array
val bitmap: Bitmap = // ... load from camera, gallery, etc.
val stream = ByteArrayOutputStream()
bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
val imageBytes = stream.toByteArray()

// Build contents: image first, then text
val contents = Contents.of(listOf(
    Content.ImageBytes(imageBytes),                    // ← the image
    Content.Text("What do you see in this image?")     // ← the question
))

conversation.sendMessageAsync(contents, callback, emptyMap())
// AI: "I see a golden retriever playing in a park with a red frisbee..."
```

> ⚠️ **Important:** Add the image BEFORE the text in the Contents list. The SDK processes them in order.

> 🚨 **CRITICAL — `visionBackend` is REQUIRED!** If your `EngineConfig` does not include `visionBackend = Backend.GPU()`, sending `Content.ImageBytes` will cause a **native SIGSEGV crash** (null pointer in `liblitertlm_jni.so`). This crash cannot be caught by try/catch — it kills the entire app. Make sure your engine is configured like this:
> ```kotlin
> val engineConfig = EngineConfig(
>     modelPath = modelPath,
>     backend = Backend.CPU(),
>     visionBackend = Backend.GPU(),  // ← WITHOUT THIS, IMAGE INPUT CRASHES!
>     maxNumTokens = 4096
> )
> ```
> Without `visionBackend`, no vision executor is created, so the image bytes hit a null pointer in the native layer.

> 📝 **Supported models:** Only Gemma 4 E2B/E4B and Gemma 3n support vision. Gemma 3 1B and DeepSeek are text-only.

---

## 🎤 Step 7 — Send Audio

```kotlin
// Audio must be raw PCM bytes (not MP3/AAC)
// Sample rate: 16000 Hz, mono, 16-bit

val audioBytes: ByteArray = // ... record from microphone or load WAV

val contents = Contents.of(listOf(
    Content.AudioBytes(audioBytes),
    Content.Text("Transcribe this audio and summarize it")
))

conversation.sendMessageAsync(contents, callback, emptyMap())
```

> 📝 **Supported models:** Gemma 4 E2B/E4B and Gemma 3n support audio. Others are text-only.

---

## 💭 Step 8 — Thinking Mode (Chain-of-Thought)

Gemma 4 can show its reasoning process before answering. Like watching it think.

```kotlin
// Enable thinking via extra context
val extraContext = mapOf("enable_thinking" to "true")

conversation.sendMessageAsync(
    Contents.of(listOf(Content.Text("If I have 3 boxes with 5 apples each, and I give away 7, how many remain?"))),
    object : MessageCallback {
        override fun onMessage(message: Message) {
            val text = message.toString()
            val thinking = message.channels["thought"]?.toString()

            if (!thinking.isNullOrEmpty()) {
                // This is the AI's internal reasoning
                println("🧠 Thinking: $thinking")
                // "Let me calculate: 3 boxes × 5 apples = 15 apples total.
                //  If I give away 7: 15 - 7 = 8 apples remain."
            }
            if (text.isNotEmpty()) {
                // This is the final answer
                println("💬 Answer: $text")
                // "You have 8 apples remaining."
            }
        }

        override fun onDone() { println("✅ Done") }
        override fun onError(t: Throwable) { println("❌ ${t.message}") }
    },
    extraContext   // ← this enables thinking mode
)
```

```
What you see:

🧠 Thinking: Let me break this down step by step.
🧠 Thinking: 3 boxes × 5 apples = 15 total apples.
🧠 Thinking: 15 - 7 = 8 apples remaining.
💬 Answer: You have 8 apples remaining.
✅ Done
```

> 📝 **Only Gemma 4** supports thinking mode. Other models ignore the `enable_thinking` context.

---

## ⚡ Step 9 — CPU vs GPU

### CPU (Default — Use This)

```kotlin
val engineConfig = EngineConfig(
    modelPath = modelPath,
    backend = Backend.CPU(),
    maxNumTokens = 4096
)
```

✅ Works on all phones
✅ Stable, no crashes
✅ Uses ~2-3 GB RAM
❌ Slower generation (5-15 tok/s depending on phone)

### GPU (Advanced — High-End Phones Only)

```kotlin
val engineConfig = EngineConfig(
    modelPath = modelPath,
    backend = Backend.GPU(),
    maxNumTokens = 4096
)
```

✅ 2-5x faster generation
❌ **Loads entire model into GPU VRAM**
❌ **WILL CRASH (SIGSEGV) on phones with < 12 GB RAM**
❌ Competes with Android's RenderThread for GPU → can freeze UI

```
⚠️  WARNING: GPU MODE CRASH EXPLAINED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Gemma 4 E2B = 2.6 GB model file
GPU loading  = ~3 GB VRAM needed
Android UI   = also uses GPU for drawing

Phone has 8 GB RAM total:
  - Android OS:     ~2 GB
  - Your app:       ~1 GB
  - Model on GPU:   ~3 GB
  - RenderThread:   needs GPU too → SIGSEGV (Fatal signal 11)
                    ═══════════════════════════════════
                    App crashes. Not catchable in Java.

FIX: Use CPU. Or only use GPU on 12GB+ RAM phones.
```

### How to safely try GPU with fallback

```kotlin
fun createEngine(modelPath: String): Engine {
    // Try GPU first on high-end devices, fall back to CPU
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)
    val totalRamGb = memInfo.totalMem / (1024L * 1024 * 1024)

    val backend = if (totalRamGb >= 12) {
        Log.d("LLM", "Device has ${totalRamGb}GB RAM — using GPU")
        Backend.GPU()
    } else {
        Log.d("LLM", "Device has ${totalRamGb}GB RAM — using CPU (GPU needs 12GB+)")
        Backend.CPU()
    }

    val config = EngineConfig(
        modelPath = modelPath,
        backend = backend,
        maxNumTokens = 4096
    )
    return Engine(config).also { it.initialize() }
}
```

---

## 🧹 Step 10 — Cleanup (Don't Leak Memory!)

```kotlin
// When you're done with the model (app closing, switching models, etc.)

conversation.close()    // ← close conversation FIRST
engine.close()          // ← then close engine

// If you want to cancel generation mid-way:
conversation.cancelProcess()
```

> ⚠️ **Always close in order:** conversation first, then engine. Closing engine without closing conversation can leak native memory.

---

## 📋 Complete Copy-Paste Example

Drop this into any Activity or ViewModel and it works:

```kotlin
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modelPath = "${filesDir}/models/gemma-4-E2B-it.litertlm"

        lifecycleScope.launch(Dispatchers.IO) {
            // Load model
            Log.d("LLM", "Loading model...")
            val config = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                maxNumTokens = 4096
            )
            engine = Engine(config).also { it.initialize() }

            conversation = engine!!.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0),
                    systemInstruction = Contents.of(listOf(
                        Content.Text("You are a helpful, concise assistant.")
                    ))
                )
            )
            Log.d("LLM", "✅ Model ready!")

            // Chat
            chat("Hello! What can you do?")
            chat("What is the capital of Japan?")
            chat("What did I just ask you?")  // Tests memory
        }
    }

    private fun chat(userMessage: String) {
        Log.d("LLM", "👤 You: $userMessage")
        val sb = StringBuilder()

        conversation?.sendMessageAsync(
            Contents.of(listOf(Content.Text(userMessage))),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    sb.append(message.toString())
                }
                override fun onDone() {
                    Log.d("LLM", "🤖 AI: $sb")
                }
                override fun onError(throwable: Throwable) {
                    Log.e("LLM", "❌ Error: ${throwable.message}")
                }
            },
            emptyMap()
        )
    }

    override fun onDestroy() {
        conversation?.close()
        engine?.close()
        super.onDestroy()
    }
}
```

---

## 🔧 Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| `Metadata version 2.3.0, expected 1.9.0` | Kotlin too old | Upgrade to Kotlin 2.2.0 |
| `kapt` build failure with Room | Room 2.6 incompatible with Kotlin 2.2 | Upgrade Room to 2.7+, switch kapt→KSP |
| `SIGSEGV (Fatal signal 11)` on model load | GPU out of memory | Switch to `Backend.CPU()` |
| `SIGSEGV` when sending `Content.ImageBytes` | Missing `visionBackend` in EngineConfig | Add `visionBackend = Backend.GPU()` to EngineConfig — without it, no vision executor is created and image bytes hit a null pointer |
| `SIGSEGV` on image with GPU backend too | Model + vision both on GPU = OOM | Keep `backend = CPU()`, only `visionBackend = GPU()` |
| Model takes 30+ seconds to load | Normal for first load | Load on background thread, show progress |
| `Model file not found` | Wrong path | Check `context.filesDir` path, verify file exists |
| Response is garbage/random | Temperature too high | Lower temperature to 0.7-1.0 |
| App killed by Android | Model uses too much RAM | Use smaller model (Gemma 3 1B = 584 MB) |
| `composeOptions` error | Old Compose compiler setup | Remove `composeOptions`, add `kotlin-compose` plugin |
| `CancellationException` on response | User cancelled or timeout | Handle gracefully, not a real error |

---

## 📊 Performance Benchmarks

Tested on mid-range Android phone (8 GB RAM, Snapdragon 7 Gen 2):

| Model | Load Time | Speed (CPU) | RAM Usage |
|-------|-----------|-------------|-----------|
| Gemma 4 E2B | ~15 sec | 8-12 tok/s | ~3.5 GB |
| Gemma 3 1B | ~3 sec | 15-25 tok/s | ~1.2 GB |
| DeepSeek R1 1.5B | ~5 sec | 10-15 tok/s | ~2.0 GB |

> Performance varies by device. Flagship phones (Pixel 9, S25 Ultra) are 2-3x faster.

---

## 🔗 Resources

| What | Link |
|------|------|
| LiteRT LM SDK | https://ai.google.dev/edge/litert |
| Gemma 4 Models | https://huggingface.co/litert-community |
| Google AI Edge Gallery (reference app) | https://github.com/google-ai-edge/gallery |
| ZeroClaw Android (production example) | https://github.com/ashokvarmamatta/ZeroClawAndroid |
| Kotlin 2.2 Migration Guide | https://kotlinlang.org/docs/whatsnew22.html |

---

<div align="center">

### Built with LiteRT LM by Google AI Edge

*Guide by [@ashokvarmamatta](https://github.com/ashokvarmamatta)*

*Learned by building [ZeroClaw Android](https://github.com/ashokvarmamatta/ZeroClawAndroid) — 180 phases, 37 tools, 10 channels, Gemma 4 on-device*

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=6,11,20&height=100&section=footer" width="100%" />

</div>
