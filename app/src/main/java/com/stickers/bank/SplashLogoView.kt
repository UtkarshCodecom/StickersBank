package com.stickers.bank

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The launch animation, drawn entirely with Canvas — no image assets:
 *  Times below are at the original pace; everything runs [PACE]× slower than written.
 *  1. 0–900 ms: "SB", in the Gang of Three display face (assets/go3v2.ttf), is traced on left
 *     to right — the pen runs round each letter's outline — and its insides fill in as the
 *     tracing finishes;
 *  2. 1050–1650 ms: "STICKERS BANK", same face, wipes in left to right underneath;
 *     1000–1900 ms: a band of light sweeps across the letters;
 *  3. a 400 ms hold;
 *  4. the screen washes to white and "Welcome" fades in in script, holds, then [onAnimationEnd].
 *
 * Starts on its own the first time it's measured (see [onSizeChanged]).
 */
class SplashLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** Called once, after the whole sequence (including the welcome text) has played. */
    var onAnimationEnd: (() -> Unit)? = null

    private val logoTypeface: Typeface =
        runCatching { Typeface.createFromAsset(context.assets, "go3v2.ttf") }.getOrElse { Typeface.DEFAULT_BOLD }

    // The whole "SB" outline (for the fill), and the same outline split into its separate
    // contours — S's edge, then B's edge and its two holes — which the pen traces one after another.
    private val glyphs = Path()
    private var contours: List<Path> = emptyList()
    private var measures: List<PathMeasure> = emptyList()
    private var lengths = FloatArray(0)
    private var drawn: List<Path> = emptyList()

    // Widths are set from the letter size in buildLetters — a solid display face wants a fine
    // outline, not the thick single-line pen the hand-built letters used.
    private val glowPaint = strokePaint(1f).apply { alpha = GLOW_ALPHA }
    private val strokePaint = strokePaint(1f)
    // Wider than the outline so the band shows as a halo lighting up around the letters — the
    // letters themselves are pure white, so a white band confined to them would be invisible.
    private val shimmerPaint = strokePaint(1f)
    private val shimmerMatrix = Matrix()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }

    // "STICKERS BANK" under the mark, and the box it's revealed across.
    private val namePath = Path()
    private val nameBounds = RectF()
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }

    private val welcomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1C1C1C.toInt()
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 52f, resources.displayMetrics)
        typeface = runCatching { Typeface.createFromAsset(context.assets, "fonts/font_1.ttf") }
            .getOrElse { Typeface.create("cursive", Typeface.NORMAL) }
    }

    private var drawProgress = 0f
    private var fill = 0f                 // letters' insides, 0 → 1 as the tracing finishes
    private var nameReveal = 0f           // how much of "STICKERS BANK" is uncovered, left to right
    private var shimmerOffset = Float.NaN // fraction of the width; NaN = no band on screen
    private var wash = 0f                 // 0 = black, 1 = white
    private var welcome = 0f              // "Welcome" entrance, 0 → 1

    private var animators: AnimatorSet? = null
    private var cancelled = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        buildLetters(w, h)
        // Band spans ±0.25 of the width around its centre; moved per frame via the local matrix
        // instead of allocating a new gradient in onDraw.
        val half = w * 0.25f
        shimmerPaint.shader = LinearGradient(
            -half, 0f, half, 0f,
            intArrayOf(Color.argb(0, 192, 192, 192), Color.WHITE, Color.argb(0, 192, 192, 192)),
            null, Shader.TileMode.CLAMP,
        )
        if (animators == null) start()
    }

    /**
     * "SB" sized to the same box the letters always had — height min(w, h) · 0.38 — but kept
     * within 70% of the width; "STICKERS BANK" under it, about as wide; the two centred together.
     */
    private fun buildLetters(w: Int, h: Int) {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = logoTypeface; textSize = 100f }
        glyphs.reset()
        text.getTextPath("SB", 0, 2, 0f, 0f, glyphs)
        val bounds = RectF().also { glyphs.computeBounds(it, true) }
        if (bounds.isEmpty) return
        val s = min(w, h) * 0.38f
        val k = min(s / bounds.height(), w * 0.7f / bounds.width())

        namePath.reset()
        text.getTextPath(NAME, 0, NAME.length, 0f, 0f, namePath)
        val nb = RectF().also { namePath.computeBounds(it, true) }
        val markW = bounds.width() * k
        val markH = bounds.height() * k
        val kn = if (nb.isEmpty) 0f else min(markW * 1.15f, w * 0.84f) / nb.width()
        val nameH = nb.height() * kn
        val gap = s * 0.14f
        val top = h / 2f - (markH + gap + nameH) / 2f

        glyphs.transform(Matrix().apply {
            setTranslate(-bounds.centerX(), -bounds.centerY())
            postScale(k, k)
            postTranslate(w / 2f, top + markH / 2f)
        })
        if (!nb.isEmpty) {
            namePath.transform(Matrix().apply {
                setTranslate(-nb.centerX(), -nb.centerY())
                postScale(kn, kn)
                postTranslate(w / 2f, top + markH + gap + nameH / 2f)
            })
            namePath.computeBounds(nameBounds, true)
        }

        strokePaint.strokeWidth = (s * 0.02f).coerceAtLeast(3f)
        glowPaint.strokeWidth = s * 0.06f
        shimmerPaint.strokeWidth = s * 0.11f

        val parts = ArrayList<Path>()
        val lens = ArrayList<Float>()
        val pm = PathMeasure(glyphs, false)
        do {
            val len = pm.length
            if (len > 0f) {
                parts += Path().also { pm.getSegment(0f, len, it, true) }
                lens += len
            }
        } while (pm.nextContour())
        contours = parts
        measures = parts.map { PathMeasure(it, false) }
        lengths = lens.toFloatArray()
        drawn = parts.map { Path() }
    }

    /** Every timing below is written at the original speed and stretched by [PACE] — one knob
     * to make the whole sequence slower or faster without re-balancing its beats. */
    private fun pace(ms: Long) = (ms * PACE).toLong()

    private fun start() {
        val ease = AccelerateDecelerateInterpolator()
        val drawOn = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = pace(900)
            interpolator = ease
            addUpdateListener { drawProgress = it.animatedValue as Float; invalidate() }
        }
        // The insides fill in over the tail of the tracing, so the solid letters "arrive" as
        // the pen closes the last outline.
        val fillIn = ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = pace(750)
            duration = pace(350)
            interpolator = ease
            addUpdateListener { fill = it.animatedValue as Float; invalidate() }
        }
        val nameIn = ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = pace(1050)
            duration = pace(600)
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { nameReveal = it.animatedValue as Float; invalidate() }
        }
        val shimmer = ValueAnimator.ofFloat(-0.2f, 1.2f).apply {
            startDelay = pace(1000)
            duration = pace(900)
            interpolator = ease
            addUpdateListener { shimmerOffset = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { shimmerOffset = Float.NaN; invalidate() }
            })
        }
        // 1900 ms + the 400 ms hold → the wash to white starts at 2300.
        val washOut = ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = pace(2300)
            duration = pace(350)
            interpolator = LinearInterpolator()
            addUpdateListener { wash = it.animatedValue as Float; invalidate() }
        }
        val welcomeIn = ValueAnimator.ofFloat(0f, 1f).apply {
            startDelay = pace(2550)
            duration = pace(420)
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { welcome = it.animatedValue as Float; invalidate() }
        }
        // Nothing to animate — just holds "Welcome" on screen long enough to read before the end.
        val welcomeHold = ValueAnimator.ofFloat(0f, 1f).apply { startDelay = pace(2970); duration = pace(750) }

        animators = AnimatorSet().apply {
            playTogether(drawOn, fillIn, nameIn, shimmer, washOut, welcomeIn, welcomeHold)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) { if (!cancelled) onAnimationEnd?.invoke() }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animators?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        if (wash > 0f) canvas.drawColor(Color.argb((255 * wash).roundToInt(), 255, 255, 255))

        val logoAlpha = 1f - wash
        if (drawProgress > 0f && logoAlpha > 0f) {
            // The pen runs through the contours in order — S, then B — as one continuous trace.
            var remaining = lengths.sum() * drawProgress
            for (i in contours.indices) {
                drawn[i].reset()
                val len = min(remaining, lengths[i])
                if (len > 0f) measures[i].getSegment(0f, len, drawn[i], true)
                remaining -= lengths[i]
            }

            glowPaint.alpha = (GLOW_ALPHA * logoAlpha).roundToInt()
            drawn.forEach { canvas.drawPath(it, glowPaint) }

            if (!shimmerOffset.isNaN()) {
                shimmerMatrix.setTranslate(width * shimmerOffset, 0f)
                shimmerPaint.shader?.setLocalMatrix(shimmerMatrix)
                shimmerPaint.alpha = (SHIMMER_ALPHA * logoAlpha).roundToInt()
                drawn.forEach { canvas.drawPath(it, shimmerPaint) }
            }

            if (fill > 0f) {
                fillPaint.alpha = (255 * fill * logoAlpha).roundToInt()
                canvas.drawPath(glyphs, fillPaint)
            }

            strokePaint.alpha = (255 * logoAlpha).roundToInt()
            drawn.forEach { canvas.drawPath(it, strokePaint) }
        }

        if (nameReveal > 0f && logoAlpha > 0f) {
            // A wipe: the clip's right edge travels across the name, uncovering it left to right.
            val pad = nameBounds.height() * 0.2f
            canvas.save()
            canvas.clipRect(
                nameBounds.left - pad, nameBounds.top - pad,
                nameBounds.left - pad + (nameBounds.width() + 2 * pad) * nameReveal, nameBounds.bottom + pad,
            )
            namePaint.alpha = (255 * logoAlpha).roundToInt()
            canvas.drawPath(namePath, namePaint)
            canvas.restore()
        }

        if (welcome > 0f) {
            welcomePaint.alpha = (255 * welcome).roundToInt()
            val scale = 0.88f + 0.12f * welcome
            val cx = width / 2f
            val cy = height / 2f
            val baseline = cy - (welcomePaint.ascent() + welcomePaint.descent()) / 2f
            canvas.save()
            canvas.scale(scale, scale, cx, cy)
            canvas.drawText("Welcome", cx, baseline, welcomePaint)
            canvas.restore()
        }
    }

    private companion object {
        const val NAME = "STICKERS BANK"
        /** Slowed from the original pace so each beat (trace, fill, name, shimmer) can be seen. */
        const val PACE = 1.4f
        const val GLOW_ALPHA = 40
        const val SHIMMER_ALPHA = 150

        fun strokePaint(width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
    }
}
