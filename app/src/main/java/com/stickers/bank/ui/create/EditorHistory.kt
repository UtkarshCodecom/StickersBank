package com.stickers.bank.ui.create

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * One undoable state of an editor's canvas — everything a user would expect Undo to put back,
 * and nothing that's merely which tool/tab is open. Shared by the sticker editor and the canvas
 * template editor, whose canvas state is the same shape.
 *
 * [items] are deep COPIES (see [copyEditorItem]): the live ones are mutated in place by drags,
 * resizes and the layer panels, so holding references would let later edits rewrite history.
 */
class EditorSnapshot(
    val base: Bitmap?,
    val baseOffset: Offset,
    val baseScale: Float,
    val baseScaleY: Float,
    val baseRotation: Float,
    val baseFlipX: Boolean,
    /** Photo drawn above the overlays instead of under them (the normal editor's "To front"). */
    val baseOnTop: Boolean = false,
    val preset: FilterPreset,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val shadows: Float,
    val highlights: Float,
    val bgColor: Color?,
    val drawStrokes: List<DrawStroke>,
    val eraseStrokes: List<List<Offset>>,
    val items: List<EditorItem>,
    val nextId: Long,
) {
    /**
     * A cheap value-identity string. The history watcher compares these rather than the objects
     * themselves, since [EditorItem]s are mutable and two snapshots taken either side of a drag
     * hold different objects that must still compare as "changed".
     */
    fun signature(): String = buildString {
        append(System.identityHashCode(base)).append('|')
        append(baseOffset).append(baseScale).append(baseScaleY).append(baseRotation).append(baseFlipX).append(baseOnTop).append('|')
        append(preset).append(brightness).append(contrast).append(saturation)
        append(warmth).append(shadows).append(highlights).append('|')
        append(bgColor).append('|')
        // Point counts, not the points: a stroke in progress grows point by point, and that IS
        // the change we want to notice, but the coordinates themselves would cost more to hash
        // than they're worth.
        append(drawStrokes.size).append(':')
        drawStrokes.forEach { append(it.points.size).append(',') }
        append('|').append(eraseStrokes.size).append(':')
        eraseStrokes.forEach { append(it.size).append(',') }
        append('|').append(nextId).append(':')
        // Position in the list IS the stacking order now (see EditorScreen's bringToFront/
        // sendToBack/moveToPriority), so its index has to be part of the signature too —
        // otherwise reordering two items wouldn't register as a change to undo.
        items.forEachIndexed { index, item ->
            append(index).append(item.id).append(item.offset).append(item.scale).append(item.rotation)
            append(item.alpha)
            when (item) {
                is TextItem -> append(item.text).append(item.colorArgb).append(item.fontPath)
                    .append(item.fontSize).append(item.letterSpacing).append(item.lineHeightMul)
                    .append(item.align).append(item.bold).append(item.italic).append(item.skewX)
                    .append(item.bend).append(item.stretchX).append(item.stretchY)
                    .append(item.outline).append(item.outlineWidth)
                    .append(item.outlineColorArgb).append(item.highlightArgb).append(item.gradient)
                    .append(item.gradientColor2Argb).append(item.shadow).append(item.shadowColorArgb)
                    .append(item.shadowRadius).append(item.glow).append(item.glowColorArgb)
                is ImageItem -> append(System.identityHashCode(item.bitmap)).append(item.widthDp)
                    .append(item.heightDp).append(item.strokeWidth).append(item.strokeColorArgb)
                    .append(item.shapeColorArgb)
            }
            append(';')
        }
    }
}

/** A deep copy of [item] keeping its own id, so a restored snapshot is the same layer, not a new one. */
fun copyEditorItem(item: EditorItem): EditorItem {
    val copy: EditorItem = when (item) {
        is TextItem -> TextItem(item.id, item.text, item.colorArgb, item.fontPath).also {
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
        is ImageItem -> ImageItem(item.id, item.bitmap, item.sizeDp, item.isShape, item.shapeColorArgb).also {
            it.strokeWidth = item.strokeWidth; it.strokeColorArgb = item.strokeColorArgb
            // Whatever box it was resized to, not the bitmap's natural ratio the constructor
            // just reset it to.
            it.widthDp = item.widthDp; it.heightDp = item.heightDp
        }
    }
    copy.offset = item.offset; copy.scale = item.scale
    copy.rotation = item.rotation; copy.alpha = item.alpha
    return copy
}

/** A copy of a drawn stroke, so undoing back past it can't be undone by later drawing into it. */
private fun copyStroke(s: DrawStroke): DrawStroke =
    DrawStroke(s.colorArgb, s.width, s.eraser).also { it.points.addAll(s.points) }

/** Snapshot with every mutable piece copied, ready to sit in a history stack untouched. */
fun EditorSnapshot.deepCopy(): EditorSnapshot = EditorSnapshot(
    base = base, baseOffset = baseOffset, baseScale = baseScale, baseScaleY = baseScaleY,
    baseRotation = baseRotation, baseFlipX = baseFlipX, baseOnTop = baseOnTop,
    preset = preset, brightness = brightness, contrast = contrast, saturation = saturation,
    warmth = warmth, shadows = shadows, highlights = highlights, bgColor = bgColor,
    drawStrokes = drawStrokes.map(::copyStroke),
    eraseStrokes = eraseStrokes.map { it.toList() },
    items = items.map(::copyEditorItem),
    nextId = nextId,
)

private const val HISTORY_LIMIT = 40

/**
 * Undo/redo stacks. The "current" state isn't stored here — it's the live editor — so [undo] and
 * [redo] are handed the current state to push onto the opposite stack as they hand back the one
 * to restore.
 */
class EditorHistory {
    private val undos = mutableStateListOf<EditorSnapshot>()
    private val redos = mutableStateListOf<EditorSnapshot>()

    val canUndo: Boolean get() = undos.isNotEmpty()
    val canRedo: Boolean get() = redos.isNotEmpty()

    /** Remember the state as it was BEFORE a change. A new change forks the timeline: redo goes. */
    fun record(before: EditorSnapshot) {
        undos.add(before)
        while (undos.size > HISTORY_LIMIT) undos.removeAt(0)
        redos.clear()
    }

    fun undo(current: EditorSnapshot): EditorSnapshot? {
        val s = undos.removeLastOrNull() ?: return null
        redos.add(current)
        return s
    }

    fun redo(current: EditorSnapshot): EditorSnapshot? {
        val s = redos.removeLastOrNull() ?: return null
        undos.add(current)
        return s
    }

    fun clear() { undos.clear(); redos.clear() }
}

/**
 * Watches the live canvas and files one history entry per settled change.
 *
 * Snapshotting from a watcher rather than from each individual mutation site is what makes undo
 * cover *everything* — dragging a sticker, nudging a slider, drawing a stroke — instead of only
 * the handful of actions someone remembered to instrument. Waiting for the signature to hold
 * still for [SETTLE_MS] is what collapses a whole drag (hundreds of state writes) into a single
 * undo step, which is what a user means by "undo that move".
 *
 * [resyncKey] must change every time an undo/redo is applied: the restored state is a history
 * *replay*, not a new edit, so the watcher restarts and re-baselines onto it rather than
 * recording it (which would clear the redo stack and trap undo in a loop). A counter, not a
 * boolean — a flag set and cleared inside one frame is a value the effect never observes.
 */
@Composable
fun EditorHistoryWatcher(
    history: EditorHistory,
    resyncKey: Int,
    snapshot: () -> EditorSnapshot,
) {
    // Seeded on first composition so the first real edit has a "before" to go back to.
    var last by remember { mutableStateOf(snapshot().deepCopy()) }
    var lastSig by remember { mutableStateOf(last.signature()) }

    LaunchedEffect(resyncKey) {
        // Effects run after the composition that changed the key, so a replay's writes are
        // already visible here — this baseline is the restored state, not the pre-undo one.
        last = snapshot().deepCopy()
        lastSig = last.signature()
        while (true) {
            delay(POLL_MS)
            val sig = snapshot().signature()
            if (sig == lastSig) continue
            // Let it settle, so one gesture is one undo step rather than one per frame.
            delay(SETTLE_MS)
            val settled = snapshot()
            if (settled.signature() != sig) continue
            history.record(last)
            last = settled.deepCopy()
            lastSig = settled.signature()
        }
    }
}

private const val POLL_MS = 250L
private const val SETTLE_MS = 300L
