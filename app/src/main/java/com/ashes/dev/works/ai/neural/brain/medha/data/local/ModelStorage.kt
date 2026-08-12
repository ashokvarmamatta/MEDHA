package com.ashes.dev.works.ai.neural.brain.medha.data.local

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/** Where multi-gigabyte model weights are read from. */
enum class ModelLocation {
    /** `filesDir/medha_models` — private to MEDHA, and deleted with it. */
    App,

    /** A folder the user picked with the system folder picker, shared with their other apps. */
    Shared,
}

/**
 * Decides where models are looked for.
 *
 * **The problem.** A narrator model is 2.4 GB. A private copy per app means MEDHA, Koeyomi and
 * anything else the user builds each pay that price for identical weights — on a phone already at
 * 99 % full that is the difference between the feature working and not. One shared folder means
 * one copy, it survives uninstall, and weights fetched on a PC can simply be dropped in.
 *
 * **The mechanism.** Koeyomi solves this with All-files access and raw `/sdcard/AIModels` paths.
 * MEDHA cannot: `MANAGE_EXTERNAL_STORAGE` needs a Play sensitive-permission declaration that is
 * routinely refused for apps that are not file managers. Instead the user grants ONE folder
 * through `ACTION_OPEN_DOCUMENT_TREE`, which costs no manifest permission, and
 * [ExternalModelStore] bridges the resulting document to something the native loader can open.
 *
 * Pointing the picker at `/sdcard/AIModels` gives full interop with Koeyomi — same folder, same
 * files, neither app downloading what the other already has. [SUGGESTED_FOLDER] is only a hint
 * shown in the UI; the user may pick anywhere.
 */
object ModelStorage {

    /** The folder Koeyomi downloads into. Suggested in the UI so the two apps line up. */
    const val SUGGESTED_FOLDER = "AIModels"

    /** MEDHA's own private folder name — unchanged, so existing installs keep their models. */
    const val APP_FOLDER = "medha_models"

    private const val PREFS = "model_storage"
    private const val KEY_LOCATION = "location"
    private const val KEY_TREE_URI = "shared_tree_uri"
    private const val KEY_FILE_URIS = "shared_file_uris"

    /**
     * Plain [android.content.SharedPreferences] rather than DataStore on purpose: this is read on
     * every model lookup, including from non-suspending paths like engine init, and a value that
     * must be awaited would turn every one of those into a coroutine.
     */
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun location(context: Context): ModelLocation =
        when (prefs(context).getString(KEY_LOCATION, null)) {
            ModelLocation.Shared.name -> ModelLocation.Shared
            else -> ModelLocation.App
        }

    fun setLocation(context: Context, location: ModelLocation) {
        prefs(context).edit().putString(KEY_LOCATION, location.name).apply()
    }

    /** The folder the user granted, or null if none has been picked yet. */
    fun sharedTreeUri(context: Context): Uri? =
        prefs(context).getString(KEY_TREE_URI, null)
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?.takeIf { ExternalModelStore.hasPermission(context, it) }

    /** Remember a freshly picked folder and switch to it. Returns false if the grant failed. */
    fun setSharedTree(context: Context, treeUri: Uri): Boolean {
        if (!ExternalModelStore.persistPermission(context, treeUri)) return false
        prefs(context).edit()
            .putString(KEY_TREE_URI, treeUri.toString())
            .putString(KEY_LOCATION, ModelLocation.Shared.name)
            .apply()
        return true
    }

    fun clearSharedTree(context: Context) {
        prefs(context).edit()
            .remove(KEY_TREE_URI)
            .putString(KEY_LOCATION, ModelLocation.App.name)
            .apply()
    }

    /** True when a folder has been picked AND the grant is still held after a reboot. */
    fun hasSharedAccess(context: Context): Boolean = sharedTreeUri(context) != null

    // ── Individually granted model FILES ────────────────────────────────────
    //
    // Separate from the folder grant, for the user who would rather hand over one file than a
    // whole directory. Same deal: persisted read access, loaded in place, never copied.

    /** Model files the user granted one at a time, minus any whose grant has lapsed. */
    fun sharedFileUris(context: Context): List<Uri> =
        (prefs(context).getStringSet(KEY_FILE_URIS, emptySet()) ?: emptySet())
            .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
            .filter { ExternalModelStore.hasPermission(context, it) }

    /** Remember a single picked model file. Returns false if the grant could not be kept. */
    fun addSharedFile(context: Context, uri: Uri): Boolean {
        if (!ExternalModelStore.persistFilePermission(context, uri)) return false
        val current = prefs(context).getStringSet(KEY_FILE_URIS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current += uri.toString()
        prefs(context).edit().putStringSet(KEY_FILE_URIS, current).apply()
        return true
    }

    fun removeSharedFile(context: Context, uri: Uri) {
        val current = prefs(context).getStringSet(KEY_FILE_URIS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current -= uri.toString()
        prefs(context).edit().putStringSet(KEY_FILE_URIS, current).apply()
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    fun appRoot(context: Context): File =
        File(context.applicationContext.filesDir, APP_FOLDER).apply { mkdirs() }

    /**
     * MEDHA's own download folder — the fallback when no shared folder has been granted.
     *
     * When one HAS been granted, downloads go there instead (see `SafDownloadSink`), so the next
     * app finds the weights already present. Writing into the picked tree needs no storage
     * permission: the SAF grant covers creating documents inside it.
     */
    fun downloadRoot(context: Context): File = appRoot(context)

    /** The `/sdcard/AIModels` path shown as a hint in the picker prompt. */
    fun suggestedPath(): String =
        File(Environment.getExternalStorageDirectory(), SUGGESTED_FOLDER).absolutePath

    fun describe(context: Context): String = when {
        location(context) == ModelLocation.App -> "This app only"
        else -> sharedTreeUri(context)?.let { ExternalModelStore.folderLabel(it) } ?: "This app only"
    }
}
