# ============================================================================
# MEDHA — R8 / ProGuard rules
#
# The release build runs with isMinifyEnabled + isShrinkResources (role 11/22 gate).
# R8 full mode is the AGP 8+ default: nothing is kept implicitly, so every
# reflective consumer below needs an explicit rule or it breaks only at runtime.
# ============================================================================

# Keep crash stack traces readable (mapping.txt still deobfuscates the rest).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Runtime annotations + generic signatures are required by Room's generated code.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# NOTE: log stripping lives in proguard-rules-release.pro, NOT here. This file is applied to
# the minified DEBUG build too, and removing Log.d/Log.i there would delete the engine
# diagnostics (model load, context budget, backend fallbacks) that on-device debugging depends on.

# ---------------------------------------------------------------------------
# Domain models
#
# Moshi/Retrofit/OkHttp rules were dropped along with the cloud path — the app has
# no HTTP client left. The model download uses HttpURLConnection, which needs none.
# ---------------------------------------------------------------------------
# Domain models are serialized by hand (org.json) but are also stored in Room.
-keep class com.ashes.dev.works.ai.neural.brain.medha.domain.model.** { *; }
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ---------------------------------------------------------------------------
# Room — entities/DAOs are reached from generated code and by name
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# LiteRT-LM — JNI entry points. Native code resolves these by exact name;
# obfuscating them produces UnsatisfiedLinkError at model load, not at build.
# ---------------------------------------------------------------------------
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class org.tensorflow.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-dontwarn com.google.ai.edge.litertlm.**
-dontwarn org.tensorflow.**

# ---------------------------------------------------------------------------
# Koin — resolves definitions via reflection on constructors
# ---------------------------------------------------------------------------
-keep class org.koin.** { *; }
-keepclassmembers class * {
    public <init>(...);
}

# ---------------------------------------------------------------------------
# Kotlin / Compose plumbing
# ---------------------------------------------------------------------------
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlinx.coroutines.**
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keepclassmembernames class kotlinx.** { volatile <fields>; }
