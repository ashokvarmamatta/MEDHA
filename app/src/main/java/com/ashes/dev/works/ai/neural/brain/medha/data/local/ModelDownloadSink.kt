package com.ashes.dev.works.ai.neural.brain.medha.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Where a model download is written, so the retry/resume loop in ChatViewModel stays one
 * implementation regardless of whether the bytes land in MEDHA's private folder or in the shared
 * folder the user granted through the folder picker.
 *
 * Both destinations support the same three things the download loop needs: how much of a partial
 * is already on disk, a stream positioned at that offset, and an atomic-ish rename at the end.
 * Notably the SAF variant needs **no storage permission** — the tree grant from the picker covers
 * creating and writing documents inside it.
 */
interface ModelDownloadSink {

    /** Human-readable destination, for logs. */
    val label: String

    /** Bytes of a previous interrupted attempt already written. 0 when starting fresh. */
    fun partialBytes(): Long

    /** Throw away the partial — used when the server ignores our Range header. */
    fun discardPartial()

    /** A stream positioned at [offset]. The caller closes it. */
    fun openAt(offset: Long): OutputStream

    /** Promote the finished partial to its real name. */
    fun finish(): Boolean

    /** Free space at the destination, or -1 when it cannot be determined. */
    fun usableSpace(): Long
}

/** Writes into MEDHA's private `filesDir/medha_models`. */
class FileDownloadSink(
    private val dir: File,
    private val fileName: String,
    tmpExt: String
) : ModelDownloadSink {

    private val tmp = File(dir, "$fileName$tmpExt")
    private val target = File(dir, fileName)

    override val label: String get() = dir.absolutePath

    override fun partialBytes(): Long = if (tmp.exists()) tmp.length() else 0L

    override fun discardPartial() { tmp.delete() }

    override fun openAt(offset: Long): OutputStream = FileOutputStream(tmp, offset > 0)

    override fun finish(): Boolean {
        if (tmp.renameTo(target)) return true
        // Cross-filesystem fallback
        return runCatching { tmp.copyTo(target, overwrite = true); tmp.delete(); true }
            .getOrDefault(false)
    }

    override fun usableSpace(): Long = runCatching { dir.usableSpace }.getOrDefault(-1L)
}

/**
 * Writes into the user's shared folder through the Storage Access Framework.
 *
 * Resume works because the document is opened "rw" and seeked with `Os.lseek` — an append-only
 * stream could not continue a partial transfer.
 */
class SafDownloadSink(
    private val context: Context,
    private val treeUri: Uri,
    private val fileName: String,
    private val tmpExt: String
) : ModelDownloadSink {

    private val tmpName = "$fileName$tmpExt"

    override val label: String get() = ExternalModelStore.folderLabel(treeUri)

    private fun tmpUri(create: Boolean): Uri? =
        if (create) ExternalModelStore.createOrFind(context, treeUri, tmpName)
        else ExternalModelStore.findChild(context, treeUri, tmpName)

    override fun partialBytes(): Long =
        tmpUri(create = false)?.let { ExternalModelStore.documentLength(context, it) } ?: 0L

    override fun discardPartial() {
        tmpUri(create = false)?.let { ExternalModelStore.deleteDocument(context, it) }
    }

    override fun openAt(offset: Long): OutputStream {
        val uri = tmpUri(create = true) ?: error("Could not create $tmpName in the shared folder")
        val (pfd, stream) = ExternalModelStore.openForAppend(context, uri, offset)
            ?: error("Could not open $tmpName for writing")
        // Closing the stream must also release the descriptor, or the fd leaks per attempt.
        return object : OutputStream() {
            override fun write(b: Int) = stream.write(b)
            override fun write(b: ByteArray, off: Int, len: Int) = stream.write(b, off, len)
            override fun flush() = stream.flush()
            override fun close() {
                try { stream.close() } finally { pfd.close() }
            }
        }
    }

    override fun finish(): Boolean {
        val uri = tmpUri(create = false) ?: return false
        // A stale file under the final name would make the rename fail or duplicate.
        ExternalModelStore.findChild(context, treeUri, fileName)
            ?.let { ExternalModelStore.deleteDocument(context, it) }
        return ExternalModelStore.rename(context, uri, fileName) != null
    }

    override fun usableSpace(): Long = ExternalModelStore.usableSpace(treeUri)
}
