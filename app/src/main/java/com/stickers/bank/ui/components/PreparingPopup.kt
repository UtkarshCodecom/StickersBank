package com.stickers.bank.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val GlassGold = Color(0xFFFFC107)

/**
 * The app's loading popup: a frosted-glass card over the app (Android 12+ blurs what's behind
 * the dialog; older phones get a more opaque dark glass, since they can't blur), with the
 * rotating [PolygonProgress] and one gold line — "[title]  42%" when there's a [progress]
 * (0–1) to show, just the title while there isn't. Dark glass so it reads on every theme. Can't
 * be dismissed: it goes away by itself once the work it covers is done.
 */
@Composable
fun PreparingPopup(title: String, subtitle: String? = null, progress: Float? = null) {
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
        val shape = RoundedCornerShape(30.dp)
        // Eased toward each new value, so the shape and the number glide instead of jumping.
        val shown by animateFloatAsState((progress ?: 0f).coerceIn(0f, 1f), animationSpec = tween(300), label = "progress")
        Column(
            Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(if (canBlur) Color.Transparent else Color(0xE61A1A20))
                .background(Brush.verticalGradient(listOf(Color(0x8C26262E), Color(0x5C16161C))))
                // A brighter top edge fading out, like light catching the rim of a glass pane.
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))), shape)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PolygonProgress(if (progress != null) shown else null, Modifier.size(112.dp))
            Text(
                if (progress != null) "$title  ${(shown * 100).roundToInt()}%" else title,
                color = GlassGold, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 22.dp),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * A gold 12-sided outline that keeps turning. With a [progress] (0–1) the outline is drawn that
 * far round — the shape completes as the work does (and shows whole while still at 0, as a
 * "starting" state). Without one it draws itself round and un-draws again, over and over.
 */
@Composable
fun PolygonProgress(progress: Float?, modifier: Modifier = Modifier, color: Color = GlassGold, sides: Int = 12) {
    val transition = rememberInfiniteTransition(label = "polygon")
    val spin by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "spin",
    )
    // Indeterminate only — 0→1: the head draws the outline round; 1→2: the tail follows.
    val phase by transition.animateFloat(
        0f, 2f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "phase",
    )
    Box(
        modifier.drawWithCache {
            val stroke = size.minDimension * 0.05f
            val radius = size.minDimension / 2f - stroke
            val center = Offset(size.width / 2f, size.height / 2f)
            val outline = Path().apply {
                for (i in 0..sides) {
                    val angle = 2.0 * PI * i / sides - PI / 2
                    val x = center.x + (radius * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            val measure = PathMeasure().apply { setPath(outline, false) }
            val length = measure.length
            val drawn = Path()
            val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                val (from, to) = when {
                    progress != null && progress <= 0.004f -> 0f to 1f
                    progress != null -> 0f to progress
                    phase < 1f -> 0f to FastOutSlowInEasing.transform(phase)
                    else -> FastOutSlowInEasing.transform(phase - 1f) to 1f
                }
                if (to - from > 0.002f) {
                    drawn.reset()
                    measure.getSegment(length * from, length * to, drawn, true)
                    rotate(spin, center) { drawPath(drawn, color, style = style) }
                }
            }
        },
    )
}
