package com.stickers.bank.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.flowOn

/**
 * On-device video → **animated WebP** encoder.
 *
 * Android has no animated-WebP encoder in the SDK, but it CAN encode a single still frame
 * ([Bitmap.compress] to WEBP). So we sample frames from the video, compress each to a still
 * WebP, pull out its `VP8 ` bitstream, and hand-mux those frames into an animated WebP
 * container (RIFF/WEBP + VP8X + ANIM + one ANMF per frame). Frames are made opaque and
 * center-cropped to 512×512, so each still is a plain lossy `VP8 ` chunk (no alpha), which
 * keeps the muxer simple. Output loops forever and is trimmed to stay under WhatsApp's 500KB.
 */
object VideoToWebp {

    private const val SIZE = 512
    private const val FPS = 12
    private const val MAX_FRAMES = 48
    private const val QUALITY = 70

    /** Duration + a strip of sampled frames, from a single [MediaMetadataRetriever] session —
     * see [loadPreview] for why this replaced separate firstFrame/durationMs/previewFrames calls. */
    data class Preview(val durationMs: Long, val frames: List<Pair<Long, Bitmap>>)

    /**
     * Everything the editor's timeline needs from a freshly-picked clip — duration and a strip
     * of sampled frames, each already 512² opaque — in ONE `MediaMetadataRetriever` session.
     * Picking a video used to open three separate sessions in sequence (a first-frame decode, a
     * duration read, then the frame strip — which itself read duration AGAIN internally): each
     * `setDataSource` re-parses the container, and on a `content://` URI can mean a fresh
     * file-descriptor round trip, so that added up to a genuinely slow "video added" step for no
     * benefit — the caller can just take `frames.first()` as its base-frame preview instead of
     * decoding frame 0 a second time. Blocking: call off the main thread.
     */
    fun loadPreview(context: Context, uri: Uri, count: Int = 24): Preview {
        if (count <= 0) return Preview(0L, emptyList())
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            val durMs = durationFrom(mmr, context, uri)
            if (durMs <= 0L) return Preview(0L, emptyList())
            val out = ArrayList<Pair<Long, Bitmap>>(count)
            val target = decodeSizeFor(mmr)
            // Sample at the MIDDLE of each slice rather than its edge: the last frame of a clip
            // is often black or a decode failure, and slice centres dodge both ends.
            for (i in 0 until count) {
                val ms = ((i + 0.5) / count * durMs).toLong().coerceIn(0L, durMs)
                val full = frameAt(mmr, ms * 1000L, target) ?: continue
                out.add(ms to squareOpaque(full))
                full.recycle()
            }
            Preview(durMs, out)
        } catch (e: Exception) {
            Preview(0L, emptyList())
        } finally {
            runCatching { mmr.release() }
        }
    }

    /**
     * [loadPreview], but streamed: the duration and the FIRST frame arrive almost at once, then
     * the rest of the strip fills in a few frames at a time — so the editor shows the clip the
     * moment it's picked instead of sitting on a blank canvas until all [count] frames are
     * decoded. Each emission is the full strip so far. Frames are decoded straight at sticker
     * size (see [frameAt]) rather than at the video's full resolution, which is most of the
     * speed-up. Runs on the IO dispatcher by itself.
     */
    fun previewFlow(context: Context, uri: Uri, count: Int = 24): kotlinx.coroutines.flow.Flow<Preview> =
        kotlinx.coroutines.flow.flow {
            val mmr = MediaMetadataRetriever()
            try {
                val durMs = runCatching {
                    mmr.setDataSource(context, uri)
                    durationFrom(mmr, context, uri)
                }.getOrDefault(0L)
                if (durMs <= 0L) {
                    emit(Preview(0L, emptyList()))
                    return@flow
                }
                val target = decodeSizeFor(mmr)
                val out = ArrayList<Pair<Long, Bitmap>>(count)
                // Sample at the MIDDLE of each slice rather than its edge: the last frame of a
                // clip is often black or a decode failure, and slice centres dodge both ends.
                for (i in 0 until count) {
                    val ms = ((i + 0.5) / count * durMs).toLong().coerceIn(0L, durMs)
                    val frame = frameAt(mmr, ms * 1000L, target) ?: continue
                    out.add(ms to squareOpaque(frame))
                    frame.recycle()
                    if (out.size == 1 || out.size % 4 == 0) emit(Preview(durMs, out.toList()))
                }
                emit(Preview(durMs, out.toList()))
            } finally {
                runCatching { mmr.release() }
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    /** Longest side to decode frames at so the SHORT side still covers the 512² sticker —
     * null when the clip's size isn't known (then frames decode at full size, as before). */
    private fun decodeSizeFor(mmr: MediaMetadataRetriever): Int? {
        val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
        val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
        if (w <= 0 || h <= 0 || minOf(w, h) <= SIZE) return null
        return (maxOf(w, h).toLong() * SIZE / minOf(w, h)).toInt()
    }

    /** Clip length in milliseconds, or 0 if it can't be read. Opens its own session — prefer
     * [loadPreview] when a frame strip is needed too, so the clip isn't parsed twice. */
    fun durationMs(context: Context, uri: Uri): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            durationFrom(mmr, context, uri)
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { mmr.release() }
        }
    }

    /** Duration from an ALREADY-open retriever's metadata, falling back to the track duration —
     * shared by every entry point so none of them has to open a second session just for this. */
    private fun durationFrom(mmr: MediaMetadataRetriever, context: Context, uri: Uri): Long {
        val meta = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        return if (meta > 0L) meta else trackDurationMs(context, uri)
    }

    /**
     * Duration straight off the video track. Some clips — screen recordings especially — carry
     * no container-level duration, and [MediaMetadataRetriever] then reports 0, which would
     * leave the editor with no timeline at all for an otherwise perfectly readable video.
     */
    private fun trackDurationMs(context: Context, uri: Uri): Long {
        val ex = android.media.MediaExtractor()
        return try {
            ex.setDataSource(context, uri, null)
            var best = 0L
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(android.media.MediaFormat.KEY_MIME)?.startsWith("video/") != true) continue
                if (f.containsKey(android.media.MediaFormat.KEY_DURATION)) {
                    best = maxOf(best, f.getLong(android.media.MediaFormat.KEY_DURATION) / 1000L)
                }
            }
            best
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { ex.release() }
        }
    }

    /**
     * Encode [uri] to an animated WebP byte array (≤ [maxBytes] where possible). Blocking —
     * call from a background dispatcher.
     */
    suspend fun encode(
        context: Context,
        uri: Uri,
        maxBytes: Int = 500_000,
        overlay: Bitmap? = null,
        colorMatrix: FloatArray? = null,
        /** Trim window from the editor's timeline. Defaults cover the whole clip. */
        startMs: Long = 0L,
        endMs: Long = Long.MAX_VALUE,
        /** The overlay for output frame `index`, `timeMs` into the sticker — for overlays that
         * move (animated stickers on top of the clip). Wins over [overlay] when it returns one. */
        overlayAt: (suspend (index: Int, timeMs: Long) -> Bitmap?)? = null,
        /** 0–1 as frames are done. */
        onProgress: (Float) -> Unit = {},
    ): ByteArray? {
        val mmr = MediaMetadataRetriever()
        val frames = ArrayList<ByteArray>() // each = a full `VP8 ` sub-chunk (incl. padding)
        // Editor overlay (text/stickers/drawings) + adjustment matrix, applied to every frame.
        val ov = overlay?.let { if (it.width == SIZE && it.height == SIZE) it else Bitmap.createScaledBitmap(it, SIZE, SIZE, true) }
        val cf = colorMatrix?.let { android.graphics.ColorMatrixColorFilter(it) }
        try {
            mmr.setDataSource(context, uri)
            // Same duration fallback the timeline uses — a clip with no container duration is
            // still perfectly encodable. Read off this same session, not a second one.
            val durMs = durationFrom(mmr, context, uri)
            // Only the trimmed window gets encoded — sampling still starts at `from`, so the
            // sticker begins exactly where the timeline's left handle was left.
            val from = startMs.coerceIn(0L, maxOf(0L, durMs))
            val to = endMs.coerceIn(from, if (durMs > 0L) durMs else endMs)
            val spanMs = (to - from).takeIf { it > 0L } ?: durMs
            val stepMs = 1000 / FPS
            val count = ((spanMs / stepMs).toInt()).coerceIn(1, MAX_FRAMES)
            val target = decodeSizeFor(mmr)
            for (i in 0 until count) {
                val full = frameAt(mmr, (from + i * stepMs) * 1000L, target) ?: continue
                val square = squareOpaque(full, cf)
                full.recycle()
                val frameOverlay = overlayAt?.invoke(i, i.toLong() * stepMs)?.let {
                    if (it.width == SIZE && it.height == SIZE) it else Bitmap.createScaledBitmap(it, SIZE, SIZE, true)
                } ?: ov
                if (frameOverlay != null) Canvas(square).drawBitmap(frameOverlay, 0f, 0f, null)
                val still = compressWebp(square, QUALITY)
                square.recycle()
                extractBitstreamChunk(still)?.let { frames.add(it) }
                onProgress((i + 1f) / count)
            }
        } catch (e: Exception) {
            return null
        } finally {
            runCatching { mmr.release() }
        }
        if (frames.isEmpty()) return null

        // Mux; if too big, uniformly drop frames (doubling each remaining frame's duration to
        // keep the timing) until it fits or we hit a small floor.
        var keep: List<ByteArray> = frames
        var durMs = 1000 / FPS
        while (true) {
            val out = mux(keep, durMs)
            if (out.size <= maxBytes || keep.size <= 6) return out
            keep = keep.filterIndexed { idx, _ -> idx % 2 == 0 }
            durMs *= 2
        }
    }

    /**
     * An animation with no video under it — animated stickers, text and drawings on a
     * TRANSPARENT background, like any other sticker. [frameAt] supplies each finished
     * frame (any size; it's fitted to 512²). Each frame keeps its alpha and replaces the one
     * before it outright (no blending), so moving parts never leave trails.
     */
    suspend fun encodeTransparent(
        count: Int,
        frameMs: Int,
        frameAt: suspend (index: Int, timeMs: Long) -> Bitmap?,
        maxBytes: Int = 500_000,
        /** 0–1 as frames are done. */
        onProgress: (Float) -> Unit = {},
    ): ByteArray? {
        val frames = ArrayList<ByteArray>()
        for (i in 0 until count) {
            val bmp = frameAt(i, i.toLong() * frameMs)
            if (bmp != null) {
                val square = if (bmp.width == SIZE && bmp.height == SIZE) bmp else BitmapUtils.fitSquare(bmp, SIZE)
                extractFrameData(compressWebp(square, QUALITY))?.let { frames.add(it) }
            }
            onProgress((i + 1f) / count)
        }
        if (frames.isEmpty()) return null
        var keep: List<ByteArray> = frames
        var durMs = frameMs
        while (true) {
            val out = mux(keep, durMs, alpha = true)
            if (out.size <= maxBytes || keep.size <= 6) return out
            keep = keep.filterIndexed { idx, _ -> idx % 2 == 0 }
            durMs *= 2
        }
    }

    // ---- frame helpers ----

    /**
     * OPTION_CLOSEST gives the frame actually at [timeUs], but it needs to decode forward from a
     * keyframe and some clips (screen recordings, long-GOP exports) simply fail at most
     * timestamps. Falling back to the nearest keyframe gives a slightly-off frame instead of no
     * frame at all, which is the difference between a working timeline and an empty one.
     */
    private fun frameAt(mmr: MediaMetadataRetriever, timeUs: Long, maxSide: Int? = null): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Decoding straight at sticker size skips building (then shrinking) a full 1080p/4K
            // bitmap per frame — several times faster, and far less memory.
            if (maxSide != null) {
                runCatching { mmr.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, maxSide, maxSide) }
                    .getOrNull()?.let { return it }
            }
            runCatching { mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) }
                .getOrNull()?.let { return it }
        }
        runCatching { mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) }
            .getOrNull()?.let { return it }
        return runCatching { mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC) }
            .getOrNull()
    }

    /** Center-crop to square and scale to 512×512 on an opaque (black) canvas, optionally recolored. */
    private fun squareOpaque(src: Bitmap, colorFilter: android.graphics.ColorMatrixColorFilter? = null): Bitmap {
        val side = minOf(src.width, src.height)
        val sx = (src.width - side) / 2
        val sy = (src.height - side) / 2
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { this.colorFilter = colorFilter }
        canvas.drawBitmap(src, Rect(sx, sy, sx + side, sy + side), Rect(0, 0, SIZE, SIZE), paint)
        return out
    }

    private fun compressWebp(bmp: Bitmap, quality: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        bmp.compress(fmt, quality, bos)
        return bos.toByteArray()
    }

    /** Pull the `VP8 ` (or `VP8L`) sub-chunk, including padding, out of a still WebP. */
    private fun extractBitstreamChunk(webp: ByteArray): ByteArray? {
        var off = 12 // skip RIFF(4) + size(4) + WEBP(4)
        while (off + 8 <= webp.size) {
            val id = String(webp, off, 4, Charsets.US_ASCII)
            val size = (webp[off + 4].toInt() and 0xFF) or
                ((webp[off + 5].toInt() and 0xFF) shl 8) or
                ((webp[off + 6].toInt() and 0xFF) shl 16) or
                ((webp[off + 7].toInt() and 0xFF) shl 24)
            val padded = size + (size and 1)
            val end = off + 8 + padded
            if (id == "VP8 " || id == "VP8L") return webp.copyOfRange(off, minOf(end, webp.size))
            off = end
        }
        return null
    }

    /** A still's image data for an ANMF frame: its `ALPH` chunk (if it has transparency) plus
     * `VP8 `, or a lone `VP8L` — each whole, with padding. */
    private fun extractFrameData(webp: ByteArray): ByteArray? {
        var off = 12
        var alph: ByteArray? = null
        while (off + 8 <= webp.size) {
            val id = String(webp, off, 4, Charsets.US_ASCII)
            val size = (webp[off + 4].toInt() and 0xFF) or
                ((webp[off + 5].toInt() and 0xFF) shl 8) or
                ((webp[off + 6].toInt() and 0xFF) shl 16) or
                ((webp[off + 7].toInt() and 0xFF) shl 24)
            val end = minOf(off + 8 + size + (size and 1), webp.size)
            when (id) {
                "ALPH" -> alph = webp.copyOfRange(off, end)
                "VP8L" -> return webp.copyOfRange(off, end)
                "VP8 " -> return (alph ?: ByteArray(0)) + webp.copyOfRange(off, end)
            }
            off = end
        }
        return null
    }

    // ---- WebP animation muxer ----

    private fun mux(frames: List<ByteArray>, durationMs: Int, alpha: Boolean = false): ByteArray {
        val body = ByteArrayOutputStream()

        // VP8X: animation flag (0x02, plus 0x10 when frames carry alpha), then canvas
        // (w-1, h-1) as 24-bit LE.
        val vp8x = ByteArrayOutputStream().apply {
            write(if (alpha) 0x12 else 0x02); write(0); write(0); write(0)
            u24(SIZE - 1); u24(SIZE - 1)
        }
        body.write(chunk("VP8X", vp8x.toByteArray()))

        // ANIM: background color (BGRA) + loop count (0 = forever).
        val anim = ByteArrayOutputStream().apply { u32(0); u16(0) }
        body.write(chunk("ANIM", anim.toByteArray()))

        // One ANMF per frame.
        for (f in frames) {
            val anmf = ByteArrayOutputStream().apply {
                u24(0); u24(0)               // frame x, y (units of 2px)
                u24(SIZE - 1); u24(SIZE - 1) // frame w-1, h-1
                u24(durationMs)              // duration ms
                // Flags: with transparency, "do not blend" (0x02) — each full frame replaces the
                // last instead of being drawn over it. Opaque frames cover everything anyway.
                write(if (alpha) 0x02 else 0)
                write(f)                     // the frame's VP8 sub-chunk
            }
            body.write(chunk("ANMF", anmf.toByteArray()))
        }

        val bodyBytes = body.toByteArray()
        return ByteArrayOutputStream().apply {
            fourcc("RIFF"); u32(4 + bodyBytes.size); fourcc("WEBP"); write(bodyBytes)
        }.toByteArray()
    }

    private fun chunk(id: String, payload: ByteArray): ByteArray = ByteArrayOutputStream().apply {
        fourcc(id); u32(payload.size); write(payload)
        if (payload.size and 1 == 1) write(0) // RIFF chunks are padded to even length
    }.toByteArray()

    private fun ByteArrayOutputStream.fourcc(s: String) = s.forEach { write(it.code) }
    private fun ByteArrayOutputStream.u16(v: Int) { write(v and 0xFF); write((v shr 8) and 0xFF) }
    private fun ByteArrayOutputStream.u24(v: Int) { write(v and 0xFF); write((v shr 8) and 0xFF); write((v shr 16) and 0xFF) }
    private fun ByteArrayOutputStream.u32(v: Int) {
        write(v and 0xFF); write((v shr 8) and 0xFF); write((v shr 16) and 0xFF); write((v shr 24) and 0xFF)
    }
}
