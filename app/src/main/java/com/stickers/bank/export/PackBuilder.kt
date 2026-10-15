package com.stickers.bank.export

import android.content.Context
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.util.BitmapUtils
import java.io.File

object PackBuilder {

    const val MIN_STICKERS = 3
    const val MAX_STICKERS = 30

    /** WhatsApp's hard limit for a single animated sticker (bytes). Oversize ones are skipped. */
    const val WA_ANIMATED_MAX_BYTES = 500_000

    private fun sanitize(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "pack" }

    /**
     * True if these bytes really are an *animated* WebP: RIFF/WEBP container with an
     * extended VP8X header whose ANIM flag (bit 1) is set.
     *
     * We check the bytes rather than trusting the sticker's `animated` flag, because a
     * single static file inside a pack declared `animated_sticker_pack` makes WhatsApp
     * reject the whole pack.
     */
    private fun isAnimatedWebp(b: ByteArray): Boolean {
        if (b.size < 21) return false
        fun tag(at: Int) = String(b, at, 4, Charsets.US_ASCII)
        if (tag(0) != "RIFF" || tag(8) != "WEBP" || tag(12) != "VP8X") return false
        return (b[20].toInt() and 0x02) != 0
    }

    /** True if an animated WebP's canvas is exactly 512×512 — WhatsApp rejects the whole pack
     * over a single animated sticker of any other size. (VP8X stores width-1 / height-1 as
     * 24-bit little-endian at bytes 24 and 27.) */
    private fun is512Canvas(b: ByteArray): Boolean {
        if (b.size < 30) return false
        fun u24(at: Int) = (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or ((b[at + 2].toInt() and 0xFF) shl 16)
        return u24(24) + 1 == 512 && u24(27) + 1 == 512
    }

    /**
     * The identifier for a pack of exactly these stickers. Built from the stickers themselves,
     * not just the screen's name: identifiers used to be the sanitized name alone, so every
     * export from the same screen ("Fresh Drops", a category…) landed on ONE identifier and
     * replaced the pack the user had added before. Now a different selection is a different
     * pack, and re-sending the very same selection finds the pack it already made.
     */
    fun identifierFor(name: String, stickers: List<Sticker>, animated: Boolean): String {
        val key = stickers.map { it.id }.sorted().joinToString("|") + if (animated) "#anim" else ""
        val hash = java.security.MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(12)
        return sanitize(name).take(32) + "_" + hash + if (animated) "_anim" else ""
    }

    /** [base], or "[base] 2", "[base] 3"… — whichever no other pack is called yet, so packs from
     * the same screen are told apart in WhatsApp's tray. A pack that already exists under
     * [identifier] keeps the name it was added with. */
    private fun uniqueName(context: Context, base: String, identifier: String): String {
        val existing = WaPackStore.loadAll(context, servableOnly = false)
        existing.firstOrNull { it.identifier == identifier }?.let { return it.name }
        val taken = existing.map { it.name.lowercase() }.toSet()
        if (base.lowercase() !in taken) return base
        var n = 2
        while ("$base $n".lowercase() in taken) n++
        return "$base $n"
    }

    /** A built pack, plus how many of the stickers asked for couldn't go in it. */
    class Result(val pack: WaStickerPack, val skipped: Int)

    /**
     * Build a WhatsApp-compliant pack on disk from [stickers]. Re-encodes each to a
     * 512x512 webp under 100KB and generates a 96x96 tray icon. Returns null if fewer than
     * [MIN_STICKERS] usable stickers were produced.
     *
     * Every distinct selection becomes a NEW pack (see [identifierFor]) — adding stickers never
     * overwrites a pack that's already in WhatsApp.
     */
    fun build(
        context: Context,
        name: String,
        stickers: List<Sticker>,
        publisher: String = "Stickers Bank",
        animated: Boolean = false,
    ): Result? {
        val chosen = stickers.distinctBy { it.id }.take(MAX_STICKERS)
        val id = identifierFor(name, chosen, animated)
        val packName = uniqueName(context, name.ifBlank { "Stickers Bank" }, id).take(120)

        // The same selection sent again: the pack is already built (and may well be in WhatsApp
        // already), so hand over what's there instead of re-downloading everything.
        WaPackStore.load(context, id)?.let { existing ->
            if (!existing.avoidCache && WaPackStore.isServeable(context, existing)) return Result(existing, 0)
        }

        val dir = WaPackStore.packDir(context, id)
        // Build into a staging folder and only swap it in once we know the pack is complete.
        // Wiping `dir` up front meant a re-export that failed halfway (offline, say) destroyed
        // the pack the user had ALREADY added to WhatsApp, leaving it broken there.
        // A ".tmp" folder has no pack.json, so WaPackStore.loadAll never advertises it.
        val staging = File(dir.parentFile, "$id.tmp").apply { deleteRecursively(); mkdirs() }

        // Fresh file names on every build. WhatsApp keeps its own copy of each sticker by file
        // name, so reusing "0.webp" for a different picture in an updated pack could leave it
        // showing the old one.
        val version = System.currentTimeMillis()
        val stamp = version.toString(36)
        fun fileName(i: Int) = "${stamp}_$i.webp"
        val trayName = "tray_$stamp.png"

        val waStickers = ArrayList<WaSticker>()
        var trayWritten = false
        var skipped = 0

        chosen.forEach { sticker ->
            val fileName = fileName(waStickers.size)
            if (animated) {
                // Animated WebP: copy the original bytes untouched so the animation survives.
                // Each one must pass WhatsApp's own checks — genuinely animated (one static frame
                // invalidates an animated pack), a 512×512 canvas, under 500KB — or the whole
                // pack is refused, so one that doesn't is left out and counted instead.
                val bytes = BitmapUtils.downloadBytes(context, sticker)
                if (bytes == null || bytes.size > WA_ANIMATED_MAX_BYTES || !isAnimatedWebp(bytes) || !is512Canvas(bytes)) {
                    skipped++
                    return@forEach
                }
                File(staging, fileName).writeBytes(bytes)
                waStickers += WaSticker(imageFileName = fileName, emojis = emojiFor(sticker))
                if (!trayWritten) {
                    // Tray icon is a static first frame of the animation.
                    BitmapUtils.loadBitmap(context, sticker)?.let {
                        File(staging, trayName).writeBytes(BitmapUtils.toTrayPng(it))
                        trayWritten = true
                    }
                }
            } else {
                val bmp = BitmapUtils.loadBitmap(context, sticker)
                if (bmp == null) {
                    skipped++
                    return@forEach
                }
                BitmapUtils.writeWebp(bmp, File(staging, fileName))
                waStickers += WaSticker(imageFileName = fileName, emojis = emojiFor(sticker))
                if (!trayWritten) {
                    File(staging, trayName).writeBytes(BitmapUtils.toTrayPng(bmp))
                    trayWritten = true
                }
            }
        }

        if (waStickers.size < MIN_STICKERS || !trayWritten) {
            // Leave whatever was already installed under this identifier untouched.
            staging.deleteRecursively()
            return null
        }

        // Commit: swap the finished files in.
        dir.listFiles()?.forEach { it.delete() }
        staging.listFiles()?.forEach { it.renameTo(File(dir, it.name)) }
        staging.deleteRecursively()

        val pack = WaStickerPack(
            identifier = id,
            name = packName,
            publisher = publisher,
            trayImageFile = trayName,
            animatedStickerPack = animated,
            imageDataVersion = version.toString(),
            // Let WhatsApp keep its own copy of the stickers. With caching off it came back to
            // this app for every sticker every time the tray opened — waking the app in the
            // background each time — and the stickers broke whenever it couldn't answer.
            avoidCache = false,
            playStoreLink = "https://play.google.com/store/apps/details?id=com.stickers.bank",
            stickers = waStickers,
        )
        WaPackStore.save(context, pack)
        return Result(pack, skipped)
    }

    private fun emojiFor(sticker: Sticker): List<String> {
        // Map a couple of common mood tags to emojis; default to a smiley.
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
}
