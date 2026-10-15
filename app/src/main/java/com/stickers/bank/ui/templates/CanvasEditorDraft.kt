package com.stickers.bank.ui.templates

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.stickers.bank.ui.create.DrawStroke
import com.stickers.bank.ui.create.EditorItem
import com.stickers.bank.ui.create.FilterPreset

/**
 * Same idea as the general editor's `EditorDraft` (see that file's doc comment) — the CONTENT of
 * an in-progress canvas template edit, kept so backing out mid-edit without exporting doesn't
 * lose the work. [items] holds the same live [EditorItem] objects, not copies. [templateApplied]
 * is stashed too: without it, re-entering would replay the template's ORIGINAL saved config on
 * top of (or instead of) the resumed edits.
 */
class CanvasEditorDraft(
    var base: Bitmap?,
    var baseOffset: Offset,
    var baseScale: Float,
    var baseScaleY: Float,
    var baseRotation: Float,
    var baseFlipX: Boolean,
    var baseAlpha: Float,
    var preset: FilterPreset,
    var brightness: Float,
    var contrast: Float,
    var saturation: Float,
    var warmth: Float,
    var shadows: Float,
    var highlights: Float,
    var bgColor: Color?,
    var drawStrokes: List<DrawStroke>,
    var items: List<EditorItem>,
    var nextId: Long,
)

/** In-memory only, one slot per template id — see EditorDraftStore for the exact same contract. */
object CanvasEditorDraftStore {
    private val drafts = mutableMapOf<String, CanvasEditorDraft>()

    fun get(templateId: String): CanvasEditorDraft? = drafts[templateId]

    fun put(templateId: String, draft: CanvasEditorDraft) {
        drafts[templateId] = draft
    }

    fun clear(templateId: String) {
        drafts.remove(templateId)
    }
}
