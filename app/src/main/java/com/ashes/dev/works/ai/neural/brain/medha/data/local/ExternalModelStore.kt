package com.ashes.dev.works.ai.neural.brain.medha.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import com.ashes.dev.works.ai.neural.brain.medha.domain.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

/**
 * Reads models out of a folder the USER owns — typically `/sdcard/AIModels`, the folder Koeyomi
 * downloads into — so one copy of the weights serves every app instead of one copy each.
 *
 * Uses the Storage Access Framework, which costs **no manifest permission**: the user grants one
 * folder and that grant is persisted across reboots. The alternative, `MANAGE_EXTERNAL_STORAGE`,
 * needs a Play sensitive-permission declaration that is routinely refused for apps that are not
 * file managers.
 *
 * The interesting part is [openForEngine] — see its docs for how a SAF document is turned into
 * something LiteRT-LM's native loader can actually open.
 */
object ExternalModelStore {

    private const val TAG = "ExternalModelStore"

    /** Intent for the folder picker. The caller persists the returned tree URI. */
    fun pickFolderIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

    /**
     * Hold on to the folder grant so it survives process death and reboot. Without this the URI
     * works until the app is killed and then silently starts failing.
     *
     * Read AND write: the same grant that lets MEDHA load a model also lets it download one INTO
     * the folder, so the weights land where the user's other apps can reach them. Writing there
     * needs no storage permission either — SAF's grant covers creating documents in the tree.
     */
    fun persistPermission(context: Context, treeUri: Uri): Boolean = try {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "Could not persist folder permission: ${e.message}")
        false
    }

    /** True if we still hold a persisted read grant for [treeUri]. */
    fun hasPermission(context: Context, treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }

    /** True if the grant also allows creating and writing documents in the folder. */
    fun canWrite(context: Context, treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isWritePermission
        }

    /** A human-readable folder name for the settings row, e.g. "AIModels". */
    fun folderLabel(treeUri: Uri): String {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return treeUri.lastPathSegment ?: "Selected folder"
        // externalstorage doc ids look like "primary:AIModels"
        return docId.substringAfter(':', docId).ifBlank { docId }
    }

    /** How deep below the picked folder to look. 1 = the folder and its immediate subfolders. */
    private const val MAX_SCAN_DEPTH = 1

    /**
     * Every supported model in the chosen folder, including one level of subfolders — Koeyomi
     * unpacks some assets into `AIModels/<subdir>/`, so a root-only scan would miss them. The
     * depth is capped because a user may well point this at `Download`, and walking that whole
     * tree would be slow for no gain.
     *
     * Detection is by file presence, exactly like Koeyomi: a model copied in from a PC is
     * indistinguishable from one an app downloaded.
     */
    fun listModels(context: Context, treeUri: Uri): List<ModelInfo> {
        val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return emptyList()
        val out = mutableListOf<ModelInfo>()
        scanInto(context, treeUri, treeDocId, prefix = "", depth = 0, out = out)
        return out.sortedBy { it.fileName.lowercase() }
    }

    private fun scanInto(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        prefix: String,
        depth: Int,
        out: MutableList<ModelInfo>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val subDirs = mutableListOf<Pair<String, String>>() // docId to displayName
        try {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val docId = cursor.getString(idCol) ?: continue
                    if (cursor.getString(mimeCol) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        // Don't descend into another app's voice/speech pack. Koeyomi's
                        // kokoro-en-v0_19/ holds espeak-ng-data with hundreds of files, and
                        // querying it over SAF is slow enough to delay engine start for nothing.
                        if (depth < MAX_SCAN_DEPTH && ModelInfo.isPlausibleModelDir(name)) {
                            subDirs += docId to name
                        }
                        continue
                    }
                    val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
                    if (!ModelInfo.isModelFile(name, size, sharedFolder = true)) continue
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    // Keep the subfolder in the shown name so two models called model.litertlm in
                    // different folders stay tellable apart.
                    out += ModelInfo.fromFileName(prefix + name, docUri.toString(), size, isShared = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Listing model folder failed: ${e.message}")
            return
        }
        subDirs.forEach { (docId, name) ->
            scanInto(context, treeUri, docId, "$prefix$name/", depth + 1, out)
        }
    }

    /** Picker for ONE model file, when the user would rather grant a single file than a folder. */
    fun pickFileIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

    /** Persist read access to a single picked model file so it survives a restart. */
    fun persistFilePermission(context: Context, uri: Uri): Boolean = try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Could not persist file permission: ${e.message}")
        false
    }

    /** Describe an individually-granted model file, or null if it is gone or not a model. */
    fun describeFile(context: Context, uri: Uri): ModelInfo? = try {
        context.contentResolver.query(
            uri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null, null, null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(0) ?: return null
            val size = if (cursor.isNull(1)) 0L else cursor.getLong(1)
            if (!ModelInfo.isModelFile(name, size, sharedFolder = true)) return null
            ModelInfo.fromFileName(name, uri.toString(), size, isShared = true)
        }
    } catch (e: Exception) {
        Log.w(TAG, "describeFile failed: ${e.message}")
        null
    }

    /**
     * A model opened in a form LiteRT-LM can load, plus whatever must stay alive for the engine's
     * lifetime. ALWAYS [close] this AFTER the engine is closed — never before, or the engine's
     * mmap is pulled out from under it.
     */
    class EngineHandle(
        val path: String,
        private val pfd: ParcelFileDescriptor?
    ) : Closeable {
        /** True when [path] is a real file rather than a `/proc/self/fd` alias. */
        val isDirectPath: Boolean get() = pfd == null
        override fun close() {
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Turn a SAF document into something `EngineConfig(modelPath = …)` accepts.
     *
     * `EngineConfig` takes a String that native code `open()`s and mmaps, so a `content://` URI
     * cannot be handed over directly — this is the reason Koeyomi reached for All-files access.
     * There are two ways around it that need no permission:
     *
     * 1. **Real path first.** An externalstorage document id (`primary:AIModels/x.litertlm`) maps
     *    deterministically onto `/storage/emulated/0/AIModels/x.litertlm`. If that file happens to
     *    be readable — it is MEDHA's own, or the device allows it — hand over the plain path. This
     *    is the simplest and fastest case.
     * 2. **File-descriptor alias.** SAF gives an open fd, and `/proc/self/fd/N` is a genuine path
     *    the native loader can `open()`. On Linux this re-opens the underlying file, so mmap works
     *    on a local document. The fd must stay open for as long as the engine holds the mapping,
     *    which is what [EngineHandle] is for.
     *
     * Returns null when the file is not reachable by a real readable path — the caller must then
     * copy it into app storage first.
     *
     * **The `/proc/self/fd/N` route does NOT work, and must not be reintroduced.** Measured on
     * device 2026-08-12: SAF hands back a perfectly good descriptor (correct 2,588,147,712 bytes),
     * but opening `/proc/self/fd/N` on Linux is not `dup()` — it re-opens the file through the
     * filesystem and runs a FRESH permission check. Without a storage permission that re-open is
     * refused, so logcat shows one
     * `MediaProvider: Permission to access file … is denied` per engine-create attempt and
     * LiteRT-LM reports `INVALID_ARGUMENT: Unsupported or unknown file format` because it never
     * read a byte. Every backend and context combination failed identically, which is what ruled
     * out the config as a cause. Koeyomi loads the same file only because it holds
     * MANAGE_EXTERNAL_STORAGE and passes a real path.
     */
    fun openForEngine(context: Context, documentUri: String): EngineHandle? {
        val uri = runCatching { Uri.parse(documentUri) }.getOrNull() ?: return null

        directPath(uri)?.let { candidate ->
            val f = File(candidate)
            if (f.isFile && f.canRead() && f.length() > 0) {
                Log.i(TAG, "External model resolved to a direct path")
                return EngineHandle(candidate, null)
            }
        }
        Log.i(TAG, "External model is not readable by path; it must be copied in first")
        return null
    }

    /**
     * Stream a shared-folder model into [dest], reporting 0..1 progress.
     *
     * Reading through SAF works fine — it is only the native loader's re-open that does not — so
     * the copy itself needs no permission. Returns false if it was cancelled or failed, having
     * cleaned up the partial file so a half-copied model is never mistaken for a real one.
     */
    suspend fun copyIn(
        context: Context,
        documentUri: String,
        dest: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(documentUri) }.getOrNull() ?: return@withContext false
        val total = sizeOf(context, documentUri)
        val tmp = File(dest.parentFile, dest.name + ".copying")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(1 shl 16)
                    var copied = 0L
                    var lastReport = 0L
                    while (true) {
                        ensureActive() // cancellation must not leave a running copy behind
                        val n = input.read(buffer)
                        if (n == -1) break
                        output.write(buffer, 0, n)
                        copied += n
                        if (total > 0 && copied - lastReport >= 4L * 1024 * 1024) {
                            onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                            lastReport = copied
                        }
                    }
                    output.flush()
                }
            } ?: return@withContext false

            if (total > 0 && tmp.length() != total) {
                Log.w(TAG, "Copy incomplete: ${tmp.length()}/$total")
                tmp.delete()
                return@withContext false
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.delete()
                return@withContext false
            }
            onProgress(1f)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Copy failed: ${e.message}")
            runCatching { tmp.delete() }
            false
        }
    }

    /**
     * Best-effort filesystem path for a document on the built-in "primary" volume or a removable
     * one. Returns null for providers with no filesystem backing.
     */
    private fun directPath(documentUri: Uri): String? {
        if (documentUri.authority != "com.android.externalstorage.documents") return null
        val docId = runCatching { DocumentsContract.getDocumentId(documentUri) }.getOrNull() ?: return null
        val volume = docId.substringBefore(':', "")
        val relative = docId.substringAfter(':', "")
        if (relative.isBlank()) return null
        return if (volume.equals("primary", ignoreCase = true)) {
            "/storage/emulated/0/$relative"
        } else {
            "/storage/$volume/$relative"
        }
    }

    // ── Writing into the shared folder ──────────────────────────────────────
    //
    // Downloading INTO the user's folder needs no storage permission either: the tree grant from
    // the picker covers DocumentsContract.createDocument, and "rw" gives a seekable descriptor so
    // an interrupted multi-gigabyte download can resume rather than restart.

    /** The document with this exact display name directly in [treeUri], or null. */
    fun findChild(context: Context, treeUri: Uri, displayName: String): Uri? {
        val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
        return try {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1).equals(displayName, ignoreCase = true)) {
                        return DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(0))
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "findChild failed: ${e.message}")
            null
        }
    }

    /** Create [displayName] in [treeUri], reusing the document if it already exists. */
    fun createOrFind(context: Context, treeUri: Uri, displayName: String): Uri? {
        findChild(context, treeUri, displayName)?.let { return it }
        val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        return try {
            // application/octet-stream, not a guessed type: some providers append an extension
            // that matches the mime, which would rename the model out from under us.
            DocumentsContract.createDocument(
                context.contentResolver, parent, "application/octet-stream", displayName
            )
        } catch (e: Exception) {
            Log.w(TAG, "createDocument failed for $displayName: ${e.message}")
            null
        }
    }

    /** Current size of a document, used as the resume offset. 0 when absent or unreadable. */
    fun documentLength(context: Context, uri: Uri): Long = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize.coerceAtLeast(0L) } ?: 0L
    } catch (_: Exception) {
        0L
    }

    /**
     * Open [uri] for writing, positioned at [offset] so a partial download continues instead of
     * starting over. `Os.lseek` on the "rw" descriptor is what makes resume possible — a plain
     * append-mode stream cannot be positioned.
     */
    fun openForAppend(context: Context, uri: Uri, offset: Long): Pair<ParcelFileDescriptor, java.io.OutputStream>? = try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "rw")
        if (pfd == null) null else {
            if (offset > 0) android.system.Os.lseek(pfd.fileDescriptor, offset, android.system.OsConstants.SEEK_SET)
            pfd to java.io.FileOutputStream(pfd.fileDescriptor)
        }
    } catch (e: Exception) {
        Log.w(TAG, "openForAppend failed: ${e.message}")
        null
    }

    /** Rename a finished `.medhatmp` document to its real name. */
    fun rename(context: Context, uri: Uri, newName: String): Uri? = try {
        DocumentsContract.renameDocument(context.contentResolver, uri, newName)
    } catch (e: Exception) {
        Log.w(TAG, "renameDocument failed: ${e.message}")
        null
    }

    fun deleteDocument(context: Context, uri: Uri): Boolean = try {
        DocumentsContract.deleteDocument(context.contentResolver, uri)
    } catch (_: Exception) {
        false
    }

    /**
     * Free space on the volume behind [treeUri], for the pre-download check. Falls back to -1
     * (unknown) for providers with no filesystem behind them, and the caller then skips the check
     * rather than refusing a download it cannot measure.
     */
    fun usableSpace(treeUri: Uri): Long {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return -1L
        val volume = docId.substringBefore(':', "")
        val root = if (volume.equals("primary", ignoreCase = true)) {
            File("/storage/emulated/0")
        } else {
            File("/storage/$volume")
        }
        return runCatching { root.usableSpace }.getOrDefault(-1L).takeIf { it > 0 } ?: -1L
    }

    /** Byte size of an external model, for the storage pre-check before a copy-in. */
    fun sizeOf(context: Context, documentUri: String): Long = try {
        context.contentResolver.openFileDescriptor(Uri.parse(documentUri), "r")
            ?.use { it.statSize.coerceAtLeast(0L) } ?: 0L
    } catch (_: Exception) {
        0L
    }
}
