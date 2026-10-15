package com.stickers.bank.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.util.BitmapUtils
import java.io.File
import java.util.ArrayList

sealed interface ExportResult {
    object Success : ExportResult
    data class Error(val message: String) : ExportResult
}

/**
 * Builds share/import intents. IMPORTANT: the actual launch must happen on the main
 * thread via an ActivityResultLauncher (see ExportUi). WhatsApp's add-pack screen only
 * appears when started for-result from the foreground; a background startActivity is
 * silently dropped ("opening WhatsApp but nothing happens").
 */
object StickerExporter {

    const val WHATSAPP = "com.whatsapp"
    const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    const val TELEGRAM = "org.telegram.messenger"

    const val ACTION_ENABLE_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    private const val EXTRA_PACK_ID = "sticker_pack_id"
    private const val EXTRA_PACK_AUTHORITY = "sticker_pack_authority"
    private const val EXTRA_PACK_NAME = "sticker_pack_name"

    private fun fileProviderAuthority(context: Context) = "${context.packageName}.fileprovider"

    fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, fileProviderAuthority(context), file)

    fun whatsAppPackage(context: Context): String? = when {
        isInstalled(context, WHATSAPP) -> WHATSAPP
        isInstalled(context, WHATSAPP_BUSINESS) -> WHATSAPP_BUSINESS
        else -> null
    }

    /**
     * Every WhatsApp flavour actually installed, plain one first. Two entries means the pack
     * could land in either app and the caller has to ask which — [whatsAppPackage] silently
     * prefers the plain one, which is wrong for anyone who does their stickers from Business.
     */
    fun whatsAppPackages(context: Context): List<String> =
        listOf(WHATSAPP, WHATSAPP_BUSINESS).filter { isInstalled(context, it) }

    /** Human-readable name for a package from [whatsAppPackages]. */
    fun whatsAppLabel(pkg: String): String =
        if (pkg == WHATSAPP_BUSINESS) "WhatsApp Business" else "WhatsApp"

    fun telegramInstalled(context: Context): Boolean = isInstalled(context, TELEGRAM)

    private fun emojisFor(sticker: Sticker): List<String> {
        val e = when {
            "love" in sticker.tags -> "❤️"
            "sad" in sticker.tags -> "😢"
            "angry" in sticker.tags -> "😠"
            "funny" in sticker.tags -> "😂"
            "cute" in sticker.tags -> "🥰"
            else -> "😀"
        }
        return listOf(e)
    }

    // ---- WhatsApp pack (real sticker pack via ContentProvider) ----

    /** Heavy: re-encodes stickers. Run off the main thread. */
    fun buildWhatsAppPack(
        context: Context,
        name: String,
        stickers: List<Sticker>,
        animated: Boolean = false,
        /** Fixed identifier (the growing "Stickers Bank" pack); null = a new pack for this selection. */
        identifier: String? = null,
    ): PackBuilder.Result? {
        if (stickers.size < PackBuilder.MIN_STICKERS) return null
        return PackBuilder.build(context, name, stickers, animated = animated, identifier = identifier)
    }

    /**
     * True when [whatsAppPackage] already has our pack [identifier] — WhatsApp's own check
     * (its sticker_whitelist_check provider, from the official sticker sample). Its add screen
     * has nothing to offer for a pack that's already there; an updated one refreshes by itself.
     * Blocking IPC: call off the main thread.
     */
    fun isPackAdded(context: Context, whatsAppPackage: String, identifier: String): Boolean = runCatching {
        val authority = "$whatsAppPackage.provider.sticker_whitelist_check"
        context.packageManager.resolveContentProvider(authority, android.content.pm.PackageManager.GET_META_DATA)
            ?: return false
        val uri = Uri.Builder().scheme("content").authority(authority).appendPath("is_whitelisted")
            .appendQueryParameter("authority", WHATSAPP_AUTHORITY)
            .appendQueryParameter("identifier", identifier)
            .build()
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow("result")) == 1
        } ?: false
    }.getOrDefault(false)

    /** Must be launched on the main thread, ideally for-result. */
    fun whatsAppAddPackIntent(pack: WaStickerPack, whatsAppPackage: String): Intent =
        Intent(ACTION_ENABLE_PACK).apply {
            putExtra(EXTRA_PACK_ID, pack.identifier)
            putExtra(EXTRA_PACK_AUTHORITY, WHATSAPP_AUTHORITY)
            putExtra(EXTRA_PACK_NAME, pack.name)
            setPackage(whatsAppPackage)
        }

    // ---- Telegram (official CREATE_STICKER_PACK import intent) ----

    private const val TELEGRAM_ACTION = "org.telegram.messenger.CREATE_STICKER_PACK"
    private const val TELEGRAM_EMOJIS_EXTRA = "STICKER_EMOJIS"
    private const val TELEGRAM_IMPORTER_EXTRA = "IMPORTER"
    private const val TELEGRAM_MAX = 120

    /**
     * Builds Telegram's real sticker-import intent (per DrKLO/TelegramStickersImport):
     * action CREATE_STICKER_PACK, a URI list in EXTRA_STREAM, and a parallel STICKER_EMOJIS
     * string list. Telegram then shows its "Create sticker set" UI. Heavy: run off the main thread.
     */
    fun buildTelegramImportIntent(
        context: Context,
        name: String,
        stickers: List<Sticker>,
        animated: Boolean = false,
    ): Intent? {
        val uris = ArrayList<Uri>()
        val emojis = ArrayList<String>()
        stickers.take(TELEGRAM_MAX).forEach { s ->
            val file = BitmapUtils.exportShareableWebp(context, s) ?: return@forEach
            uris += uriFor(context, file)
            emojis += emojisFor(s).joinToString("")
        }
        if (uris.isEmpty()) return null

        return Intent(TELEGRAM_ACTION).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putStringArrayListExtra(TELEGRAM_EMOJIS_EXTRA, emojis)
            putExtra(TELEGRAM_IMPORTER_EXTRA, context.packageName)
            // FLAG_GRANT_READ_URI_PERMISSION only covers the intent's data and ClipData — and
            // Android copies EXTRA_STREAM into ClipData only for the standard SEND actions, not a
            // custom one like this. Without the ClipData Telegram can't open our FileProvider
            // files, and the import fails. (Telegram's own sample sidesteps this by using
            // world-readable android.resource:// URIs, which ours can't be.)
            clipData = ClipData.newRawUri("", uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(TELEGRAM)
        }
    }

    // ---- Single sticker as a plain image share (chooser) ----

    fun shareSingleIntent(context: Context, sticker: Sticker, targetPackage: String? = null): Intent? {
        val file = BitmapUtils.exportShareableWebp(context, sticker) ?: return null
        val uri = uriFor(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/webp"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }
    }

    // ---- Animated (.mp4) → WhatsApp GIF ----

    private fun downloadToCache(context: Context, url: String, name: String): File? = runCatching {
        val dir = File(context.cacheDir, "gif").apply { mkdirs() }
        val file = File(dir, name)
        java.net.URL(url).openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
        file
    }.getOrNull()

    /** Share animated .mp4 stickers; WhatsApp shows the "GIF" option for short clips. */
    fun animatedGifIntent(context: Context, stickers: List<Sticker>, targetPackage: String?): Intent? {
        val uris = ArrayList<Uri>()
        stickers.take(PackBuilder.MAX_STICKERS).forEach { s ->
            if (s.image.startsWith("http")) {
                downloadToCache(context, s.image, "${s.id}.mp4")?.let { uris += uriFor(context, it) }
            }
        }
        if (uris.isEmpty()) return null
        val single = uris.size == 1
        return Intent(if (single) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/mp4"
            if (single) putExtra(Intent.EXTRA_STREAM, uris[0]) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }
    }
}
