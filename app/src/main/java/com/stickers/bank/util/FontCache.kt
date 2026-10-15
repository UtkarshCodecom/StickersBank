package com.stickers.bank.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads and locally caches admin-uploaded custom fonts so the editor can use them exactly
 * like a bundled asset font. Keyed by URL — a font never changes once uploaded, so the cache
 * file just needs to exist, never to be refreshed.
 */
object FontCache {
    private fun cacheFile(context: Context, url: String): File {
        val ext = url.substringAfterLast('.', "ttf").substringBefore('?').ifBlank { "ttf" }
        val name = Integer.toHexString(url.hashCode()) + "." + ext
        return File(File(context.cacheDir, "fonts"), name)
    }

    /** Disk-only check — never touches the network. Used by non-suspend render paths. */
    fun cachedFileIfPresent(context: Context, url: String): File? =
        cacheFile(context, url).takeIf { it.exists() && it.length() > 0 }

    /** Returns the local file, downloading it first if this is the first time it's needed. */
    suspend fun localFile(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        val file = cacheFile(context, url)
        if (file.exists() && file.length() > 0) return@withContext file
        val bytes = BitmapUtils.downloadUrlBytes(url) ?: return@withContext null
        runCatching {
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            file
        }.getOrNull()
    }
}
