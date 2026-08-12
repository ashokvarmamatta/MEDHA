package com.ashes.dev.works.ai.neural.brain.medha.data.local

import android.content.Context

/**
 * Engine settings that must be readable synchronously during model load, and one crash guard.
 *
 * Plain SharedPreferences rather than DataStore on purpose: engine init is not a good place to
 * suspend on a Flow, and the GPU guard below has to be written and read around a call that can
 * take the whole process down.
 */
class EnginePrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("engine_prefs", Context.MODE_PRIVATE)

    /**
     * Prefer the GPU for inference.
     *
     * Defaults to FALSE on evidence, not preference. Measured 2026-08-12 on a Redmi
     * (7.3 GB, Android 16): with `Backend.GPU()` the engine creates successfully and then dies
     * with `SIGSEGV, code 2 (SEGV_ACCERR) … in tid RenderThread` — LiteRT-LM's GPU backend
     * colliding with the UI's own GPU context. Koeyomi pins everything to CPU for exactly this
     * reason. The toggle is still offered, because other GPUs may well be fine, but it is opt-in.
     */
    var preferGpu: Boolean
        get() = prefs.getBoolean(KEY_PREFER_GPU, false)
        set(value) = prefs.edit().putBoolean(KEY_PREFER_GPU, value).apply()

    /**
     * Set immediately BEFORE a GPU engine create, and cleared only once the GPU has proved itself
     * by completing a whole generation.
     *
     * Why this exists: LiteRT-LM's GPU backend can fail as a **native crash** rather than a Kotlin
     * exception. A native crash kills the process, so no `catch` runs and no fallback happens —
     * and on the next launch we would try GPU again and crash again, a boot loop the user cannot
     * escape from inside the app.
     *
     * **Clearing this after `Engine()` returns is not enough, and that mistake cost a real boot
     * loop** (2026-08-12): engine create SUCCEEDS on GPU and the process dies afterwards, on
     * RenderThread, during use. The marker had already been cleared, so nothing was recorded and
     * every relaunch went straight back to the GPU. It now survives until a generation finishes.
     *
     * A user who loads a model and never sends a message leaves this set, so the next launch
     * conservatively assumes a crash and uses CPU. That costs speed, never correctness.
     */
    var gpuAttemptInFlight: Boolean
        get() = prefs.getBoolean(KEY_GPU_IN_FLIGHT, false)
        set(value) = prefs.edit().putBoolean(KEY_GPU_IN_FLIGHT, value).commit().let { }

    /** Model whose GPU attempt crashed, so a different model still gets its chance at the GPU. */
    var gpuCrashedFor: String?
        get() = prefs.getString(KEY_GPU_CRASHED_FOR, null)
        set(value) = prefs.edit().putString(KEY_GPU_CRASHED_FOR, value).apply()

    /**
     * One-time reset of a GPU preference saved by a build whose crash guard could not catch a
     * post-create GPU segfault.
     *
     * Those builds cleared the in-flight marker as soon as `Engine()` returned, so a device where
     * the GPU backend dies on RenderThread would relaunch straight back into the crash with
     * nothing recorded — and the crash arrives fast enough that the user cannot reach the setting
     * to turn it off. Anyone carrying `preferGpu = true` from such a build gets it cleared once;
     * re-enabling GPU afterwards is a deliberate choice and is left alone.
     */
    fun migrateUnsafeGpuPreference() {
        if (prefs.getInt(KEY_GUARD_VERSION, 0) >= GUARD_VERSION) return
        val hadGpu = prefs.getBoolean(KEY_PREFER_GPU, false)
        prefs.edit()
            .putInt(KEY_GUARD_VERSION, GUARD_VERSION)
            .apply {
                if (hadGpu) {
                    putBoolean(KEY_PREFER_GPU, false)
                    putBoolean(KEY_MIGRATION_NOTICE, true)
                }
            }
            .apply()
    }

    /** True if [migrateUnsafeGpuPreference] actually turned GPU off, so we can say so. */
    fun consumeMigrationNotice(): Boolean {
        val pending = prefs.getBoolean(KEY_MIGRATION_NOTICE, false)
        if (pending) prefs.edit().remove(KEY_MIGRATION_NOTICE).apply()
        return pending
    }

    /** Called at startup: promote an unfinished GPU attempt into a recorded crash. */
    fun reconcileGpuCrash(): String? {
        if (!gpuAttemptInFlight) return null
        val victim = prefs.getString(KEY_GPU_PENDING_MODEL, null)
        gpuAttemptInFlight = false
        gpuCrashedFor = victim
        return victim
    }

    /** Remember which model we are about to hand to the GPU, for [reconcileGpuCrash]. */
    fun markGpuAttempt(modelName: String) {
        prefs.edit()
            .putString(KEY_GPU_PENDING_MODEL, modelName)
            .putBoolean(KEY_GPU_IN_FLIGHT, true)
            .commit()
    }

    fun clearGpuAttempt() {
        prefs.edit().putBoolean(KEY_GPU_IN_FLIGHT, false).commit()
    }

    /** User explicitly re-enabled GPU — forget the recorded crash and try again. */
    fun clearGpuCrash() {
        prefs.edit().remove(KEY_GPU_CRASHED_FOR).apply()
    }

    private companion object {
        const val KEY_PREFER_GPU = "prefer_gpu"
        const val KEY_GPU_IN_FLIGHT = "gpu_attempt_in_flight"
        const val KEY_GPU_PENDING_MODEL = "gpu_pending_model"
        const val KEY_GPU_CRASHED_FOR = "gpu_crashed_for"
        const val KEY_GUARD_VERSION = "guard_version"
        const val KEY_MIGRATION_NOTICE = "migration_notice"
        /** Bump when the crash guard changes in a way that invalidates a stored GPU preference. */
        const val GUARD_VERSION = 1
    }
}
