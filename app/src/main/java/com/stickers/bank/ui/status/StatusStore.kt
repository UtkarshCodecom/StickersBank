package com.stickers.bank.ui.status

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/** Which WhatsApp the statuses come from — each keeps its own Status folder (and its own grant). */
enum class StatusSource(val label: String, val pkg: String, private val folder: String) {
    WHATSAPP("WhatsApp", "com.whatsapp", "WhatsApp"),
    BUSINESS("WP Business", "com.whatsapp.w4b", "WhatsApp Business");

    /** Where WhatsApp keeps statuses you've viewed: under Android/media since Android 11, at the
     * storage root before that. */
    val statusDocId: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "primary:Android/media/$pkg/$folder/Media/.Statuses"
        } else {
            "primary:$folder/Media/.Statuses"
        }
}

data class StatusItem(val uri: Uri, val id: String, val name: String, val mime: String, val modified: Long) {
    val isVideo: Boolean get() = mime.startsWith("video/")
    /** A voice status. */
    val isAudio: Boolean get() = mime.startsWith("audio/")

    companion object {
        private val IMAGE_EXT = mapOf("jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png", "webp" to "image/webp", "gif" to "image/gif", "heic" to "image/heic")
        private val VIDEO_EXT = mapOf("mp4" to "video/mp4", "3gp" to "video/3gpp", "mkv" to "video/x-matroska", "webm" to "video/webm", "mov" to "video/quicktime")
        private val AUDIO_EXT = mapOf("opus" to "audio/ogg", "ogg" to "audio/ogg", "m4a" to "audio/mp4", "aac" to "audio/aac", "mp3" to "audio/mpeg", "amr" to "audio/amr")

        /** The type to treat a status file as — from its name when the folder reports something
         * generic (some phones say application/octet-stream for every file in there), so no
         * status is dropped just because its type wasn't recognised. Null = not a status. */
        fun mimeFor(name: String, reported: String): String? {
            if (reported.startsWith("image/") || reported.startsWith("video/") || reported.startsWith("audio/")) return reported
            val ext = name.substringAfterLast('.', "").lowercase()
            return IMAGE_EXT[ext] ?: VIDEO_EXT[ext] ?: AUDIO_EXT[ext]
        }
    }
}

/**
 * Reads WhatsApp's Status folder and saves from it. Since Android 11 another app's media folder
 * can't be read with a storage permission at all — the user grants this ONE folder once through
 * the system folder picker (opened straight onto it, see [initialUri]), and the grant is kept
 * across restarts. No broad storage permission is needed for reading, and saving goes through
 * MediaStore (no permission on Android 10+).
 */
class StatusStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("status_saver", Context.MODE_PRIVATE)

    /** The picker opens here, so the user only has to tap "Use this folder". */
    fun initialUri(source: StatusSource): Uri =
        DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, source.statusDocId)

    /** A folder the user picked is only useful if it really is the Status folder. */
    fun isStatusFolder(tree: Uri): Boolean =
        runCatching { DocumentsContract.getTreeDocumentId(tree).endsWith(".Statuses") }.getOrDefault(false)

    /** The kept grant for [source], or null if there isn't one (or the user revoked it). */
    fun grantedTree(source: StatusSource): Uri? {
        val uri = prefs.getString(source.name, null)?.let(Uri::parse) ?: return null
        val stillGranted = app.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        return uri.takeIf { stillGranted }
    }

    fun keepGrant(source: StatusSource, tree: Uri) {
        app.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit().putString(source.name, tree.toString()).apply()
    }

    /** Images and videos in the folder, newest first. Blocking — call off the main thread. */
    fun list(tree: Uri): List<StatusItem> {
        val parent = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        val out = ArrayList<StatusItem>()
        runCatching {
            app.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                ),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    if (name.startsWith(".")) continue // .nomedia and half-written temp files
                    if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    val mime = StatusItem.mimeFor(name, c.getString(2).orEmpty()) ?: continue
                    out += StatusItem(DocumentsContract.buildDocumentUriUsingTree(tree, id), id, name, mime, c.getLong(3))
                }
            }
        }
        return out.sortedByDescending { it.modified }
    }

    fun savedIds(): Set<String> = prefs.getStringSet(KEY_SAVED, emptySet()).orEmpty()

    /** Copies a status into the gallery (Pictures or Movies / "Stickers Bank Status"). Blocking. */
    fun save(item: StatusItem): Boolean = runCatching {
        val cr = app.contentResolver
        val sub = "Stickers Bank Status"
        val directory = when {
            item.isVideo -> Environment.DIRECTORY_MOVIES
            item.isAudio -> Environment.DIRECTORY_MUSIC
            else -> Environment.DIRECTORY_PICTURES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = when {
                item.isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                item.isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/$sub")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val dest = cr.insert(collection, values) ?: return false
            val copied = cr.openInputStream(item.uri)?.use { input ->
                cr.openOutputStream(dest)?.use { output -> input.copyTo(output); true }
            } == true
            if (!copied) {
                cr.delete(dest, null, null)
                return false
            }
            cr.update(dest, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } else {
            // Android 9 and older: a plain public folder (needs WRITE_EXTERNAL_STORAGE, which the
            // screen asks for first on those versions).
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(directory), sub).apply { mkdirs() }
            val file = File(dir, item.name)
            cr.openInputStream(item.uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return false
            android.media.MediaScannerConnection.scanFile(app, arrayOf(file.absolutePath), arrayOf(item.mime), null)
        }
        prefs.edit().putStringSet(KEY_SAVED, savedIds() + item.id).apply()
        true
    }.getOrDefault(false)

    private companion object {
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        const val KEY_SAVED = "saved_ids"
    }
}
