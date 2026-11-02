package com.stickers.bank.ui.studio

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.min

/** One spoken word and when it's said, in ms from the start of the video. */
data class CaptionWord(val text: String, val startMs: Long, val endMs: Long)

/** One caption on screen: a few words shown together. */
data class CaptionCue(val words: List<CaptionWord>) {
    val startMs: Long get() = words.first().startMs
    val endMs: Long get() = words.last().endMs
    val text: String get() = words.joinToString(" ") { it.text }

    /** The same cue with its text retyped: the new words share its time evenly. */
    fun withText(newText: String): CaptionCue {
        val parts = newText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return this
        val span = (endMs - startMs).coerceAtLeast(300L)
        val each = span / parts.size
        return CaptionCue(parts.mapIndexed { i, w -> CaptionWord(w, startMs + i * each, startMs + (i + 1) * each) })
    }
}

/**
 * Words → captions: a new caption starts at a pause, once a line would get long, or after ~3s,
 * so each one reads at a glance.
 */
fun groupIntoCues(words: List<CaptionWord>): List<CaptionCue> {
    val cues = ArrayList<CaptionCue>()
    var cur = ArrayList<CaptionWord>()
    fun flush() { if (cur.isNotEmpty()) { cues += CaptionCue(cur.toList()); cur = ArrayList() } }
    for (w in words) {
        if (cur.isNotEmpty()) {
            val chars = cur.sumOf { it.text.length + 1 } + w.text.length
            val gap = w.startMs - cur.last().endMs
            val dur = w.endMs - cur.first().startMs
            if (chars > 26 || gap > 700 || dur > 3000) flush()
        }
        cur += w
    }
    flush()
    return cues
}

/** How the captions look on the video. */
enum class CaptionStyle(val label: String) {
    CLASSIC("Classic"),
    BOX("Box"),
    KARAOKE("Karaoke"),
    POP("Pop"),
    NEON("Neon"),
}

/**
 * Draws the caption showing at a given moment, in a given [CaptionStyle] — plain Android Canvas,
 * so the same code draws the live preview over the player and every frame of the exported video.
 */
object CaptionRenderer {

    /** How long a caption stays up after its last word. */
    private const val LINGER_MS = 350L
    private const val GOLD = 0xFFFFC107.toInt()
    private const val POP_YELLOW = 0xFFFFD400.toInt()

    fun activeIndex(cues: List<CaptionCue>, t: Long): Int = cues.indexOfLast { t >= it.startMs && t <= it.endMs + LINGER_MS }

    /** Changes exactly when the drawn caption would — so the exporter only redraws when needed. */
    fun stateKey(cues: List<CaptionCue>, t: Long, style: CaptionStyle): Any {
        val i = activeIndex(cues, t)
        if (i < 0) return -1
        val cue = cues[i]
        return when (style) {
            CaptionStyle.KARAOKE -> i to cue.words.count { it.startMs <= t }
            CaptionStyle.POP -> {
                val wi = cue.words.indexOfLast { it.startMs <= t }.coerceAtLeast(0)
                Triple(i, wi, ((t - cue.words[wi].startMs) / 40L).coerceIn(0L, 5L))
            }
            CaptionStyle.NEON -> i to (t / 120L)
            else -> i
        }
    }

    fun draw(canvas: Canvas, w: Float, h: Float, cues: List<CaptionCue>, t: Long, style: CaptionStyle, bold: Typeface, display: Typeface) {
        val i = activeIndex(cues, t)
        if (i < 0) return
        val cue = cues[i]
        val base = min(w, h) * 0.062f
        if (style == CaptionStyle.POP) {
            drawPop(canvas, w, h, cue, t, base, display)
            return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = bold; textSize = base }
        val space = paint.measureText(" ")
        val lines = wrap(cue.words, paint, w * 0.86f, space)
        val lineH = base * 1.3f
        var y = h * 0.86f - (lines.size - 1) * lineH
        val neonColor = Color.HSVToColor(floatArrayOf(((t / 12L) % 360L).toFloat(), 0.75f, 1f))
        for (line in lines) {
            val widths = line.map { paint.measureText(it.text) }
            val lineW = widths.sum() + space * (line.size - 1)
            var x = (w - lineW) / 2f
            if (style == CaptionStyle.BOX) {
                val pad = base * 0.35f
                val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB3000000.toInt() }
                canvas.drawRoundRect(RectF(x - pad, y - base * 0.98f, x + lineW + pad, y + base * 0.34f), base * 0.3f, base * 0.3f, bg)
            }
            line.forEachIndexed { k, word ->
                val color = when {
                    style == CaptionStyle.KARAOKE && word.startMs <= t -> GOLD
                    else -> Color.WHITE
                }
                when (style) {
                    CaptionStyle.BOX -> { paint.color = Color.WHITE; paint.style = Paint.Style.FILL; canvas.drawText(word.text, x, y, paint) }
                    CaptionStyle.NEON -> {
                        paint.style = Paint.Style.FILL
                        paint.color = Color.WHITE
                        paint.setShadowLayer(base * 0.5f, 0f, 0f, neonColor)
                        canvas.drawText(word.text, x, y, paint)
                        canvas.drawText(word.text, x, y, paint)
                        paint.clearShadowLayer()
                    }
                    else -> outlined(canvas, word.text, x, y, paint, color)
                }
                x += widths[k] + space
            }
            y += lineH
        }
    }

    /** One word at a time, big, popping in — alternating yellow and white. */
    private fun drawPop(canvas: Canvas, w: Float, h: Float, cue: CaptionCue, t: Long, base: Float, display: Typeface) {
        val wi = cue.words.indexOfLast { it.startMs <= t }.coerceAtLeast(0)
        val word = cue.words[wi]
        val age = (t - word.startMs).coerceAtLeast(0L).toFloat()
        val scale = when {
            age < 90f -> 0.6f + 0.5f * (age / 90f)
            age < 180f -> 1.1f - 0.1f * ((age - 90f) / 90f)
            else -> 1f
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = display
            textSize = base * 1.7f
            textAlign = Paint.Align.CENTER
        }
        val text = word.text.uppercase()
        val maxW = w * 0.9f
        val measured = paint.measureText(text)
        if (measured > maxW) paint.textSize *= maxW / measured
        canvas.save()
        canvas.translate(w / 2f, h * 0.8f)
        canvas.scale(scale, scale)
        outlined(canvas, text, 0f, 0f, paint, if (wi % 2 == 0) POP_YELLOW else Color.WHITE)
        canvas.restore()
    }

    /** Text with a thick dark outline, the classic subtitle look that reads on any video. */
    private fun outlined(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = paint.textSize * 0.16f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = 0xFF111111.toInt()
        canvas.drawText(text, x, y, paint)
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawText(text, x, y, paint)
    }

    /** Words into lines no wider than [maxW]. */
    private fun wrap(words: List<CaptionWord>, paint: Paint, maxW: Float, space: Float): List<List<CaptionWord>> {
        val lines = ArrayList<List<CaptionWord>>()
        var cur = ArrayList<CaptionWord>()
        var curW = 0f
        for (word in words) {
            val ww = paint.measureText(word.text)
            if (cur.isNotEmpty() && curW + space + ww > maxW) {
                lines += cur; cur = ArrayList(); curW = 0f
            }
            curW += (if (cur.isEmpty()) 0f else space) + ww
            cur += word
        }
        if (cur.isNotEmpty()) lines += cur
        return lines
    }
}
