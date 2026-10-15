package com.stickers.bank.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

/**
 * Reads an animated WebP into plain frames the editor can play and bake into a new animation.
 *
 * Android can PLAY animated WebP (AnimatedImageDrawable) but offers no way to step through its
 * frames, so this walks the container itself: each ANMF chunk's image data is re-wrapped as a
 * standalone still WebP, decoded with BitmapFactory, and composited onto a canvas the way the
 * format says (offset, alpha-blend or replace, dispose-to-transparent) — giving the same full
 * frames a player would show. The result is resampled onto an even grid of at most
 * [MAX_FRAMES] frames and stored at most [MAX_SIDE] px, to keep memory in check.
 */
object AnimatedWebp {

    private const val MAX_FRAMES = 36
    private const val MAX_SIDE = 384
    /** Output frame rate the editor's encoder uses — no point keeping frames finer than this. */
    private const val STEP_MS = 1000 / 12

    /** Evenly timed frames: frame k is shown from k*[frameMs] to (k+1)*[frameMs], looping. */
    class Frames(val frames: List<Bitmap>, val frameMs: Int) {
        val durationMs: Long get() = frames.size.toLong() * frameMs
        fun indexAt(timeMs: Long): Int =
            if (frames.size <= 1 || frameMs <= 0) 0 else ((timeMs.coerceAtLeast(0L) / frameMs) % frames.size).toInt()
        fun at(timeMs: Long): Bitmap = frames[indexAt(timeMs)]
    }

    private class Anmf(val x: Int, val y: Int, val w: Int, val h: Int, val durationMs: Int, val noBlend: Boolean, val dispose: Boolean, val data: ByteArray)

    /** Null when [bytes] isn't an animated WebP (or nothing in it could be decoded). */
    fun decode(bytes: ByteArray): Frames? = runCatching { decodeOrThrow(bytes) }.getOrNull()

    private fun decodeOrThrow(b: ByteArray): Frames? {
        if (b.size < 30 || tag(b, 0) != "RIFF" || tag(b, 8) != "WEBP") return null
        var canvasW = 0
        var canvasH = 0
        val frames = ArrayList<Anmf>()
        var off = 12
        while (off + 8 <= b.size) {
            val id = tag(b, off)
            val size = u32(b, off + 4)
            val start = off + 8
            if (size < 0 || start + size > b.size) break
            when (id) {
                "VP8X" -> { canvasW = u24(b, start + 4) + 1; canvasH = u24(b, start + 7) + 1 }
                "ANMF" -> frames += Anmf(
                    x = u24(b, start) * 2, y = u24(b, start + 3) * 2,
                    w = u24(b, start + 6) + 1, h = u24(b, start + 9) + 1,
                    durationMs = u24(b, start + 12),
                    noBlend = (b[start + 15].toInt() and 0x02) != 0,
                    dispose = (b[start + 15].toInt() and 0x01) != 0,
                    data = b.copyOfRange(start + 16, start + size),
                )
            }
            off = start + size + (size and 1)
        }
        if (frames.size < 2 || canvasW <= 0 || canvasH <= 0) return null

        // Players treat a 0 (or tiny) frame duration as 100ms; do the same so timing matches.
        val durations = frames.map { if (it.durationMs < 20) 100 else it.durationMs }
        val total = durations.sum().coerceAtLeast(1)
        val count = ceil(total / STEP_MS.toDouble()).toInt().coerceIn(1, MAX_FRAMES)
        val frameMs = (total / count).coerceAtLeast(1)

        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(canvasW, canvasH))
        val outW = (canvasW * scale).toInt().coerceAtLeast(1)
        val outH = (canvasH * scale).toInt().coerceAtLeast(1)

        val canvasBmp = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBmp)
        val over = Paint(Paint.FILTER_BITMAP_FLAG)
        val clear = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        val out = ArrayList<Bitmap>(count)
        var t = 0
        var nextSample = 0
        for ((i, f) in frames.withIndex()) {
            val rect = Rect(f.x, f.y, f.x + f.w, f.y + f.h)
            if (f.noBlend) canvas.drawRect(rect, clear)
            stillFrame(f)?.let { canvas.drawBitmap(it, null, rect, over); it.recycle() }
            val end = t + durations[i]
            // Keep a copy for every sample time that lands while this frame is on screen.
            while (nextSample < count && (nextSample + 0.5) * frameMs < end) {
                out += if (scale < 1f) Bitmap.createScaledBitmap(canvasBmp, outW, outH, true)
                else canvasBmp.copy(Bitmap.Config.ARGB_8888, false)
                nextSample++
            }
            if (f.dispose) canvas.drawRect(rect, clear)
            t = end
        }
        canvasBmp.recycle()
        if (out.isEmpty()) return null
        return Frames(out, frameMs)
    }

    /** One ANMF's image data as a still WebP BitmapFactory can read. */
    private fun stillFrame(f: Anmf): Bitmap? {
        val d = f.data
        var alph: ByteArray? = null
        var vp8: ByteArray? = null
        var vp8l: ByteArray? = null
        var off = 0
        while (off + 8 <= d.size) {
            val id = tag(d, off)
            val size = u32(d, off + 4)
            val end = (off + 8 + size + (size and 1)).coerceAtMost(d.size)
            val whole = d.copyOfRange(off, end)
            when (id) { "ALPH" -> alph = whole; "VP8 " -> vp8 = whole; "VP8L" -> vp8l = whole }
            off = end
        }
        val body = ByteArrayOutputStream()
        when {
            vp8l != null -> body.write(vp8l)
            vp8 != null && alph != null -> {
                // Lossy with a separate alpha plane needs the extended header to be read as such.
                val vp8x = ByteArrayOutputStream().apply {
                    write(0x10); write(0); write(0); write(0) // flags: has alpha
                    u24(f.w - 1); u24(f.h - 1)
                }.toByteArray()
                body.write(chunkHeader("VP8X", vp8x.size)); body.write(vp8x)
                body.write(alph); body.write(vp8)
            }
            vp8 != null -> body.write(vp8)
            else -> return null
        }
        val payload = body.toByteArray()
        val file = ByteArrayOutputStream().apply {
            write("RIFF".toByteArray(Charsets.US_ASCII)); u32(4 + payload.size)
            write("WEBP".toByteArray(Charsets.US_ASCII)); write(payload)
        }.toByteArray()
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return BitmapFactory.decodeByteArray(file, 0, file.size, opts)
    }

    private fun chunkHeader(id: String, size: Int) = ByteArrayOutputStream().apply {
        write(id.toByteArray(Charsets.US_ASCII)); u32(size)
    }.toByteArray()

    private fun tag(b: ByteArray, at: Int) = String(b, at, 4, Charsets.US_ASCII)
    private fun u24(b: ByteArray, at: Int) =
        (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or ((b[at + 2].toInt() and 0xFF) shl 16)
    private fun u32(b: ByteArray, at: Int) = u24(b, at) or ((b[at + 3].toInt() and 0xFF) shl 24)
    private fun ByteArrayOutputStream.u24(v: Int) { write(v and 0xFF); write((v shr 8) and 0xFF); write((v shr 16) and 0xFF) }
    private fun ByteArrayOutputStream.u32(v: Int) { u24(v); write((v shr 24) and 0xFF) }
}
