package com.stickers.bank.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val GlassGold = Color(0xFFF5B301)

/**
 * "Preparing stickers…" — a frosted-glass card over the app, which Android 12+ blurs behind the
 * dialog (older phones get a more opaque dark glass instead, since they can't blur). Dark glass
 * rather than light, so the white text reads on every theme, light or dark. Can't be dismissed:
 * it goes away by itself once the work it covers is done.
 */
@Composable
fun PreparingPopup(title: String, subtitle: String? = null) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setDimAmount(if (canBlur) 0.25f else 0.45f)
            if (canBlur) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = 56 }
            }
        }
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .padding(horizontal = 56.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(if (canBlur) Color.Transparent else Color(0xE61A1A20))
                .background(Brush.verticalGradient(listOf(Color(0x8C26262E), Color(0x5C16161C))))
                // A brighter top edge fading out, like light catching the rim of a glass pane.
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.06f))), shape)
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) { ZigZagSpinner(Modifier.size(76.dp)) }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 18.dp))
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * A ring with a zig-zag edge that draws itself round while the whole thing spins, then its tail
 * catches up and erases it, and round again. A faint full ring underneath shows the track.
 */
@Composable
fun ZigZagSpinner(modifier: Modifier = Modifier, color: Color = GlassGold) {
    val transition = rememberInfiniteTransition(label = "zigzag")
    val spin by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "spin",
    )
    // 0→1: the head draws the ring round; 1→2: the tail follows and un-draws it.
    val phase by transition.animateFloat(
        0f, 2f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "phase",
    )
    Box(
        modifier.drawWithCache {
            val stroke = size.minDimension * 0.065f
            val radius = size.minDimension / 2f - stroke * 2f
            val amplitude = size.minDimension * 0.055f
            val points = 36 // 18 teeth: alternating out/in
            val center = Offset(size.width / 2f, size.height / 2f)
            val ring = Path().apply {
                for (i in 0..points) {
                    val angle = 2.0 * PI * i / points - PI / 2
                    val r = if (i % 2 == 0) radius + amplitude else radius - amplitude
                    val x = center.x + (r * cos(angle)).toFloat()
                    val y = center.y + (r * sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val measure = PathMeasure().apply { setPath(ring, false) }
            val length = measure.length
            val drawn = Path()
            val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val track = Stroke(width = stroke * 0.6f, join = StrokeJoin.Round)
            onDrawBehind {
                drawPath(ring, color.copy(alpha = 0.16f), style = track)
                val (from, to) = if (phase < 1f) 0f to FastOutSlowInEasing.transform(phase)
                else FastOutSlowInEasing.transform(phase - 1f) to 1f
                if (to - from > 0.002f) {
                    drawn.reset()
                    measure.getSegment(length * from, length * to, drawn, true)
                    rotate(spin, center) { drawPath(drawn, color, style = style) }
                }
            }
        },
    )
}
