# ============================================================================
# Release-only R8 rules — applied ON TOP of proguard-rules.pro.
#
# Everything here must be something you would NOT want in a debuggable build.
# ============================================================================

# ---------------------------------------------------------------------------
# Strip logging from release builds.
#
# addLog() mirrors user prompts into Log.d/Log.i ("User: <prompt>"), so shipping these calls
# would leak user content into logcat (role 22 — no PII in logs). Log.e/Log.w survive so crash
# triage still has something to work with.
#
# Deliberately NOT in the shared rules file: the debug build is now minified as well, and
# stripping these there would remove the engine diagnostics (model load, context budget,
# backend fallback ladder) used to debug on device.
# ---------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
