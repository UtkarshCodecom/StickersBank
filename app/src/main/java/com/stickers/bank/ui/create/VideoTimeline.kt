package com.stickers.bank.ui.create

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold

private val STRIP_HEIGHT = 56.dp
private val HANDLE_WIDTH = 14.dp

/** mm:ss.t — short enough to sit under a trim handle without wrapping. */
private fun formatMs(ms: Long): String {
    val totalTenths = (ms / 100).coerceAtLeast(0L)
    val m = totalTenths / 600
    val s = (totalTenths / 10) % 60
    val t = totalTenths % 10
    return if (m > 0) "%d:%02d.%d".format(m, s, t) else "%d.%ds".format(s, t)
}

/**
 * The animation editor's timeline: a filmstrip of the clip's own frames with draggable trim
 * handles at each end and a scrubbable playhead.
 *
 * Everything here works off [frames] — the frames already decoded once, up front — so dragging
 * the playhead is a lookup, not a decode, and scrubbing stays smooth. The parent owns playback:
 * it advances [playheadMs] on a timer while [playing], and paints whichever frame this reports.
 */
@Composable
fun VideoTimeline(
    frames: List<Pair<Long, Bitmap>>,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    playing: Boolean,
    onTogglePlay: () -> Unit,
    onTrimStart: (Long) -> Unit,
    onTrimEnd: (Long) -> Unit,
    onScrub: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0L) return
    val density = LocalDensity.current

    // Inset well clear of the screen edges: at a smaller margin the left trim handle lands
    // inside the system back-gesture strip, and grabbing it navigates away instead of trimming.
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Gold).clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (playing) "Pause" else "Play",
                    tint = OnGold, modifier = Modifier.size(20.dp),
                )
            }
            Text(
                formatMs(playheadMs), color = AppOnSurface, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp),
            )
            Text(
                "  ·  keeping ${formatMs(endMs - startMs)} of ${formatMs(durationMs)}",
                color = AppOnSurfaceMuted, fontSize = 11.sp,
            )
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(top = 6.dp).height(STRIP_HEIGHT)
                .clip(RoundedCornerShape(10.dp))
                .background(AppSurfaceVariant),
        ) {
            val trackW = maxWidth
            val trackPx = with(density) { trackW.toPx() }
            // Time <-> x mapping. The strip spans the WHOLE clip; trimming dims the outside
            // rather than rescaling, so the handles stay where the content is.
            fun msToDp(ms: Long) = trackW * (ms.toFloat() / durationMs).coerceIn(0f, 1f)
            fun xToMs(x: Float) = ((x / trackPx).coerceIn(0f, 1f) * durationMs).toLong()

            // Filmstrip.
            Row(Modifier.fillMaxSize()) {
                frames.forEach { (_, bmp) ->
                    Image(
                        bmp.asImageBitmap(), null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            // Dim what the trim throws away.
            if (startMs > 0L) {
                Box(Modifier.width(msToDp(startMs)).fillMaxHeight().background(Color.Black.copy(alpha = 0.6f)))
            }
            if (endMs < durationMs) {
                Box(
                    Modifier.align(Alignment.CenterEnd)
                        .width(trackW - msToDp(endMs)).fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.6f)),
                )
            }
            // Kept region outlined, so the selection reads even at a glance.
            Box(
                Modifier.offset(x = msToDp(startMs)).width((msToDp(endMs) - msToDp(startMs)).coerceAtLeast(2.dp))
                    .fillMaxHeight().border(2.dp, Gold, RoundedCornerShape(4.dp)),
            )

            // Tap or drag anywhere on the strip to move the playhead.
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(durationMs, trackPx) {
                        detectTapGestures { pos -> onScrub(xToMs(pos.x)) }
                    }
                    .pointerInput(durationMs, trackPx) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            onScrub(xToMs(change.position.x))
                        }
                    },
            )

            // Playhead.
            Box(
                Modifier.offset(x = msToDp(playheadMs) - 1.dp)
                    .width(2.dp).fillMaxHeight().background(Color.White),
            )

            // Trim handles last, so they take drags in preference to the scrub layer above.
            // Each drag event is a pixel delta applied to the CURRENT trim, converted here where
            // startMs/endMs are read fresh (see TrimHandle on why the callback must stay current).
            fun dxToMs(dx: Float) = (dx / trackPx * durationMs).toLong()
            TrimHandle(
                xOffset = msToDp(startMs),
                onDrag = { dx -> onTrimStart(startMs + dxToMs(dx)) },
            )
            TrimHandle(
                xOffset = (msToDp(endMs) - HANDLE_WIDTH).coerceAtLeast(0.dp),
                onDrag = { dx -> onTrimEnd(endMs + dxToMs(dx)) },
            )
        }
    }
}

/**
 * A grab bar at one end of the kept region. Reports raw pixel deltas; the caller converts.
 *
 * The callback goes through [rememberUpdatedState] because `pointerInput(Unit)` starts its block
 * exactly once — it would otherwise hold the very first `onDrag` lambda forever, and that
 * lambda's captured trim position never advances, so a long drag collapses to whatever its last
 * single event moved.
 */
@Composable
private fun TrimHandle(xOffset: androidx.compose.ui.unit.Dp, onDrag: (Float) -> Unit) {
    val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    Box(
        Modifier
            .offset(x = xOffset)
            .width(HANDLE_WIDTH)
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(Gold)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    currentOnDrag(drag.x)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(2.dp).height(18.dp).background(OnGold.copy(alpha = 0.7f)))
    }
}
