package com.stickers.bank.ui.templates

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.FlipToFront
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.stickers.bank.admin.data.AdminRepository
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.data.model.TEMPLATE_CANVAS
import com.stickers.bank.data.model.TemplateLayer
import com.stickers.bank.ui.SystemChrome
import com.stickers.bank.ui.components.SquircleShape
import com.stickers.bank.ui.create.AdjustSlider
import com.stickers.bank.ui.create.AdjustTool
import com.stickers.bank.ui.create.AlignBtn
import com.stickers.bank.ui.create.BackgroundSwatchRow
import com.stickers.bank.ui.create.CenterGuides
import com.stickers.bank.ui.create.ColorSwatchRow
import com.stickers.bank.ui.create.DecoratorSheet
import com.stickers.bank.ui.create.DrawStroke
import com.stickers.bank.ui.create.EditableBase
import com.stickers.bank.ui.create.EditorHistory
import com.stickers.bank.ui.create.EditorHistoryWatcher
import com.stickers.bank.ui.create.EditorItem
import com.stickers.bank.ui.create.EditorSnapshot
import com.stickers.bank.ui.create.FilterPreset
import com.stickers.bank.ui.create.FontChipRow
import com.stickers.bank.ui.create.HighlightSwatchRow
import com.stickers.bank.ui.create.ImageItem
import com.stickers.bank.ui.create.LayerTool
import com.stickers.bank.ui.create.OverlayView
import com.stickers.bank.ui.create.PillButton
import com.stickers.bank.ui.create.ShapeKind
import com.stickers.bank.ui.create.StickerEmojiPicker
import com.stickers.bank.ui.create.TextItem
import com.stickers.bank.ui.create.TogglePill
import com.stickers.bank.ui.create.Tool
import com.stickers.bank.ui.create.ToolIconRow
import com.stickers.bank.ui.create.UniversalEraseLayer
import com.stickers.bank.ui.create.bringToFront
import com.stickers.bank.ui.create.buildColorFilter
import com.stickers.bank.ui.create.copyEditorItem
import com.stickers.bank.ui.create.deepCopy
import com.stickers.bank.ui.create.dialogFieldColors
import com.stickers.bank.ui.create.makeShapeBitmap
import com.stickers.bank.ui.create.moveToPriority
import com.stickers.bank.ui.create.priorityFromTop
import com.stickers.bank.ui.create.sendToBack
import com.stickers.bank.ui.create.shapeGlyph
import com.stickers.bank.ui.create.snapToCenter
import com.stickers.bank.ui.crop.CropDialog
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.colorToHex
import com.stickers.bank.ui.theme.neuClickable
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.ui.theme.parseHexColor
import com.stickers.bank.util.BitmapUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------------------------
// A SEPARATE editor, reachable only from Canvas Templates. It reuses the same underlying
// engine as the general sticker/photo editor (EditorItem/TextItem/ImageItem, the overlay
// renderer, the crop/text/sticker dialogs, every slider/swatch control) but with entirely
// different chrome, built to match Canva's own layout: the canvas dominates the screen, a
// single slim icon row sits at the very bottom, and every tool opens either a compact docked
// panel (Adjust/Filter/Background/text & layer style — stays non-modal so you can see the live
// result while you drag) or a real bottom-sheet popup (adding new content: photo/text/overlay/
// shape/sticker/emoji). Selecting an element swaps that same bottom row for Duplicate/Delete/
// Style, right on screen — no burying them in a menu.
//
// This is a PHOTO editor: a template composes onto a photo, previewed live, and Export writes
// the flattened result straight to the gallery. No sticker/animated-video output path here —
// that's the general editor's job (see EditorScreen.kt).
// ---------------------------------------------------------------------------------------------

private val TplBg = Color(0xFFFFFFFF)
private val TplSurface = Color(0xFFF5F5F7)
private val TplSurfaceVariant = Color(0xFFEBEBF0)
private val TplElevated = Color(0xFFFFFFFF)
private val TplOnSurface = Color(0xFF17171C)
private val TplOnSurfaceMuted = Color(0xFF6E6E76)
private val TplOutline = Color(0xFFE2E2E8)

/** Stand-in for a template image layer that hasn't downloaded yet. */
private fun blankBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

private sealed interface CropTarget {
    object Base : CropTarget
    object NewOverlay : CropTarget
    data class Replace(val item: ImageItem) : CropTarget
    /** Re-crop a layer already on the canvas — crop, remove background, flip — in place. */
    data class Recrop(val item: ImageItem) : CropTarget
}

/** Tools that need to stay live on the canvas while active (freehand gestures), so they show a
 * compact docked control strip instead of a picker/panel. */
private enum class ToolMode { NONE, DRAW, ERASE }

/** Non-modal docked panels — deliberately NOT a ModalBottomSheet, so the canvas keeps showing
 * a live, undimmed preview while you drag a slider or pick a color. */
private enum class Panel { ADJUST, FILTER, BACKGROUND, TEXT_STYLE, LAYER_STYLE }

/** Modal bottom-sheet popups — for browsing/adding new content, where there's nothing on the
 * canvas yet to preview live against. */
private enum class Sheet { PHOTO, TEXT_EDIT, OVERLAY, SHAPE }

/**
 * A blank canvas the user made themselves is opened through this same editor, with a synthetic
 * id instead of a real template's: `new:<width/height ratio>`. Returns that ratio, or null for
 * an ordinary template id.
 */
fun blankCanvasRatio(templateId: String): Float? =
    templateId.removePrefix(BLANK_CANVAS_PREFIX)
        .takeIf { templateId.startsWith(BLANK_CANVAS_PREFIX) }
        ?.toFloatOrNull()
        ?.takeIf { it > 0f }

const val BLANK_CANVAS_PREFIX = "new:"

/** The id that opens a blank canvas of [ratio] (width / height). */
fun blankCanvasId(ratio: Float): String = "$BLANK_CANVAS_PREFIX$ratio"

@Composable
fun CanvasTemplateEditorScreen(
    nav: NavHostController,
    templateId: String,
    // Admin-authoring mode: opened from the in-app admin panel to BUILD a new template (instead
    // of a normal user opening an existing one to fill in) — see CanvasTemplateEditorContent's
    // "Save Template" top-bar action. adminCategory is the row it'll be filed under.
    adminMode: Boolean = false,
    adminCategory: String = "",
) {
    // Users opening a real template (not the admin building one, not a blank "new" canvas).
    LaunchedEffect(templateId) {
        if (!adminMode && !templateId.startsWith(BLANK_CANVAS_PREFIX)) ServiceLocator.analytics.templateOpened(templateId)
    }
    // Ask the app shell to paint white behind the status bar (with dark icons) for as long as
    // this screen is up — otherwise the dark backdrop collage shows through above this screen's
    // own white top bar, which reads as broken rather than intentional.
    DisposableEffect(Unit) {
        SystemChrome.lightStatusBar = true
        onDispose { SystemChrome.lightStatusBar = false }
    }
    val lightColors = MaterialTheme.colorScheme.copy(
        background = TplBg, surface = TplSurface, surfaceVariant = TplSurfaceVariant,
        onBackground = TplOnSurface, onSurface = TplOnSurface, onSurfaceVariant = TplOnSurfaceMuted,
        outline = TplOutline, primary = Gold, onPrimary = OnGold,
    )
    MaterialTheme(colorScheme = lightColors) {
        Box(Modifier.fillMaxSize().background(TplBg)) {
            CanvasTemplateEditorContent(nav, templateId, adminMode, adminCategory)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanvasTemplateEditorContent(
    nav: NavHostController,
    templateId: String,
    adminMode: Boolean = false,
    adminCategory: String = "",
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val showAd = com.stickers.bank.ads.rememberInterstitial()
    val graphicsLayer = rememberGraphicsLayer()
    val adminRepo = remember { AdminRepository() }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var savingTemplate by remember { mutableStateOf(false) }

    // Backing out mid-edit (without exporting) stashes the CONTENT here — see CanvasEditorDraft —
    // so re-entering this same template resumes it instead of replaying the saved config again.
    val draft = remember(templateId) { CanvasEditorDraftStore.get(templateId) }
    var base by remember { mutableStateOf(draft?.base) }
    var baseOffset by remember { mutableStateOf(draft?.baseOffset ?: Offset.Zero) }
    var baseScale by remember { mutableStateOf(draft?.baseScale ?: 1f) }
    // Split from baseScale only when a corner handle stretches one axis; pinch moves both.
    var baseScaleY by remember { mutableStateOf(draft?.baseScaleY ?: 1f) }
    var baseRotation by remember { mutableStateOf(draft?.baseRotation ?: 0f) }
    var baseFlipX by remember { mutableStateOf(draft?.baseFlipX ?: false) }
    var baseAlpha by remember { mutableStateOf(draft?.baseAlpha ?: 1f) }
    // The photo itself can be "the selected layer", same as an overlay — that's what shows its
    // border + corner resize handles.
    var baseSelected by remember { mutableStateOf(false) }
    // Alignment guides: flash while a drag lands dead-center on either axis — same debounce
    // pattern as the general sticker editor (see its own comment for why).
    var snapGuideX by remember { mutableStateOf(false) }
    var snapGuideY by remember { mutableStateOf(false) }
    var lastSnapAt by remember { mutableStateOf(0L) }
    fun applyCenterSnap(raw: Offset): Offset {
        val (snapped, sx, sy) = snapToCenter(raw)
        snapGuideX = sx; snapGuideY = sy
        if (sx || sy) lastSnapAt = System.currentTimeMillis()
        return snapped
    }
    androidx.compose.runtime.LaunchedEffect(lastSnapAt) {
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
    var drawErase by remember { mutableStateOf(false) }

    // Same live EditorItem objects as before the dispose, not copies.
    val items: SnapshotStateList<EditorItem> = remember { mutableStateListOf<EditorItem>().apply { draft?.items?.let { addAll(it) } } }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    // Built up via each item's own multi-select handle (tap to add/remove) — separate from
    // `selectedId`, which stays the plain single "tap the item itself" selection.
    var multiSelectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var nextId by remember { mutableStateOf(draft?.nextId ?: 1L) }
    // Ratchets true the moment an export actually completes — see exportToGallery — so backing
    // out AFTER a successful export doesn't resurrect stale content next time.
    var committed by remember { mutableStateOf(false) }

    var toolMode by remember { mutableStateOf(ToolMode.NONE) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var adjustTool by remember { mutableStateOf(AdjustTool.BRIGHTNESS) }
    var layerTool by remember { mutableStateOf(LayerTool.OPACITY) }
    val tool = when (toolMode) { ToolMode.DRAW -> Tool.DRAW; ToolMode.ERASE -> Tool.ERASE; ToolMode.NONE -> Tool.MOVE }

    fun deselect() { selectedId = null; panel = null; multiSelectedIds = emptySet(); baseSelected = false }

    fun toggleMultiSelect(id: Long) {
        multiSelectedIds = if (id in multiSelectedIds) multiSelectedIds - id else multiSelectedIds + id
        selectedId = id
        panel = null
    }

    var editingTextItem by remember { mutableStateOf<TextItem?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showDecorator by remember { mutableStateOf(false) }
    var pickerTab by remember { mutableStateOf(0) }
    var canvasWidthPx by remember { mutableStateOf(0f) }
    var canvasHeightPx by remember { mutableStateOf(0f) }
    // Already true when a draft was resumed — the config-rebuild effect below must NOT replay
    // the template's originally-saved layers on top of (or instead of) the resumed edits.
    var templateApplied by remember { mutableStateOf(draft != null) }
    var templateTitle by remember { mutableStateOf("") }
    var loadedTemplate by remember { mutableStateOf<com.stickers.bank.data.model.CanvasTemplate?>(null) }
    // width/height, e.g. 1.0 square, 0.8 for 4:5 — known as soon as the template's metadata
    // loads, well before its full layer config is ready, so the canvas can be sized to it
    // immediately instead of always defaulting to square. Starts at 1f only for the brief
    // instant before metadata arrives; a template whose own ratio is unset (0) resolves to
    // DEFAULT_TEMPLATE_RATIO, matching the shape its card already shows in the browse grid.
    var templateRatio by remember { mutableStateOf(1f) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var exporting by remember { mutableStateOf(false) }
    // True only while flatten() is baking the canvas into a bitmap — hides the multi-select
    // handles (which otherwise show on every item regardless of selection, so they can be
    // tapped without selecting first) so they never end up burned into the exported image.
    var isCapturing by remember { mutableStateOf(false) }

    // ---- Undo / redo ----------------------------------------------------------------
    // A watcher files one entry per settled change (see EditorHistoryWatcher), so undo covers
    // drags and slider nudges too, not just the discrete add/delete actions.
    val history = remember { EditorHistory() }
    var replayCount by remember { mutableStateOf(0) }

    fun snapshot() = EditorSnapshot(
        base = base, baseOffset = baseOffset, baseScale = baseScale, baseScaleY = baseScaleY,
        baseRotation = baseRotation, baseFlipX = baseFlipX,
        preset = preset, brightness = brightness, contrast = contrast, saturation = saturation,
        warmth = warmth, shadows = shadows, highlights = highlights, bgColor = bgColor,
        drawStrokes = drawStrokes.toList(), eraseStrokes = eraseStrokes.map { it.toList() },
        items = items.toList(), nextId = nextId,
    )

    fun applySnapshot(s: EditorSnapshot) {
        base = s.base
        baseOffset = s.baseOffset; baseScale = s.baseScale; baseScaleY = s.baseScaleY
        baseRotation = s.baseRotation; baseFlipX = s.baseFlipX
        preset = s.preset; brightness = s.brightness; contrast = s.contrast; saturation = s.saturation
        warmth = s.warmth; shadows = s.shadows; highlights = s.highlights
        bgColor = s.bgColor
        drawStrokes.clear(); drawStrokes.addAll(s.drawStrokes.map { st ->
            DrawStroke(st.colorArgb, st.width, st.eraser).also { it.points.addAll(st.points) }
        })
        eraseStrokes.clear(); eraseStrokes.addAll(s.eraseStrokes.map { pts -> mutableStateListOf<Offset>().apply { addAll(pts) } })
        items.clear(); items.addAll(s.items.map(::copyEditorItem))
        nextId = s.nextId
        deselect()
    }

    fun undo() {
        val s = history.undo(snapshot().deepCopy()) ?: return
        applySnapshot(s); replayCount++
    }
    fun redo() {
        val s = history.redo(snapshot().deepCopy()) ?: return
        applySnapshot(s); replayCount++
    }

    EditorHistoryWatcher(history, replayCount) { snapshot() }

    fun resetBaseAdjust() {
        baseOffset = Offset.Zero; baseScale = 1f; baseScaleY = 1f; baseRotation = 0f; baseFlipX = false
        baseAlpha = 1f
        eraseStrokes.clear()
        preset = FilterPreset.ORIGINAL
        brightness = 0f; contrast = 1f; saturation = 1f; warmth = 0f; shadows = 0f; highlights = 0f
    }

    /** Removes the base photo entirely — selecting it used to be a dead end (no delete, no
     * panel at all), unlike an overlay image which has always had one. Also clears every
     * adjustment tied to it, so a later "Photo" pick starts from a clean slate instead of
     * inheriting a crop/filter/position that no longer has a photo to apply to. */
    fun deleteBase() {
        base = null
        resetBaseAdjust()
        baseSelected = false
    }

    // Flush the CURRENT content into the draft store on the way out — unless an export just
    // completed (see exportToGallery), in which case the draft is cleared instead so a future
    // edit of this same template starts from the saved config again, not stale local work.
    DisposableEffect(Unit) {
        onDispose {
            if (committed) {
                CanvasEditorDraftStore.clear(templateId)
            } else {
                CanvasEditorDraftStore.put(
                    templateId,
                    CanvasEditorDraft(
                        base = base, baseOffset = baseOffset, baseScale = baseScale, baseScaleY = baseScaleY,
                        baseRotation = baseRotation, baseFlipX = baseFlipX, baseAlpha = baseAlpha,
                        preset = preset, brightness = brightness, contrast = contrast, saturation = saturation,
                        warmth = warmth, shadows = shadows, highlights = highlights, bgColor = bgColor,
                        drawStrokes = drawStrokes.toList(), items = items.toList(), nextId = nextId,
                    ),
                )
            }
        }
    }

    // Template metadata (title, ratio) loads independently of the canvas being measured — the
    // ratio has to be known BEFORE the canvas is sized, not after, or it'd always measure itself
    // as a square first and only reshape once this arrived (a visible snap on open).
    androidx.compose.runtime.LaunchedEffect(templateId) {
        // A blank user-made canvas ("new:<ratio>") has no template behind it to wait for — just
        // take the ratio out of the id and stop, leaving an empty canvas of that shape.
        blankCanvasRatio(templateId)?.let { ratio ->
            templateRatio = ratio
            templateTitle = "My canvas"
            templateApplied = true
            return@LaunchedEffect
        }
        val t = ServiceLocator.catalogRepository.canvasTemplates
            .mapNotNull { list -> list.firstOrNull { it.id == templateId } }
            .first()
        loadedTemplate = t
        templateTitle = t.title
        templateRatio = t.ratio.takeIf { it > 0f } ?: DEFAULT_TEMPLATE_RATIO
    }

    // Rebuild the canvas the admin saved, layer for layer — identical mapping to the general
    // editor's old template-load logic, just living here now since this is the only screen a
    // template can open in.
    androidx.compose.runtime.LaunchedEffect(templateId) {
        if (templateApplied) return@LaunchedEffect
        // Three things have to arrive before the template can be rebuilt, and any may still be
        // pending on first composition: the template's own metadata, the measured canvas size
        // (which depends on that metadata's ratio), and the catalog itself for the base fetch.
        // All are awaited as flows rather than read once — reading `.value` here would silently
        // give up whenever the editor opened before the catalog finished loading.
        val t = androidx.compose.runtime.snapshotFlow { loadedTemplate }.first { it != null }!!
        androidx.compose.runtime.snapshotFlow { canvasWidthPx > 0f && canvasHeightPx > 0f }.first { it }
        val widthPx = canvasWidthPx
        val heightPx = canvasHeightPx
        val cfg = t.config
        templateApplied = true
        if (cfg == null) return@LaunchedEffect

        parseHexColor(cfg.bgColor)?.let { bgColor = it }
        // Reference-canvas quantities (offsets, sizes) all scale uniformly off the WIDTH axis —
        // only position itself is per-axis (see toOffset) — so a shape or photo never stretches
        // just because the canvas isn't square.
        val pxScale = widthPx / TEMPLATE_CANVAS
        cfg.baseImage?.let { url ->
            // Its transform applies immediately; the bitmap itself streams in separately so a
            // slow or unreachable base photo can't hold up the layers drawn on top of it.
            baseOffset = Offset(cfg.baseOffsetX * pxScale, cfg.baseOffsetY * pxScale)
            baseScale = cfg.baseScale
            baseScaleY = cfg.baseScaleY
            baseRotation = cfg.baseRotation
            baseFlipX = cfg.baseFlipX
            baseAlpha = cfg.baseAlpha
            launch { base = withContext(Dispatchers.IO) { BitmapUtils.loadUrlBitmap(context, url) } }
        }

        val toOffset = { xr: Float, yr: Float -> Offset((xr - 0.5f) * widthPx, (yr - 0.5f) * heightPx) }
        val sizeScale = pxScale / density.density

        // Every layer is created and added UP FRONT, in order, so z-order is correct and the
        // whole template appears at once. Image layers start on a blank placeholder bitmap and
        // fetch in parallel, swapping themselves in when they arrive (ImageItem.bitmap is
        // observable). Loading them inline instead would make one slow or unreachable image
        // stall every layer behind it.
        for (layer in cfg.layers) {
            val item: EditorItem = when (layer) {
                is TemplateLayer.Text -> TextItem(
                    nextId++, layer.text, (parseHexColor(layer.color) ?: Color.White).toArgb(), null,
                ).apply {
                    fontSize = layer.size * sizeScale
                    bold = layer.bold
                    italic = layer.italic
                    align = layer.align
                    outline = layer.outline
                    outlineWidth = layer.outlineWidth
                    parseHexColor(layer.outlineColor)?.let { outlineColorArgb = it.toArgb() }
                    letterSpacing = layer.letterSpacing
                    lineHeightMul = layer.lineHeight
                    highlightArgb = parseHexColor(layer.highlight)?.toArgb()
                    gradient = layer.gradient
                    parseHexColor(layer.gradientColor2)?.let { gradientColor2Argb = it.toArgb() }
                    shadow = layer.shadow
                    parseHexColor(layer.shadowColor)?.let { shadowColorArgb = it.toArgb() }
                    shadowRadius = layer.shadowBlur
                    glow = layer.glow
                    parseHexColor(layer.glowColor)?.let { glowColorArgb = it.toArgb() }
                    skewX = layer.skewX
                    bend = layer.bend
                    stretchX = layer.stretchX
                    stretchY = layer.stretchY
                }
                is TemplateLayer.Image -> ImageItem(
                    nextId++, blankBitmap(), sizeDp = layer.size * sizeScale,
                ).apply {
                    // The web editor's own resize handles save an aspect-correct box
                    // (boxWidth/boxHeight) independent of `size` — use it when present instead
                    // of the square default the constructor just picked from the blank bitmap.
                    // Absent on any template saved before that feature existed.
                    if (layer.boxWidth != null && layer.boxHeight != null) {
                        widthDp = layer.boxWidth * sizeScale
                        heightDp = layer.boxHeight * sizeScale
                    }
                    strokeWidth = layer.strokeWidth * sizeScale
                    parseHexColor(layer.strokeColor)?.let { strokeColorArgb = it.toArgb() }
                    launch {
                        withContext(Dispatchers.IO) { BitmapUtils.loadUrlBitmap(context, layer.url) }
                            ?.let { bitmap = it }
                    }
                }
                is TemplateLayer.Shape -> (parseHexColor(layer.color) ?: Gold).toArgb().let { colorArgb ->
                    ImageItem(
                        nextId++,
                        makeShapeBitmap(com.stickers.bank.ui.create.shapeKindOf(layer.kind), colorArgb, 512),
                        sizeDp = layer.size * sizeScale,
                        isShape = true,
                        initialShapeColorArgb = colorArgb,
                    )
                }
            }
            item.apply {
                offset = toOffset(layer.xr, layer.yr)
                scale = layer.scale
                rotation = layer.rotation
                alpha = layer.alpha
                items.add(this)
            }
        }
    }

    var pendingCrop by remember { mutableStateOf<Bitmap?>(null) }
    var cropTarget by remember { mutableStateOf<CropTarget>(CropTarget.Base) }

    val pickBase = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        sheet = null
        if (uri != null) scope.launch {
            cropTarget = CropTarget.Base
            pendingCrop = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri) }
        }
    }
    val pickOverlay = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        sheet = null
        if (uri != null) scope.launch {
            cropTarget = CropTarget.NewOverlay
            pendingCrop = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri, 1024) }
        }
    }
    val pickReplacement = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            pendingCrop = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri, 1024) }
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        sheet = null
        if (bmp != null) { cropTarget = CropTarget.Base; pendingCrop = bmp }
    }

    fun addText(text: String) {
        // Black by default — canvases are mostly light designs, where white text vanished.
        val i = TextItem(nextId++, text, Color.Black.toArgb(), null); items.add(i); selectedId = i.id; panel = null
    }
    fun addEmoji(emoji: String) {
        val i = TextItem(nextId++, emoji, Color.White.toArgb(), null).apply { fontSize = 72f }
        items.add(i); selectedId = i.id; panel = null
    }
    fun addStickerOverlay(bmp: Bitmap) {
        val i = ImageItem(nextId++, bmp); items.add(i); selectedId = i.id; panel = null
    }
    fun addShape(kind: ShapeKind, color: Color) {
        val i = ImageItem(nextId++, makeShapeBitmap(kind, color.toArgb()), isShape = true, initialShapeColorArgb = color.toArgb())
        items.add(i); selectedId = i.id; panel = null
    }

    fun copyOf(item: EditorItem): EditorItem {
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
            is ImageItem -> ImageItem(nextId++, item.bitmap, item.sizeDp, item.isShape, item.shapeColorArgb).also {
                it.strokeWidth = item.strokeWidth; it.strokeColorArgb = item.strokeColorArgb
                it.widthDp = item.widthDp; it.heightDp = item.heightDp
            }
        }
        copy.offset = item.offset + Offset(40f, 40f); copy.scale = item.scale
        copy.rotation = item.rotation; copy.alpha = item.alpha
        return copy
    }

    fun duplicate(item: EditorItem) {
        val copy = copyOf(item)
        items.add(copy); selectedId = copy.id
    }

    fun deleteItem(item: EditorItem) {
        items.remove(item); deselect()
    }

    fun duplicateGroup() {
        val originals = items.filter { it.id in multiSelectedIds }
        val copies = originals.map { copyOf(it) }
        items.addAll(copies)
        multiSelectedIds = copies.map { it.id }.toSet()
        selectedId = copies.lastOrNull()?.id
    }

    fun deleteGroup() {
        items.removeAll { it.id in multiSelectedIds }
        deselect()
    }

    /** Flattens the current canvas — used by both Preview (shown, not saved) and Export. */
    suspend fun flatten(): Bitmap {
        isCapturing = true
        withFrameNanos { }; withFrameNanos { }
        val raw = graphicsLayer.toImageBitmap().asAndroidBitmap()
        isCapturing = false
        return raw.copy(Bitmap.Config.ARGB_8888, false) ?: raw
    }

    fun openPreview() {
        scope.launch {
            deselect()
            previewBitmap = flatten()
        }
    }

    fun saveFlattenedToGallery() {
        scope.launch {
            deselect()
            exporting = true
            val ok = withContext(Dispatchers.IO) {
                BitmapUtils.saveToGallery(context, flatten(), "StickersBank_${System.currentTimeMillis()}")
            }
            exporting = false
            Toast.makeText(
                context, if (ok) "Saved to your gallery" else "Couldn't save to the gallery", Toast.LENGTH_SHORT,
            ).show()
            if (ok) { committed = true; nav.popBackStack() }
        }
    }

    fun exportToGallery() {
        if (exporting) return
        exporting = true
        // Ad first (admin template or own canvas alike), then the actual save.
        showAd { saveFlattenedToGallery() }
    }

    /**
     * Admin-only: serializes the CURRENT canvas (base + every overlay, in its live draw order)
     * into the same Firestore shape CatalogRepository.parseTemplateConfig reads back — see
     * TemplateConfig's kdoc. Sizes are converted from this screen's live on-canvas dp into
     * TEMPLATE_CANVAS-reference px (the inverse of the "rebuild the canvas" LaunchedEffect above)
     * so the template renders identically on any device regardless of ITS canvas's pixel size;
     * angles/ratios/multipliers pass through unscaled, same asymmetry as that rebuild effect.
     * Every real image (base + non-shape/shape layers alike — a shape layer is just a colored
     * bitmap once rendered, so it round-trips fine as a plain "image" layer) is re-uploaded to R2
     * at save time since a live EditorItem only ever holds its bitmap, never a URL.
     */
    fun saveAsTemplate(title: String) {
        scope.launch {
            deselect()
            savingTemplate = true
            val result = runCatching {
                val widthPx = canvasWidthPx
                val heightPx = canvasHeightPx
                val pxScale = widthPx / TEMPLATE_CANVAS
                val sizeScale = pxScale / density.density
                val keyPrefix = "${adminCategory.ifBlank { "template" }}_${System.currentTimeMillis()}"

                fun xrOf(it: EditorItem) = 0.5f + it.offset.x / widthPx
                fun yrOf(it: EditorItem) = 0.5f + it.offset.y / heightPx

                val baseImageUrl = base?.let { bmp -> adminRepo.uploadTemplateArt(bmp, keyPrefix, "base") }

                val layerMaps = items.mapIndexed { index, item ->
                    when (item) {
                        is TextItem -> mapOf(
                            "type" to "text",
                            "xr" to xrOf(item), "yr" to yrOf(item),
                            "scale" to item.scale, "rotation" to item.rotation, "alpha" to item.alpha,
                            "text" to item.text,
                            "color" to colorToHex(Color(item.colorArgb)),
                            "size" to (item.fontSize / sizeScale),
                            "bold" to item.bold, "italic" to item.italic, "align" to item.align,
                            "outline" to item.outline,
                            "outlineColor" to colorToHex(Color(item.outlineColorArgb)),
                            "outlineWidth" to item.outlineWidth,
                            "letterSpacing" to item.letterSpacing,
                            "lineHeight" to item.lineHeightMul,
                            "highlight" to item.highlightArgb?.let { colorToHex(Color(it)) },
                            "gradient" to item.gradient,
                            "gradientColor2" to colorToHex(Color(item.gradientColor2Argb)),
                            "shadow" to item.shadow,
                            "shadowColor" to colorToHex(Color(item.shadowColorArgb)),
                            "shadowBlur" to item.shadowRadius,
                            "glow" to item.glow,
                            "glowColor" to colorToHex(Color(item.glowColorArgb)),
                            "skewX" to item.skewX, "bend" to item.bend,
                            "stretchX" to item.stretchX, "stretchY" to item.stretchY,
                        )
                        is ImageItem -> {
                            val url = adminRepo.uploadTemplateArt(item.bitmap, keyPrefix, "layer$index")
                            mapOf(
                                "type" to "image",
                                "xr" to xrOf(item), "yr" to yrOf(item),
                                "scale" to item.scale, "rotation" to item.rotation, "alpha" to item.alpha,
                                "url" to url,
                                "baseSize" to (maxOf(item.widthDp, item.heightDp) / sizeScale),
                                "baseW" to (item.widthDp / sizeScale),
                                "baseH" to (item.heightDp / sizeScale),
                                "strokeW" to (item.strokeWidth / sizeScale),
                                "strokeColor" to colorToHex(Color(item.strokeColorArgb)),
                            )
                        }
                    }
                }

                val configMap = mapOf(
                    "bgColor" to (bgColor?.let { colorToHex(it) } ?: ""),
                    "baseImage" to (baseImageUrl ?: ""),
                    "base" to mapOf(
                        "x" to baseOffset.x / pxScale, "y" to baseOffset.y / pxScale,
                        // scaleY separately: a corner handle can stretch the photo taller or
                        // wider than its uniform scale, and that has to survive a reopen.
                        "scale" to baseScale, "scaleY" to baseScaleY, "rotation" to baseRotation, "flipX" to baseFlipX,
                        "alpha" to baseAlpha,
                    ),
                    "layers" to layerMaps,
                )

                val thumbnail = flatten()
                // loadedTemplate is only ever non-null when templateId names a REAL, already-
                // saved template (see the metadata LaunchedEffect above) — i.e. this session
                // opened an existing card to edit it, not a blank "new:<ratio>" canvas. Updating
                // that same doc in place is what makes this an edit instead of a duplicate.
                val editingId = loadedTemplate?.id
                if (editingId != null) {
                    adminRepo.updateCanvasTemplate(
                        id = editingId, title = title, thumbnail = thumbnail,
                        ratio = templateRatio, config = configMap,
                    )
                } else {
                    adminRepo.addCanvasTemplate(
                        title = title, thumbnail = thumbnail, category = adminCategory,
                        tags = emptyList(), ratio = templateRatio, config = configMap,
                    )
                }
            }
            savingTemplate = false
            if (result.isSuccess) {
                showSaveTemplateDialog = false
                Toast.makeText(context, if (loadedTemplate != null) "Template updated" else "Template saved", Toast.LENGTH_SHORT).show()
                committed = true
                nav.popBackStack()
            } else {
                Toast.makeText(context, "Couldn't save: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val selectedItem = items.firstOrNull { it.id == selectedId }
    val colorFilter = remember(preset, brightness, contrast, saturation, warmth, shadows, highlights) {
        buildColorFilter(preset, brightness, contrast, saturation, warmth, shadows, highlights)
    }
    val canExport = base != null || items.isNotEmpty() || drawStrokes.isNotEmpty()

    Column(Modifier.fillMaxSize()) {
        // ---- Top bar: back, title, Preview, Export ----
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconBtn(Icons.AutoMirrored.Rounded.ArrowBack) { nav.popBackStack() }
            Text(
                templateTitle.ifBlank { "Template" }.uppercase(),
                color = TplOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            com.stickers.bank.ui.create.UndoRedoButton(
                Icons.AutoMirrored.Rounded.Undo, "Undo", history.canUndo,
            ) { undo() }
            com.stickers.bank.ui.create.UndoRedoButton(
                Icons.AutoMirrored.Rounded.Redo, "Redo", history.canRedo,
            ) { redo() }
            Box(Modifier.width(6.dp))
            TplTopBarButton("Preview", Icons.Rounded.Visibility, filled = false, enabled = canExport, onClick = ::openPreview)
            Box(Modifier.width(8.dp))
            if (adminMode) {
                TplTopBarButton(
                    if (loadedTemplate != null) "Update Template" else "Save Template",
                    Icons.Rounded.CloudDownload, filled = true, enabled = canExport && !savingTemplate,
                ) {
                    showSaveTemplateDialog = true
                }
            } else {
                TplTopBarButton("Export", Icons.Rounded.CloudDownload, filled = true, enabled = canExport && !exporting) { exportToGallery() }
            }
        }

        // ---- Canvas — dominates the screen, like the Canva reference ----
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(10.dp)) {
            // Fit the template's own ratio (width/height) inside the available space — like
            // ContentScale.Fit — instead of always forcing a square, so a portrait/landscape
            // template actually renders and exports in its real shape.
            val availRatio = maxWidth.value / maxHeight.value
            val canvasWidth = if (availRatio > templateRatio) maxHeight * templateRatio else maxWidth
            val canvasHeight = if (availRatio > templateRatio) maxHeight else maxWidth / templateRatio
            val widthPx = with(LocalDensity.current) { canvasWidth.toPx() }
            val heightPx = with(LocalDensity.current) { canvasHeight.toPx() }
            androidx.compose.runtime.LaunchedEffect(widthPx, heightPx) {
                canvasWidthPx = widthPx; canvasHeightPx = heightPx
            }

            Box(
                Modifier.align(Alignment.Center).size(width = canvasWidth, height = canvasHeight)
                    .clip(SquircleShape(20.dp)).background(TplElevated).border(1.dp, TplOutline, SquircleShape(20.dp)),
            ) {
                Box(
                    modifier = Modifier.matchParentSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContentCapture(graphicsLayer)
                        .pointerInput(tool) { if (tool == Tool.MOVE) detectTapGestures { deselect() } },
                    contentAlignment = Alignment.Center,
                ) {
                    bgColor?.let { Box(Modifier.matchParentSize().background(it)) }
                    base?.let { bmp ->
                        EditableBase(
                            bitmap = bmp, offset = baseOffset, scale = baseScale, scaleY = baseScaleY,
                            rotation = baseRotation, flipX = baseFlipX, alpha = baseAlpha,
                            colorFilter = colorFilter,
                            onTransform = { pan, zoom, rot ->
                                baseOffset = applyCenterSnap(baseOffset + pan); baseScale *= zoom; baseScaleY *= zoom
                                baseRotation += rot; deselect(); baseSelected = true
                            },
                            transformEnabled = tool == Tool.MOVE,
                            onDoubleTap = { sheet = Sheet.PHOTO },
                            onTap = { deselect(); baseSelected = true },
                            selected = baseSelected,
                            onResize = { sx, sy, shift ->
                                baseScale = sx; baseScaleY = sy; baseOffset += shift
                            },
                        )
                    }
                    Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                        items.forEach { item ->
                            key(item.id) {
                                val inGroup = item.id in multiSelectedIds
                                OverlayView(
                                    item = item,
                                    selected = item.id == selectedId,
                                    interactive = tool == Tool.MOVE,
                                    onSelect = {
                                        selectedId = item.id; panel = null; baseSelected = false
                                        // A plain tap on an item outside the current multi-selection
                                        // drops the group back to just that one item; tapping (or
                                        // dragging) one already in the group leaves it alone so it
                                        // can still be group-dragged right after.
                                        if (!inGroup) multiSelectedIds = emptySet()
                                    },
                                    onEditText = { editingTextItem = it; sheet = Sheet.TEXT_EDIT },
                                    onReplaceImage = {
                                        cropTarget = CropTarget.Replace(it)
                                        pickReplacement.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    multiSelectEnabled = !isCapturing,
                                    multiSelected = inGroup,
                                    onToggleMultiSelect = { toggleMultiSelect(item.id) },
                                    groupDragActive = inGroup && multiSelectedIds.size > 1,
                                    onGroupDrag = { pan -> items.forEach { if (it.id in multiSelectedIds) it.offset += pan } },
                                    onCenterSnap = { sx, sy ->
                                        snapGuideX = sx; snapGuideY = sy
                                        if (sx || sy) lastSnapAt = System.currentTimeMillis()
                                    },
                                )
                            }
                        }
                        com.stickers.bank.ui.create.DrawLayer(strokes = drawStrokes, active = tool == Tool.DRAW, color = drawColor, width = drawWidth, eraser = drawErase)
                    }
                    UniversalEraseLayer(active = tool == Tool.ERASE, brush = brush, strokes = eraseStrokes)
                }
                // On top of everything else in the canvas — visible over both the photo and
                // whatever it's being dragged past.
                CenterGuides(snapGuideX, snapGuideY, Modifier.matchParentSize())

                if (base == null && items.isEmpty() && drawStrokes.isEmpty() && bgColor == null) {
                    TplAddPhotoPlaceholder { sheet = Sheet.PHOTO }
                }
            }
            // "Done" for a selection, pinned right under the top bar's Export so it's always in
            // reach — it used to sit at the far end of the scrolling selection toolbar, off-screen.
            if ((selectedId != null || baseSelected) && multiSelectedIds.size <= 1) {
                Row(
                    Modifier.align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(50)).background(Gold)
                        .clickable { deselect() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Check, "Done", tint = OnGold, modifier = Modifier.size(18.dp))
                    Text("Done", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // ---- Bottom zone: swaps between the main tool row, a selection toolbar, a docked
        // panel, or compact draw/erase controls — always slim, canvas stays dominant. ----
        Box(Modifier.fillMaxWidth().background(TplElevated).border(1.dp, TplOutline, RoundedCornerShape(0.dp))) {
            when {
                toolMode == ToolMode.DRAW -> DrawControlsRow(
                    drawErase = drawErase, onDrawErase = { drawErase = it },
                    drawColor = drawColor, onDrawColor = { drawColor = it; drawErase = false },
                    drawWidth = drawWidth, onDrawWidth = { drawWidth = it },
                    onUndo = { if (drawStrokes.isNotEmpty()) drawStrokes.removeAt(drawStrokes.lastIndex) },
                    onDone = { toolMode = ToolMode.NONE },
                )
                toolMode == ToolMode.ERASE -> EraseControlsRow(
                    brush = brush, onBrush = { brush = it },
                    onUndo = { if (eraseStrokes.isNotEmpty()) eraseStrokes.removeAt(eraseStrokes.lastIndex) },
                    onDone = { toolMode = ToolMode.NONE },
                )
                multiSelectedIds.size > 1 -> GroupSelectionToolbar(
                    count = multiSelectedIds.size,
                    onDuplicate = { duplicateGroup() },
                    onDelete = { deleteGroup() },
                    onDone = { deselect() },
                )
                panel == Panel.ADJUST -> Column(Modifier.fillMaxWidth()) {
                    PanelHeader("Adjust") { panel = null }
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 250.dp).verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp).padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Base photo's own opacity — kept out of the AdjustTool row below (that
                        // one's shared with the general sticker editor, and there it lives in
                        // the Layer tab instead) since a template's base photo has no other
                        // place in this screen a slider like this could go.
                        AdjustSlider("Photo opacity", baseAlpha, 0.1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { baseAlpha = it }, { baseAlpha = 1f })
                        ToolIconRow(AdjustTool.entries, adjustTool, { it.label }, { it.icon }) { adjustTool = it }
                        when (adjustTool) {
                            AdjustTool.BRIGHTNESS -> AdjustSlider("Brightness", brightness, -100f..100f, 5f, { "${it.roundToInt()}" }, { brightness = it }, { brightness = 0f })
                            AdjustTool.CONTRAST -> AdjustSlider("Contrast", contrast, 0.5f..2f, 0.05f, { "${(it * 100).roundToInt()}%" }, { contrast = it }, { contrast = 1f })
                            AdjustTool.SATURATION -> AdjustSlider("Saturation", saturation, 0f..2f, 0.05f, { "${(it * 100).roundToInt()}%" }, { saturation = it }, { saturation = 1f })
                            AdjustTool.WARMTH -> AdjustSlider("Warmth", warmth, -100f..100f, 5f, { "${it.roundToInt()}" }, { warmth = it }, { warmth = 0f })
                            AdjustTool.SHADOWS -> AdjustSlider("Shadows", shadows, -100f..100f, 5f, { "${it.roundToInt()}" }, { shadows = it }, { shadows = 0f })
                            AdjustTool.HIGHLIGHTS -> AdjustSlider("Highlights", highlights, -100f..100f, 5f, { "${it.roundToInt()}" }, { highlights = it }, { highlights = 0f })
                        }
                    }
                }
                panel == Panel.FILTER -> Column(Modifier.fillMaxWidth()) {
                    PanelHeader("Filter") { panel = null }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                            ) { Text(p.label, color = if (sel) OnGold else TplOnSurface, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                }
                panel == Panel.BACKGROUND -> Column(Modifier.fillMaxWidth()) {
                    PanelHeader("Background") { panel = null }
                    Box(Modifier.padding(horizontal = 14.dp).padding(bottom = 16.dp)) { BackgroundSwatchRow(bgColor) { bgColor = it } }
                }
                panel == Panel.TEXT_STYLE && selectedItem is TextItem -> {
                    val item = selectedItem
                    Column(Modifier.fillMaxWidth()) {
                        PanelHeader("Text style") { panel = null }
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp).padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TplSectionLabel("COLOR"); ColorSwatchRow(item.colorArgb) { item.colorArgb = it }
                            TplSectionLabel("FONT"); FontChipRow(item.fontPath) { item.fontPath = it }
                            AdjustSlider("Text size", item.fontSize, 16f..140f, 2f, { "${it.roundToInt()}" }, { item.fontSize = it }, { item.fontSize = 40f })
                            AdjustSlider("Letter spacing", item.letterSpacing, 0f..24f, 1f, { "${it.roundToInt()}" }, { item.letterSpacing = it }, { item.letterSpacing = 0f })
                            AdjustSlider("Line height", item.lineHeightMul, 0.8f..2.5f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.lineHeightMul = it }, { item.lineHeightMul = 1.1f })
                            AdjustSlider("Opacity", item.alpha, 0.1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.alpha = it }, { item.alpha = 1f })
                            TplSectionLabel("TRANSFORM")
                            AdjustSlider("Rotation", item.rotation, -180f..180f, 5f, { "${it.roundToInt()}°" }, { item.rotation = it }, { item.rotation = 0f })
                            AdjustSlider("Scale", item.scale, 0.2f..4f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.scale = it }, { item.scale = 1f })
                            AdjustSlider("X offset", item.offset.x, -400f..400f, 5f, { "${it.roundToInt()}" }, { item.offset = item.offset.copy(x = it) }, { item.offset = item.offset.copy(x = 0f) })
                            AdjustSlider("Y offset", item.offset.y, -400f..400f, 5f, { "${it.roundToInt()}" }, { item.offset = item.offset.copy(y = it) }, { item.offset = item.offset.copy(y = 0f) })
                            AdjustSlider("X stretch", item.stretchX, 0.3f..3f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.stretchX = it }, { item.stretchX = 1f })
                            AdjustSlider("Y stretch", item.stretchY, 0.3f..3f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.stretchY = it }, { item.stretchY = 1f })
                            AdjustSlider("Skew", item.skewX, -1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.skewX = it }, { item.skewX = 0f })
                            AdjustSlider("Bend", item.bend, -100f..100f, 5f, { "${it.roundToInt()}" }, { item.bend = it }, { item.bend = 0f })
                            TplSectionLabel("ALIGN")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AlignBtn(Icons.AutoMirrored.Rounded.FormatAlignLeft, item.align == 0) { item.align = 0 }
                                AlignBtn(Icons.Rounded.FormatAlignCenter, item.align == 1) { item.align = 1 }
                                AlignBtn(Icons.AutoMirrored.Rounded.FormatAlignRight, item.align == 2) { item.align = 2 }
                            }
                            TplSectionLabel("STYLE")
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TogglePill("Bold", item.bold) { item.bold = !item.bold }
                                TogglePill("Italic", item.italic) { item.italic = !item.italic }
                                TogglePill("Outline", item.outline) { item.outline = !item.outline }
                                TogglePill("Shadow", item.shadow) { item.shadow = !item.shadow }
                                TogglePill("Glow", item.glow) { item.glow = !item.glow }
                                TogglePill("Gradient", item.gradient) { item.gradient = !item.gradient }
                            }
                            if (item.gradient) { TplSectionLabel("GRADIENT 2ND COLOR"); ColorSwatchRow(item.gradientColor2Argb) { item.gradientColor2Argb = it } }
                            if (item.glow) { TplSectionLabel("GLOW COLOR"); ColorSwatchRow(item.glowColorArgb) { item.glowColorArgb = it } }
                            if (item.outline) {
                                AdjustSlider("Outline width", item.outlineWidth, 2f..30f, 1f, { "${it.roundToInt()}" }, { item.outlineWidth = it }, { item.outlineWidth = 8f })
                                TplSectionLabel("OUTLINE COLOR"); ColorSwatchRow(item.outlineColorArgb) { item.outlineColorArgb = it }
                            }
                            if (item.shadow) {
                                AdjustSlider("Shadow blur", item.shadowRadius, 0f..30f, 1f, { "${it.roundToInt()}" }, { item.shadowRadius = it }, { item.shadowRadius = 6f })
                                TplSectionLabel("SHADOW COLOR"); ColorSwatchRow(item.shadowColorArgb) { item.shadowColorArgb = it }
                            }
                            TplSectionLabel("HIGHLIGHT"); HighlightSwatchRow(item.highlightArgb) { item.highlightArgb = it }
                            PriorityPicker(item, items)
                        }
                    }
                }
                panel == Panel.LAYER_STYLE && selectedItem is ImageItem -> {
                    val item = selectedItem
                    Column(Modifier.fillMaxWidth()) {
                        PanelHeader("Layer style") { panel = null }
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = 250.dp).verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp).padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ToolIconRow(LayerTool.entries, layerTool, { it.label }, { it.icon }) { layerTool = it }
                            when (layerTool) {
                                LayerTool.OPACITY -> AdjustSlider("Opacity", item.alpha, 0.1f..1f, 0.05f, { "${(it * 100).roundToInt()}%" }, { item.alpha = it }, { item.alpha = 1f })
                                LayerTool.SIZE -> AdjustSlider("Size", item.scale, 0.2f..4f, 0.1f, { "${(it * 100).roundToInt()}%" }, { item.scale = it }, { item.scale = 1f })
                                LayerTool.ROTATE -> AdjustSlider("Rotation", item.rotation, -180f..180f, 5f, { "${it.roundToInt()}°" }, { item.rotation = it }, { item.rotation = 0f })
                            }
                            AdjustSlider("X offset", item.offset.x, -400f..400f, 5f, { "${it.roundToInt()}" }, { item.offset = item.offset.copy(x = it) }, { item.offset = item.offset.copy(x = 0f) })
                            AdjustSlider("Y offset", item.offset.y, -400f..400f, 5f, { "${it.roundToInt()}" }, { item.offset = item.offset.copy(y = it) }, { item.offset = item.offset.copy(y = 0f) })
                            if (item.isShape) {
                                TplSectionLabel("SHAPE COLOR")
                                ColorSwatchRow(item.shapeColorArgb) { item.shapeColorArgb = it }
                            }
                            AdjustSlider("Stroke", item.strokeWidth, 0f..24f, 1f, { if (it <= 0f) "off" else "${it.roundToInt()}px" }, { item.strokeWidth = it }, { item.strokeWidth = 0f })
                            TplSectionLabel("STROKE COLOR")
                            ColorSwatchRow(item.strokeColorArgb) { item.strokeColorArgb = it }
                            PriorityPicker(item, items)
                        }
                    }
                }
                // The base photo used to have no toolbar at all once selected — no delete, no
                // replace shortcut, nothing — while an overlay image got the full SelectionToolbar.
                // Same treatment here closes that gap instead of leaving it a dead end.
                baseSelected && base != null -> BaseSelectionToolbar(
                    onReplace = { sheet = Sheet.PHOTO },
                    onFlip = { baseFlipX = !baseFlipX },
                    onResetTransform = { baseOffset = Offset.Zero; baseScale = 1f; baseScaleY = 1f; baseRotation = 0f; baseAlpha = 1f },
                    onDelete = ::deleteBase,
                )
                selectedItem != null -> SelectionToolbar(
                    item = selectedItem,
                    items = items,
                    onEdit = {
                        when (selectedItem) {
                            is TextItem -> { editingTextItem = selectedItem; sheet = Sheet.TEXT_EDIT }
                            is ImageItem -> {
                                cropTarget = CropTarget.Replace(selectedItem)
                                pickReplacement.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                    },
                    onStyle = { panel = if (selectedItem is TextItem) Panel.TEXT_STYLE else Panel.LAYER_STYLE },
                    onCrop = (selectedItem as? ImageItem)?.takeIf { !it.isShape && it.animation == null }?.let { img ->
                        { cropTarget = CropTarget.Recrop(img); pendingCrop = img.bitmap }
                    },
                    onDuplicate = { duplicate(selectedItem) },
                    onDelete = { deleteItem(selectedItem) },
                    onBringToFront = { bringToFront(items, selectedItem) },
                    onSendToBack = { sendToBack(items, selectedItem) },
                )
                else -> MainToolRow(
                    onPhoto = { sheet = Sheet.PHOTO },
                    onText = { editingTextItem = null; sheet = Sheet.TEXT_EDIT },
                    onSticker = { pickerTab = 1; showPicker = true },
                    onDecorator = { showDecorator = true },
                    onOverlay = { sheet = Sheet.OVERLAY },
                    onEmoji = { pickerTab = 0; showPicker = true },
                    onShape = { sheet = Sheet.SHAPE },
                    onBackground = { panel = Panel.BACKGROUND },
                    onAdjust = { panel = Panel.ADJUST },
                    onFilter = { panel = Panel.FILTER },
                    onDraw = { toolMode = ToolMode.DRAW },
                    onErase = { toolMode = ToolMode.ERASE },
                    onCrop = {
                        if (base == null) Toast.makeText(context, "Add a photo first", Toast.LENGTH_SHORT).show()
                        else { cropTarget = CropTarget.Base; pendingCrop = base }
                    },
                    onFlip = { baseFlipX = !baseFlipX },
                    onResetAdjust = { resetBaseAdjust() },
                )
            }
        }
    }

    pendingCrop?.let { picked ->
        CropDialog(
            bitmap = picked,
            onCancel = { pendingCrop = null },
            onDone = { result ->
                when (val t = cropTarget) {
                    CropTarget.Base -> { base = result; resetBaseAdjust() }
                    CropTarget.NewOverlay -> { val i = ImageItem(nextId++, result); items.add(i); selectedId = i.id; panel = null }
                    is CropTarget.Replace -> { t.item.bitmap = result; selectedId = t.item.id; panel = null }
                    is CropTarget.Recrop -> { t.item.bitmap = result; t.item.refitTo(result); selectedId = t.item.id; panel = null }
                }
                pendingCrop = null
            },
        )
    }

    if (sheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sheet = null; editingTextItem = null }, sheetState = sheetState, containerColor = TplElevated) {
            when (sheet) {
                Sheet.PHOTO -> PhotoSheetContent(
                    onGallery = { pickBase.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onCamera = { takePhoto.launch(null) },
                )
                Sheet.TEXT_EDIT -> TextEditSheetContent(
                    initial = editingTextItem?.text ?: "",
                    onCancel = { sheet = null; editingTextItem = null },
                    onConfirm = { txt ->
                        val existing = editingTextItem
                        if (existing != null) existing.text = txt else if (txt.isNotBlank()) addText(txt)
                        sheet = null; editingTextItem = null
                    },
                )
                Sheet.OVERLAY -> OverlaySheetContent(
                    onGallery = { pickOverlay.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
                Sheet.SHAPE -> ShapeSheetContent(onPick = { kind, color -> addShape(kind, color); sheet = null })
                null -> {}
            }
        }
    }

    if (showPicker) {
        StickerEmojiPicker(
            initialTab = pickerTab,
            onEmoji = { addEmoji(it); showPicker = false },
            onSticker = { sticker: Sticker ->
                scope.launch {
                    val bmp = withContext(Dispatchers.IO) { BitmapUtils.loadBitmap(context, sticker) }
                    if (bmp != null) {
                        // Through the crop step first (crop, remove background, flip, or skip).
                        cropTarget = CropTarget.NewOverlay
                        pendingCrop = bmp
                    } else Toast.makeText(context, "Couldn't load that sticker.", Toast.LENGTH_SHORT).show()
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
                    else Toast.makeText(context, "Couldn't load that decoration.", Toast.LENGTH_SHORT).show()
                    showDecorator = false
                }
            },
            onDismiss = { showDecorator = false },
        )
    }

    previewBitmap?.let { bmp ->
        TplPreviewDialog(bitmap = bmp, onDismiss = { previewBitmap = null }, onExport = { previewBitmap = null; exportToGallery() })
    }

    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            initialTitle = templateTitle,
            category = adminCategory,
            saving = savingTemplate,
            editing = loadedTemplate != null,
            onDismiss = { if (!savingTemplate) showSaveTemplateDialog = false },
            onConfirm = { title -> templateTitle = title; saveAsTemplate(title) },
        )
    }
}

private fun Modifier.drawWithContentCapture(layer: androidx.compose.ui.graphics.layer.GraphicsLayer): Modifier =
    this.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }

// ---------------------------------------------------------------------------------------------
// Chrome pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun RoundIconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).neuClickable(cornerRadius = 20.dp, elevation = 6.dp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = TplOnSurface, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun TplTopBarButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (filled) Gold else TplSurfaceVariant
    val fg = if (filled) OnGold else TplOnSurface
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(bg.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = fg.copy(alpha = if (enabled) 1f else 0.6f), modifier = Modifier.size(15.dp))
        Text(
            label, color = fg.copy(alpha = if (enabled) 1f else 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Small header every docked panel starts with: a title and a gold "done" checkmark. */
@Composable
private fun PanelHeader(title: String, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), color = TplOnSurfaceMuted, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(
            Modifier.clip(CircleShape).background(Gold).clickable(onClick = onDone).padding(8.dp),
        ) { Icon(Icons.Rounded.Check, "Done", tint = OnGold, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun ToolButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = TplOnSurface, modifier = Modifier.size(21.dp))
        Text(label, color = TplOnSurfaceMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
    }
}

/** The default bottom row: everything you can add or adjust, one slim scrollable strip — like
 * Canva's own bottom nav. Nothing here ever grows the panel taller than this single row. */
@Composable
private fun MainToolRow(
    onPhoto: () -> Unit, onText: () -> Unit, onSticker: () -> Unit, onDecorator: () -> Unit, onOverlay: () -> Unit,
    onEmoji: () -> Unit, onShape: () -> Unit, onBackground: () -> Unit, onAdjust: () -> Unit,
    onFilter: () -> Unit, onDraw: () -> Unit, onErase: () -> Unit, onCrop: () -> Unit,
    onFlip: () -> Unit, onResetAdjust: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToolButton("Photo", Icons.Rounded.Image, onPhoto)
        ToolButton("Text", Icons.Rounded.TextFields, onText)
        ToolButton("Sticker", Icons.Rounded.EmojiEmotions, onSticker)
        ToolButton("Decorator", Icons.Rounded.Style, onDecorator)
        ToolButton("Overlay", Icons.Rounded.Layers, onOverlay)
        ToolButton("Emoji", Icons.Rounded.Mood, onEmoji)
        ToolButton("Shape", Icons.Rounded.Category, onShape)
        ToolButton("Background", Icons.Rounded.FormatColorFill, onBackground)
        ToolButton("Adjust", Icons.Rounded.Tune, onAdjust)
        ToolButton("Filter", Icons.Rounded.AutoAwesome, onFilter)
        ToolButton("Draw", Icons.Rounded.WaterDrop, onDraw)
        ToolButton("Erase", Icons.Rounded.Refresh, onErase)
        ToolButton("Crop", Icons.Rounded.Crop, onCrop)
        Box {
            var moreOpen by remember { mutableStateOf(false) }
            ToolButton("More", Icons.Rounded.MoreHoriz) { moreOpen = true }
            DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                DropdownMenuItem(text = { Text("Flip photo") }, onClick = { onFlip(); moreOpen = false })
                DropdownMenuItem(text = { Text("Reset adjustments") }, onClick = { onResetAdjust(); moreOpen = false })
            }
        }
    }
}

/** Shown the moment any image or text is selected — Duplicate and Delete right on screen,
 * plus a Style/Edit shortcut into that item's panel. Matches the Canva reference's contextual
 * toolbar: it REPLACES the main row rather than adding a second layer of chrome. */
@Composable
private fun SelectionToolbar(
    item: EditorItem,
    items: SnapshotStateList<EditorItem>,
    onEdit: () -> Unit,
    onStyle: () -> Unit,
    /** Crop / remove background for an image layer; null hides the button (text, shapes). */
    onCrop: (() -> Unit)?,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(if (item is TextItem) "Edit" else "Replace", if (item is TextItem) Icons.Rounded.Edit else Icons.Rounded.AddPhotoAlternate, onEdit)
        if (onCrop != null) ToolButton("Crop · BG", Icons.Rounded.Crop, onCrop)
        ToolButton("Style", Icons.Rounded.Tune, onStyle)
        PriorityButton(item, items)
        ToolButton("To front", Icons.Rounded.FlipToFront, onBringToFront)
        ToolButton("To back", Icons.Rounded.FlipToBack, onSendToBack)
        ToolButton("Duplicate", Icons.Rounded.ContentCopy, onDuplicate)
        ToolButton("Delete", Icons.Rounded.Delete, onDelete)
    }
}

/**
 * Sets a layer's exact stacking position by number instead of one-step-at-a-time — "3 of 7"
 * means 3 layers sit above it and 3 below. 1 is always topmost (matching
 * [EditorItem.priorityFromTop]'s own convention) regardless of how many layers are on the
 * canvas, so it reads the same whether there are 2 layers or 20.
 */
@Composable
private fun PriorityPicker(item: EditorItem, items: SnapshotStateList<EditorItem>) {
    var expanded by remember { mutableStateOf(false) }
    val total = items.size
    val current = item.priorityFromTop(items)
    TplSectionLabel("LAYER PRIORITY")
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TplSurfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${ordinal(current)} of $total" + when {
                    total == 1 -> ""
                    current == 1 -> "  ·  above all"
                    current == total -> "  ·  behind all"
                    else -> ""
                },
                color = TplOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.ArrowDropDown, null, tint = TplOnSurfaceMuted)
        }
        PriorityMenu(expanded, { expanded = false }, item, items)
    }
}

/** Compact version of [PriorityPicker] for the selection toolbar: the layer's number ("1" =
 * above everything) with the total under it; tapping opens the same menu. */
@Composable
private fun PriorityButton(item: EditorItem, items: SnapshotStateList<EditorItem>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable { expanded = true }.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.height(21.dp).border(1.5.dp, TplOnSurface, RoundedCornerShape(6.dp)).padding(start = 6.dp, end = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${item.priorityFromTop(items)}", color = TplOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.ArrowDropDown, "Change layer priority", tint = TplOnSurface, modifier = Modifier.size(14.dp))
            }
            Text("of ${items.size}", color = TplOnSurfaceMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
        }
        PriorityMenu(expanded, { expanded = false }, item, items)
    }
}

/** Every stacking position, 1st (above all) to last (behind all). Picking one moves the layer
 * there; the layers in between each shift one place to make room. */
@Composable
private fun PriorityMenu(expanded: Boolean, onDismiss: () -> Unit, item: EditorItem, items: SnapshotStateList<EditorItem>) {
    val total = items.size
    val current = item.priorityFromTop(items)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        (1..total).forEach { p ->
            DropdownMenuItem(
                text = {
                    Text(
                        ordinal(p) + when {
                            total == 1 -> " — only layer"
                            p == 1 -> " — above all"
                            p == total -> " — behind all"
                            else -> ""
                        },
                        fontWeight = if (p == current) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = { moveToPriority(items, item, p); onDismiss() },
            )
        }
    }
}

private fun ordinal(n: Int): String =
    n.toString() + if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

/**
 * The base photo's own toolbar — Replace, Flip, Reset (position/scale/rotation back to fit) and
 * Delete. Deliberately simpler than [SelectionToolbar] (no Style panel, no Duplicate — there's
 * only ever one base photo, and its own resize handles already cover size/position), but Delete
 * is exactly the thing that was missing before: the base photo used to be selectable with
 * nothing you could actually DO once selected.
 */
@Composable
private fun BaseSelectionToolbar(
    onReplace: () -> Unit,
    onFlip: () -> Unit,
    onResetTransform: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton("Replace", Icons.Rounded.AddPhotoAlternate, onReplace)
        ToolButton("Flip", Icons.Rounded.Flip, onFlip)
        ToolButton("Reset", Icons.Rounded.Refresh, onResetTransform)
        ToolButton("Delete", Icons.Rounded.Delete, onDelete)
    }
}

/**
 * Shown once 2+ items are picked via their own multi-select handles — Duplicate/Delete act on
 * the whole group at once. Takes over from [SelectionToolbar] (which only ever shows one item's
 * own actions) the moment a second item joins the selection.
 */
@Composable
private fun GroupSelectionToolbar(
    count: Int,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$count selected", color = TplOnSurfaceMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        ToolButton("Duplicate", Icons.Rounded.ContentCopy, onDuplicate)
        ToolButton("Delete", Icons.Rounded.Delete, onDelete)
        Box(
            Modifier.clip(CircleShape).background(Gold).clickable(onClick = onDone).padding(10.dp),
        ) { Icon(Icons.Rounded.Check, "Done", tint = OnGold, modifier = Modifier.size(18.dp)) }
    }
}

/** Compact controls docked at the bottom while Draw is active — the canvas above stays live
 * and touchable, since a full modal sheet would block the very gesture this tool needs. */
@Composable
private fun DrawControlsRow(
    drawErase: Boolean, onDrawErase: (Boolean) -> Unit,
    drawColor: Int, onDrawColor: (Int) -> Unit,
    drawWidth: Float, onDrawWidth: (Float) -> Unit,
    onUndo: () -> Unit, onDone: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        PanelHeader("Draw") { onDone() }
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TogglePill("Brush", !drawErase) { onDrawErase(false) }
                TogglePill("Eraser", drawErase) { onDrawErase(true) }
                Box(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(TplSurfaceVariant).clickable(onClick = onUndo)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Icon(Icons.Rounded.Refresh, "Undo", tint = TplOnSurface, modifier = Modifier.size(16.dp)) }
            }
            if (!drawErase) ColorSwatchRow(drawColor, onDrawColor)
            AdjustSlider(if (drawErase) "Eraser size" else "Brush size", drawWidth, 4f..60f, 2f, { "${it.roundToInt()}" }, onDrawWidth) { onDrawWidth(16f) }
        }
    }
}

@Composable
private fun EraseControlsRow(brush: Float, onBrush: (Float) -> Unit, onUndo: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        PanelHeader("Erase") { onDone() }
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TplPanelHint("Drag on the canvas to erase anything — photo, text, stickers or drawings.")
            AdjustSlider("Eraser size", brush, 15f..160f, 5f, { "${it.roundToInt()}" }, onBrush) { onBrush(60f) }
            PillButton("Undo erase", Icons.Rounded.Refresh, Modifier.fillMaxWidth(), onClick = onUndo)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Modal bottom-sheet contents — for adding new content, where there's nothing to preview live.
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotoSheetContent(onGallery: () -> Unit, onCamera: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 22.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add a photo", color = TplOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
        PillButton("Choose from gallery", Icons.Rounded.Image, Modifier.fillMaxWidth(), filled = true, onClick = onGallery)
        PillButton("Take a photo", Icons.Rounded.PhotoCamera, Modifier.fillMaxWidth(), onClick = onCamera)
    }
}

@Composable
private fun TextEditSheetContent(initial: String, onConfirm: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 22.dp, top = 4.dp),
    ) {
        Text(if (initial.isBlank()) "Add text" else "Edit text", color = TplOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(14.dp))
        TextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text("Type here") }, modifier = Modifier.fillMaxWidth(),
            colors = dialogFieldColors(),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Cancel", Icons.Rounded.Check, Modifier.weight(1f), onClick = onCancel)
            PillButton(if (initial.isBlank()) "Add" else "Save", Icons.Rounded.Check, Modifier.weight(1f), filled = true) {
                if (text.isNotBlank()) onConfirm(text)
            }
        }
    }
}

@Composable
private fun OverlaySheetContent(onGallery: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 22.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add an image overlay", color = TplOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
        TplPanelHint("Adds a photo as its own movable, resizable layer on top of everything else.")
        PillButton("Choose from gallery", Icons.Rounded.Image, Modifier.fillMaxWidth(), filled = true, onClick = onGallery)
    }
}

@Composable
private fun ShapeSheetContent(onPick: (ShapeKind, Color) -> Unit) {
    var color by remember { mutableStateOf(Color.White) }
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 22.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Add shape", color = TplOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
        ColorSwatchRow(color.toArgb()) { color = Color(it) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShapeKind.entries.forEach { k ->
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(TplSurfaceVariant)
                        .clickable { onPick(k, color) },
                    contentAlignment = Alignment.Center,
                ) { Text(shapeGlyph(k), color = color, fontSize = 28.sp) }
            }
        }
    }
}

@Composable
private fun TplSectionLabel(text: String) {
    Text(text, color = TplOnSurfaceMuted, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun TplPanelHint(text: String) {
    Text(text, color = TplOnSurfaceMuted, fontSize = 12.sp)
}

/**
 * Empty-canvas "tap to add a photo" state. Used to be one baked yellow-on-black illustration
 * (ic_pick_image) — fixed colors regardless of context, so it never matched this screen's own
 * light theme. Built from this screen's own tokens (TplSurfaceVariant/TplOnSurface/Gold, the
 * same ones every other circle button and heading here already uses) instead, so it always
 * reads as part of THIS editor rather than a mismatched drop-in graphic.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.TplAddPhotoPlaceholder(onClick: () -> Unit) {
    Column(
        Modifier.matchParentSize().clickable(onClick = onClick).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Gold),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, null, tint = OnGold, modifier = Modifier.size(30.dp))
        }
        Text(
            "Add a photo", color = TplOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            "Tap to pick one from your gallery, or add text, emoji & stickers below.",
            color = TplOnSurfaceMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Read-only, full-screen render of the flattened canvas — lets you check the result without
 * committing to a save. Plain background, no checkerboard: this is meant to show what you'll
 * actually get, not a transparency-editing aid. The bitmap is already in the template's own
 * aspect ratio (the canvas it was flattened from), so `Fit` renders it true to shape. */
/** Admin-only: names the template right before it's uploaded — see CanvasTemplateEditorContent's
 * "Save Template" top-bar action. */
@Composable
private fun SaveTemplateDialog(
    initialTitle: String,
    category: String,
    saving: Boolean,
    editing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(SquircleShape(20.dp)).background(TplSurface)
                .border(1.dp, TplOutline, SquircleShape(20.dp)).padding(20.dp),
        ) {
            Text(
                if (editing) "Update template" else "Save as template",
                color = TplOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Black,
            )
            Text(
                "Row: ${category.ifBlank { "Uncategorized" }}", color = TplOnSurfaceMuted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
            )
            TextField(
                value = title, onValueChange = { title = it }, singleLine = true,
                placeholder = { Text("Template title") }, modifier = Modifier.fillMaxWidth(),
                colors = dialogFieldColors(),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Cancel", Icons.Rounded.Check, Modifier.weight(1f)) { if (!saving) onDismiss() }
                PillButton(
                    when { saving -> if (editing) "Updating…" else "Saving…"; editing -> "Update"; else -> "Save" },
                    Icons.Rounded.Check, Modifier.weight(1f), filled = true,
                ) { if (!saving && title.isNotBlank()) onConfirm(title.trim()) }
            }
        }
    }
}

@Composable
private fun TplPreviewDialog(bitmap: Bitmap, onDismiss: () -> Unit, onExport: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(TplBg)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconBtn(Icons.Rounded.Check) { onDismiss() }
                Text(
                    "PREVIEW", color = TplOnSurfaceMuted, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                TplTopBarButton("Export", Icons.Rounded.CloudDownload, filled = true, enabled = true, onClick = onExport)
            }
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(20.dp)
                    .clip(SquircleShape(20.dp)).background(TplElevated).border(1.dp, TplOutline, SquircleShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
