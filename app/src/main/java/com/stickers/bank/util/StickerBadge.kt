package com.stickers.bank.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.stickers.bank.R
import com.stickers.bank.data.model.Sticker
import java.io.File

/**
 * Puts a small Stickers Bank logo in the top-right corner of stickers the user made themselves,
 * when they leave My Stickers for WhatsApp or a bank. Done on a COPY at that moment — the
 * user's own sticker stays clean in My Stickers, and editing it again never stacks a second
 * logo. Copies are cached under filesDir/branded, so a sticker is only stamped once.
 *
 * Catalog stickers are left alone; only custom (made-in-the-app) ones carry the badge.
 */
object StickerBadge {

    /** Badge width as a share of the sticker's, and its inset from the top-right corner. */
    private const val SIZE_FRACTION = 0.15f
    private const val MARGIN_FRACTION = 0.035f

    private fun dir(context: Context) = File(context.filesDir, "branded").apply { mkdirs() }

    private fun isBranded(context: Context, sticker: Sticker) =
        sticker.custom && File(sticker.image).parentFile?.absolutePath == dir(context).absolutePath

    /** [stickers] with every custom one swapped for its badged copy (same id, so banks and packs
     * still see the same sticker). Blocking and possibly slow for animations: call off the main
     * thread. A sticker that can't be stamped goes through as it was. */
    suspend fun branded(context: Context, stickers: List<Sticker>): List<Sticker> = stickers.map { brandedCopy(context, it) ?: it }

    private suspend fun brandedCopy(context: Context, sticker: Sticker): Sticker? {
        if (!sticker.custom || sticker.isVideoClip || isBranded(context, sticker)) return null
        val out = File(dir(context), "${sticker.id.replace(Regex("[^A-Za-z0-9_-]"), "_")}.webp")
        if (out.length() > 0L) return sticker.copy(image = out.absolutePath)
        val badge = BitmapFactory.decodeResource(context.resources, R.drawable.sticker_badge) ?: return null
        val ok = runCatching {
            if (sticker.isAnimatedSticker) {
                val bytes = BitmapUtils.downloadBytes(context, sticker) ?: return null
                val anim = AnimatedWebp.decode(bytes, maxSide = 512) ?: return null
                val encoded = VideoToWebp.encodeTransparent(
                    count = anim.frames.size, frameMs = anim.frameMs,
                    frameAt = { i, _ -> stamp(anim.frames[i], badge) },
                ) ?: return null
                out.writeBytes(encoded)
            } else {
                val bmp = BitmapUtils.loadBitmap(context, sticker) ?: return null
                BitmapUtils.writeWebp(stamp(BitmapUtils.fitSquare(bmp, 512), badge), out)
            }
            true
        }.getOrDefault(false)
        if (!ok) { out.delete(); return null }
        return sticker.copy(image = out.absolutePath)
    }

    /** A copy of [src] with the badge drawn in its top-right corner. */
    fun stamp(src: Bitmap, badge: Bitmap): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val size = out.width * SIZE_FRACTION
        val margin = out.width * MARGIN_FRACTION
        val right = out.width - margin
        Canvas(out).drawBitmap(
            badge, null, RectF(right - size, margin, right, margin + size),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        return out
    }
}
