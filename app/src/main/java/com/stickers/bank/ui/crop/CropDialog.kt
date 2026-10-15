package com.stickers.bank.ui.crop

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppOutline
import com.stickers.bank.ui.theme.neuClickable
import com.stickers.bank.ui.theme.neuRaised
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class AspectPreset(val label: String, val ratio: Float?) // null = free

/** Alpha-mask applied to the cropped bitmap on top of the rectangular crop above. NONE and
 * RECTANGLE both leave the crop untouched — RECTANGLE is offered as an explicit choice
 * (alongside CIRCLE/SQUARE) even though it's a no-op, so "what shape is this trimmed to" always
 * has a visible answer instead of an implicit default. */
enum class TrimShape(val label: String) { NONE("None"), CIRCLE("Circle"), SQUARE("Square"), RECTANGLE("Rectangle") }

private val ASPECT_PRESETS = listOf(
    AspectPreset("Free", null),
    AspectPreset("1:1", 1f),
    AspectPreset("4:5", 4f / 5f),
    AspectPreset("3:4", 3f / 4f),
    AspectPreset("2:3", 2f / 3f),
    AspectPreset("9:16", 9f / 16f),
    AspectPreset("16:9", 16f / 9f),
)

/**
 * The crop step every gallery import (and "Crop" tool tap) goes through. Full-screen so there's
 * real room to work: a draggable/resizable crop box with a rule-of-thirds grid and clearly
 * outlined corner handles (not a flat colour block — those get lost against busy photos),
 * aspect-ratio presets (locking one keeps corner-drags on-ratio, anchored at the opposite
 * corner), and a 90° rotate before cropping. "Skip crop" imports the original untouched. Purely
 * local — nothing uploaded.
 */
@Composable
fun CropDialog(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        var rotation by remember { mutableStateOf(0) } // 0/90/180/270, applied before cropping
        // Mirror image (left↔right) and water image (upside down), applied after the rotation.
        var mirror by remember { mutableStateOf(false) }
        var water by remember { mutableStateOf(false) }
        val rotated = remember(bitmap, rotation, mirror, water) { transformBitmap(bitmap, rotation, mirror, water) }

        var ratio by remember { mutableStateOf<Float?>(null) }
        var trimShape by remember { mutableStateOf(TrimShape.NONE) }
        // Live crop box, in container pixels. Null until the image is measured (or reset by
        // a rotation / ratio change).
        var crop by remember { mutableStateOf<Rect?>(null) }
        var imageRect by remember { mutableStateOf(Rect.Zero) }
        // "Remove background": the person cut out of the photo on-device (see PersonCutout).
        // Computed once per rotation and then cropped like the photo itself.
        val context = LocalContext.current
        var removeBg by remember { mutableStateOf(false) }
        var cutout by remember(rotated) { mutableStateOf<PersonCutout.Cutout?>(null) }
        var cutting by remember { mutableStateOf(false) }
        val shown = if (removeBg) cutout?.bitmap ?: rotated else rotated
        LaunchedEffect(removeBg, rotated) {
            if (!removeBg || cutout != null) return@LaunchedEffect
            cutting = true
            val result = PersonCutout.cut(rotated)
            cutting = false
            if (result == null) {
                removeBg = false
                Toast.makeText(context, "Couldn't find a person in this photo.", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }
            cutout = result
            // Frame the person: with no ratio locked, the box snaps to them (a little margin).
            val r = imageRect
            if (ratio == null && r.width > 0f) {
                val k = r.width / rotated.width
                val b = result.bounds
                val padX = b.width() * 0.04f * k
                val padY = b.height() * 0.04f * k
                crop = Rect(
                    (r.left + b.left * k - padX).coerceAtLeast(r.left),
                    (r.top + b.top * k - padY).coerceAtLeast(r.top),
                    (r.left + b.right * k + padX).coerceAtMost(r.right),
                    (r.top + b.bottom * k + padY).coerceAtMost(r.bottom),
                )
            }
        }

        Column(
            Modifier.fillMaxSize().background(AppBackground).statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 6.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).neuClickable(cornerRadius = 20.dp, elevation = 6.dp, onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Cancel", tint = AppOnSurface, modifier = Modifier.size(18.dp)) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("CROP PHOTO", color = Gold, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
                    Text("Drag the box or a corner, or pinch to resize it. The photo stays put.", color = AppOnSurfaceMuted, fontSize = 11.sp)
                }
            }

            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).padding(16.dp)
                    // Two fingers resize/move the CROP BOX — never the photo. (A pinch used to
                    // zoom and pan the photo itself, so it slid around under the box while
                    // cropping.) Runs on the Initial pass so a genuine pinch is taken before the
                    // box's own one-finger drag sees it; a single finger is left untouched and
                    // falls through to the box and its corners as before.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            do {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                if (event.changes.count { it.pressed } >= 2) {
                                    val cur = crop
                                    if (cur != null && imageRect.width > 0f) {
                                        crop = pinchBox(cur, imageRect, event.calculateZoom(), event.calculatePan(), minSize = 48f)
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val boxW = constraints.maxWidth.toFloat()
                val boxH = constraints.maxHeight.toFloat()
                // Where the bitmap actually lands once fit inside the box (letterboxed) — the
                // UNZOOMED reference frame the crop box's own coordinates live in.
                val fit = min(boxW / rotated.width, boxH / rotated.height)
                val dispW = rotated.width * fit
                val dispH = rotated.height * fit
                val left = (boxW - dispW) / 2f
                val top = (boxH - dispH) / 2f
                val newImageRect = Rect(left, top, left + dispW, top + dispH)
                if (newImageRect != imageRect) {
                    imageRect = newImageRect
                    crop = fittedRect(newImageRect, ratio)
                }
                val c = crop ?: newImageRect

                if (removeBg && cutout != null) {
                    // A checkerboard where the background was, so "transparent" reads as such.
                    Canvas(Modifier.fillMaxSize()) {
                        val r = imageRect
                        val tile = 10.dp.toPx()
                        clipRect(r.left, r.top, r.right, r.bottom) {
                            drawRect(Color(0xFFF2F2F2), r.topLeft, r.size)
                            var y = r.top
                            var row = 0
                            while (y < r.bottom) {
                                var x = r.left + if (row % 2 == 0) 0f else tile
                                while (x < r.right) {
                                    drawRect(Color(0xFFD6D6D6), Offset(x, y), Size(tile, tile))
                                    x += tile * 2
                                }
                                y += tile
                                row++
                            }
                        }
                    }
                }
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )

                // Dim outside the box, a crisp white outline, and a rule-of-thirds grid inside —
                // the standard photo-editor crop language, so the selection reads at a glance
                // instead of needing to be puzzled out. With a trim shape picked, the part of the
                // box that shape will actually throw away is dimmed too and the shape itself is
                // outlined, so the circle/square is visible here at full size on the real photo
                // rather than only in a thumbnail.
                Canvas(Modifier.fillMaxSize()) {
                    val dim = Color.Black.copy(alpha = 0.62f)
                    drawRect(dim, Offset(0f, 0f), Size(size.width, c.top))
                    drawRect(dim, Offset(0f, c.bottom), Size(size.width, size.height - c.bottom))
                    drawRect(dim, Offset(0f, c.top), Size(c.left, c.height))
                    drawRect(dim, Offset(c.right, c.top), Size(size.width - c.right, c.height))

                    // The trim shape's own footprint inside the box: the centered square the
                    // SQUARE/CIRCLE trims cut down to (CropDialog's applyTrimShape does exactly
                    // this on the pixels), or the whole box for NONE/RECTANGLE.
                    val trimRect = trimRectFor(c, trimShape)
                    if (trimRect != c) {
                        // Anything inside the crop box but outside the trim shape is on its way
                        // out — dim it a second time so it reads as "not kept".
                        val cut = Color.Black.copy(alpha = 0.45f)
                        drawRect(cut, Offset(c.left, c.top), Size(c.width, trimRect.top - c.top))
                        drawRect(cut, Offset(c.left, trimRect.bottom), Size(c.width, c.bottom - trimRect.bottom))
                        drawRect(cut, Offset(c.left, trimRect.top), Size(trimRect.left - c.left, trimRect.height))
                        drawRect(cut, Offset(trimRect.right, trimRect.top), Size(c.right - trimRect.right, trimRect.height))
                    }
                    if (trimShape == TrimShape.CIRCLE) {
                        // The corners the circle mask clears, dimmed by painting the square's
                        // area and punching the circle back out of it.
                        val layer = Color.Black.copy(alpha = 0.45f)
                        clipPath(
                            androidx.compose.ui.graphics.Path().apply {
                                addOval(trimRect)
                            },
                            clipOp = androidx.compose.ui.graphics.ClipOp.Difference,
                        ) {
                            drawRect(layer, topLeft = trimRect.topLeft, size = trimRect.size)
                        }
                    }

                    val gridColor = Color.White.copy(alpha = 0.55f)
                    for (i in 1..2) {
                        val x = c.left + c.width * i / 3f
                        drawLine(gridColor, Offset(x, c.top), Offset(x, c.bottom), strokeWidth = 1.dp.toPx())
                        val y = c.top + c.height * i / 3f
                        drawLine(gridColor, Offset(c.left, y), Offset(c.right, y), strokeWidth = 1.dp.toPx())
                    }
                    drawRect(Color.White, topLeft = c.topLeft, size = c.size, style = Stroke(width = 1.5.dp.toPx()))

                    // The shape's own edge, in gold, so it stands apart from the white crop box.
                    when (trimShape) {
                        TrimShape.CIRCLE -> drawOval(
                            Gold, topLeft = trimRect.topLeft, size = trimRect.size,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        TrimShape.SQUARE -> drawRect(
                            Gold, topLeft = trimRect.topLeft, size = trimRect.size,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        TrimShape.NONE, TrimShape.RECTANGLE -> Unit
                    }
                }

                val minSize = 48f
                // The box itself — drag anywhere inside to move it. `align(TopStart)` matters
                // here: BoxWithConstraints centers un-aligned children by default, which would
                // otherwise add its own centering shift on top of this offset.
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(c.left.roundToInt(), c.top.roundToInt()) }
                        .size(with(LocalDensity.current) { c.width.toDp() }, with(LocalDensity.current) { c.height.toDp() })
                        .pointerInput(imageRect) {
                            detectDragGestures { _, drag ->
                                val cur = crop ?: return@detectDragGestures
                                val dx = drag.x.coerceIn(imageRect.left - cur.left, imageRect.right - cur.right)
                                val dy = drag.y.coerceIn(imageRect.top - cur.top, imageRect.bottom - cur.bottom)
                                crop = cur.translate(dx, dy)
                            }
                        },
                )

                // Four corner handles, each an L-bracket tracing the two edges that meet there
                // (outlined in black so it stays visible over light photos too). With a ratio
                // locked, dragging any corner resizes on-ratio, anchored at the opposite corner.
                CornerHandle(c.left, c.top, HandleCorner.TOP_LEFT) { dx, dy ->
                    val cur = crop ?: return@CornerHandle
                    crop = resizeCorner(cur, imageRect, ratio, anchor = Corner.BOTTOM_RIGHT, dx = dx, dy = dy, minSize = minSize)
                }
                CornerHandle(c.right, c.top, HandleCorner.TOP_RIGHT) { dx, dy ->
                    val cur = crop ?: return@CornerHandle
                    crop = resizeCorner(cur, imageRect, ratio, anchor = Corner.BOTTOM_LEFT, dx = dx, dy = dy, minSize = minSize)
                }
                CornerHandle(c.left, c.bottom, HandleCorner.BOTTOM_LEFT) { dx, dy ->
                    val cur = crop ?: return@CornerHandle
                    crop = resizeCorner(cur, imageRect, ratio, anchor = Corner.TOP_RIGHT, dx = dx, dy = dy, minSize = minSize)
                }
                CornerHandle(c.right, c.bottom, HandleCorner.BOTTOM_RIGHT) { dx, dy ->
                    val cur = crop ?: return@CornerHandle
                    crop = resizeCorner(cur, imageRect, ratio, anchor = Corner.TOP_LEFT, dx = dx, dy = dy, minSize = minSize)
                }

                if (c.width < imageRect.width - 1f || c.height < imageRect.height - 1f) {
                    Box(
                        Modifier.align(Alignment.TopStart)
                            .offset { IntOffset(c.left.roundToInt() + 8, c.top.roundToInt() + 8) }
                            .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "${(c.width / imageRect.width * rotated.width).roundToInt()} × ${(c.height / imageRect.height * rotated.height).roundToInt()}",
                            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (cutting) {
                    Column(
                        Modifier.align(Alignment.Center).clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 22.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        com.stickers.bank.ui.components.ZigZagSpinner(Modifier.size(48.dp))
                        Text(
                            "Removing background…", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }

            Text(
                "BACKGROUND", color = AppOnSurfaceMuted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(false to "Keep Background", true to "Remove Background").forEach { (value, label) ->
                    val sel = removeBg == value
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) Gold else Color.Transparent)
                            .border(1.dp, if (sel) Gold else AppOutline, RoundedCornerShape(50))
                            .clickable(enabled = !cutting) { removeBg = value }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) { Text(label, color = if (sel) OnGold else AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Text(
                "ASPECT RATIO", color = AppOnSurfaceMuted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ASPECT_PRESETS.forEach { p ->
                    val sel = p.ratio == ratio
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) Gold else Color.Transparent)
                            .border(1.dp, if (sel) Gold else AppOutline, RoundedCornerShape(50))
                            .clickable {
                                ratio = p.ratio
                                if (imageRect.width > 0f) crop = fittedRect(imageRect, p.ratio)
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) { Text(p.label, color = if (sel) OnGold else AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                Box(
                    Modifier.clip(CircleShape).background(Color.Transparent).border(1.dp, AppOutline, CircleShape)
                        .clickable { rotation = (rotation + 90) % 360; crop = null }
                        .padding(10.dp),
                ) { Icon(Icons.Rounded.Rotate90DegreesCcw, "Rotate 90°", tint = AppOnSurface, modifier = Modifier.size(18.dp)) }
                // Flipping keeps the photo's size, so the crop box is mirrored with it and stays
                // on the same part of the picture.
                FlipChip("Mirror", mirror, iconRotation = 0f) {
                    mirror = !mirror
                    crop = crop?.let { Rect(imageRect.left + imageRect.right - it.right, it.top, imageRect.left + imageRect.right - it.left, it.bottom) }
                }
                FlipChip("Water", water, iconRotation = 90f) {
                    water = !water
                    crop = crop?.let { Rect(it.left, imageRect.top + imageRect.bottom - it.bottom, it.right, imageRect.top + imageRect.bottom - it.top) }
                }
            }

            Text(
                "TRIM SHAPE", color = AppOnSurfaceMuted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrimShape.entries.forEach { s ->
                    val sel = s == trimShape
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (sel) Gold else Color.Transparent)
                            .border(1.dp, if (sel) Gold else AppOutline, RoundedCornerShape(50))
                            .clickable { trimShape = s }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) { Text(s.label, color = if (sel) OnGold else AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CropButton("Skip crop", Modifier.weight(1f), Color.Transparent, AppOnSurfaceMuted, outline = true) {
                    if (!cutting) onDone(applyTrimShape(shown, trimShape))
                }
                CropButton("Apply crop", Modifier.weight(1.4f), Gold, OnGold, icon = Icons.Rounded.Check) {
                    if (cutting) return@CropButton
                    val c = crop
                    val out = if (c == null || imageRect.width <= 0f) shown else cropToBitmap(shown, c, imageRect)
                    onDone(applyTrimShape(out, trimShape))
                }
            }
        }
    }
}

private enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
private enum class HandleCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * Where [shape] actually lands inside the crop box [c] — the on-screen twin of what
 * [applyTrimShape] does to the pixels, so the overlay drawn on the photo and the bitmap that
 * comes out of Apply can't drift apart. SQUARE and CIRCLE both cut down to the largest centered
 * square first (a circle mask on a non-square rect would come out an ellipse); NONE and
 * RECTANGLE keep the box as-is.
 */
private fun trimRectFor(c: Rect, shape: TrimShape): Rect = when (shape) {
    TrimShape.NONE, TrimShape.RECTANGLE -> c
    TrimShape.SQUARE, TrimShape.CIRCLE -> {
        val side = min(c.width, c.height)
        val left = c.left + (c.width - side) / 2f
        val top = c.top + (c.height - side) / 2f
        Rect(left, top, left + side, top + side)
    }
}

/** The largest rect of [ratio] (or the full rect, if free) centered inside [bounds]. */
private fun fittedRect(bounds: Rect, ratio: Float?): Rect {
    if (ratio == null) return bounds
    val boundsRatio = bounds.width / bounds.height
    val (w, h) = if (boundsRatio > ratio) bounds.height * ratio to bounds.height else bounds.width to bounds.width / ratio
    val left = bounds.left + (bounds.width - w) / 2f
    val top = bounds.top + (bounds.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

/**
 * Resize [cur] by dragging the corner opposite [anchor] by (dx, dy), clamped to [bounds]. When
 * [ratio] is set, the drag's dominant axis drives the size and the other side is derived to
 * keep that ratio, still anchored at the fixed corner.
 */
private fun resizeCorner(cur: Rect, bounds: Rect, ratio: Float?, anchor: Corner, dx: Float, dy: Float, minSize: Float): Rect {
    val anchorX = if (anchor == Corner.TOP_LEFT || anchor == Corner.BOTTOM_LEFT) cur.left else cur.right
    val anchorY = if (anchor == Corner.TOP_LEFT || anchor == Corner.TOP_RIGHT) cur.top else cur.bottom
    val movingX = if (anchor == Corner.TOP_LEFT || anchor == Corner.BOTTOM_LEFT) cur.right else cur.left
    val movingY = if (anchor == Corner.TOP_LEFT || anchor == Corner.TOP_RIGHT) cur.bottom else cur.top

    var newMovingX = movingX + dx
    var newMovingY = movingY + dy

    if (ratio != null) {
        val w = (newMovingX - anchorX)
        val h = (newMovingY - anchorY)
        // Drive off whichever axis moved more, derive the other from the ratio.
        if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
            newMovingY = anchorY + kotlin.math.abs(w) / ratio * (if (h < 0) -1f else 1f)
        } else {
            newMovingX = anchorX + kotlin.math.abs(h) * ratio * (if (w < 0) -1f else 1f)
        }
    }

    val minX = min(anchorX, newMovingX).coerceIn(bounds.left, bounds.right)
    val maxX = max(anchorX, newMovingX).coerceIn(bounds.left, bounds.right)
    val minY = min(anchorY, newMovingY).coerceIn(bounds.top, bounds.bottom)
    val maxY = max(anchorY, newMovingY).coerceIn(bounds.top, bounds.bottom)
    if (maxX - minX < minSize || maxY - minY < minSize) return cur
    return Rect(minX, minY, maxX, maxY)
}

/** [src] turned by [degrees], then flipped left↔right ([mirror]) and/or upside down ([water]). */
private fun transformBitmap(src: Bitmap, degrees: Int, mirror: Boolean, water: Boolean): Bitmap {
    if (degrees == 0 && !mirror && !water) return src
    return runCatching {
        val m = android.graphics.Matrix().apply {
            postRotate(degrees.toFloat())
            postScale(if (mirror) -1f else 1f, if (water) -1f else 1f)
        }
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }.getOrDefault(src)
}

/** A flip toggle in the ratio row: lit gold while that flip is on. */
@Composable
private fun FlipChip(label: String, on: Boolean, iconRotation: Float, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (on) Gold else Color.Transparent)
            .border(1.dp, if (on) Gold else AppOutline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Flip, null, tint = if (on) OnGold else AppOnSurface,
            modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = iconRotation },
        )
        Text(
            label, color = if (on) OnGold else AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** An L-bracket tracing the two box edges meeting at [corner] — a black shadow bar under a
 * white one on each arm, so it reads over both light and dark photos, unlike a flat colour
 * block. Built from plain positioned Boxes rather than a Canvas draw, deliberately: it's a
 * simpler primitive with fewer places for a px/dp mismatch to silently make it invisible. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.CornerHandle(x: Float, y: Float, corner: HandleCorner, onDrag: (Float, Float) -> Unit) {
    val d = LocalDensity.current
    val touchDp = 36.dp
    val armDp = 16.dp
    val strokeDp = 3.dp
    val shadowDp = 6.dp
    val center = touchDp / 2
    val dirRight = corner == HandleCorner.TOP_LEFT || corner == HandleCorner.BOTTOM_LEFT
    val dirDown = corner == HandleCorner.TOP_LEFT || corner == HandleCorner.TOP_RIGHT
    val hStart = if (dirRight) center else center - armDp
    val vStart = if (dirDown) center else center - armDp

    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset((x - with(d) { touchDp.toPx() } / 2f).roundToInt(), (y - with(d) { touchDp.toPx() } / 2f).roundToInt()) }
            .size(touchDp)
            .pointerInput(Unit) { detectDragGestures { _, drag -> onDrag(drag.x, drag.y) } },
    ) {
        // Horizontal arm: a soft dark shadow bar, then a crisp white bar centered on top of it.
        Box(Modifier.offset(x = hStart, y = center - shadowDp / 2).size(armDp, shadowDp).background(Color.Black.copy(alpha = 0.4f)))
        Box(Modifier.offset(x = hStart, y = center - strokeDp / 2).size(armDp, strokeDp).background(Color.White))
        // Vertical arm, same treatment.
        Box(Modifier.offset(x = center - shadowDp / 2, y = vStart).size(shadowDp, armDp).background(Color.Black.copy(alpha = 0.4f)))
        Box(Modifier.offset(x = center - strokeDp / 2, y = vStart).size(strokeDp, armDp).background(Color.White))
    }
}

@Composable
private fun CropButton(
    label: String,
    modifier: Modifier,
    bg: Color,
    fg: Color,
    outline: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier.neuRaised(cornerRadius = 14.dp, elevation = 6.dp, background = bg).clip(RoundedCornerShape(14.dp))
            .then(if (outline) Modifier.border(1.dp, AppOutline, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp)); Box(Modifier.size(6.dp)) }
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** The largest centered square cut from [src]. Used both as SQUARE's own output and as CIRCLE's
 * first step (a circle mask on a non-square rect would come out an ellipse, not a circle). */
private fun centerSquare(src: Bitmap): Bitmap = runCatching {
    val side = min(src.width, src.height)
    if (side == src.width && side == src.height) return src
    val x = (src.width - side) / 2
    val y = (src.height - side) / 2
    Bitmap.createBitmap(src, x, y, side, side)
}.getOrDefault(src)

/** Punches [src] into [shape] by zeroing the alpha channel outside it — NONE/RECTANGLE pass the
 * bitmap through untouched. */
private fun applyTrimShape(src: Bitmap, shape: TrimShape): Bitmap = when (shape) {
    TrimShape.NONE, TrimShape.RECTANGLE -> src
    TrimShape.SQUARE -> centerSquare(src)
    TrimShape.CIRCLE -> runCatching {
        val square = centerSquare(src)
        val size = square.width
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(square, 0f, 0f, paint)
        output
    }.getOrDefault(src)
}

/**
 * The crop box after a two-finger frame: scaled by [zoom] around its own center (so a locked
 * ratio stays locked), moved by [pan], then held to at least [minSize] and inside [bounds].
 */
private fun pinchBox(cur: Rect, bounds: Rect, zoom: Float, pan: Offset, minSize: Float): Rect {
    var w = cur.width * zoom
    var h = cur.height * zoom
    if (w > bounds.width || h > bounds.height) {
        val f = min(bounds.width / w, bounds.height / h)
        w *= f; h *= f
    }
    if (w < minSize || h < minSize) {
        val f = max(minSize / w, minSize / h)
        w = (w * f).coerceAtMost(bounds.width); h = (h * f).coerceAtMost(bounds.height)
    }
    val left = (cur.center.x + pan.x - w / 2f).coerceIn(bounds.left, bounds.right - w)
    val top = (cur.center.y + pan.y - h / 2f).coerceIn(bounds.top, bounds.bottom - h)
    return Rect(left, top, left + w, top + h)
}

/** Map the on-screen crop box back to source-bitmap pixels and cut it out. */
private fun cropToBitmap(src: Bitmap, crop: Rect, imageRect: Rect): Bitmap = runCatching {
    val scale = src.width / imageRect.width
    val x = ((crop.left - imageRect.left) * scale).roundToInt().coerceIn(0, src.width - 1)
    val y = ((crop.top - imageRect.top) * scale).roundToInt().coerceIn(0, src.height - 1)
    val w = (crop.width * scale).roundToInt().coerceIn(1, src.width - x)
    val h = (crop.height * scale).roundToInt().coerceIn(1, src.height - y)
    Bitmap.createBitmap(src, x, y, max(1, w), max(1, h))
}.getOrDefault(src)
