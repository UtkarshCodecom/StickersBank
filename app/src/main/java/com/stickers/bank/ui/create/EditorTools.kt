package com.stickers.bank.ui.create

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Catalog
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.components.SbSearchBar
import com.stickers.bank.ui.components.checkerboard
import com.stickers.bank.ui.components.editorCheckerboard
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.util.FontCache
import kotlin.math.roundToInt

// ---------- Fonts (built-in assets + admin-uploaded custom URLs) ----------

/**
 * Resolves a [TextItem.fontPath] to a [FontFamily] — an asset path (built-in) loads instantly;
 * an "http…" URL (an admin-uploaded custom font) downloads + caches on first use, falling back
 * to the default font until it's ready, then recomposing with the real one.
 */
@Composable
fun rememberFontFamily(path: String?): FontFamily {
    if (path == null) return FontFamily.Default
    val context = LocalContext.current
    if (!path.startsWith("http")) {
        return remember(path) { runCatching { FontFamily(Font(path, context.assets)) }.getOrDefault(FontFamily.Default) }
    }
    var family by remember(path) { mutableStateOf<FontFamily>(FontFamily.Default) }
    LaunchedEffect(path) {
        val file = FontCache.localFile(context, path)
        if (file != null) family = FontFamily(Font(file))
    }
    return family
}

// ---------- Alignment guides ----------

/** How close (in px) a drag has to land to the canvas's own center before it snaps dead-on and
 * the matching guide line lights up. Item/base offsets are already measured from the canvas
 * center (0,0), so "centered" is just "close to zero" on either axis. */
private const val CENTER_SNAP_PX = 14f

/** Snaps [raw] to exactly 0 on whichever axis is within [CENTER_SNAP_PX] of it, alongside which
 * axes snapped — the screen uses the two booleans to flash the matching [CenterGuides] line. */
fun snapToCenter(raw: Offset): Triple<Offset, Boolean, Boolean> {
    val sx = kotlin.math.abs(raw.x) < CENTER_SNAP_PX
    val sy = kotlin.math.abs(raw.y) < CENTER_SNAP_PX
    return Triple(Offset(if (sx) 0f else raw.x, if (sy) 0f else raw.y), sx, sy)
}

/**
 * The vertical/horizontal center lines that flash while dragging something into dead-center on
 * the canvas — the same alignment-guide language every other design editor (Canva, PowerPoint,
 * Keynote…) shows. A plain sibling drawn ON TOP of the canvas content rather than baked into it,
 * sized to match it exactly so its own midpoint lines up with the (0,0) origin every item's and
 * the base photo's offset is already measured from.
 */
@Composable
fun CenterGuides(showX: Boolean, showY: Boolean, modifier: Modifier = Modifier) {
    if (!showX && !showY) return
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        if (showX) drawLine(Gold, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = stroke)
        if (showY) drawLine(Gold, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = stroke)
    }
}

// ---------- Base image (filter + erase) ----------

/**
 * The base/background photo. Laid out as a box the exact size of the photo itself (its own
 * aspect ratio, fitted to the canvas at scale 1) rather than a canvas-filling square — so it
 * gets the same treatment an overlay [ImageItem] gets: a selection border on its real edges and
 * four corner handles that set width and height independently (see [BaseResizeHandle]).
 *
 * [scaleX]/[scaleY] are applied by *sizing the box*, not by a `graphicsLayer` scale, so drag
 * deltas inside it stay in real screen units and the corner math matches [ResizeHandle]'s
 * exactly. Only rotation and the flip stay as layer transforms.
 */
@Composable
fun EditableBase(
    bitmap: android.graphics.Bitmap,
    offset: Offset,
    scale: Float,
    rotation: Float,
    flipX: Boolean,
    colorFilter: ColorFilter?,
    onTransform: (Offset, Float, Float) -> Unit,
    transformEnabled: Boolean,
    onDoubleTap: () -> Unit = {},
    /** 1 = fully opaque, matching every overlay item's own alpha convention. */
    alpha: Float = 1f,
    /** Independent vertical scale. Defaults to [scale] (uniform) until a corner handle splits them. */
    scaleY: Float = scale,
    /** Tapping the photo — the screen uses this to move selection off any overlay item. */
    onTap: () -> Unit = {},
    /** Draws the border + corner handles. */
    selected: Boolean = false,
    /** (newScaleX, newScaleY, offsetDelta) from a corner drag; null disables the handles. */
    onResize: ((Float, Float, Offset) -> Unit)? = null,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val density = LocalDensity.current
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        // The photo's "natural" on-canvas size: fitted (letterboxed) inside the canvas at scale 1.
        val fitPx = minOf(constraints.maxWidth / bw, constraints.maxHeight / bh)
        val fitW = bw * fitPx
        val fitH = bh * fitPx
        val wDp = with(density) { (fitW * scale).toDp() }
        val hDp = with(density) { (fitH * scaleY).toDp() }

        var mod = Modifier
            .size(wDp, hDp)
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y
                scaleX = if (flipX) -1f else 1f
                rotationZ = rotation
            }
        if (transformEnabled) {
            mod = mod
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rot -> onTransform(pan, zoom, rot) }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        // A plain tap on the photo is a selection change, not a no-op — without
                        // this the photo swallowed the tap and a selected overlay kept its border.
                        onTap = { onTap() },
                        // Same swap-the-photo gesture overlay layers already have.
                        onDoubleTap = { onDoubleTap() },
                    )
                }
        }
        if (selected) mod = mod.border(1.5.dp, Gold, RoundedCornerShape(2.dp))

        Box(mod) {
            Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bitmap.width, bitmap.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    colorFilter = colorFilter,
                    alpha = alpha,
                )
            }
            if (selected && transformEnabled && onResize != null) {
                BaseResizeHandle(Alignment.TopStart, -1f, -1f, fitW, fitH, scale, scaleY, rotation, onResize)
                BaseResizeHandle(Alignment.TopEnd, 1f, -1f, fitW, fitH, scale, scaleY, rotation, onResize)
                BaseResizeHandle(Alignment.BottomStart, -1f, 1f, fitW, fitH, scale, scaleY, rotation, onResize)
                BaseResizeHandle(Alignment.BottomEnd, 1f, 1f, fitW, fitH, scale, scaleY, rotation, onResize)
            }
        }
    }
}

/** [ResizeHandle]'s math for the base photo, whose box size is a scale multiple of its fitted
 * size rather than a stored width/height — so it reports back new scales instead of new dp. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.BaseResizeHandle(
    align: Alignment,
    sx: Float,
    sy: Float,
    fitW: Float,
    fitH: Float,
    scaleX: Float,
    scaleY: Float,
    rotation: Float,
    onResize: (Float, Float, Offset) -> Unit,
) {
    Box(
        Modifier
            .align(align)
            .offset(x = 11.dp * sx, y = 11.dp * sy)
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Gold, CircleShape)
            .pointerInput(fitW, fitH) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val curW = fitW * scaleX
                    val curH = fitH * scaleY
                    val minPx = 48f
                    val newW = (curW + sx * dragAmount.x).coerceAtLeast(minPx)
                    val newH = (curH + sy * dragAmount.y).coerceAtLeast(minPx)
                    val localShiftX = sx * (newW - curW) / 2f
                    val localShiftY = sy * (newH - curH) / 2f
                    val rad = Math.toRadians(rotation.toDouble())
                    val cos = kotlin.math.cos(rad).toFloat()
                    val sin = kotlin.math.sin(rad).toFloat()
                    onResize(
                        newW / fitW,
                        newH / fitH,
                        Offset(
                            localShiftX * cos - localShiftY * sin,
                            localShiftX * sin + localShiftY * cos,
                        ),
                    )
                }
            },
    )
}

/**
 * Universal eraser — drawn ON TOP of the whole composite (photo, text, stickers, drawings) so a
 * drag punches transparent holes through everything, not just the base photo. Lives inside the
 * capture layer, so erased areas are transparent in the saved sticker too. Works with no photo.
 */
@Composable
fun UniversalEraseLayer(active: Boolean, brush: Float, strokes: SnapshotStateList<SnapshotStateList<Offset>>) {
    val mod = if (active) {
        Modifier.fillMaxSize().pointerInput(brush) {
            detectDragGestures(
                onDragStart = { pos -> strokes.add(mutableStateListOf(pos)) },
                onDrag = { change, _ -> change.consume(); strokes.lastOrNull()?.add(change.position) },
            )
        }
    } else {
        Modifier.fillMaxSize()
    }
    Canvas(modifier = mod) {
        strokes.forEach { pts ->
            if (pts.size == 1) {
                drawCircle(Color.Transparent, radius = brush / 2f, center = pts[0], blendMode = BlendMode.Clear)
            } else if (pts.size > 1) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                }
                drawPath(path, Color.Transparent, style = Stroke(brush, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
            }
        }
    }
}

// ---------- Freehand draw layer ----------

@Composable
fun DrawLayer(strokes: SnapshotStateList<DrawStroke>, active: Boolean, color: Int, width: Float, eraser: Boolean = false) {
    // Own offscreen layer so an eraser stroke's BlendMode.Clear removes ONLY earlier brush
    // strokes — not the photo, text or stickers underneath.
    val base = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    val mod = if (active) {
        base.pointerInput(color, width, eraser) {
            detectDragGestures(
                onDragStart = { pos -> strokes.add(DrawStroke(color, width, eraser).apply { points.add(pos) }) },
                onDrag = { change, _ -> change.consume(); strokes.lastOrNull()?.points?.add(change.position) },
            )
        }
    } else {
        base
    }
    Canvas(modifier = mod) {
        strokes.forEach { s ->
            val pts = s.points
            val strokeColor = if (s.eraser) Color.Transparent else Color(s.colorArgb)
            val blend = if (s.eraser) BlendMode.Clear else BlendMode.SrcOver
            if (pts.size == 1) {
                drawCircle(strokeColor, radius = s.width / 2f, center = pts[0], blendMode = blend)
            } else if (pts.size > 1) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                }
                drawPath(path, strokeColor, style = Stroke(s.width, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
            }
        }
    }
}

// ---------- Overlays ----------

@Composable
fun OverlayView(
    item: EditorItem,
    selected: Boolean,
    interactive: Boolean,
    onSelect: () -> Unit,
    onEditText: (TextItem) -> Unit,
    onReplaceImage: (ImageItem) -> Unit = {},
    // Multi-select is opt-in — the general sticker editor doesn't pass these, so its behavior
    // (single selection only) is unchanged. The canvas template editor turns it on.
    multiSelectEnabled: Boolean = false,
    multiSelected: Boolean = false,
    onToggleMultiSelect: () -> Unit = {},
    // When 2+ items are multi-selected, dragging any one of them should move the whole group
    // together instead of just itself — the screen applies `pan` to every selected item.
    groupDragActive: Boolean = false,
    onGroupDrag: (Offset) -> Unit = {},
    // Reports whether this drag just snapped to dead-center on the canvas, on either axis, so
    // the screen hosting the alignment-guide lines knows which to flash. Never fires during a
    // group drag — "center" isn't well-defined for a whole group at once.
    onCenterSnap: (x: Boolean, y: Boolean) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    // Unbounded, so a layer can be sized past the canvas edge (the canvas clips it) — the
    // canvas's own size used to cap it, which is why a corner drag couldn't grow it any more.
    var mod = Modifier.wrapContentSize(Alignment.Center, unbounded = true).graphicsLayer {
        translationX = item.offset.x
        translationY = item.offset.y
        scaleX = item.scale
        scaleY = item.scale
        rotationZ = item.rotation
        alpha = item.alpha
    }
    if (interactive) {
        mod = mod
            .pointerInput(item.id, groupDragActive) {
                detectTransformGestures { _, pan, zoom, rot ->
                    if (groupDragActive) {
                        onGroupDrag(pan)
                    } else {
                        val (snapped, sx, sy) = snapToCenter(item.offset + pan)
                        item.offset = snapped
                        item.scale *= zoom; item.rotation += rot
                        onCenterSnap(sx, sy)
                    }
                    onSelect()
                }
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onSelect() },
                    // Double-tap edits what the layer *is*: retype text, or swap the photo.
                    onDoubleTap = {
                        when (item) {
                            is TextItem -> onEditText(item)
                            is ImageItem -> onReplaceImage(item)
                        }
                    },
                )
            }
    }
    mod = mod.then(
        if (selected || multiSelected) {
            Modifier.border(1.5.dp, if (multiSelected) MultiSelectBlue else Gold, RoundedCornerShape(6.dp)).padding(4.dp)
        } else Modifier.padding(4.dp),
    )

    Box(mod) {
        OverlayContent(item, context, Modifier)
        // A small handle fixed at the same corner on every item — tap it to add/remove this
        // item from the multi-selection without disturbing the normal single-select-and-drag
        // behavior of tapping the item's own body.
        if (multiSelectEnabled && interactive) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset((-11).dp, (-11).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (multiSelected) MultiSelectBlue else Color.White)
                    .border(1.5.dp, if (multiSelected) Color.White else MultiSelectBlue, CircleShape)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleMultiSelect,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (multiSelected) Icon(Icons.Rounded.Check, "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        // Paint-app-style corner handles — drag one to set width/height independently, instead
        // of only the uniform pinch/slider `scale`. Anchored at the OPPOSITE corner (dragging
        // bottom-right keeps top-left in place), which needs `offset` nudged by half the size
        // change on each drag — see ResizeHandle for the rotation-aware math. The canvas template
        // editor keeps multi-select on essentially all the time, whose own toggle circle already
        // sits at the top-start corner — skip only that one handle there instead of hiding the
        // whole feature, since the other three corners still resize the box fine on their own.
        if (item is ImageItem && selected && interactive) {
            if (!multiSelectEnabled) ResizeHandle(Alignment.TopStart, sx = -1f, sy = -1f, item = item)
            ResizeHandle(Alignment.TopEnd, sx = 1f, sy = -1f, item = item)
            ResizeHandle(Alignment.BottomStart, sx = -1f, sy = 1f, item = item)
            ResizeHandle(Alignment.BottomEnd, sx = 1f, sy = 1f, item = item)
        }
    }
}

private val MultiSelectBlue = Color(0xFF3B82F6)
private const val MIN_IMAGE_ITEM_DP = 24f

/**
 * One corner drag handle for [ImageItem] resizing. [sx]/[sy] are this corner's own sign
 * (-1 = start/top side, +1 = end/bottom side) — growing the box in that corner's direction while
 * the OPPOSITE corner stays anchored means the box's center has to shift by half the applied
 * size delta, in the corner's local direction, then rotated into the parent's coordinate space
 * by the item's current [ImageItem.rotation] (translation is applied in parent space by
 * `graphicsLayer`, unlike scale/rotation which pivot around the layer's own center — see
 * [OverlayView]'s `mod`). Because this handle is a CHILD of that already-transformed box, the
 * drag deltas [detectDragGestures] reports here are already local (pre-rotation, pre-scale) —
 * Compose's pointer dispatch walks back through ancestor `graphicsLayer`s — so no separate
 * scale/rotation correction is needed on the *input* side, only on where the anchor compensation
 * gets applied.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.ResizeHandle(align: Alignment, sx: Float, sy: Float, item: ImageItem) {
    val density = LocalDensity.current
    Box(
        Modifier
            .align(align)
            .offset(x = 11.dp * sx, y = 11.dp * sy)
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, Gold, CircleShape)
            .pointerInput(item.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val ldx = with(density) { dragAmount.x.toDp().value }
                    val ldy = with(density) { dragAmount.y.toDp().value }
                    val newWidth = (item.widthDp + sx * ldx).coerceAtLeast(MIN_IMAGE_ITEM_DP)
                    val newHeight = (item.heightDp + sy * ldy).coerceAtLeast(MIN_IMAGE_ITEM_DP)
                    val dW = newWidth - item.widthDp
                    val dH = newHeight - item.heightDp
                    item.widthDp = newWidth
                    item.heightDp = newHeight
                    val localShiftX = sx * dW / 2f
                    val localShiftY = sy * dH / 2f
                    val rad = Math.toRadians(item.rotation.toDouble())
                    val cos = kotlin.math.cos(rad).toFloat()
                    val sin = kotlin.math.sin(rad).toFloat()
                    item.offset += Offset(
                        localShiftX * cos - localShiftY * sin,
                        localShiftX * sin + localShiftY * cos,
                    )
                }
            },
    )
}

/**
 * Stretches the content by [sx]/[sy] AND reports the stretched size as its own, so the selection
 * outline and handles wrap the text as it now looks (a bare graphicsLayer scale would draw past
 * them). Measured against the incoming bounds divided by the stretch, so a widened line wraps
 * where it would have to after stretching, instead of overflowing the canvas.
 */
private fun Modifier.stretch(sx: Float, sy: Float): Modifier =
    if (sx == 1f && sy == 1f) this else this.layout { measurable, constraints ->
        val inner = constraints.copy(
            minWidth = 0, minHeight = 0,
            maxWidth = if (constraints.hasBoundedWidth) (constraints.maxWidth / sx).toInt() else Constraints.Infinity,
            maxHeight = if (constraints.hasBoundedHeight) (constraints.maxHeight / sy).toInt() else Constraints.Infinity,
        )
        val p = measurable.measure(inner)
        val w = (p.width * sx).roundToInt()
        val h = (p.height * sy).roundToInt()
        layout(w, h) {
            p.placeWithLayer((w - p.width) / 2, (h - p.height) / 2) { scaleX = sx; scaleY = sy }
        }
    }

@Composable
private fun OverlayContent(item: EditorItem, context: android.content.Context, mod: Modifier) {
    when (item) {
        is TextItem -> {
            // Reading `family` here (even though only the straight-text branch renders it)
            // means a custom font finishing its download — which changes `family` — is also
            // what re-triggers the bend-text bitmap below to pick it up, via the remember key.
            val family = rememberFontFamily(item.fontPath)
            @Suppress("NAME_SHADOWING") val mod = mod.stretch(item.stretchX, item.stretchY)
            if (item.bend != 0f) {
                val bmp = remember(
                    item.text, item.fontSize, item.bend, item.colorArgb, item.gradient, item.gradientColor2Argb,
                    item.outline, item.outlineWidth, item.outlineColorArgb, item.shadow, item.shadowRadius,
                    item.shadowColorArgb, item.glow, item.glowColorArgb, item.letterSpacing, item.bold, item.fontPath,
                    family,
                ) { renderCurvedText(context, item) }
                if (bmp != null) {
                    androidx.compose.foundation.Image(bmp.asImageBitmap(), null, modifier = mod)
                } else {
                    Box(mod) { Text(item.text, color = Color(item.colorArgb), fontFamily = family, fontSize = item.fontSize.sp, fontWeight = FontWeight.Bold) }
                }
            } else {
                val gradientBrush = if (item.gradient) {
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(item.colorArgb), Color(item.gradientColor2Argb)))
                } else null
                val base = TextStyle(
                    fontFamily = family,
                    fontSize = item.fontSize.sp,
                    fontWeight = if (item.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (item.italic) FontStyle.Italic else FontStyle.Normal,
                    letterSpacing = item.letterSpacing.sp,
                    lineHeight = (item.fontSize * item.lineHeightMul).sp,
                    textAlign = when (item.align) { 0 -> TextAlign.Left; 2 -> TextAlign.Right; else -> TextAlign.Center },
                    textGeometricTransform = TextGeometricTransform(scaleX = 1f, skewX = item.skewX),
                    shadow = if (item.shadow) Shadow(Color(item.shadowColorArgb), Offset(item.shadowRadius, item.shadowRadius), item.shadowRadius) else null,
                )
                Box(modifier = mod) {
                    val hl = item.highlightArgb
                    val inner = if (hl != null) {
                        Modifier.background(Color(hl), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    } else Modifier
                    Box(inner) {
                        if (item.glow) {
                            Text(item.text, style = base.copy(color = Color(item.glowColorArgb), shadow = Shadow(Color(item.glowColorArgb), Offset.Zero, 26f)))
                        }
                        if (item.outline) {
                            Text(item.text, style = base.copy(color = Color(item.outlineColorArgb), drawStyle = Stroke(width = item.outlineWidth, join = StrokeJoin.Round)))
                        }
                        if (gradientBrush != null) Text(item.text, style = base.copy(brush = gradientBrush))
                        else Text(item.text, style = base.copy(color = Color(item.colorArgb)))
                    }
                }
            }
        }
        is ImageItem -> StrokedImage(item, mod)
    }
}

/**
 * An image layer, with an optional sticker-style outline traced around its own cut-out (not
 * a bounding box). Drawn by stamping the image's silhouette — tinted solid via a SrcIn colour
 * filter — at several offsets around a circle of radius [ImageItem.strokeWidth], then the real
 * image on top. Same technique the admin web editor uses, so a template's stroke looks the same
 * on both.
 */
@Composable
private fun StrokedImage(item: ImageItem, modifier: Modifier) {
    Box(
        modifier
            .size(item.widthDp.dp, item.heightDp.dp)
            .drawWithCache {
                // The current frame for an animated sticker, the still otherwise.
                val img = item.shownBitmap.asImageBitmap()
                val strokePx = item.strokeWidth.dp.toPx()
                // Fills the box exactly (no letterboxing), matching the plain Image() below
                // (ContentScale.FillBounds) — otherwise the stroke silhouette wouldn't line up
                // with what's actually visible. The box's own w/h ratio no longer always matches
                // the bitmap's own once a corner handle has resized it non-uniformly, which is
                // exactly the point of that handle — stretch to fit, not fit-and-letterbox.
                val dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                val dstOffset = IntOffset.Zero
                val paint = Paint().apply {
                    colorFilter = ColorFilter.tint(Color(item.strokeColorArgb), BlendMode.SrcIn)
                }
                val steps = 16
                onDrawWithContent {
                    if (strokePx > 0f) {
                        drawIntoCanvas { canvas ->
                            for (i in 0 until steps) {
                                val angle = (i / steps.toFloat()) * (2f * Math.PI.toFloat())
                                canvas.save()
                                canvas.translate(kotlin.math.cos(angle) * strokePx, kotlin.math.sin(angle) * strokePx)
                                canvas.drawImageRect(image = img, dstOffset = dstOffset, dstSize = dstSize, paint = paint)
                                canvas.restore()
                            }
                        }
                    }
                    drawContent()
                }
            },
    ) {
        androidx.compose.foundation.Image(
            bitmap = item.shownBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(),
            // FillBounds, not the default Fit: the box is already sized to widthDp/heightDp (see
            // above), so this always fills it exactly — including when a corner handle has made
            // the box a different ratio than the bitmap's own, which is the point of that handle.
            contentScale = ContentScale.FillBounds,
            // A shape is a flat-filled bitmap — re-tinting it (instead of re-rendering) is how
            // its color can change after the fact.
            colorFilter = if (item.isShape) ColorFilter.tint(Color(item.shapeColorArgb), BlendMode.SrcIn) else null,
        )
    }
}

// ---------- Panels ----------

@Composable
fun FilterPanel(
    preset: FilterPreset, onPreset: (FilterPreset) -> Unit,
    brightness: Float, onBrightness: (Float) -> Unit,
    contrast: Float, onContrast: (Float) -> Unit,
    saturation: Float, onSaturation: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPreset.entries.forEach { p ->
                val sel = p == preset
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(if (sel) Gold else AppSurfaceVariant)
                        .clickable { onPreset(p) }.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(p.label, color = if (sel) AppBackground else AppOnSurface, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        LabeledSlider("Brightness", brightness, -100f..100f, onBrightness)
        LabeledSlider("Contrast", contrast, 0.5f..2f, onContrast)
        LabeledSlider("Saturation", saturation, 0f..2f, onSaturation)
    }
}

@Composable
fun DrawPanel(color: Int, onColor: (Int) -> Unit, width: Float, onWidth: (Float) -> Unit, onUndo: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TEXT_COLORS.forEach { c ->
                    val argb = c.toArgb()
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(c)
                            .border(if (argb == color) 3.dp else 1.dp, if (argb == color) Gold else AppSurfaceVariant, CircleShape)
                            .clickable { onColor(argb) },
                    )
                }
            }
            Icon(Icons.Rounded.Undo, "Undo", tint = AppOnSurface, modifier = Modifier.padding(start = 10.dp).size(24.dp).clickable(onClick = onUndo))
        }
        LabeledSlider("Brush size", width, 4f..60f, onWidth)
    }
}

@Composable
fun BackgroundPanel(current: Color?, onPick: (Color?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).checkerboard(8f)
                .border(if (current == null) 3.dp else 1.dp, if (current == null) Gold else AppSurfaceVariant, CircleShape)
                .clickable { onPick(null) },
            contentAlignment = Alignment.Center,
        ) { Text("∅", color = AppOnSurface, fontSize = 14.sp) }
        val colors = listOf(Color.White, Color.Black, Gold, Color(0xFFE5484D), Color(0xFF46A758), Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFF8B5CF6))
        colors.forEach { c ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c)
                    .border(if (current == c) 3.dp else 1.dp, if (current == c) Gold else AppSurfaceVariant, CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
fun ItemControls(
    item: EditorItem,
    onOpacity: (Float) -> Unit,
    onTextColor: (Int) -> Unit,
    onFont: (String?) -> Unit,
    onTextSize: (Float) -> Unit,
    onOutline: () -> Unit,
    onEditText: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (item is TextItem) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TEXT_COLORS.forEach { c ->
                    val argb = c.toArgb()
                    Box(Modifier.size(28.dp).clip(CircleShape).background(c).border(if (argb == item.colorArgb) 3.dp else 1.dp, if (argb == item.colorArgb) Gold else AppSurfaceVariant, CircleShape).clickable { onTextColor(argb) })
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EDITOR_FONTS.forEach { (_, path) ->
                    val sel = path == item.fontPath
                    val family = rememberFontFamily(path)
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) AppSurfaceVariant else AppSurface).border(1.dp, if (sel) Gold else Color.Transparent, RoundedCornerShape(10.dp)).clickable { onFont(path) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text("Aa", color = if (sel) Gold else AppOnSurface, fontFamily = family, fontSize = 16.sp)
                    }
                }
            }
            LabeledSlider("Text size", item.fontSize, 16f..120f, onTextSize)
        }
        LabeledSlider("Opacity", item.alpha, 0.1f..1f, onOpacity)
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (item is TextItem) {
                SmallPill("Edit", Icons.Rounded.Edit) { onEditText() }
                TogglePill("Outline", item.outline) { onOutline() }
            }
            SmallPill("Copy", Icons.Rounded.ContentCopy) { onDuplicate() }
            SmallPill("Delete", Icons.Rounded.Delete, danger = true) { onDelete() }
        }
    }
}

@Composable
private fun SmallPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.neuRaised(cornerRadius = 50.dp, elevation = 5.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) Danger else Gold, modifier = Modifier.size(16.dp))
        Text("  $label", color = if (danger) Danger else AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TogglePill(label: String, on: Boolean, onClick: () -> Unit) {
    // On reads as pressed INTO the panel, off floats above it — same language as the tool rail.
    Box(
        Modifier
            .then(
                if (on) Modifier.neuInset(cornerRadius = 50.dp, elevation = 5.dp, background = Gold)
                else Modifier.neuRaised(cornerRadius = 50.dp, elevation = 5.dp),
            )
            .clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (on) AppBackground else AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.width(88.dp))
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = AppSurfaceVariant),
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------- Emoji + sticker picker ----------

private val EMOJIS = listOf(
    "😀", "😂", "🤣", "😍", "😎", "😭", "😡", "🥰", "😱", "🤔", "😏", "😉", "🙃", "😴", "🤗", "😇",
    "👍", "👎", "👏", "🙏", "💪", "🔥", "❤️", "💔", "💯", "✨", "⭐", "🎉", "😜", "🤪", "😅", "😬",
    "🥳", "😤", "😐", "🙄", "😳", "🤯", "🥲", "😢", "💃", "🕺", "👀", "💀", "🤡", "👑", "🌟", "⚡",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerEmojiPicker(
    onEmoji: (String) -> Unit,
    onSticker: (Sticker) -> Unit,
    onDismiss: () -> Unit,
    initialTab: Int = 0,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(initialTab) }
    val catalog by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val adminEmojis by ServiceLocator.catalogRepository.emojis.collectAsStateSafe()
    // Admin-added emojis show first, then the built-ins (deduped) — so anything added from the
    // web admin always appears without hiding the defaults.
    val emojiList = remember(adminEmojis) { (adminEmojis + EMOJIS).distinct() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AppSurface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp).padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabPill("Emoji", tab == 0) { tab = 0 }
                TabPill("Stickers", tab == 1) { tab = 1 }
            }
            when (tab) {
                0 -> LazyVerticalGrid(
                    columns = GridCells.Fixed(6), modifier = Modifier.heightIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(emojiList) { e ->
                        Box(Modifier.aspectRatio(1f).neuInset(cornerRadius = 10.dp, elevation = 4.dp).clickable { onEmoji(e) }, contentAlignment = Alignment.Center) {
                            Text(e, fontSize = 26.sp)
                        }
                    }
                }
                else -> StickerBrowser(catalog, onSticker)
            }
        }
    }
}

private val DecoratorTint = Color.Black.copy(alpha = 0.62f)

/**
 * The Decorator: admin-curated transparent overlays (glasses, a cap, etc.) — tap one to drop it
 * onto the canvas as a new layer. A see-through black sheet 80% of the screen tall, with no scrim,
 * so the photo being decorated stays visible behind it. Admin-managed category toggles run along
 * the bottom; only categories that actually hold a decoration get one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoratorSheet(onPick: (com.stickers.bank.data.model.Decoration) -> Unit, onDismiss: () -> Unit) {
    val decorations by ServiceLocator.catalogRepository.decorations.collectAsStateSafe()
    val categories by ServiceLocator.catalogRepository.decorationCategories.collectAsStateSafe()
    val usedCategories = remember(decorations, categories) {
        val used = decorations.mapTo(HashSet()) { it.categoryId }
        categories.filter { it.id in used }
    }
    var categoryId by remember { mutableStateOf<String?>(null) } // null = All
    val shown = remember(decorations, categoryId) {
        if (categoryId == null) decorations else decorations.filter { it.categoryId == categoryId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DecoratorTint,
        tonalElevation = 0.dp,
        scrimColor = Color.Transparent,
        dragHandle = {
            Box(
                Modifier.padding(vertical = 10.dp).size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.45f)),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f).navigationBarsPadding().padding(horizontal = 14.dp)) {
            Text(
                "DECORATOR", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 10.dp),
            )
            if (decorations.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Decorations coming soon", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.id }) { d ->
                        Box(
                            // Medium-grey checks, so a transparent PNG still reads as something
                            // and dark art (black sunglasses) doesn't vanish into the black tint.
                            Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).editorCheckerboard().clickable { onPick(d) },
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = d.image, contentDescription = null,
                                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp),
                            )
                        }
                    }
                }
            }
            if (usedCategories.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DecoratorToggle("All", categoryId == null) { categoryId = null }
                    usedCategories.forEach { c -> DecoratorToggle(c.name, categoryId == c.id) { categoryId = c.id } }
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DecoratorToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(label, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Stands in for a real category id in the language dropdown, to narrow the picker to the
 * user's own creations. Not a catalog id, so it can never collide with one. */
private const val MINE_SOURCE_ID = "__my_stickers__"

/** Sticker tab: filter by type (all/static/animated), language (category) → category (collection), and search. */
@Composable
private fun StickerBrowser(catalog: Catalog, onSticker: (Sticker) -> Unit) {
    var type by remember { mutableStateOf("all") } // all | static | animated
    var langId by remember { mutableStateOf<String?>(null) }
    var collId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    // The user's own editor creations, offered here next to the catalog so a sticker they made
    // can be dropped into a new one. Newest first, matching the My Stickers folder's own order.
    val custom by ServiceLocator.customStickerRepository.stickers.collectAsStateSafe()
    val mine = remember(custom) { custom.reversed() }

    // Picking a different language resets the category choice.
    LaunchedEffect(langId) { collId = null }

    val collections = remember(catalog, langId) {
        val cats = if (langId == null) catalog.categories else catalog.categories.filter { it.id == langId }
        cats.flatMap { c -> c.groups.flatMap { g -> g.collections } }
    }

    val results = remember(catalog, mine, type, langId, collId, query, collections) {
        val base = when {
            // Search spans everything the picker can offer, the user's own stickers included —
            // those carry whatever caption and tags were typed into the save dialog, so searching
            // by tag finds them exactly the way it finds catalog stickers.
            query.isNotBlank() -> (mine + catalog.allStickers()).filter { it.matches(query) }
            langId == MINE_SOURCE_ID -> mine
            collId != null -> collections.firstOrNull { it.id == collId }?.stickers ?: emptyList()
            langId != null -> catalog.categories.firstOrNull { it.id == langId }?.let { cat ->
                cat.stickers + cat.groups.flatMap { g -> g.stickers + g.collections.flatMap { it.stickers } }
            } ?: emptyList()
            // Unfiltered: the user's own first, so they don't have to scroll the whole catalog
            // to reach the stickers they just made.
            else -> mine + catalog.allStickers()
        }
        base.filter {
            when (type) { "static" -> !it.animated; "animated" -> it.animated; else -> true }
        }.distinctBy { it.id }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PickerDropdown(
                current = when (type) { "static" -> "Stickers"; "animated" -> "Animated"; else -> "All" },
                options = listOf<Pair<String?, String>>("all" to "All", "static" to "Stickers", "animated" to "Animated"),
                onSelect = { type = it ?: "all" },
            )
            PickerDropdown(
                current = when (langId) {
                    null -> "Language"
                    MINE_SOURCE_ID -> "My Stickers"
                    else -> catalog.categories.firstOrNull { it.id == langId }?.name ?: "Language"
                },
                options = listOf<Pair<String?, String>>(
                    null to "All languages",
                    MINE_SOURCE_ID to "My Stickers",
                ) + catalog.categories.map { it.id to it.name },
                onSelect = { langId = it },
            )
            PickerDropdown(
                current = collections.firstOrNull { it.id == collId }?.name ?: "Category",
                options = listOf<Pair<String?, String>>(null to "All categories") + collections.map { it.id to it.name },
                onSelect = { collId = it },
            )
        }
        SbSearchBar(query, { query = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp), placeholder = "Search by name or tag — yours included")

        if (results.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                Text("Nothing here yet", color = AppOnSurfaceMuted, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4), modifier = Modifier.heightIn(max = 320.dp),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(results, key = { it.id }) { s ->
                    Box(
                        Modifier.aspectRatio(1f).neuInset(cornerRadius = 10.dp, elevation = 4.dp).clickable { onSticker(s) },
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(model = s.coilModel(), contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp))
                        if (s.animated) {
                            Box(
                                Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(Gold).padding(horizontal = 4.dp),
                            ) { Text("▶", color = OnGold, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerDropdown(current: String, options: List<Pair<String?, String>>, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.neuRaised(cornerRadius = 50.dp, elevation = 5.dp).clip(RoundedCornerShape(50)).clickable { open = true }
                .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current, color = AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.ArrowDropDown, null, tint = AppOnSurfaceMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); open = false })
            }
        }
    }
}

@Composable
private fun TabPill(label: String, sel: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (sel) Gold else AppSurfaceVariant).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 9.dp)) {
        Text(label, color = if (sel) AppBackground else AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ---------- Dialogs ----------

@Composable
fun TextInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppSurface,
        title = { Text("Text", color = AppOnSurface) },
        text = { TextField(text, { text = it }, placeholder = { Text("Type here", color = AppOnSurfaceMuted) }, colors = dialogFieldColors()) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Add", color = Gold, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) } },
    )
}

@Composable
fun SaveDialog(onDismiss: () -> Unit, onSave: (String, List<String>) -> Unit) {
    var caption by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppSurface,
        title = { Text("Save sticker", color = AppOnSurface) },
        text = {
            Column {
                TextField(caption, { caption = it }, singleLine = true, placeholder = { Text("Caption (optional)", color = AppOnSurfaceMuted) }, colors = dialogFieldColors())
                androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
                TextField(tags, { tags = it }, singleLine = true, placeholder = { Text("Tags, comma separated", color = AppOnSurfaceMuted) }, colors = dialogFieldColors())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tagList = tags.split(",", " ").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                onSave(caption.trim(), tagList)
            }) { Text("Save", color = Gold, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) } },
    )
}

@Composable
fun dialogFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppSurfaceVariant, unfocusedContainerColor = AppSurfaceVariant,
    focusedIndicatorColor = Gold, unfocusedIndicatorColor = Color.Transparent,
    cursorColor = Gold, focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface,
)

/**
 * Renders **curved (bent) text** to a bitmap via native drawTextOnPath — the Compose Text
 * composable can't arc text. Supports fill/gradient, outline stroke, shadow, and glow through
 * the native Paint. Returns null on any failure (caller falls back to straight text).
 */
fun renderCurvedText(context: android.content.Context, item: TextItem): android.graphics.Bitmap? = runCatching {
    val text = item.text.ifBlank { return@runCatching null }
    val dp = context.resources.displayMetrics.density
    val tf = item.fontPath?.let { p ->
        if (p.startsWith("http")) {
            // A custom font: only usable here once already cached (this runs synchronously
            // during composition) — the caller re-invokes this once the download completes.
            FontCache.cachedFileIfPresent(context, p)?.let { f -> runCatching { android.graphics.Typeface.createFromFile(f) }.getOrNull() }
        } else {
            runCatching { android.graphics.Typeface.createFromAsset(context.assets, p) }.getOrNull()
        }
    } ?: android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (item.bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    val sizePx = item.fontSize * dp
    fun mk() = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx
        typeface = tf
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = item.letterSpacing / item.fontSize.coerceAtLeast(1f)
        isFakeBoldText = item.bold && item.fontPath != null
    }
    val textW = mk().measureText(text).coerceAtLeast(1f)
    val bendN = (item.bend / 100f).coerceIn(-1f, 1f)
    val sweep = kotlin.math.abs(bendN) * 150f
    val sweepRad = Math.toRadians(sweep.toDouble())
    val radius = if (sweep < 1f) 0f else (textW / sweepRad).toFloat()
    val padX = sizePx
    val sag = if (radius == 0f) 0f else (radius * (1 - kotlin.math.cos(sweepRad / 2))).toFloat()
    val w = (textW + padX * 2).toInt().coerceIn(1, 2048)
    val h = (sizePx * 2f + sag + padX).toInt().coerceIn(1, 2048)
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val cx = w / 2f
    val path = android.graphics.Path()
    when {
        radius == 0f -> {
            val y = h / 2f + sizePx * 0.35f
            path.moveTo(padX, y); path.lineTo(w - padX, y)
        }
        bendN > 0f -> { // arch up ∩
            val cyc = sizePx + radius
            path.addArc(android.graphics.RectF(cx - radius, cyc - radius, cx + radius, cyc + radius), 270f - sweep / 2f, sweep)
        }
        else -> { // smile ∪
            val cyc = h - sizePx - radius
            path.addArc(android.graphics.RectF(cx - radius, cyc - radius, cx + radius, cyc + radius), 90f - sweep / 2f, sweep)
        }
    }
    if (item.outline) {
        val op = mk().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = item.outlineWidth
            strokeJoin = android.graphics.Paint.Join.ROUND
            color = item.outlineColorArgb
        }
        canvas.drawTextOnPath(text, path, 0f, 0f, op)
    }
    val fp = mk().apply {
        style = android.graphics.Paint.Style.FILL
        if (item.gradient) {
            shader = android.graphics.LinearGradient(cx - textW / 2f, 0f, cx + textW / 2f, 0f, item.colorArgb, item.gradientColor2Argb, android.graphics.Shader.TileMode.CLAMP)
        } else {
            color = item.colorArgb
        }
        if (item.glow) setShadowLayer(24f, 0f, 0f, item.glowColorArgb)
        else if (item.shadow) setShadowLayer(item.shadowRadius.coerceAtLeast(0.1f), item.shadowRadius * 0.6f, item.shadowRadius * 0.6f, item.shadowColorArgb)
    }
    canvas.drawTextOnPath(text, path, 0f, 0f, fp)
    bmp
}.getOrNull()

// ---------- Color filters ----------

enum class FilterPreset(val label: String) {
    ORIGINAL("Original"), MONO("Mono"), SEPIA("Sepia"), VIVID("Vivid"),
    COOL("Cool"), WARM("Warm"), NOIR("Noir"), FADE("Fade")
}

private fun presetMatrix(preset: FilterPreset): android.graphics.ColorMatrix? = when (preset) {
    FilterPreset.ORIGINAL -> null
    FilterPreset.MONO -> android.graphics.ColorMatrix().apply { setSaturation(0f) }
    FilterPreset.SEPIA -> android.graphics.ColorMatrix(
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    FilterPreset.VIVID -> android.graphics.ColorMatrix().apply { setSaturation(1.6f) }
    FilterPreset.COOL -> android.graphics.ColorMatrix(
        floatArrayOf(0.9f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1.25f, 0f, 0f, 0f, 0f, 0f, 1f, 0f),
    )
    FilterPreset.WARM -> android.graphics.ColorMatrix(
        floatArrayOf(1.25f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0.85f, 0f, 0f, 0f, 0f, 0f, 1f, 0f),
    )
    FilterPreset.NOIR -> android.graphics.ColorMatrix().apply {
        setSaturation(0f)
        postConcat(android.graphics.ColorMatrix(floatArrayOf(1.3f, 0f, 0f, 0f, -30f, 0f, 1.3f, 0f, 0f, -30f, 0f, 0f, 1.3f, 0f, -30f, 0f, 0f, 0f, 1f, 0f)))
    }
    FilterPreset.FADE -> android.graphics.ColorMatrix(
        floatArrayOf(0.8f, 0f, 0f, 0f, 30f, 0f, 0.8f, 0f, 0f, 30f, 0f, 0f, 0.8f, 0f, 30f, 0f, 0f, 0f, 1f, 0f),
    )
}

/**
 * Builds the live preview color filter. brightness/warmth/shadows/highlights are in
 * roughly -100..100, contrast/saturation are multipliers around 1. Warmth shifts red up /
 * blue down; shadows lifts the offset; highlights is an overall gain (both are linear
 * approximations, plenty for a sticker editor).
 */
fun buildColorFilter(
    preset: FilterPreset,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float = 0f,
    shadows: Float = 0f,
    highlights: Float = 0f,
): ColorFilter? {
    val arr = buildColorMatrixArray(preset, brightness, contrast, saturation, warmth, shadows, highlights) ?: return null
    return ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(arr))
}

/** The raw 4×5 color matrix (or null if neutral) — used to apply the same look to video frames. */
fun buildColorMatrixArray(
    preset: FilterPreset,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float = 0f,
    shadows: Float = 0f,
    highlights: Float = 0f,
): FloatArray? {
    val neutral = preset == FilterPreset.ORIGINAL && brightness == 0f && contrast == 1f &&
        saturation == 1f && warmth == 0f && shadows == 0f && highlights == 0f
    if (neutral) return null

    val cm = android.graphics.ColorMatrix().apply { setSaturation(saturation) }
    val c = contrast
    val tr = (1f - c) * 128f
    cm.postConcat(
        android.graphics.ColorMatrix(
            floatArrayOf(c, 0f, 0f, 0f, tr, 0f, c, 0f, 0f, tr, 0f, 0f, c, 0f, tr, 0f, 0f, 0f, 1f, 0f),
        ),
    )
    // Highlights → overall gain.
    val g = 1f + highlights / 300f
    cm.postConcat(
        android.graphics.ColorMatrix(
            floatArrayOf(g, 0f, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, 0f, g, 0f, 0f, 0f, 0f, 0f, 1f, 0f),
        ),
    )
    // Brightness + shadows → offset; warmth → red up / blue down.
    val off = brightness + shadows * 0.4f
    val w = warmth * 0.6f
    cm.postConcat(
        android.graphics.ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, off + w,
                0f, 1f, 0f, 0f, off,
                0f, 0f, 1f, 0f, off - w,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
    presetMatrix(preset)?.let { cm.postConcat(it) }
    return cm.array
}
