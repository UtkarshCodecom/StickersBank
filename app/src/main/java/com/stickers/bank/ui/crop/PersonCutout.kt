package com.stickers.bank.ui.crop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * "Remove background, keep the person" for the crop step. Uses Google's selfie-segmentation
 * model, which is bundled in the app and runs entirely on the phone: no upload, no internet, no
 * account or API key, no per-use cost. It scores every pixel for "is this a person", and that
 * score becomes the pixel's alpha, so hair and edges fade out softly instead of being cut jagged.
 */
object PersonCutout {

    /** The photo with everything but the person made transparent, and where the person is
     * (in the photo's own pixels). */
    class Cutout(val bitmap: Bitmap, val bounds: Rect)

    /** Segmenting runs at most this big; the mask is scaled back up (smoothly) to the photo. */
    private const val MAX_DIM = 1024

    /** Null when no person was found (or the model couldn't run). */
    suspend fun cut(src: Bitmap): Cutout? = withContext(Dispatchers.Default) {
        runCatching { cutBlocking(src) }.getOrNull()
    }

    private suspend fun cutBlocking(original: Bitmap): Cutout? {
        // A hardware bitmap can't be drawn into a software canvas — work from a plain copy.
        val src = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && original.config == Bitmap.Config.HARDWARE) {
            original.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        } else original
        val scale = min(1f, MAX_DIM.toFloat() / max(src.width, src.height))
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(src, (src.width * scale).roundToInt().coerceAtLeast(1), (src.height * scale).roundToInt().coerceAtLeast(1), true)
        } else src
        val segmenter = Segmentation.getClient(
            SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).build(),
        )
        try {
            val mask = segmenter.process(InputImage.fromBitmap(small, 0)).await()
            val mw = mask.width
            val mh = mask.height
            val buffer = mask.buffer.apply { rewind() }
            val alpha = IntArray(mw * mh)
            var minX = mw; var minY = mh; var maxX = -1; var maxY = -1
            var kept = 0
            for (i in 0 until mw * mh) {
                val confidence = buffer.float
                // A short ramp rather than a hard threshold: confident pixels stay fully, unsure
                // edge pixels (hair, motion blur) fade, the clear background goes.
                val a = ((confidence - 0.35f) / 0.35f).coerceIn(0f, 1f)
                val ai = (a * 255f).roundToInt()
                alpha[i] = ai shl 24
                if (ai >= 128) {
                    kept++
                    val x = i % mw; val y = i / mw
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
            // Under ~1% of the picture: there's no one there worth keeping.
            if (kept < mw * mh / 100) return null

            val maskBitmap = Bitmap.createBitmap(alpha, mw, mh, Bitmap.Config.ARGB_8888)
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            Canvas(out).apply {
                drawBitmap(src, 0f, 0f, null)
                drawBitmap(
                    maskBitmap, null, Rect(0, 0, src.width, src.height),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) },
                )
            }
            maskBitmap.recycle()
            if (small !== src) small.recycle()
            val kx = src.width.toFloat() / mw
            val ky = src.height.toFloat() / mh
            val bounds = Rect(
                (minX * kx).toInt(), (minY * ky).toInt(),
                ((maxX + 1) * kx).roundToInt().coerceAtMost(src.width), ((maxY + 1) * ky).roundToInt().coerceAtMost(src.height),
            )
            return Cutout(out, bounds)
        } finally {
            segmenter.close()
        }
    }
}
