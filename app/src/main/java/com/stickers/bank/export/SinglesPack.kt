package com.stickers.bank.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.stickers.bank.R
import com.stickers.bank.data.model.Sticker

/**
 * The running "Stickers Bank" pack in WhatsApp that single stickers go into — one now, another
 * tomorrow, both end up in the same pack. Its members are kept in the app as the "Stickers Bank"
 * bank (see BankRepository.addToSinglesPack), so they can be seen and removed there too.
 *
 * WhatsApp's rules shape the rest:
 *  - a pack is all-static or all-animated, so the two kinds get separate packs;
 *  - max [PackBuilder.MAX_STICKERS] per pack, so the 31st single starts "Stickers Bank 2";
 *  - min [PackBuilder.MIN_STICKERS], so a pack still short of that is padded (see [PackBuilder]).
 */
object SinglesPack {
    const val NAME = "Stickers Bank"

    /** Identifier, display name and members of the pack [sticker] belongs in, given every single
     * added so far ([all], oldest first, already including [sticker]). */
    data class Target(val identifier: String, val name: String, val stickers: List<Sticker>, val animated: Boolean)

    fun targetFor(all: List<Sticker>, sticker: Sticker): Target {
        val animated = sticker.isAnimatedSticker
        val sameKind = all.filter { it.isAnimatedSticker == animated }
        val page = (sameKind.indexOfFirst { it.id == sticker.id }.coerceAtLeast(0)) / PackBuilder.MAX_STICKERS
        val members = sameKind.chunked(PackBuilder.MAX_STICKERS).getOrElse(page) { listOf(sticker) }
        // Not "stickers_bank": that's what a multi-select export named "Stickers Bank" sanitizes
        // to, and WhatsApp would treat the two as one pack and overwrite one with the other.
        val identifier = "sb_singles" + (if (animated) "_anim" else "") + (if (page > 0) "_${page + 1}" else "")
        val name = NAME + (if (animated) " Animated" else "") + (if (page > 0) " ${page + 1}" else "")
        return Target(identifier, name, members, animated)
    }

    /**
     * Two Stickers Bank-branded stills that stand in while a static singles pack is still short
     * of WhatsApp's minimum: the round SB mark, and the name as a wordmark. Both are dropped
     * again as soon as the pack has enough real stickers of its own.
     */
    fun brandFillers(context: Context): List<Bitmap> = listOfNotNull(logoSticker(context), wordmarkSticker())

    private fun logoSticker(context: Context): Bitmap? = runCatching {
        val src = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground) ?: return null
        // The launcher foreground carries the adaptive-icon safe-zone margin — the art itself is
        // the middle ~68% — so crop to that before scaling it up to fill the sticker.
        val inset = (src.width * 0.16f).toInt()
        val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            src, Rect(inset, inset, src.width - inset, src.height - inset), Rect(16, 16, 496, 496),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        out
    }.getOrNull()

    private fun wordmarkSticker(): Bitmap {
        val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 92f
            color = Color.WHITE
        }
        // A thick dark outline, like most stickers — reads on both WhatsApp's light and dark chats.
        val outline = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = 18f
            strokeJoin = Paint.Join.ROUND
            color = Color.parseColor("#1C1C1C")
        }
        listOf("STICKERS" to 236f, "BANK" to 348f).forEach { (word, y) ->
            canvas.drawText(word, 256f, y, outline)
            canvas.drawText(word, 256f, y, fill)
        }
        return out
    }
}
