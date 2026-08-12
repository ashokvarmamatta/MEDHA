plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ashes.dev.works.ai.neural.brain.medha"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ashes.dev.works.ai.neural.brain.medha"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // arm64 only. LiteRT-LM ships a 25.7 MB liblitertlm_jni.so per ABI, and the x86_64 copy
        // is emulator-only — every phone install was carrying it for nothing. minSdk is 31, so
        // there is no 32-bit-only device left to support either.
        //
        // Trade-off: the app no longer runs on an x86_64 emulator. Test on a physical device
        // (or add "x86_64" back temporarily) if you need the emulator.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            // Role 11/22 gate: a release build must be shrunk, obfuscated and non-debuggable.
            // Keep rules for the reflective consumers (Moshi, Retrofit, Room, LiteRT-LM JNI)
            // live in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                // Release-only: strips Log.d/v/i so user prompts never reach logcat.
                "proguard-rules-release.pro"
            )
        }
        debug {
            // NOTE: deliberately NO applicationIdSuffix. The GitHub release workflow ships the
            // DEBUG apk to users, so changing its applicationId would orphan every existing
            // install. Revisit once release signing exists in CI (see AUDIT_ROLES.md).
            //
            // Minified as well, because the DEBUG apk is what users actually receive — leaving it
            // unshrunk meant shipping ~90 MB of dead code. It keeps debug signing and
            // isDebuggable, so it still installs and attaches like a debug build, and it
            // deliberately does NOT apply proguard-rules-release.pro, so engine logging survives.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        // Never let a release be assembled over a fatal lint issue.
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        htmlReport = true
        textReport = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
    }
}

// Role 13: Room must export its schema so a real migration can be written against a known
// previous version. Without this there is no schema history and every version bump is stuck
// on fallbackToDestructiveMigration (which wipes the user's chat history).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    // material-icons-extended removed: it declares every Material icon as its own property,
    // ~32 MB of dex (classes13/14/15). R8 strips the unused ones, but the GitHub workflow ships
    // the DEBUG apk where nothing is shrunk. An audit found 19 icons in use, 16 already in core;
    // the other three are hand-authored in ui/icons/MedhaIcons.kt.
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    // Removed (role 11 — unused dependencies): accompanist-permissions,
    // play-services-location and the four CameraX artifacts had zero imports anywhere in
    // app/src. They only added download size and third-party manifest entries.
    // Removed with the cloud path: retrofit, converter-moshi, okhttp, logging-interceptor
    // and moshi. The only network call left is the model download, which uses
    // HttpURLConnection directly — no HTTP client library needed.
    implementation(libs.androidx.datastore.preferences)
    // com.google.android.material removed: its ONLY use was the XML theme parent, which pulled
    // in ~1,100 classes of the View system (Material Components + appcompat + fragment +
    // recyclerview + constraintlayout) into a 100%-Compose app. themes.xml now uses the
    // platform DayNight theme instead.
    implementation(libs.litertlm)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    "ksp"(libs.androidx.room.compiler)
}
