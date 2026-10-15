package com.stickers.bank.ui.create

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * A snapshot of the editor's actual CONTENT (photo + adjustments + layers) — not every UI nicety
 * (which tool/tab is open, current brush size, erase strokes) — kept so backing out mid-edit
 * without hitting Done doesn't lose the work. [items] holds the SAME live [EditorItem] objects
 * being edited, not copies — their own `offset`/`scale`/`rotation`/`bitmap` are already mutable
 * state, so just keeping the references alive across a dispose/recompose is enough to restore
 * them exactly as they were, no serialization needed.
 */
class EditorDraft(
    var isAnim: Boolean,
    var videoUri: Uri?,
    var base: Bitmap?,
    var baseOffset: Offset,
    var baseScale: Float,
    var baseScaleY: Float,
    var baseRotation: Float,
    var baseFlipX: Boolean,
    var baseAlpha: Float,
    var baseOnTop: Boolean = false,
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

/**
 * In-memory only — deliberately does NOT survive the app process being killed, only backing out
 * and back in within the same running session (e.g. via Create, or re-opening the same sticker).
 * One slot per sticker id, plus a single shared "new" slot for a from-scratch sticker (there's
 * only ever one of those in flight at a time). Cleared the moment a save actually completes, so
 * a resumed draft never lingers after the user got what they came for.
 */
object EditorDraftStore {
    private val drafts = mutableMapOf<String, EditorDraft>()

    fun key(stickerId: String?): String = stickerId ?: "new"

    fun get(stickerId: String?): EditorDraft? = drafts[key(stickerId)]

    fun put(stickerId: String?, draft: EditorDraft) {
        drafts[key(stickerId)] = draft
    }

    fun clear(stickerId: String?) {
        drafts.remove(key(stickerId))
    }
}
