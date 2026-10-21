package com.stickers.bank.ui.create

import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.SideEffect
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.Brightness5
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipToFront
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Rotate90DegreesCcw
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tonality
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.components.SquircleShape
import com.stickers.bank.ui.components.checkerboard
import com.stickers.bank.ui.components.editorCheckerboard
import com.stickers.bank.ui.theme.AccentTint
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Danger
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import com.stickers.bank.data.model.TEMPLATE_CANVAS
import com.stickers.bank.data.model.TemplateLayer
import com.stickers.bank.ui.crop.CropDialog
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.parseHexColor
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceElevated
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.neuClickable
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.util.BitmapUtils
import com.stickers.bank.util.VideoToWebp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ---------- Continuous-corner (squircle) shapes used across the editor ----------

private val CanvasSquircle = SquircleShape(20.dp)
private val CardSquircle = SquircleShape(14.dp)
private val ChipSquircle = SquircleShape(12.dp)

// ---------- Model ----------

val EDITOR_FONTS: List<Pair<String, String?>> = listOf(
    "Default" to null,
    "Quicksand" to "fonts/Quicksand-Medium.ttf",
) + (1..10).map { "Font $it" to "fonts/font_$it.ttf" }

val TEXT_COLORS: List<Color> = listOf(
    Color.White, Color.Black, Gold, Color(0xFFE5484D), Color(0xFF46A758),
    Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFF14B8A6),
)

sealed class EditorItem {
    abstract val id: Long
    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableStateOf(1f)
    var rotation by mutableStateOf(0f)
    var alpha by mutableStateOf(1f)
}

/**
 * The stacking order IS the list's own order — index 0 paints first (furthest back), the last
 * index paints last (frontmost). A freshly added item already lands at the end (every add site
 * appends), so it's naturally on top without needing a separate promotion mechanism; these three
 * just reposition an existing item within that same order.
 */
fun bringToFront(items: SnapshotStateList<EditorItem>, item: EditorItem) {
    items.remove(item); items.add(item)
}

fun sendToBack(items: SnapshotStateList<EditorItem>, item: EditorItem) {
    items.remove(item); items.add(0, item)
}

/** 1-based, 1 = frontmost — the convention the canvas editor's own priority dropdown shows. */
fun EditorItem.priorityFromTop(items: List<EditorItem>): Int = items.size - items.indexOf(this)

/** Moves [item] so its [priorityFromTop] becomes [priority], clamped to the valid 1..size range. */
fun moveToPriority(items: SnapshotStateList<EditorItem>, item: EditorItem, priority: Int) {
    val total = items.size
    items.remove(item)
    val target = (total - priority).coerceIn(0, items.size)
    items.add(target, item)
}

class TextItem(override val id: Long, text: String, colorArgb: Int, fontPath: String?) : EditorItem() {
    var text by mutableStateOf(text)
    var colorArgb by mutableStateOf(colorArgb)
    var fontPath by mutableStateOf(fontPath)
    var outline by mutableStateOf(false)
    var fontSize by mutableStateOf(40f)
    var letterSpacing by mutableStateOf(0f)      // sp
    var lineHeightMul by mutableStateOf(1.1f)    // × font size
    var align by mutableStateOf(1)               // 0 left · 1 center · 2 right
    var bold by mutableStateOf(true)
    var italic by mutableStateOf(false)
    var skewX by mutableStateOf(0f)              // -1..1 horizontal shear
    var stretchX by mutableStateOf(1f)           // width-only stretch, × (1 = none)
    var stretchY by mutableStateOf(1f)           // height-only stretch, × (1 = none)
    var bend by mutableStateOf(0f)               // -100..100 curved text
    var highlightArgb by mutableStateOf<Int?>(null) // background highlight
    var gradient by mutableStateOf(false)
    var gradientColor2Argb by mutableStateOf(0xFFFFC107.toInt())
    var outlineWidth by mutableStateOf(8f)
    var outlineColorArgb by mutableStateOf(0xFF000000.toInt())
    var shadow by mutableStateOf(false)
    var shadowColorArgb by mutableStateOf(0xFF000000.toInt())
    var shadowRadius by mutableStateOf(6f)
    var glow by mutableStateOf(false)
    var glowColorArgb by mutableStateOf(0xFFFFC107.toInt())
}

/**
 * [bitmap] is observable so double-tapping the layer can swap the photo in place, keeping
 * whatever position/scale/rotation the user already gave it. [sizeDp] is the LONGER side of the
 * box it's drawn into at scale=1 — 150dp for stickers/overlays added in this editor, or a
 * template's own reference size (converted to this canvas's dp) so it matches what the admin
 * built. The box's actual [widthDp]/[heightDp] derive from [bitmap]'s own pixel aspect ratio at
 * construction time (via [resetSizeFromBitmap]) so a wide or tall photo keeps its real shape
 * instead of being forced into a square — [sizeDp] alone used to BE the square side length.
 * [widthDp]/[heightDp] are independently draggable afterwards (the corner resize handles in
 * [OverlayView]), which is what actually differs a photo's box from a plain uniform [scale].
 */
class ImageItem(
    override val id: Long,
    bitmap: Bitmap,
    val sizeDp: Float = 150f,
    /** True only for a shape added via the Shape tool — a flat-filled bitmap whose color can
     * be changed after the fact by tinting it, unlike a real photo/sticker. */
    val isShape: Boolean = false,
    initialShapeColorArgb: Int = android.graphics.Color.WHITE,
    animation: com.stickers.bank.util.AnimatedWebp.Frames? = null,
) : EditorItem() {
    var bitmap by mutableStateOf(bitmap)
    /** Frames of an animated sticker (Animation mode only) — null for a still. [bitmap] stays
     * the first frame, which is what Sticker mode and anything static uses. */
    var animation by mutableStateOf(animation)
    /** Where in [animation] it is right now; the editor's clock (or the exporter) moves it. */
    var animTimeMs by mutableStateOf(0L)
    /** What to draw: the current frame of an animation, otherwise the still. */
    val shownBitmap: Bitmap get() = animation?.at(animTimeMs) ?: bitmap
    var widthDp by mutableStateOf(sizeDp)
    var heightDp by mutableStateOf(sizeDp)
    init { resetSizeFromBitmap(bitmap) }

    /** Re-derives [widthDp]/[heightDp] from a bitmap's own w/h ratio, [sizeDp] as the long side.
     * Called once at construction (real bitmap) and NOT on a later swap-in-place, since by then
     * the user may have deliberately resized the box to something the new photo shouldn't reset. */
    private fun resetSizeFromBitmap(bmp: Bitmap) {
        val w = bmp.width.toFloat(); val h = bmp.height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (w >= h) { widthDp = sizeDp; heightDp = sizeDp * h / w }
        else { heightDp = sizeDp; widthDp = sizeDp * w / h }
    }
    /** After a re-crop changes the picture's shape: the box takes the new w/h ratio, keeping its
     * current longer side, so the layer stays the size it was instead of stretching. */
    fun refitTo(bmp: Bitmap) {
        val w = bmp.width.toFloat(); val h = bmp.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val long = maxOf(widthDp, heightDp)
        if (w >= h) { widthDp = long; heightDp = long * h / w } else { heightDp = long; widthDp = long * w / h }
    }
    // Sticker-style outline around the image's own silhouette (see OverlayView). 0 = off.
    var strokeWidth by mutableStateOf(0f)
    var strokeColorArgb by mutableStateOf(android.graphics.Color.WHITE)
    // Only meaningful when `isShape` — applied as a SrcIn tint at render time (see OverlayView)
    // instead of re-rendering the bitmap, so changing color is instant and reversible.
    var shapeColorArgb by mutableStateOf(initialShapeColorArgb)
}

class DrawStroke(val colorArgb: Int, val width: Float, val eraser: Boolean = false) {
    val points: SnapshotStateList<Offset> = mutableStateListOf()
}

enum class Tool { MOVE, ERASE, DRAW }

enum class EditorTab(val label: String) {
    CROP("Crop"), ADJUST("Adjust"), FILTER("Filter"), DRAW("Draw"), ERASE("Erase"), LAYER("Layer"), TEXT("Text")
}

enum class AdjustTool(val label: String, val icon: ImageVector) {
    BRIGHTNESS("Brightness", Icons.Rounded.Brightness6),
    CONTRAST("Contrast", Icons.Rounded.Contrast),
    SATURATION("Saturation", Icons.Rounded.Opacity),
    WARMTH("Warmth", Icons.Rounded.Thermostat),
    SHADOWS("Shadows", Icons.Rounded.Tonality),
    HIGHLIGHTS("Highlights", Icons.Rounded.Brightness5),
}

enum class LayerTool(val label: String, val icon: ImageVector) {
    OPACITY("Opacity", Icons.Rounded.Opacity),
    SIZE("Size", Icons.Rounded.ZoomOutMap),
    ROTATE("Rotate", Icons.Rounded.Rotate90DegreesCcw),
}

enum class ShapeKind { CIRCLE, SQUARE, ROUNDED, TRIANGLE, HEART }

/** Where a freshly-picked (and optionally cropped) photo should land. */
private sealed interface CropTarget {
    object Base : CropTarget
    object NewOverlay : CropTarget
    data class Replace(val item: ImageItem) : CropTarget
    /** Re-crop a layer already on the canvas (an overlay photo or a sticker) — crop, cut out the
     * background, flip — keeping where it sits. */
    data class Recrop(val item: ImageItem) : CropTarget
}

// ---------- Screen ----------

@Composable
fun EditorScreen(nav: NavHostController, stickerId: String?, animated: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val showAd = com.stickers.bank.ads.rememberInterstitial()
    val graphicsLayer = rememberGraphicsLayer()
    val overlayLayer = rememberGraphicsLayer() // captures ONLY the overlays (for animation frames)

    // Backing out mid-edit (without hitting Done) stashes the CONTENT here — see EditorDraft —
    // so re-entering this same sticker (or Create, for a from-scratch one) resumes it instead of
    // starting blank. Read once per composition; flushed back on dispose further down.
    val draft = remember(stickerId) { EditorDraftStore.get(stickerId) }
    // Sticker vs Animation mode (the toggle in the top bar). Animation mode encodes a picked
    // short video into an animated WebP on-device (see VideoToWebp).
    var isAnim by remember { mutableStateOf(draft?.isAnim ?: animated) }
    var videoUri by remember { mutableStateOf(draft?.videoUri) }

    // ---- Animation timeline ----------------------------------------------------------------
    // Frames are decoded once when a clip is picked (see pickVideo) and drive both the scrub
    // strip and the preview, so dragging the playhead is an array lookup, not a decode.
    var clipFrames by remember { mutableStateOf<List<Pair<Long, Bitmap>>>(emptyList()) }
    var clipDurationMs by remember { mutableStateOf(0L) }
    var framesLoading by remember { mutableStateOf(false) }
    var trimStartMs by remember { mutableStateOf(0L) }
    var trimEndMs by remember { mutableStateOf(0L) }
    var playheadMs by remember { mutableStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    // The clip currently being read — a new pick cancels it, so two strips never interleave.
    var clipLoad by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    // The canvas square's side in dp, as last laid out (layers sized to fill it need it).
    var canvasSideDp by remember { mutableStateOf(0f) }

    var base by remember { mutableStateOf(draft?.base) }
    var baseOffset by remember { mutableStateOf(draft?.baseOffset ?: Offset.Zero) }
    var baseScale by remember { mutableStateOf(draft?.baseScale ?: 1f) }
    // Split from baseScale only when a corner handle stretches one axis; pinch moves both.
    var baseScaleY by remember { mutableStateOf(draft?.baseScaleY ?: 1f) }
    var baseRotation by remember { mutableStateOf(draft?.baseRotation ?: 0f) }
    var baseFlipX by remember { mutableStateOf(draft?.baseFlipX ?: false) }
    // The photo normally sits under every overlay; its Layer panel's "To front" lifts it above
    // them. Photo mode only — in Animation mode the overlays get baked onto the video frames.
    var baseOnTop by remember { mutableStateOf(draft?.baseOnTop ?: false) }
    var baseAlpha by remember { mutableStateOf(draft?.baseAlpha ?: 1f) }
    // The photo itself can be "the selected layer" now, same as an overlay — that's what shows
    // its border + corner resize handles.
    var baseSelected by remember { mutableStateOf(false) }
    // Alignment guides: flash while a drag lands dead-center on either axis. lastSnapAt is a
    // self-canceling debounce — see its LaunchedEffect below — so the guide keeps showing while
    // actively centered but disappears shortly after the drag actually stops.
    var snapGuideX by remember { mutableStateOf(false) }
    var snapGuideY by remember { mutableStateOf(false) }
    var lastSnapAt by remember { mutableStateOf(0L) }
    fun applyCenterSnap(raw: Offset): Offset {
        val (snapped, sx, sy) = snapToCenter(raw)
        snapGuideX = sx; snapGuideY = sy
        if (sx || sy) lastSnapAt = System.currentTimeMillis()
        return snapped
    }
    LaunchedEffect(lastSnapAt) {
        if (lastSnapAt == 0L) return@LaunchedEffect
        kotlinx.coroutines.delay(180)
        snapGuideX = false; snapGuideY = false
    }

    var preset by remember { mutableStateOf(draft?.preset ?: FilterPreset.ORIGINAL) }
    var brightness by remember { mutableStateOf(draft?.brightness ?: 0f) }
    var contrast by remember { mutableStateOf(draft?.contrast ?: 1f) }
    var saturation by remember { mutableStateOf(draft?.saturation ?: 1f) }
    var warmth by remember { mutableStateOf(draft?.warmth ?: 0f) }
    var shadows by remember { mutableStateOf(draft?.shadows ?: 0f) }
    var highlights by remember { mutableStateOf(draft?.highlights ?: 0f) }
    var bgColor by remember { mutableStateOf(draft?.bgColor) }

    val eraseStrokes = remember { mutableStateListOf<SnapshotStateList<Offset>>() }
    var brush by remember { mutableStateOf(60f) }

    val drawStrokes = remember { mutableStateListOf<DrawStroke>().apply { draft?.drawStrokes?.let { addAll(it) } } }
    var drawColor by remember { mutableStateOf(Gold.toArgb()) }
    var drawWidth by remember { mutableStateOf(16f) }
    var drawErase by remember { mutableStateOf(false) } // brush erases only your drawings

    // Same live EditorItem objects as before the dispose, not copies — each one already carries
    // its own mutable offset/scale/rotation/bitmap, so just keeping the references is enough.
    val items: SnapshotStateList<EditorItem> = remember { mutableStateListOf<EditorItem>().apply { draft?.items?.let { addAll(it) } } }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var nextId by remember { mutableStateOf(draft?.nextId ?: 1L) }
    // Ratchets true the moment a save actually completes — see saveSticker/saveAnimated — so
    // backing out AFTER a successful save doesn't resurrect stale content next time.
    var committed by remember { mutableStateOf(false) }
    // True for exactly the span of saveSticker/saveAnimated's work — drives the full-screen
    // SavingOverlay so a slow save (especially video encoding) reads as "in progress" rather
    // than a Done tap that appeared to do nothing.
    var saving by remember { mutableStateOf(false) }
    // 0–1 while an animation is being encoded (shown as a percentage); null = no figure to show.
    var saveProgress by remember { mutableStateOf<Float?>(null) }

    // Re-decode the strip when a draft brought a clip back with us — pickVideo only runs for a
    // freshly chosen video, so without this a resumed animation draft would have no timeline.
    LaunchedEffect(videoUri, isAnim) {
        val uri = videoUri
        if (!isAnim || uri == null || clipFrames.isNotEmpty() || framesLoading) return@LaunchedEffect
        framesLoading = true
        VideoToWebp.previewFlow(context, uri).collect { preview ->
            clipDurationMs = preview.durationMs
            clipFrames = preview.frames
            if (trimEndMs <= 0L) trimEndMs = preview.durationMs
        }
        framesLoading = false
    }

    // The canvas always shows the frame under the playhead, so scrubbing previews the clip with
    // every overlay and adjustment already on top of it — the whole point of the timeline.
    LaunchedEffect(playheadMs, clipFrames) {
        if (clipFrames.isEmpty()) return@LaunchedEffect
        base = clipFrames.minByOrNull { kotlin.math.abs(it.first - playheadMs) }?.second ?: base
    }

    // Playback: advance in real time, looping inside the trim window. Frame-accurate enough at
    // the strip's own sampling rate, and it never blocks on the decoder.
    LaunchedEffect(playing, trimStartMs, trimEndMs, clipDurationMs) {
        if (!playing || clipDurationMs <= 0L) return@LaunchedEffect
        if (playheadMs !in trimStartMs..trimEndMs) playheadMs = trimStartMs
        var last = System.currentTimeMillis()
        while (true) {
            withFrameNanos { }
            val now = System.currentTimeMillis()
            val next = playheadMs + (now - last)
            last = now
            playheadMs = if (next >= trimEndMs) trimStartMs else next
        }
    }

    // Animated stickers play on the canvas: in step with the clip while it plays (so what you
    // see is what gets saved), on their own otherwise. Paused while saving — the exporter sets
    // each frame's time itself then.
    val hasAnimatedItems = isAnim && items.any { it is ImageItem && it.animation != null }
    // The base is the VIDEO's frame only in Animation mode with a clip; otherwise (Sticker mode,
    // or a photo in Animation mode) it's a photo, with all the photo controls.
    val clipMode = isAnim && videoUri != null
    // Something's there in Animation mode, but nothing that moves — Done can't make an animation.
    val animNeedsMotion = isAnim && videoUri == null && !hasAnimatedItems &&
        (base != null || items.isNotEmpty() || drawStrokes.isNotEmpty() || bgColor != null)
    LaunchedEffect(hasAnimatedItems) {
        if (!hasAnimatedItems) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (true) {
            withFrameNanos { }
            if (saving) continue
            val t = if (playing && videoUri != null) playheadMs - trimStartMs else System.currentTimeMillis() - start
            // Only on an actual frame change, so nothing redraws 60 times a second for nothing.
            items.forEach { item ->
                val anim = (item as? ImageItem)?.animation ?: return@forEach
                if (anim.indexAt(t) != anim.indexAt(item.animTimeMs)) item.animTimeMs = t
            }
        }
    }

    var tab by remember { mutableStateOf(EditorTab.ADJUST) }
    var adjustTool by remember { mutableStateOf(AdjustTool.BRIGHTNESS) }
    var layerTool by remember { mutableStateOf(LayerTool.OPACITY) }
    var railActive by remember { mutableStateOf<Int>(0) }
    val tool = when (tab) {
        EditorTab.DRAW -> Tool.DRAW
        EditorTab.ERASE -> Tool.ERASE
        else -> Tool.MOVE
    }

    var showTextDialog by remember { mutableStateOf(false) }
    var editingTextItem by remember { mutableStateOf<TextItem?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showDecorator by remember { mutableStateOf(false) }
    var pickerTab by remember { mutableStateOf(0) } // 0 = emoji, 1 = stickers
    var showShapePicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // ---- Undo / redo ----------------------------------------------------------------
    // A watcher files one entry per settled change (see EditorHistoryWatcher), so undo covers
    // drags and slider nudges too, not just the discrete add/delete actions.
    val history = remember { EditorHistory() }
    var replayCount by remember { mutableStateOf(0) }

    fun snapshot() = EditorSnapshot(
        base = base, baseOffset = baseOffset, baseScale = baseScale, baseScaleY = baseScaleY,
        baseRotation = baseRotation, baseFlipX = baseFlipX, baseOnTop = baseOnTop,
        preset = preset, brightness = brightness, contrast = contrast, saturation = saturation,
        warmth = warmth, shadows = shadows, highlights = highlights, bgColor = bgColor,
        drawStrokes = drawStrokes.toList(), eraseStrokes = eraseStrokes.map { it.toList() },
        items = items.toList(), nextId = nextId,
    )

    fun apply(s: EditorSnapshot) {
        base = s.base
        baseOffset = s.baseOffset; baseScale = s.baseScale; baseScaleY = s.baseScaleY
        baseRotation = s.baseRotation; baseFlipX = s.baseFlipX; baseOnTop = s.baseOnTop
        preset = s.preset; brightness = s.brightness; contrast = s.contrast; saturation = s.saturation
        warmth = s.warmth; shadows = s.shadows; highlights = s.highlights
        bgColor = s.bgColor
        drawStrokes.clear(); drawStrokes.addAll(s.drawStrokes.map { st ->
            DrawStroke(st.colorArgb, st.width, st.eraser).also { it.points.addAll(st.points) }
        })
        eraseStrokes.clear(); eraseStrokes.addAll(s.eraseStrokes.map { pts -> mutableStateListOf<Offset>().apply { addAll(pts) } })
        items.clear(); items.addAll(s.items.map(::copyEditorItem))
        nextId = s.nextId
        // Whatever was selected may not exist any more.
        selectedId = null; baseSelected = false
    }

    // Both bump `replayCount` so the watcher re-baselines onto the restored state instead of
    // recording it as a fresh edit.
    fun undo() {
        val s = history.undo(snapshot().deepCopy()) ?: return
        apply(s); replayCount++
    }
    fun redo() {
        val s = history.redo(snapshot().deepCopy()) ?: return
        apply(s); replayCount++
    }

    EditorHistoryWatcher(history, replayCount) { snapshot() }

    fun resetBaseAdjust() {
        baseOffset = Offset.Zero; baseScale = 1f; baseScaleY = 1f; baseRotation = 0f; baseFlipX = false
        baseAlpha = 1f; baseOnTop = false
        eraseStrokes.clear()
        preset = FilterPreset.ORIGINAL
        brightness = 0f; contrast = 1f; saturation = 1f; warmth = 0f; shadows = 0f; highlights = 0f
    }

    /** Removes the base photo — selecting it used to be a dead end (no panel, no delete),
     * unlike an overlay image which has always had one. Sticker mode only: in Animation mode
     * `base` is just a preview frame pulled from the picked video, not a standalone photo, so
     * "deleting" it wouldn't touch the actual video — that mode keeps its own Video tool instead. */
    fun deleteBase() {
        base = null
        resetBaseAdjust()
        baseSelected = false
    }

    /** Animation mode's "Delete video": drops the clip and its timeline. Text, stickers and
     * drawings stay — on their own they still make an animation (see saveAnimated). */
    fun removeVideo() {
        clipLoad?.cancel()
        videoUri = null
        clipFrames = emptyList(); clipDurationMs = 0L
        trimStartMs = 0L; trimEndMs = 0L; playheadMs = 0L; playing = false
        framesLoading = false
        base = null
        resetBaseAdjust()
        baseSelected = false
    }

    // Universal reset: wipe the whole editor back to a clean slate but KEEP the base photo /
    // sticker you're editing. Everything else — text, stickers, drawings, erases, adjustments,
    // filters, background — is removed.
    fun resetEditor() {
        items.clear(); selectedId = null
        drawStrokes.clear(); drawErase = false
        bgColor = null
        resetBaseAdjust()
    }

    LaunchedEffect(stickerId) {
        // Skip re-loading the original from disk when a draft already restored one — it may
        // carry crop/adjust edits the saved sticker itself doesn't have yet. An animated sticker
        // opened in Animation mode is loaded further down (see "Editing an animation").
        if (stickerId != null && draft?.base == null && !(isAnim && animated)) {
            val sticker = (ServiceLocator.customStickerRepository.stickers.value +
                ServiceLocator.catalogRepository.catalogStickers).firstOrNull { it.id == stickerId }
            if (sticker != null) base = withContext(Dispatchers.IO) { BitmapUtils.loadBitmap(context, sticker) }
        }
    }

    // Flush the CURRENT content into the draft store on the way out — unless a save just
    // completed (see saveSticker/saveAnimated), in which case the draft is cleared instead so a
    // future edit of this same sticker starts fresh rather than resurrecting stale work.
    DisposableEffect(Unit) {
        onDispose {
            if (committed) {
                EditorDraftStore.clear(stickerId)
            } else {
                EditorDraftStore.put(
                    stickerId,
                    EditorDraft(
                        isAnim = isAnim, videoUri = videoUri, base = base,
                        baseOffset = baseOffset, baseScale = baseScale, baseScaleY = baseScaleY,
                        baseRotation = baseRotation, baseFlipX = baseFlipX, baseAlpha = baseAlpha, baseOnTop = baseOnTop,
                        preset = preset, brightness = brightness, contrast = contrast, saturation = saturation,
                        warmth = warmth, shadows = shadows, highlights = highlights, bgColor = bgColor,
                        drawStrokes = drawStrokes.toList(), items = items.toList(), nextId = nextId,
                    ),
                )
            }
        }
    }

    // Every gallery import stops at the crop step first: the picked photo lands here, the
    // CropDialog below shows it, and only then does it reach [cropTarget].
    var pendingCrop by remember { mutableStateOf<Bitmap?>(null) }
    var cropTarget by remember { mutableStateOf<CropTarget>(CropTarget.Base) }

    val pickBase = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            cropTarget = CropTarget.Base
            pendingCrop = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri) }
        }
    }
    val pickOverlay = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            cropTarget = CropTarget.NewOverlay
            pendingCrop = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri, 1024) }
        }
    }
    // Double-tapping an existing photo layer swaps just that photo (also via crop).
    val pickReplacement = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            pendingCrop = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri, 1024) }
        }
    }
    /** Makes [uri] the clip: reads its frames into the timeline (streamed — see previewFlow). */
    fun startClip(uri: Uri) {
        clipLoad?.cancel()
        // A photo that was the background (Animation mode, before any video) becomes a layer
        // over the clip instead of being thrown away — kept where and how big it was.
        val photo = if (videoUri == null) base else null
        if (photo != null && canvasSideDp > 0f) {
            val layer = ImageItem(nextId++, photo, sizeDp = canvasSideDp - 8f).apply {
                offset = baseOffset; scale = baseScale; rotation = baseRotation; alpha = baseAlpha
            }
            items.add(0, layer)
        }
        clipLoad = scope.launch {
            videoUri = uri
            resetBaseAdjust()
            // Decode the scrub strip once, here: the timeline and the frame preview both read
            // from it, so scrubbing never has to touch the decoder again. One retriever session
            // for duration + the whole strip (see loadPreview) — this used to be three separate
            // sessions (a first-frame decode, a duration read, then the strip), which made
            // picking a video noticeably slow.
            clipFrames = emptyList()
            clipDurationMs = 0L
            trimStartMs = 0L; trimEndMs = 0L; playheadMs = 0L; playing = false
            // Drop the previous clip's frame straight away — it used to linger (and flicker)
            // on the canvas until the new clip had been read.
            base = null
            framesLoading = true
            // Streamed: the first frame shows almost at once, the strip fills in behind it.
            VideoToWebp.previewFlow(context, uri).collect { preview ->
                clipDurationMs = preview.durationMs
                clipFrames = preview.frames
                if (base == null) base = preview.frames.firstOrNull()?.second
                trimEndMs = preview.durationMs
            }
            framesLoading = false
            if (clipFrames.isNotEmpty()) Toast.makeText(context, "Video added — trim it below, then tap ✓", Toast.LENGTH_SHORT).show()
        }
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) startClip(uri)
    }

    // ---- Editing an animation ----
    // An animated sticker opened with Edit comes in MOVING: an animated WebP as a canvas-filling
    // animated layer (text, stickers and drawings go on top; saving re-makes the animation), an
    // old .mp4 clip as the video. It used to open as a still first frame with the empty-canvas
    // "Add a video" prompt sitting over it.
    var editLoading by remember { mutableStateOf(false) }
    LaunchedEffect(stickerId) {
        if (stickerId == null || !isAnim || !animated) return@LaunchedEffect
        // Backing out mid-edit keeps a draft; that's the newer state, so don't load over it.
        if (draft != null && (draft.items.isNotEmpty() || draft.videoUri != null)) return@LaunchedEffect
        val sticker = (ServiceLocator.customStickerRepository.stickers.value +
            ServiceLocator.catalogRepository.catalogStickers).firstOrNull { it.id == stickerId } ?: return@LaunchedEffect
        editLoading = true
        if (sticker.isVideoClip) {
            val file = withContext(Dispatchers.IO) {
                BitmapUtils.downloadBytes(context, sticker)?.let { bytes ->
                    java.io.File(context.cacheDir, "edit_clip_${sticker.id.hashCode()}.mp4").apply { writeBytes(bytes) }
                }
            }
            editLoading = false
            if (file != null) startClip(Uri.fromFile(file))
            else Toast.makeText(context, "Couldn't load this animation. Check your connection.", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }
        val frames = withContext(Dispatchers.IO) {
            BitmapUtils.downloadBytes(context, sticker)?.let { com.stickers.bank.util.AnimatedWebp.decode(it, maxSide = 512) }
        }
        // Sized to fill the canvas (less OverlayView's 4dp padding on each side).
        val side = snapshotFlow { canvasSideDp }.first { it > 0f }
        editLoading = false
        if (frames != null) {
            items.add(0, ImageItem(nextId++, frames.frames.first(), sizeDp = side - 8f, animation = frames))
        } else {
            Toast.makeText(context, "Couldn't load this animation. Check your connection.", Toast.LENGTH_SHORT).show()
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        // Camera shots go through the same crop step as gallery picks.
        if (bmp != null) { cropTarget = CropTarget.Base; pendingCrop = bmp }
    }

    fun addText(text: String) {
        val i = TextItem(nextId++, text, Color.White.toArgb(), null); items.add(i); selectedId = i.id; tab = EditorTab.TEXT
    }
    fun addEmoji(emoji: String) {
        val i = TextItem(nextId++, emoji, Color.White.toArgb(), null).apply { fontSize = 72f }
        items.add(i); selectedId = i.id; tab = EditorTab.LAYER
    }
    fun addStickerOverlay(bmp: Bitmap) {
        val i = ImageItem(nextId++, bmp); items.add(i); selectedId = i.id; tab = EditorTab.LAYER
    }
    fun addShape(kind: ShapeKind, color: Color) {
        val i = ImageItem(nextId++, makeShapeBitmap(kind, color.toArgb()), isShape = true, initialShapeColorArgb = color.toArgb())
        items.add(i); selectedId = i.id; tab = EditorTab.LAYER
    }
    fun duplicate(item: EditorItem) {
        val copy: EditorItem = when (item) {
            is TextItem -> TextItem(nextId++, item.text, item.colorArgb, item.fontPath).also {
                it.outline = item.outline; it.fontSize = item.fontSize
                it.letterSpacing = item.letterSpacing; it.lineHeightMul = item.lineHeightMul; it.align = item.align
                it.bold = item.bold; it.italic = item.italic; it.shadow = item.shadow; it.highlightArgb = item.highlightArgb
                it.skewX = item.skewX; it.bend = item.bend
                it.stretchX = item.stretchX; it.stretchY = item.stretchY
                it.gradient = item.gradient; it.gradientColor2Argb = item.gradientColor2Argb
                it.outlineWidth = item.outlineWidth; it.outlineColorArgb = item.outlineColorArgb
                it.shadowColorArgb = item.shadowColorArgb; it.shadowRadius = item.shadowRadius
                it.glow = item.glow; it.glowColorArgb = item.glowColorArgb
            }
            is ImageItem -> ImageItem(nextId++, item.bitmap, item.sizeDp, item.isShape, item.shapeColorArgb, item.animation).also {
                it.strokeWidth = item.strokeWidth; it.strokeColorArgb = item.strokeColorArgb
                // Keep whatever box the original was resized to, not the bitmap's natural ratio
                // the constructor just reset it to.
                it.widthDp = item.widthDp; it.heightDp = item.heightDp
            }
        }
        copy.offset = item.offset + Offset(40f, 40f); copy.scale = item.scale
        copy.rotation = item.rotation; copy.alpha = item.alpha
        items.add(copy); selectedId = copy.id
    }

    fun saveSticker(caption: String, tags: List<String>) {
        scope.launch {
            saving = true
            // Both, or the selection border / corner handles bake into the saved sticker.
            selectedId = null; baseSelected = false
            withFrameNanos { }; withFrameNanos { }
            val image = graphicsLayer.toImageBitmap()
            withContext(Dispatchers.IO) {
                val raw = image.asAndroidBitmap()
                val soft = raw.copy(Bitmap.Config.ARGB_8888, false) ?: raw
                val square = BitmapUtils.fitSquare(soft, 512)
                val file = ServiceLocator.customStickerRepository.newImageFile("webp")
                BitmapUtils.writeWebp(square, file)
                ServiceLocator.customStickerRepository.add(file, caption, tags, animated)
            }
            committed = true
            saving = false
            // Stay in the editor after saving — the user keeps their canvas and only leaves
            // when they press back. (Was popping straight out before.)
            Toast.makeText(context, "Sticker saved to My Stickers", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveAnimated(caption: String, tags: List<String>) {
        val uri = videoUri
        val animatedItems = items.filterIsInstance<ImageItem>().filter { it.animation != null }
        if (uri == null && animatedItems.isEmpty()) return
        scope.launch {
            saveProgress = 0f
            saving = true
            playing = false
            // Both, or the selection border / corner handles bake into the saved sticker.
            selectedId = null; baseSelected = false
            withFrameNanos { }; withFrameNanos { }
            // Frames are most of the work; the last few percent are writing the file.
            val onProgress: (Float) -> Unit = { saveProgress = it * 0.95f }

            /** [layer] as it looks with every animated sticker [timeMs] in — the same frames
             * that played on the canvas. Consecutive output frames that land on the same sticker
             * frames reuse the last capture instead of re-rendering. */
            var lastKey: List<Int>? = null
            var lastCapture: Bitmap? = null
            suspend fun captureAt(layer: androidx.compose.ui.graphics.layer.GraphicsLayer, timeMs: Long): Bitmap? {
                val key = animatedItems.map { it.animation?.indexAt(timeMs) ?: 0 }
                if (key == lastKey && lastCapture != null) return lastCapture
                val img = withContext(Dispatchers.Main) {
                    animatedItems.forEach { it.animTimeMs = timeMs }
                    withFrameNanos { }; withFrameNanos { }
                    runCatching { layer.toImageBitmap() }.getOrNull()
                } ?: return null
                val raw = img.asAndroidBitmap()
                val soft = raw.copy(Bitmap.Config.ARGB_8888, false) ?: raw
                val square = if (soft.width == 512 && soft.height == 512) soft else BitmapUtils.fitSquare(soft, 512)
                lastKey = key; lastCapture = square
                return square
            }

            val bytes = if (uri != null) {
                // Over a video: overlays (text/stickers/drawings) + adjustments baked onto every
                // frame — captured once, or per frame when there are animated stickers in it.
                val overlayImg = if (animatedItems.isEmpty()) runCatching { overlayLayer.toImageBitmap() }.getOrNull() else null
                // Where the clip was moved/resized to, in canvas-side fractions (see Placement).
                val sidePx = with(density) { canvasSideDp.dp.toPx() }.coerceAtLeast(1f)
                val placement = VideoToWebp.Placement(
                    offsetX = baseOffset.x / sidePx, offsetY = baseOffset.y / sidePx,
                    scaleX = baseScale, scaleY = baseScaleY, rotation = baseRotation,
                    flipX = baseFlipX, alpha = baseAlpha, background = bgColor?.toArgb(),
                )
                val matrix = buildColorMatrixArray(preset, brightness, contrast, saturation, warmth, shadows, highlights)
                withContext(Dispatchers.IO) {
                    val ov = overlayImg?.let { img ->
                        val raw = img.asAndroidBitmap()
                        val soft = raw.copy(Bitmap.Config.ARGB_8888, false) ?: raw
                        if (soft.width == 512 && soft.height == 512) soft else BitmapUtils.fitSquare(soft, 512)
                    }
                    VideoToWebp.encode(
                        context, uri, overlay = ov, colorMatrix = matrix,
                        // The timeline's trim window; untouched defaults still mean the whole clip.
                        startMs = trimStartMs,
                        endMs = if (trimEndMs > trimStartMs) trimEndMs else Long.MAX_VALUE,
                        overlayAt = if (animatedItems.isEmpty()) null else { _, t -> captureAt(overlayLayer, t) },
                        onProgress = onProgress,
                        placement = placement,
                    )
                }
            } else {
                // No video: the animated stickers ARE the animation — the whole canvas (text,
                // drawings, background colour, erasing included) on a transparent background,
                // as long as the longest sticker's loop.
                val stepMs = 1000 / 12
                val longest = animatedItems.maxOf { it.animation?.durationMs ?: 0L }.coerceIn(500L, 4_000L)
                val count = (longest / stepMs).toInt().coerceIn(2, 48)
                withContext(Dispatchers.IO) {
                    VideoToWebp.encodeTransparent(count, stepMs, frameAt = { _, t -> captureAt(graphicsLayer, t) }, onProgress = onProgress)
                }
            }
            if (bytes == null) {
                saving = false
                saveProgress = null
                Toast.makeText(context, "Couldn't make this animation. Try a shorter clip.", Toast.LENGTH_LONG).show()
                return@launch
            }
            withContext(Dispatchers.IO) {
                val file = ServiceLocator.customStickerRepository.newImageFile("webp")
                file.writeBytes(bytes)
                ServiceLocator.customStickerRepository.add(file, caption, tags, animated = true)
            }
            saveProgress = 1f
            withFrameNanos { }
            committed = true
            saving = false
            saveProgress = null
            // Stay in the editor after saving (see saveSticker).
            Toast.makeText(context, "Animated sticker saved", Toast.LENGTH_SHORT).show()
        }
    }

    val selectedItem = items.firstOrNull { it.id == selectedId }
    val colorFilter = remember(preset, brightness, contrast, saturation, warmth, shadows, highlights) {
        buildColorFilter(preset, brightness, contrast, saturation, warmth, shadows, highlights)
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar: back on the left, the Sticker / Animation switch in the MIDDLE (bigger, the
        // main choice on this screen), Done on the right.
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Box(Modifier.align(Alignment.CenterStart)) {
                IconButtonRound(Icons.AutoMirrored.Rounded.ArrowBack) { nav.popBackStack() }
            }
            // Sticker / Animation switch. Selected side shows its label.
            // Locked only by what can't cross between the modes: a saved sticker being edited
            // keeps its kind, and a video or an animated sticker can't go into a still. A photo,
            // text, stickers and drawings work in both, so they no longer lock it.
            val lockReason = when {
                stickerId != null -> "You're editing a saved ${if (animated) "animation" else "sticker"}, so its type can't change."
                videoUri != null -> "Delete the video first to switch to Sticker mode."
                hasAnimatedItems -> "Remove the animated stickers first to switch to Sticker mode."
                else -> null
            }
            val modeLocked = lockReason != null
            fun switchMode(toAnim: Boolean) {
                if (toAnim == isAnim) return
                if (lockReason != null) Toast.makeText(context, lockReason, Toast.LENGTH_LONG).show()
                else isAnim = toAnim
            }
            Row(
                Modifier.align(Alignment.Center)
                    .neuRaised(cornerRadius = 50.dp, elevation = 7.dp).clip(RoundedCornerShape(50)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Always tappable, so a locked switch can explain itself instead of ignoring taps.
                ModeSwitchBtn("Sticker", Icons.Rounded.Image, !isAnim, dimmed = modeLocked) { switchMode(false) }
                ModeSwitchBtn("Animation", Icons.Rounded.Movie, isAnim, dimmed = modeLocked) { switchMode(true) }
            }
            // Dimmed in Animation mode until there's a video or an animated sticker — a photo,
            // text or drawing alone can't make an animation. A tap still says why.
            val doneReady = !isAnim || videoUri != null || hasAnimatedItems
            PillButton(
                "Done", Icons.Rounded.Check, filled = true,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).graphicsLayer { alpha = if (doneReady) 1f else 0.4f },
                onClick = {
                    if (isAnim) {
                        if (!doneReady) {
                            Toast.makeText(
                                context,
                                if (animNeedsMotion) "A photo alone doesn't save as an animation. Add a video or an animated sticker."
                                else "Add a video or an animated sticker first",
                                Toast.LENGTH_LONG,
                            ).show()
                        } else showAd { showSaveDialog = true }
                    } else if (base == null && items.isEmpty() && drawStrokes.isEmpty()) {
                        Toast.makeText(context, "Add an image, text or drawing first", Toast.LENGTH_SHORT).show()
                    } else showAd { showSaveDialog = true }
                },
            )
        }

        // Undo/redo on the left, Reset-everything on the right. The top bar above is already
        // full (back + mode switch + Done), and this row had the space going spare.
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 6.dp, top = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UndoRedoButton(Icons.AutoMirrored.Rounded.Undo, "Undo", history.canUndo) { undo() }
            UndoRedoButton(Icons.AutoMirrored.Rounded.Redo, "Redo", history.canRedo) { redo() }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .neuRaised(cornerRadius = 50.dp, elevation = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .clickable { resetEditor() }
                    .padding(horizontal = 14.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = Gold, modifier = Modifier.size(15.dp))
                Text("Reset all", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }

        // Canvas area. The tool rail used to be overlaid down its left edge; it's a horizontal
        // row just above the bottom panel now (see ToolRailRow below), so the canvas gets the
        // full width to itself.
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            // Width is the free axis on a normal phone once the bottom UI below is trimmed
            // down — no -20dp margin, no side padding, so the square actually reaches the
            // true screen edges instead of floating in a 10dp gutter on each side.
            val side = minOf(maxWidth, maxHeight - 8.dp)
            SideEffect { canvasSideDp = side.value }

            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(side)
                    .clip(CanvasSquircle)
                    .background(AppBackground)
                    .editorCheckerboard() // visible grey checkerboard = erased/empty areas read as transparent
                     ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        // Offscreen so the universal eraser's BlendMode.Clear clears to *transparent*
                        // on screen (revealing the checkerboard), not to opaque black.
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        }
                        .pointerInput(tool) {
                            if (tool == Tool.MOVE) detectTapGestures { selectedId = null; baseSelected = false }
                        }
                        .pointerInput(tool) {
                            if (tool == Tool.MOVE) detectTransformGestures { _, _, zoom, rot ->
                                val layer = items.firstOrNull { it.id == selectedId } ?: return@detectTransformGestures
                                layer.scale = (layer.scale * zoom).coerceIn(0.1f, 12f); layer.rotation += rot
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    bgColor?.let { Box(Modifier.matchParentSize().background(it)) }
                    val photoOnTop = baseOnTop && !clipMode
                    val baseView: @Composable () -> Unit = { base?.let { bmp ->
                        EditableBase(
                            bitmap = bmp,
                            offset = baseOffset, scale = baseScale, scaleY = baseScaleY,
                            rotation = baseRotation, flipX = baseFlipX, alpha = baseAlpha,
                            colorFilter = colorFilter,
                            onTransform = { pan, zoom, rot ->
                                val layer = items.firstOrNull { it.id == selectedId }
                                if (layer != null && (zoom != 1f || rot != 0f)) {
                                    // Two fingers anywhere on the canvas resize/turn the selected
                                    // sticker or text — a small one can't fit two fingers on it.
                                    layer.scale = (layer.scale * zoom).coerceIn(0.1f, 12f); layer.rotation += rot
                                } else {
                                    // The photo — or the video, which can be moved and resized
                                    // too; the saved animation keeps it exactly where it's put.
                                    baseOffset = applyCenterSnap(baseOffset + pan); baseScale *= zoom; baseScaleY *= zoom
                                    baseRotation += rot; selectedId = null; baseSelected = true; tab = EditorTab.LAYER
                                }
                            },
                            transformEnabled = tool == Tool.MOVE,
                            // pickBase's own callback sets cropTarget = Base before routing to crop.
                            onDoubleTap = {
                                if (clipMode) pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                else pickBase.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            // Jump straight to the Layer tab — selecting it used to leave you on
                            // whatever tab you were already on, with nothing there to act on it.
                            onTap = { selectedId = null; baseSelected = true; tab = EditorTab.LAYER },
                            selected = baseSelected,
                            onResize = { sx, sy, shift ->
                                baseScale = sx; baseScaleY = sy; baseOffset += shift
                            },
                        )
                    } }
                    if (!photoOnTop) baseView()
                    // Overlays are captured into their own layer so Animation mode can bake them
                    // onto each video frame (transparent everywhere there's no text/sticker/drawing).
                    Box(
                        Modifier.matchParentSize().drawWithContent {
                            overlayLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(overlayLayer)
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        items.forEach { item ->
                            key(item.id) {
                                OverlayView(
                                    item = item,
                                    selected = item.id == selectedId,
                                    interactive = tool == Tool.MOVE,
                                    onSelect = {
                                        selectedId = item.id
                                        baseSelected = false
                                        tab = if (item is TextItem) EditorTab.TEXT else EditorTab.LAYER
                                    },
                                    onEditText = { editingTextItem = it; showTextDialog = true },
                                    onReplaceImage = {
                                        cropTarget = CropTarget.Replace(it)
                                        pickReplacement.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    onCenterSnap = { sx, sy ->
                                        snapGuideX = sx; snapGuideY = sy
                                        if (sx || sy) lastSnapAt = System.currentTimeMillis()
                                    },
                                )
                            }
                        }
                        DrawLayer(strokes = drawStrokes, active = tool == Tool.DRAW, color = drawColor, width = drawWidth, eraser = drawErase)
                    }
                    if (photoOnTop) baseView()
                    // Universal eraser sits on top of everything (photo + overlays), so a drag
                    // erases the whole composite — captured into the sticker as transparency.
                    UniversalEraseLayer(active = tool == Tool.ERASE, brush = brush, strokes = eraseStrokes)
                }
                // On top of everything else in the canvas — the guide has to be visible over
                // both the photo and any overlay it's being dragged past.
                CenterGuides(snapGuideX, snapGuideY, Modifier.matchParentSize())
                if (animNeedsMotion) {
                    Row(
                        Modifier.align(Alignment.TopCenter).padding(10.dp)
                            .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = Gold, modifier = Modifier.size(15.dp))
                        Text(
                            "A photo doesn't save — add a video or an animated sticker",
                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }

                if (isAnim && videoUri == null) {
                    // Only while there's nothing at all — animated stickers, text or drawings on
                    // their own are an animation too, and this would cover them.
                    if (editLoading) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            com.stickers.bank.ui.components.PolygonProgress(null, Modifier.size(54.dp))
                            Text("Loading animation…", color = AppOnSurfaceMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                        }
                    } else if (base == null && items.isEmpty() && drawStrokes.isEmpty() && bgColor == null) {
                        AddPhotoPlaceholder(isVideo = true) { pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                    }
                } else if (isAnim && base == null && framesLoading) {
                    // A clip IS picked and its first frame is on its way — not the "Add a video"
                    // prompt, which used to flash up here while the clip was being read.
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        com.stickers.bank.ui.components.PolygonProgress(null, Modifier.size(54.dp))
                        Text("Loading clip…", color = AppOnSurfaceMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    }
                } else if (isAnim && base == null) {
                    // Video chosen but a preview frame couldn't be decoded — still saveable.
                    AddPhotoPlaceholder(isVideo = true) { pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                } else if (!isAnim && base == null && items.isEmpty() && drawStrokes.isEmpty() && bgColor == null) {
                    AddPhotoPlaceholder { pickBase.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
            }

        }

        // Animation timeline — only in Animation mode, and only once a clip is loaded. Sits
        // right under the canvas so scrubbing and what it previews read as one thing.
        if (isAnim && videoUri != null) {
            if (clipFrames.isEmpty() && framesLoading) {
                // Same height as the timeline that replaces it, so the canvas above doesn't
                // jump when the strip arrives.
                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(96.dp), contentAlignment = Alignment.Center) { // = VideoTimeline's height
                    Text("Reading the clip…", color = AppOnSurfaceMuted, fontSize = 11.sp)
                }
            } else if (clipFrames.isEmpty()) {
                // Decoded nothing — an codec this device can't read. Say so rather than leaving
                // a blank gap where the timeline should be.
                Text(
                    "Couldn't read frames from this clip — it may use a format this phone can't " +
                        "decode. Try another video.",
                    color = AppOnSurfaceMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            } else {
                VideoTimeline(
                    frames = clipFrames,
                    durationMs = clipDurationMs,
                    startMs = trimStartMs,
                    endMs = trimEndMs,
                    playheadMs = playheadMs,
                    playing = playing,
                    onTogglePlay = { playing = !playing },
                    // Handles can't cross, and a sub-second clip isn't worth encoding.
                    onTrimStart = { ms -> trimStartMs = ms.coerceIn(0L, (trimEndMs - 300L).coerceAtLeast(0L)) },
                    onTrimEnd = { ms -> trimEndMs = ms.coerceIn(trimStartMs + 300L, clipDurationMs) },
                    onScrub = { ms -> playing = false; playheadMs = ms.coerceIn(0L, clipDurationMs) },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // Tool row — same tools for Sticker and Animation (overlays get baked onto frames).
        // Horizontal and docked right above the tabs/bottom panel rather than railed down the
        // canvas's left edge, so the canvas itself keeps the full width.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RailButton(if (isAnim) "Video" else "Photo", if (isAnim) Icons.Rounded.Movie else Icons.Rounded.Image, railActive == 0) {
                railActive = 0
                if (isAnim) pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                else pickBase.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            if (isAnim) {
                // A photo works in an animation too — as the background with no video, or as a
                // layer over one. On its own it doesn't move, so Done waits for a video or an
                // animated sticker (see the note on the canvas).
                RailButton("Photo", Icons.Rounded.Image, railActive == 7) {
                    railActive = 7
                    if (videoUri == null) pickBase.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    else pickOverlay.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            RailButton("Text", Icons.Rounded.TextFields, railActive == 1) {
                railActive = 1; editingTextItem = null; showTextDialog = true
            }
            RailButton("Decorator", Icons.Rounded.Style, railActive == 6) {
                railActive = 6; showDecorator = true
            }
            RailButton("Sticker", Icons.Rounded.EmojiEmotions, railActive == 2) {
                railActive = 2; pickerTab = 1; showPicker = true
            }
            RailButton("Overlay", Icons.Rounded.Layers, railActive == 3) {
                railActive = 3
                pickOverlay.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            RailButton("Emoji", Icons.Rounded.Mood, railActive == 5) {
                railActive = 5; pickerTab = 0; showPicker = true
            }
            RailButton("Shape", Icons.Rounded.Category, railActive == 4) {
                railActive = 4; showShapePicker = true
            }
            Box {
                var moreOpen by remember { mutableStateOf(false) }
                RailButton("More", Icons.Rounded.MoreHoriz, false) { moreOpen = true }
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    run {
                        DropdownMenuItem(
                            text = { Text(if (clipMode) "Flip video" else "Flip photo") },
                            enabled = base != null,
                            onClick = { baseFlipX = !baseFlipX; moreOpen = false },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Duplicate layer") },
                        enabled = selectedItem != null,
                        onClick = { selectedItem?.let { duplicate(it) }; moreOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Reset adjustments") },
                        onClick = { resetBaseAdjust(); moreOpen = false },
                    )
                    DropdownMenuItem(
                        text = { Text(if (clipMode) "Reset everything (keep video)" else "Reset everything (keep photo)") },
                        onClick = { resetEditor(); moreOpen = false },
                    )
                }
            }
        }

        // Tabs — available for both Sticker and Animation.
        EditorTabBar(tab) { tab = it }

        // Bottom panel — icon-row + a single active slider, per tab.
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(AppSurface),
        ) {
            // Fixed height so the panel never resizes when you switch tabs; taller tabs scroll.
            Column(
                // In Animation mode with a clip, the timeline above takes ~100dp; the panel gives
                // that up (it scrolls) instead of the canvas shrinking to make room.
                Modifier.fillMaxWidth().height(if (isAnim && videoUri != null) 158.dp else 258.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (tab) {
                    EditorTab.CROP -> {
                        // A selected overlay photo or sticker gets the same crop step as the photo:
                        // crop, remove the background, mirror/water flip.
                        val layer = (selectedItem as? ImageItem)?.takeIf { !it.isShape }
                        if (layer != null) {
                            SectionLabel("SELECTED LAYER")
                            if (layer.animation != null) {
                                PanelHint("Animated stickers can't be cropped — resize them on the canvas instead.")
                            } else {
                                PanelHint("Crop the selected layer, remove its background, or flip it.")
                                PillButton("Crop · remove background", Icons.Rounded.Crop, Modifier.fillMaxWidth(), filled = true) {
                                    cropTarget = CropTarget.Recrop(layer)
                                    pendingCrop = layer.bitmap
                                }
                            }
                            if (!clipMode && base != null) SectionLabel("PHOTO")
                        }
                        if (clipMode) {
                            PanelHint("A video is cropped to a square for you. Trim its length on the timeline, and place stickers and text on top.")
                        } else if (base == null) {
                            if (layer == null) {
                                PanelHint("Add a photo, or tap a sticker or overlay on the canvas, then crop it from here — background removal included.")
                            }
                        } else {
                            PanelHint("Redraw the crop box over the photo as it looks right now (after any adjustments/erasing already applied).")
                            PillButton("Crop photo", Icons.Rounded.Crop, Modifier.fillMaxWidth(), filled = true) {
                                cropTarget = CropTarget.Base
                                pendingCrop = base
                            }
                        }
                    }

                    EditorTab.ADJUST -> {
                        // Always visible. Adjustments apply to the photo; if there's no photo yet
                        // they simply have nothing to act on (Background works without one).
                        ToolIconRow(AdjustTool.entries, adjustTool, { it.label }, { it.icon }) { adjustTool = it }
                        when (adjustTool) {
                            AdjustTool.BRIGHTNESS -> AdjustSlider("Brightness", brightness, -100f..100f, 5f, { "${it.roundToInt()}" }, { brightness = it }, { brightness = 0f })
                            AdjustTool.CONTRAST -> AdjustSlider("Contrast", contrast, 0.5f..2f, 0.05f, { "${(it * 100).roundToInt()}%" }, { contrast = it }, { contrast = 1f })
                            AdjustTool.SATURATION -> AdjustSlider("Saturation", saturation, 0f..2f, 0.05f, { "${(it * 100).roundToInt()}%" }, { saturation = it }, { saturation = 1f })
                            AdjustTool.WARMTH -> AdjustSlider("Warmth", warmth, -100f..100f, 5f, { "${it.roundToInt()}" }, { warmth = it }, { warmth = 0f })
                            AdjustTool.SHADOWS -> AdjustSlider("Shadows", shadows, -100f..100f, 5f, { "${it.roundToInt()}" }, { shadows = it }, { shadows = 0f })
                            AdjustTool.HIGHLIGHTS -> AdjustSlider("Highlights", highlights, -100f..100f, 5f, { "${it.roundToInt()}" }, { highlights = it }, { highlights = 0f })
                        }
                        SectionLabel("BACKGROUND")
                        BackgroundSwatchRow(bgColor) { bgColor = it }
                    }

                    EditorTab.FILTER -> {
                        SectionLabel("FILTERS")
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterPreset.entries.forEach { p ->
                                val sel = p == preset
                                Box(
                                    Modifier
                                        .then(
                                            if (sel) Modifier.neuInset(cornerRadius = 50.dp, elevation = 5.dp, background = Gold)
                                            else Modifier.neuRaised(cornerRadius = 50.dp, elevation = 5.dp),
                                        )
                                        .clip(RoundedCornerShape(50))
                                        .clickable { preset = p }.padding(horizontal = 16.dp, vertical = 10.dp),
                                ) { Text(p.label, color = if (sel) AppBackground else AppOnSurface, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                            }
                        }
                    }

                    EditorTab.DRAW -> {
                        PanelHint(if (drawErase) "Drag over your drawing to rub it out — only brush strokes are removed." else "Drag on the canvas to draw.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TogglePill("Brush", !drawErase) { drawErase = false }
                            TogglePill("Eraser", drawErase) { drawErase = true }
                        }
                        if (!drawErase) {
                            SectionLabel("BRUSH COLOR")
                            ColorSwatchRow(drawColor) { drawColor = it; drawErase = false }
                        }
                        AdjustSlider(if (drawErase) "Eraser size" else "Brush size", drawWidth, 4f..60f, 2f, { "${it.roundToInt()}" }, { drawWidth = it }, { drawWidth = 16f })
                        PillButton("Undo last stroke", Icons.Rounded.Refresh, Modifier.fillMaxWidth()) {
                            if (drawStrokes.isNotEmpty()) drawStrokes.removeAt(drawStrokes.lastIndex)
                        }
                    }

                    EditorTab.ERASE -> {
                        PanelHint(
                            if (clipMode) "Drag on the canvas to erase. Over a video only text, stickers and drawings can be erased — the clip itself stays whole."
                            else "Drag on the canvas to erase anything — photo, text, stickers or drawings.",
                        )
                        AdjustSlider("Eraser size", brush, 15f..160f, 5f, { "${it.roundToInt()}" }, { brush = it }, { brush = 60f })
                        PillButton("Undo erase", Icons.Rounded.Refresh, Modifier.fillMaxWidth()) {
                            if (eraseStrokes.isNotEmpty()) eraseStrokes.removeAt(eraseStrokes.lastIndex)
                        }
                    }

                    EditorTab.LAYER -> {
                        val item = selectedItem
                        if (baseSelected && base != null && clipMode) {
                            // In Animation mode the "base" is the clip's preview frame, so this is
                            // about the video: swap it or take it out. (Its look is set with Adjust
                            // and Filter; its length on the timeline.)
                            SectionLabel("VIDEO")
                            PanelHint("Drag, pinch or pull a corner to move and resize the clip — the saved animation keeps it there. Trim it on the timeline above.")
                            AdjustSlider("Opacity", baseAlpha, 0.1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { baseAlpha = it }, { baseAlpha = 1f })
                            AdjustSlider("Rotation", baseRotation, -180f..180f, 5f, { "${it.roundToInt()}°" }, { baseRotation = it }, { baseRotation = 0f })
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PillButton("Flip video", Icons.Rounded.Flip, Modifier.weight(1f)) { baseFlipX = !baseFlipX }
                                PillButton("Fill canvas", Icons.Rounded.Refresh, Modifier.weight(1f)) {
                                    baseOffset = Offset.Zero; baseScale = 1f; baseScaleY = 1f; baseRotation = 0f
                                }
                            }
                            PillButton("Replace video", Icons.Rounded.Movie, Modifier.fillMaxWidth(), filled = true) {
                                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                            PillButton("Delete video", Icons.Rounded.Delete, Modifier.fillMaxWidth(), danger = true) { removeVideo() }
                        } else if (baseSelected && base != null) {
                            // The base photo used to land here with nothing to show — no
                            // controls, no delete — while an overlay image got the full panel
                            // below. Simpler set than an overlay item (no size slider: the
                            // on-canvas corner handles already cover size, and there's only
                            // ever one base photo so there's nothing to duplicate).
                            SectionLabel("PHOTO")
                            AdjustSlider("Opacity", baseAlpha, 0.1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { baseAlpha = it }, { baseAlpha = 1f })
                            AdjustSlider("Rotation", baseRotation, -180f..180f, 5f, { "${it.roundToInt()}°" }, { baseRotation = it }, { baseRotation = 0f })
                            PillButton("Flip photo", Icons.Rounded.Flip, Modifier.fillMaxWidth()) { baseFlipX = !baseFlipX }
                            // The photo has no place in the overlays' own order, so it's just above
                            // all of them or below all of them.
                            if (!clipMode) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PillButton("To front", Icons.Rounded.FlipToFront, Modifier.weight(1f), filled = baseOnTop) { baseOnTop = true }
                                    PillButton("To back", Icons.Rounded.FlipToBack, Modifier.weight(1f), filled = !baseOnTop) { baseOnTop = false }
                                }
                            }
                            PillButton("Replace photo", Icons.Rounded.AddPhotoAlternate, Modifier.fillMaxWidth(), filled = true) {
                                pickBase.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            PillButton("Delete photo", Icons.Rounded.Delete, Modifier.fillMaxWidth(), danger = true) { deleteBase() }
                        } else if (item == null) {
                            SectionLabel("LAYER")
                            PanelHint("These controls apply to a selected item. Tap a sticker, emoji, shape or text on the canvas first — then adjust its size, rotation and opacity here.")
                        } else {
                            ToolIconRow(LayerTool.entries, layerTool, { it.label }, { it.icon }) { layerTool = it }
                            when (layerTool) {
                                LayerTool.OPACITY -> AdjustSlider("Opacity", item.alpha, 0.1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.alpha = it }, { item.alpha = 1f })
                                LayerTool.SIZE -> AdjustSlider("Size", item.scale, 0.2f..4f, 0.1f, { "${(it * 100).roundToInt()}%" }, { item.scale = it }, { item.scale = 1f })
                                LayerTool.ROTATE -> AdjustSlider("Rotation", item.rotation, -180f..180f, 5f, { "${it.roundToInt()}°" }, { item.rotation = it }, { item.rotation = 0f })
                            }
                            if (item is ImageItem && item.isShape) {
                                SectionLabel("SHAPE COLOR")
                                ColorSwatchRow(item.shapeColorArgb) { item.shapeColorArgb = it }
                            }
                            // Photo layers get an explicit swap button too — double-tapping the
                            // layer does the same thing, but that isn't discoverable on its own.
                            if (item is ImageItem && item.animation != null) {
                                PanelHint("Animated sticker — it plays in the saved animation. Drag a corner, or pinch anywhere on the canvas, to resize it.")
                            }
                            if (item is ImageItem) {
                                if (!item.isShape && item.animation == null) {
                                    PillButton("Crop · remove background", Icons.Rounded.Crop, Modifier.fillMaxWidth(), filled = true) {
                                        cropTarget = CropTarget.Recrop(item)
                                        pendingCrop = item.bitmap
                                    }
                                }
                                PillButton("Replace image", Icons.Rounded.AddPhotoAlternate, Modifier.fillMaxWidth()) {
                                    cropTarget = CropTarget.Replace(item)
                                    pickReplacement.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                                AdjustSlider(
                                    "Stroke", item.strokeWidth, 0f..24f, 1f,
                                    { if (it <= 0f) "off" else "${it.roundToInt()}px" },
                                    { item.strokeWidth = it }, { item.strokeWidth = 0f },
                                )
                                ColorSwatchRow(item.strokeColorArgb) { item.strokeColorArgb = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PillButton("To front", Icons.Rounded.FlipToFront, Modifier.weight(1f)) {
                                    bringToFront(items, item)
                                }
                                PillButton("To back", Icons.Rounded.FlipToBack, Modifier.weight(1f)) {
                                    sendToBack(items, item)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PillButton("Duplicate", Icons.Rounded.ContentCopy, Modifier.weight(1f)) { duplicate(item) }
                                PillButton("Delete", Icons.Rounded.Delete, Modifier.weight(1f), danger = true) {
                                    items.remove(item); selectedId = null
                                }
                            }
                        }
                    }

                    EditorTab.TEXT -> {
                        PillButton("＋ Add text", Icons.Rounded.TextFields, Modifier.fillMaxWidth(), filled = true) {
                            editingTextItem = null; showTextDialog = true
                        }
                        val item = selectedItem as? TextItem
                        if (item == null) {
                            PanelHint("Add text, or tap existing text on the canvas to style it.")
                        } else {
                            // Edit / Delete up top so they're always reachable without scrolling.
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PillButton("Edit text", Icons.Rounded.TextFields, Modifier.weight(1f)) { editingTextItem = item; showTextDialog = true }
                                PillButton("Delete", Icons.Rounded.Delete, Modifier.weight(1f), danger = true) { items.remove(item); selectedId = null }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PillButton("To front", Icons.Rounded.FlipToFront, Modifier.weight(1f)) {
                                    bringToFront(items, item)
                                }
                                PillButton("To back", Icons.Rounded.FlipToBack, Modifier.weight(1f)) {
                                    sendToBack(items, item)
                                }
                            }
                            SectionLabel("COLOR")
                            ColorSwatchRow(item.colorArgb) { item.colorArgb = it }
                            SectionLabel("FONT")
                            FontChipRow(item.fontPath) { item.fontPath = it }
                            AdjustSlider("Text size", item.fontSize, 16f..140f, 2f, { "${it.roundToInt()}" }, { item.fontSize = it }, { item.fontSize = 40f })
                            AdjustSlider("Letter spacing", item.letterSpacing, 0f..24f, 1f, { "${it.roundToInt()}" }, { item.letterSpacing = it }, { item.letterSpacing = 0f })
                            AdjustSlider("Line height", item.lineHeightMul, 0.8f..2.5f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.lineHeightMul = it }, { item.lineHeightMul = 1.1f })
                            AdjustSlider("Opacity", item.alpha, 0.1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.alpha = it }, { item.alpha = 1f })
                            SectionLabel("TRANSFORM")
                            AdjustSlider("Rotation", item.rotation, -180f..180f, 5f, { "${it.roundToInt()}°" }, { item.rotation = it }, { item.rotation = 0f })
                            AdjustSlider("Scale", item.scale, 0.2f..4f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.scale = it }, { item.scale = 1f })
                            AdjustSlider("X offset", item.offset.x, -400f..400f, 5f, { "${it.roundToInt()}" }, { item.offset = item.offset.copy(x = it) }, { item.offset = item.offset.copy(x = 0f) })
                            AdjustSlider("Y offset", item.offset.y, -400f..400f, 5f, { "${it.roundToInt()}" }, { item.offset = item.offset.copy(y = it) }, { item.offset = item.offset.copy(y = 0f) })
                            AdjustSlider("X stretch", item.stretchX, 0.3f..3f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.stretchX = it }, { item.stretchX = 1f })
                            AdjustSlider("Y stretch", item.stretchY, 0.3f..3f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.stretchY = it }, { item.stretchY = 1f })
                            AdjustSlider("Skew", item.skewX, -1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.skewX = it }, { item.skewX = 0f })
                            AdjustSlider("Bend", item.bend, -100f..100f, 5f, { "${it.roundToInt()}" }, { item.bend = it }, { item.bend = 0f })
                            SectionLabel("ALIGN")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AlignBtn(Icons.AutoMirrored.Rounded.FormatAlignLeft, item.align == 0) { item.align = 0 }
                                AlignBtn(Icons.Rounded.FormatAlignCenter, item.align == 1) { item.align = 1 }
                                AlignBtn(Icons.AutoMirrored.Rounded.FormatAlignRight, item.align == 2) { item.align = 2 }
                            }
                            SectionLabel("STYLE")
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TogglePill("Bold", item.bold) { item.bold = !item.bold }
                                TogglePill("Italic", item.italic) { item.italic = !item.italic }
                                TogglePill("Outline", item.outline) { item.outline = !item.outline }
                                TogglePill("Shadow", item.shadow) { item.shadow = !item.shadow }
                                TogglePill("Glow", item.glow) { item.glow = !item.glow }
                                TogglePill("Gradient", item.gradient) { item.gradient = !item.gradient }
                            }
                            if (item.gradient) {
                                SectionLabel("GRADIENT 2ND COLOR")
                                ColorSwatchRow(item.gradientColor2Argb) { item.gradientColor2Argb = it }
                            }
                            if (item.glow) {
                                SectionLabel("GLOW COLOR")
                                ColorSwatchRow(item.glowColorArgb) { item.glowColorArgb = it }
                            }
                            if (item.outline) {
                                AdjustSlider("Outline width", item.outlineWidth, 2f..30f, 1f, { "${it.roundToInt()}" }, { item.outlineWidth = it }, { item.outlineWidth = 8f })
                                SectionLabel("OUTLINE COLOR")
                                ColorSwatchRow(item.outlineColorArgb) { item.outlineColorArgb = it }
                            }
                            if (item.shadow) {
                                AdjustSlider("Shadow blur", item.shadowRadius, 0f..30f, 1f, { "${it.roundToInt()}" }, { item.shadowRadius = it }, { item.shadowRadius = 6f })
                                SectionLabel("SHADOW COLOR")
                                ColorSwatchRow(item.shadowColorArgb) { item.shadowColorArgb = it }
                            }
                            SectionLabel("HIGHLIGHT")
                            HighlightSwatchRow(item.highlightArgb) { item.highlightArgb = it }
                        }
                    }
                }
            }
        }
    }

    // The universal crop step — shown between picking a photo and it landing on the canvas.
    pendingCrop?.let { picked ->
        CropDialog(
            bitmap = picked,
            onCancel = { pendingCrop = null },
            onDone = { result ->
                when (val t = cropTarget) {
                    CropTarget.Base -> { base = result; resetBaseAdjust() }
                    CropTarget.NewOverlay -> {
                        val i = ImageItem(nextId++, result); items.add(i); selectedId = i.id; tab = EditorTab.LAYER
                    }
                    is CropTarget.Replace -> { t.item.bitmap = result; t.item.animation = null; selectedId = t.item.id; tab = EditorTab.LAYER }
                    is CropTarget.Recrop -> { t.item.bitmap = result; t.item.refitTo(result); selectedId = t.item.id; tab = EditorTab.LAYER }
                }
                pendingCrop = null
            },
        )
    }

    if (showTextDialog) {
        TextInputDialog(
            initial = editingTextItem?.text ?: "",
            onDismiss = { showTextDialog = false; editingTextItem = null },
            onConfirm = { txt ->
                val existing = editingTextItem
                if (existing != null) existing.text = txt else if (txt.isNotBlank()) addText(txt)
                showTextDialog = false; editingTextItem = null
            },
        )
    }
    if (showPicker) {
        StickerEmojiPicker(
            initialTab = pickerTab,
            onEmoji = { addEmoji(it); showPicker = false },
            onSticker = { sticker ->
                scope.launch {
                    // In Animation mode an animated sticker comes in as its frames, so it plays on
                    // the canvas and moves in the saved animation. Sticker mode makes a still, so
                    // there it's just the first frame, as before.
                    val frames = if (isAnim && sticker.isAnimatedSticker) {
                        withContext(Dispatchers.IO) {
                            BitmapUtils.downloadBytes(context, sticker)?.let { com.stickers.bank.util.AnimatedWebp.decode(it) }
                        }
                    } else null
                    if (frames != null) {
                        val i = ImageItem(nextId++, frames.frames.first(), animation = frames)
                        items.add(i); selectedId = i.id; tab = EditorTab.LAYER
                    } else {
                        val bmp = withContext(Dispatchers.IO) { BitmapUtils.loadBitmap(context, sticker) }
                        if (bmp != null) {
                            // Through the crop step first, same as a photo: crop, remove the
                            // background, flip — or "Skip crop" to drop it on as it is.
                            cropTarget = CropTarget.NewOverlay
                            pendingCrop = bmp
                        } else Toast.makeText(context, "Couldn't load that sticker. Check your connection.", Toast.LENGTH_SHORT).show()
                    }
                    showPicker = false
                }
            },
            onDismiss = { showPicker = false },
        )
    }
    if (showDecorator) {
        DecoratorSheet(
            onPick = { dec ->
                scope.launch {
                    val bmp = withContext(Dispatchers.IO) { BitmapUtils.loadUrlBitmap(context, dec.image) }
                    if (bmp != null) addStickerOverlay(bmp)
                    else Toast.makeText(context, "Couldn't load that decoration. Check your connection.", Toast.LENGTH_SHORT).show()
                    showDecorator = false
                }
            },
            onDismiss = { showDecorator = false },
        )
    }
    if (showShapePicker) {
        ShapePickerDialog(
            onPick = { kind, color -> addShape(kind, color); showShapePicker = false },
            onDismiss = { showShapePicker = false },
        )
    }
    if (showSaveDialog) {
        SaveDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { c, t ->
                showSaveDialog = false
                if (isAnim) saveAnimated(c, t) else saveSticker(c, t)
            },
        )
    }
    if (saving) SavingOverlay(saveProgress)
}

/** The save in progress: the app's loading popup — with the percentage while an animation is
 * encoded frame by frame, just turning for a quick still save. */
@Composable
private fun SavingOverlay(progress: Float? = null) {
    com.stickers.bank.ui.components.PreparingPopup(
        if (progress != null) "Making your animation…" else "Saving…",
        progress = progress,
    )
}

// ---------- Reusable pieces ----------

@Composable
fun RailButton(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    // Fixed width, not fillMaxWidth: these sit in a horizontally-scrolling row, whose width
    // constraint is unbounded. (See ToolRailRow's call site.)
    // Selected reads as physically pressed INTO the surface (neuInset), not just tinted — the
    // same embossed-button language the rest of the neumorphic UI uses for a "held down" state,
    // just kept on for as long as this tool stays active instead of only while the finger's down.
    Column(
        Modifier.width(52.dp)
            .then(if (selected) Modifier.neuInset(cornerRadius = 14.dp, elevation = 5.dp) else Modifier)
            .clip(ChipSquircle)
            .clickable(onClick = onClick).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (selected) Gold else AppOnSurface, modifier = Modifier.size(18.dp))
        Text(label, color = if (selected) Gold else AppOnSurfaceMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
    }
}

/** Horizontal scroll row of icon+label chips (adjust tools, layer tools). */
@Composable
fun <T> ToolIconRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { t ->
            val sel = t == selected
            // Same pressed-vs-floating language as the tool rail above: the active tool sits
            // carved INTO the panel, every other one floats off it.
            Column(
                Modifier
                    .then(
                        if (sel) Modifier.neuInset(cornerRadius = 12.dp, elevation = 5.dp)
                        else Modifier.neuRaised(cornerRadius = 12.dp, elevation = 5.dp),
                    )
                    .clip(ChipSquircle)
                    .clickable { onSelect(t) }.padding(horizontal = 11.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon(t), label(t), tint = if (sel) Gold else AppOnSurface, modifier = Modifier.size(17.dp))
                Text(label(t), color = if (sel) Gold else AppOnSurfaceMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** A single labelled slider with −/+/↺ controls (the active adjustment). */
@Composable
fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().neuRaised(cornerRadius = 14.dp, elevation = 7.dp).clip(CardSquircle).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label.uppercase(), color = AppOnSurfaceMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(format(value), color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            SqBtn("−") { onChange((value - step).coerceIn(range.start, range.endInclusive)) }
            SqBtn("+") { onChange((value + step).coerceIn(range.start, range.endInclusive)) }
            SqBtn(icon = Icons.Rounded.Refresh, onClick = onReset)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = AppSurfaceVariant),
        )
    }
}

@Composable
private fun SqBtn(text: String? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 5.dp).size(34.dp)
            .neuClickable(cornerRadius = 9.dp, elevation = 5.dp, onClick = onClick)
            .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, null, tint = Gold, modifier = Modifier.size(15.dp))
        else Text(text ?: "", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditorTabBar(selected: EditorTab, onSelect: (EditorTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        EditorTab.entries.forEach { t ->
            val sel = t == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .then(
                        if (sel) Modifier.neuInset(cornerRadius = 10.dp, elevation = 4.dp)
                        else Modifier.neuRaised(cornerRadius = 10.dp, elevation = 4.dp),
                    )
                    .clip(RoundedCornerShape(10.dp)).clickable { onSelect(t) }.padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                Text(t.label.uppercase(), color = if (sel) Gold else AppOnSurfaceMuted, fontSize = 11.sp, letterSpacing = 0.5.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                Box(Modifier.padding(top = 3.dp).size(width = 16.dp, height = 2.dp).clip(RoundedCornerShape(50)).background(if (sel) Gold else Color.Transparent))
            }
        }
    }
}

@Composable
fun PillButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, filled: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val bg = if (filled) Gold else AppSurfaceVariant
    val fg = when { danger -> Danger; filled -> AppBackground; else -> AppOnSurface }
    Row(
        modifier.neuClickable(cornerRadius = 12.dp, elevation = 6.dp, background = bg, onClick = onClick).padding(vertical = 9.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Text("  $label", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
}

/** Horizontal tool-rail pill (Photo/Text/Sticker/Overlay/Emoji/Shape). */
@Composable
private fun ToolPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        Modifier.neuClickable(cornerRadius = 12.dp, elevation = 6.dp, onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = Gold, modifier = Modifier.size(22.dp))
        Text(label, color = AppOnSurface, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 3.dp))
    }
}

/** A hand-drawn-style dashed rounded frame drawn ON TOP of the content (marker/brush look). */
private fun Modifier.brushFrame(color: Color): Modifier = drawWithContent {
    drawContent()
    val inset = 3.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(20f, 14f), 0f)
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        style = Stroke(width = 3.dp.toPx(), pathEffect = dash),
    )
}

/**
 * Empty-canvas "tap to add a photo/video" state, shared by Sticker and Animation mode. Used to
 * be one illustrated PNG with GALLERY/CAMERA/UNSPLASH buttons baked into the artwork — none of
 * which did anything distinct, since the whole image was one big click target wired to a single
 * picker. Replaced with a plain icon + heading + hint that says what tapping actually does.
 */
@Composable
private fun BoxScope.AddPhotoPlaceholder(isVideo: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.matchParentSize().clickable(onClick = onClick).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).neuRaised(cornerRadius = 36.dp, elevation = 7.dp).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isVideo) Icons.Rounded.Movie else Icons.Rounded.AddPhotoAlternate,
                null, tint = Gold, modifier = Modifier.size(30.dp),
            )
        }
        // White, not the theme's on-surface/muted pair: this sits on the canvas checkerboard,
        // which is mid-to-dark grey in every palette, so theme-colored text disappeared into it.
        Text(
            if (isVideo) "Add a video" else "Add a photo",
            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            if (isVideo) "Tap to pick a short clip, or add an animated sticker below."
            else "Tap to pick one from your gallery, or add text, emoji & stickers below.",
            color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center,
            // Both hints reserve two lines, so swapping Sticker↔Animation can't change this
            // column's height — that height change was what made the icon above jump.
            minLines = 2,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ModeSwitchBtn(
    label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier,
    dimmed: Boolean = false, onClick: () -> Unit,
) {
    // Locked (see EditorScreen's `lockReason`): the non-selected side is dimmed, since a dimmed
    // `selected` side would look the same — the selected pill is already visually "on". Still
    // clickable: the tap explains why it won't switch.
    val alpha = if (!dimmed || selected) 1f else 0.35f
    Row(
        modifier.clip(RoundedCornerShape(50)).background(if (selected) Gold else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = (if (selected) AppBackground else AppOnSurfaceMuted).copy(alpha = alpha), modifier = Modifier.size(21.dp))
        if (selected) {
            Text(
                label, color = OnGold, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp, modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

/** Animation-mode bottom bar: pick a short video (encoded to an animated WebP on save). */
@Composable
private fun AnimBar(hasVideo: Boolean, onPickVideo: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(AppSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PillButton(
            if (hasVideo) "Choose a different video" else "Choose video",
            Icons.Rounded.Movie, Modifier.fillMaxWidth(), filled = true, onClick = onPickVideo,
        )
        PanelHint(
            if (hasVideo) "Ready — tap ✓ to save. The clip loops as a 512² animated sticker (trimmed to stay under 500KB for WhatsApp)."
            else "Pick a short clip — a few seconds works best. It becomes a looping animated sticker.",
        )
    }
}

@Composable
fun PanelHint(text: String) {
    Text(text, color = AppOnSurfaceMuted, fontSize = 12.sp)
}

@Composable
fun ColorSwatchRow(selectedArgb: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TEXT_COLORS.forEach { c ->
            val argb = c.toArgb()
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(c)
                    .border(if (argb == selectedArgb) 3.dp else 1.dp, if (argb == selectedArgb) Gold else AppSurfaceVariant, CircleShape)
                    .clickable { onPick(argb) },
            )
        }
    }
}

@Composable
fun FontChipRow(selectedPath: String?, onPick: (String?) -> Unit) {
    val customFonts by ServiceLocator.catalogRepository.customFonts.collectAsStateSafe()
    val hiddenPaths by ServiceLocator.catalogRepository.hiddenFontPaths.collectAsStateSafe()
    val fontOrder by ServiceLocator.catalogRepository.fontOrder.collectAsStateSafe()
    // Built-ins (minus any the admin hid) then whatever the admin has uploaded — a custom
    // font's "path" is just its URL, which rememberFontFamily recognises and downloads/caches on
    // first use — then re-sorted into the admin's chosen order. The sort is stable, so anything
    // the admin hasn't placed yet keeps this natural order after the placed ones.
    val entries = remember(customFonts, hiddenPaths, fontOrder) {
        val all = EDITOR_FONTS.filter { it.second == null || it.second !in hiddenPaths }.map { (it.second ?: "default") to it.second } +
            customFonts.map { "custom:${it.id}" to it.url }
        val rank = fontOrder.withIndex().associate { it.value to it.index }
        all.sortedBy { rank[it.first] ?: Int.MAX_VALUE }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { (_, path) ->
            val sel = path == selectedPath
            val family = rememberFontFamily(path)
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) AppSurfaceVariant else AppSurface)
                    .border(1.dp, if (sel) Gold else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { onPick(path) }.padding(horizontal = 16.dp, vertical = 9.dp),
            ) { Text("Aa", color = if (sel) Gold else AppOnSurface, fontFamily = family, fontSize = 16.sp) }
        }
    }
}

@Composable
fun AlignBtn(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).neuRaised(cornerRadius = 12.dp, elevation = 6.dp, background = if (selected) Gold else AppSurfaceVariant).clip(ChipSquircle).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (selected) AppBackground else AppOnSurface, modifier = Modifier.size(20.dp)) }
}

@Composable
fun HighlightSwatchRow(current: Int?, onPick: (Int?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(AppSurfaceVariant)
                .border(if (current == null) 3.dp else 1.dp, if (current == null) Gold else AppSurfaceVariant, CircleShape)
                .clickable { onPick(null) },
            contentAlignment = Alignment.Center,
        ) { Text("∅", color = AppOnSurface, fontSize = 14.sp) }
        TEXT_COLORS.forEach { c ->
            val argb = c.toArgb()
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c)
                    .border(if (argb == current) 3.dp else 1.dp, if (argb == current) Gold else AppSurfaceVariant, CircleShape)
                    .clickable { onPick(argb) },
            )
        }
    }
}

@Composable
fun BackgroundSwatchRow(current: Color?, onPick: (Color?) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(AppSurfaceVariant)
                .border(if (current == null) 3.dp else 1.dp, if (current == null) Gold else AppSurfaceVariant, CircleShape)
                .clickable { onPick(null) },
            contentAlignment = Alignment.Center,
        ) { Text("∅", color = AppOnSurface, fontSize = 14.sp) }
        listOf(Color.Black, Color.White, Color(0xFF46A758), Gold, Color(0xFFE5484D), Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFFEC4899)).forEach { c ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c)
                    .border(if (current == c) 3.dp else 1.dp, if (current == c) Gold else AppSurfaceVariant, CircleShape)
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
fun ShapePickerDialog(onPick: (ShapeKind, Color) -> Unit, onDismiss: () -> Unit) {
    var color by remember { mutableStateOf(Color.White) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Add shape", color = AppOnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ColorSwatchRow(color.toArgb()) { color = Color(it) }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShapeKind.entries.forEach { k ->
                        Box(
                            Modifier.size(52.dp).neuRaised(cornerRadius = 12.dp, elevation = 6.dp).clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(k, color) },
                            contentAlignment = Alignment.Center,
                        ) { Text(shapeGlyph(k), color = color, fontSize = 26.sp) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close", color = AppOnSurfaceMuted) } },
    )
}

/** Map a template's shape name (as the web admin writes it) onto the editor's own enum. */
fun shapeKindOf(name: String): ShapeKind = when (name.lowercase()) {
    "square" -> ShapeKind.SQUARE
    "rounded" -> ShapeKind.ROUNDED
    "triangle" -> ShapeKind.TRIANGLE
    "heart" -> ShapeKind.HEART
    else -> ShapeKind.CIRCLE
}

fun shapeGlyph(k: ShapeKind): String = when (k) {
    ShapeKind.CIRCLE -> "●"; ShapeKind.SQUARE -> "■"; ShapeKind.ROUNDED -> "▢"
    ShapeKind.TRIANGLE -> "▲"; ShapeKind.HEART -> "♥"
}

@Composable
fun IconButtonRound(icon: ImageVector, gold: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).neuClickable(cornerRadius = 20.dp, elevation = 6.dp, background = if (gold) Gold else AppSurface, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (gold) AppBackground else AppOnSurface, modifier = Modifier.size(22.dp)) }
}

/** Undo/redo chip. Greys out (and stops responding) when there's nothing left on that stack —
 * shared by the sticker editor and the canvas template editor's top bars. */
@Composable
fun UndoRedoButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) AppSurfaceVariant else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, label,
            tint = if (enabled) AppOnSurface else AppOnSurfaceMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(19.dp),
        )
    }
}

/** Render a filled shape into a transparent 512×512 bitmap for use as an overlay sticker. */
fun makeShapeBitmap(kind: ShapeKind, colorArgb: Int, size: Int = 512): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
    val m = size * 0.1f
    val s = size - 2 * m
    when (kind) {
        ShapeKind.CIRCLE -> canvas.drawCircle(size / 2f, size / 2f, s / 2f, paint)
        ShapeKind.SQUARE -> canvas.drawRect(m, m, m + s, m + s, paint)
        ShapeKind.ROUNDED -> canvas.drawRoundRect(m, m, m + s, m + s, s * 0.2f, s * 0.2f, paint)
        ShapeKind.TRIANGLE -> canvas.drawPath(
            Path().apply { moveTo(size / 2f, m); lineTo(m + s, m + s); lineTo(m, m + s); close() }, paint,
        )
        ShapeKind.HEART -> canvas.drawPath(
            Path().apply {
                val cx = size / 2f; val top = size * 0.30f
                moveTo(cx, size * 0.82f)
                cubicTo(size * 0.08f, size * 0.52f, size * 0.20f, size * 0.16f, cx, top)
                cubicTo(size * 0.80f, size * 0.16f, size * 0.92f, size * 0.52f, cx, size * 0.82f)
                close()
            },
            paint,
        )
    }
    return bmp
}
