package com.stickers.bank.export

import android.content.Context
import com.stickers.bank.data.AppJson
import java.io.File

const val WHATSAPP_AUTHORITY = "com.stickers.bank.stickercontentprovider"

@kotlinx.serialization.Serializable
data class WaSticker(
    val imageFileName: String,
    val emojis: List<String> = listOf("😀"),
)

@kotlinx.serialization.Serializable
data class WaStickerPack(
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val animatedStickerPack: Boolean = false,
    val imageDataVersion: String = "1",
    val avoidCache: Boolean = false,
    val publisherEmail: String = "",
    val publisherWebsite: String = "",
    val privacyPolicyWebsite: String = "",
    val licenseAgreementWebsite: String = "",
    val playStoreLink: String = "",
    val iosAppLink: String = "",
    val stickers: List<WaSticker> = emptyList(),
)

/**
 * Persists built WhatsApp packs on disk under filesDir/wa_packs/<identifier>/, each
 * with its webp stickers, tray.png, and a pack.json descriptor. The content provider
 * reads back from here so packs survive process restarts.
 */
object WaPackStore {
    private fun root(context: Context) = File(context.filesDir, "wa_packs").apply { mkdirs() }
    fun packDir(context: Context, identifier: String) =
        File(root(context), identifier).apply { mkdirs() }

    private fun descriptor(context: Context, identifier: String) =
        File(packDir(context, identifier), "pack.json")

    fun save(context: Context, pack: WaStickerPack) {
        descriptor(context, pack.identifier)
            .writeText(AppJson.encodeToString(WaStickerPack.serializer(), pack))
    }

    fun load(context: Context, identifier: String): WaStickerPack? = runCatching {
        if (identifier.isBlank() || identifier.contains('/') || identifier.startsWith(".")) return null
        val f = File(File(root(context), identifier), "pack.json")
        if (!f.exists()) null
        else AppJson.decodeFromString(WaStickerPack.serializer(), f.readText())
    }.getOrNull()

    /**
     * Every pack we can still stand behind.
     *
     * WhatsApp validates EVERY pack the provider reports, not just the one being added — so a
     * single leftover broken pack (files cleared, an export that died halfway) makes an
     * unrelated add fail with "There's a problem with the sticker pack". Packs whose files are
     * no longer on disk are therefore filtered out instead of being advertised.
     */
    fun loadAll(context: Context, servableOnly: Boolean = true): List<WaStickerPack> =
        root(context).listFiles()?.mapNotNull { dir ->
            if (!dir.isDirectory || dir.name.endsWith(".tmp")) null
            else load(context, dir.name)?.takeIf { !servableOnly || isServeable(context, it) }
        } ?: emptyList()

    /** Descriptor, tray icon and every sticker file present, with a legal sticker count. */
    fun isServeable(context: Context, pack: WaStickerPack): Boolean {
        if (pack.stickers.size !in 3..30) return false
        val dir = File(root(context), pack.identifier)
        if (File(dir, pack.trayImageFile).length() <= 0L) return false
        return pack.stickers.all { File(dir, it.imageFileName).length() > 0L }
    }

    /** A file of a pack, for the content provider. Never creates anything, and refuses names
     * that would step outside the pack's own folder. */
    fun file(context: Context, identifier: String, fileName: String): File {
        fun unsafe(s: String) = s.isBlank() || s.contains('/') || s.contains('\\') || s.startsWith(".")
        if (unsafe(identifier) || unsafe(fileName)) return File(root(context), "missing")
        return File(File(root(context), identifier), fileName)
    }
}
