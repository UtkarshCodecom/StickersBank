package com.stickers.bank.ui.studio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** One loop of every face animation, in ms — the saved sticker loops it forever. */
const val FACE_LOOP_MS = 2000

/** Frames in the saved animated sticker (12 fps over [FACE_LOOP_MS]). */
const val FACE_FRAMES = 24

/** The motion a cut-out face is played in. Every one is periodic over the loop, so it never jumps. */
enum class FaceTemplate(val label: String, val emoji: String) {
    BOUNCE("Bounce", "⬆️"),
    POP("Pop", "💥"),
    SPIN("Spin", "🌀"),
    SHAKE("Shake", "🫨"),
    FLOAT("Float", "🎈"),
    JELLY("Jelly", "🍮"),
    SWING("Swing", "🕺"),
    HEARTS("Hearts", "💖"),
    PARTY("Party", "🎉"),
    RAINBOW("Rainbow", "🌈"),
}

/**
 * Prepares the face and draws one frame of a [FaceTemplate] — plain Android Canvas, so the very
 * same code draws the live preview and every frame of the saved sticker. No network, no AI
 * beyond the on-device cut-out that made [face].
 */
object FaceRenderer {

    private const val TAU = (2 * PI).toFloat()

    /**
     * The person cut out of the photo, cropped close, given a white sticker outline, and kept at
     * most [maxSide] px. [bounds] = where the person is in [cutout] (null = use all of it).
     */
    fun prepare(cutout: Bitmap, bounds: Rect?, maxSide: Int = 460): Bitmap {
        val src = if (bounds != null && bounds.width() > 4 && bounds.height() > 4) {
            val pad = (max(bounds.width(), bounds.height()) * 0.04f).toInt()
            val r = Rect(
                (bounds.left - pad).coerceAtLeast(0), (bounds.top - pad).coerceAtLeast(0),
                (bounds.right + pad).coerceAtMost(cutout.width), (bounds.bottom + pad).coerceAtMost(cutout.height),
            )
            Bitmap.createBitmap(cutout, r.left, r.top, r.width(), r.height())
        } else cutout
        val scale = min(1f, maxSide / max(src.width, src.height).toFloat())
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
        } else src
        val stroke = max(4, (max(scaled.width, scaled.height) * 0.022f).toInt())
        val out = Bitmap.createBitmap(scaled.width + stroke * 2, scaled.height + stroke * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        // The outline: the silhouette stamped in white all round, then the face on top.
        val white = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) }
        for (i in 0 until 16) {
            val a = TAU * i / 16
            canvas.drawBitmap(scaled, stroke + cos(a) * stroke, stroke + sin(a) * stroke, white)
        }
        canvas.drawBitmap(scaled, stroke.toFloat(), stroke.toFloat(), null)
        return out
    }

    /** No person found: the photo itself as a rounded square, outlined like a sticker. */
    fun roundedPhoto(photo: Bitmap, maxSide: Int = 460): Bitmap {
        val side = min(photo.width, photo.height)
        val square = Bitmap.createBitmap(photo, (photo.width - side) / 2, (photo.height - side) / 2, side, side)
        val s = min(side, maxSide)
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawRoundRect(RectF(0f, 0f, s.toFloat(), s.toFloat()), s * 0.18f, s * 0.18f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(square, null, Rect(0, 0, s, s), paint)
        return prepare(out, null, maxSide)
    }

    /**
     * Draws [face] in [template] at loop position [t] (0–1) onto a square of [size] px, with
     * [caption] (if any) along the bottom. The canvas isn't cleared — the caller gives a clear one.
     */
    fun draw(canvas: Canvas, size: Float, face: Bitmap, template: FaceTemplate, t: Float, caption: String, typeface: Typeface) {
        val hasCaption = caption.isNotBlank()
        val box = size * (if (hasCaption) 0.68f else 0.80f)
        val fit = min(box / face.width, box / face.height)
        val fw = face.width * fit
        val fh = face.height * fit
        val cx = size / 2f
        val cy = size * (if (hasCaption) 0.43f else 0.5f)

        var dx = 0f; var dy = 0f; var sx = 1f; var sy = 1f; var rot = 0f
        var pivot = 0f // where it squashes/swings from: -0.5 top, 0 centre, 0.5 bottom (of the face)
        when (template) {
            FaceTemplate.BOUNCE -> {
                val u = (t * 2f) % 1f
                val h = sin(PI * u).toFloat()
                dy = -h * 0.12f * size
                val squash = (1f - h).pow(5)
                sx = 1f + 0.12f * squash; sy = 1f - 0.12f * squash; pivot = 0.5f
            }
            FaceTemplate.POP -> { val s = 1f + 0.12f * sin(TAU * t * 2f); sx = s; sy = s }
            FaceTemplate.SPIN -> rot = 360f * t
            FaceTemplate.SHAKE -> { rot = 9f * sin(TAU * t * 4f); dx = 0.025f * size * sin(TAU * t * 6f) }
            FaceTemplate.FLOAT -> { dy = 0.06f * size * sin(TAU * t); rot = 4f * sin(TAU * t) }
            FaceTemplate.JELLY -> { val w = sin(TAU * t * 2f); sx = 1f + 0.10f * w; sy = 1f - 0.10f * w; pivot = 0.5f }
            FaceTemplate.SWING -> { rot = 14f * sin(TAU * t); pivot = -0.5f }
            FaceTemplate.HEARTS -> { val b = heartbeat(t); sx = b; sy = b }
            FaceTemplate.PARTY -> { val u = (t * 2f) % 1f; dy = -sin(PI * u).toFloat() * 0.06f * size; rot = 6f * sin(TAU * t * 2f) }
            FaceTemplate.RAINBOW -> dy = 0.04f * size * sin(TAU * t)
        }

        if (template == FaceTemplate.HEARTS) particles(canvas, size, t, front = false)

        canvas.save()
        canvas.translate(cx + dx, cy + dy)
        val py = pivot * fh
        canvas.translate(0f, py)
        canvas.rotate(rot)
        canvas.scale(sx, sy)
        canvas.translate(0f, -py)
        val dst = RectF(-fw / 2f, -fh / 2f, fw / 2f, fh / 2f)
        if (template == FaceTemplate.RAINBOW) {
            // A glow round the silhouette, cycling through the rainbow over the loop.
            val hue = (t * 360f) % 360f
            val glow = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(Color.HSVToColor(floatArrayOf(hue, 0.85f, 1f)), PorterDuff.Mode.SRC_IN)
            }
            val r = size * 0.018f
            for (i in 0 until 12) {
                val a = TAU * i / 12
                canvas.drawBitmap(face, null, RectF(dst.left + cos(a) * r, dst.top + sin(a) * r, dst.right + cos(a) * r, dst.bottom + sin(a) * r), glow)
            }
        }
        canvas.drawBitmap(face, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restore()

        if (template == FaceTemplate.HEARTS) particles(canvas, size, t, front = true)
        if (template == FaceTemplate.PARTY) confetti(canvas, size, t)
        if (hasCaption) caption(canvas, size, caption, typeface)
    }

    /** Two quick beats per loop, like a heart: lub-dub, rest. */
    private fun heartbeat(t: Float): Float {
        fun g(c: Float): Float { val d = min(abs(t - c), 1f - abs(t - c)); return exp(-(d / 0.05f).pow(2)) }
        return 1f + 0.13f * (g(0.08f) + 0.7f * g(0.28f))
    }

    /** Hearts drifting up past the face (half behind it, half in front). */
    private fun particles(canvas: Canvas, size: Float, t: Float, front: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        for (i in 0 until 8) {
            if ((i % 2 == 0) != front) continue
            val p = (t + i / 8f) % 1f
            val x = size * (0.12f + 0.76f * frac(i * 0.618f)) + 0.03f * size * sin(TAU * (p * 2f + i * 0.3f))
            val y = size * (0.98f - 0.95f * p)
            paint.textSize = size * (0.06f + 0.035f * frac(i * 0.37f))
            paint.alpha = (255 * sin(PI * p)).toInt().coerceIn(0, 255)
            canvas.drawText(if (i % 3 == 0) "💖" else "❤️", x, y, paint)
        }
    }

    /** Confetti falling over the whole square. */
    private fun confetti(canvas: Canvas, size: Float, t: Float) {
        val colors = intArrayOf(0xFFFFC107.toInt(), 0xFFE5484D.toInt(), 0xFF3B82F6.toInt(), 0xFF46A758.toInt(), 0xFFEC4899.toInt(), 0xFF8B5CF6.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = size * 0.022f
        val h = size * 0.042f
        for (i in 0 until 18) {
            val p = (t + i / 18f) % 1f
            val x = size * frac(i * 0.618f + 0.05f) + 0.04f * size * sin(TAU * (p + i * 0.1f))
            val y = -0.05f * size + 1.1f * size * p
            paint.color = colors[i % colors.size]
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(720f * p + i * 40f)
            canvas.drawRect(-w / 2f, -h / 2f, w / 2f, h / 2f, paint)
            canvas.restore()
        }
    }

    /** The caption along the bottom: bold white with a dark outline, shrunk to fit one line. */
    private fun caption(canvas: Canvas, size: Float, text: String, typeface: Typeface) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = size * 0.105f
            color = Color.WHITE
        }
        val maxW = size * 0.92f
        val measured = fill.measureText(text)
        if (measured > maxW) fill.textSize *= maxW / measured
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = fill.textSize * 0.18f
            strokeJoin = Paint.Join.ROUND
            color = 0xFF1C1C1C.toInt()
        }
        val y = size * 0.94f
        canvas.drawText(text, size / 2f, y, stroke)
        canvas.drawText(text, size / 2f, y, fill)
    }

    private fun frac(v: Float) = v - kotlin.math.floor(v)
}
