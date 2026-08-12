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

# Runtime annotations + generic signatures are required by Moshi/Retrofit reflection.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ---------------------------------------------------------------------------
# Strip logging from release builds.
# addLog() mirrors user prompts into Log.d/Log.i ("User: <prompt>"), so shipping
# these calls would leak user content into logcat (role 22 — no PII in logs).
# Log.e/Log.w are kept so crash triage still has something to work with.
# ---------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ---------------------------------------------------------------------------
# Moshi — reflective adapters (KotlinJsonAdapterFactory) + codegen
# ---------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
# Generated adapters look up their target by name.
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *

# The Gemini DTOs are parsed reflectively — keep them and their members intact.
-keep class com.ashes.dev.works.ai.neural.brain.medha.data.remote.** { *; }
# Domain models are serialized by hand (org.json) but are also stored in Room.
-keep class com.ashes.dev.works.ai.neural.brain.medha.domain.model.** { *; }

# ---------------------------------------------------------------------------
# Retrofit + OkHttp
# ---------------------------------------------------------------------------
-keepattributes Exceptions
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Retrofit service interfaces are implemented by a runtime proxy.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking @interface retrofit2.http.**

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
