package com.stickers.bank.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Full-width bottom-bar outline whose top edge sweeps UP to a horn at each screen edge
 * (like liquid climbing the walls — "capillary"). Continuous-curvature cubic transitions,
 * not plain rounded corners: the tips sit at the very edges (y=0) and the middle rests at
 * [flare] below them.
 */
class CapillaryBarShape(private val flare: Dp, private val spread: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val f = with(density) { flare.toPx() }
        val s = with(density) { spread.toPx() }.coerceAtMost(size.width / 2f)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            cubicTo(0f, f * 0.62f, s * 0.5f, f, s, f)          // left horn → flat top
            lineTo(w - s, f)
            cubicTo(w - s * 0.5f, f, w, f * 0.62f, w, 0f)      // flat top → right horn
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A "bucket"/oval panel: the top edge is narrower than the bottom (inset by [topInset] on
 * each side) with generously rounded corners, so it reads like an egg/oval that widens as
 * it goes down. Used for the Create-tab hero.
 */
class BucketShape(private val topInset: Dp, private val corner: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val ti = with(density) { topInset.toPx() }.coerceAtMost(size.width / 2f - 1f)
        val r = with(density) { corner.toPx() }
        val w = size.width
        val h = size.height
        val path = Path().apply {
            roundedPolygon(
                listOf(Offset(ti, 0f), Offset(w - ti, 0f), Offset(w, h), Offset(0f, h)),
                r,
            )
        }
        return Outline.Generic(path)
    }
}

/**
 * Apple-style **continuous corner** (squircle / superellipse) rounded rectangle — the corner
 * follows |x|^n + |y|^n = r^n rather than a circular arc, so edges flow into corners smoothly.
 * [smoothness] ~2 = circle, ~4 = iOS squircle, higher = squarer with softer transition.
 */
class SquircleShape(private val radius: Dp, private val smoothness: Double = 4.0) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            superellipseCorner(w - r, r, -90.0, r, smoothness)
            lineTo(w, h - r)
            superellipseCorner(w - r, h - r, 0.0, r, smoothness)
            lineTo(r, h)
            superellipseCorner(r, h - r, 90.0, r, smoothness)
            lineTo(0f, r)
            superellipseCorner(r, r, 180.0, r, smoothness)
            close()
        }
        return Outline.Generic(path)
    }
}

private fun Path.superellipseCorner(cx: Float, cy: Float, startDeg: Double, r: Float, n: Double, steps: Int = 16) {
    val e = 2.0 / n
    for (i in 0..steps) {
        val rad = Math.toRadians(startDeg + 90.0 * i / steps)
        val c = cos(rad)
        val s = sin(rad)
        val dx = r * sign(c) * abs(c).pow(e)
        val dy = r * sign(s) * abs(s).pow(e)
        lineTo(cx + dx.toFloat(), cy + dy.toFloat())
    }
}

/** Builds a closed path through [pts] with each corner rounded by up to [radius]. */
private fun Path.roundedPolygon(pts: List<Offset>, radius: Float) {
    val n = pts.size
    for (i in 0 until n) {
        val cur = pts[i]
        val prev = pts[(i - 1 + n) % n]
        val next = pts[(i + 1) % n]
        val d1x = prev.x - cur.x
        val d1y = prev.y - cur.y
        val l1 = hypot(d1x.toDouble(), d1y.toDouble()).toFloat().coerceAtLeast(0.01f)
        val d2x = next.x - cur.x
        val d2y = next.y - cur.y
        val l2 = hypot(d2x.toDouble(), d2y.toDouble()).toFloat().coerceAtLeast(0.01f)
        val r = minOf(radius, l1 / 2f, l2 / 2f)
        val aX = cur.x + d1x / l1 * r
        val aY = cur.y + d1y / l1 * r
        val bX = cur.x + d2x / l2 * r
        val bY = cur.y + d2y / l2 * r
        if (i == 0) moveTo(aX, aY) else lineTo(aX, aY)
        quadraticBezierTo(cur.x, cur.y, bX, bY)
    }
    close()
}
