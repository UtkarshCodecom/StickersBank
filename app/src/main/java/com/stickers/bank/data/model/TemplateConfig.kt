package com.stickers.bank.data.model

/**
 * The reference canvas the web admin builds templates on. Layer positions are stored relative
 * to it (and font/shape sizes in its pixels), so a template lands identically on any phone
 * regardless of that phone's actual canvas size.
 */
const val TEMPLATE_CANVAS = 512f

/** A saved editor canvas — what the admin built, rebuilt layer-for-layer in the app's editor. */
data class TemplateConfig(
    val bgColor: String? = null,
    val baseImage: String? = null,
    // How the admin left the base photo — offset (reference px from centre), scale, rotation.
    // Without this, the base photo always reloads centred/unscaled regardless of how it was
    // actually arranged in the web editor.
    val baseOffsetX: Float = 0f,
    val baseOffsetY: Float = 0f,
    val baseScale: Float = 1f,
    /** The base photo's vertical scale — differs from [baseScale] once a corner handle has
     * stretched it on one axis. Templates saved before this existed carry just [baseScale]. */
    val baseScaleY: Float = baseScale,
    val baseRotation: Float = 0f,
    val baseFlipX: Boolean = false,
    val baseAlpha: Float = 1f,
    val layers: List<TemplateLayer> = emptyList(),
)

/**
 * One layer of a template. [xr]/[yr] are 0..1 across the canvas (0.5 = dead centre); [scale],
 * [rotation] and [alpha] carry the transform the admin left it at.
 */
sealed interface TemplateLayer {
    val xr: Float
    val yr: Float
    val scale: Float
    val rotation: Float
    val alpha: Float

    /** Live, retypable text — the thing users are meant to change. */
    data class Text(
        override val xr: Float,
        override val yr: Float,
        override val scale: Float,
        override val rotation: Float,
        override val alpha: Float,
        val text: String,
        val color: String? = null,
        val size: Float = 56f,
        val bold: Boolean = true,
        val italic: Boolean = false,
        val align: Int = 1,
        val outline: Boolean = false,
        val outlineColor: String? = null,
        val outlineWidth: Float = 8f,
        val letterSpacing: Float = 0f,
        val lineHeight: Float = 1.1f,
        val highlight: String? = null,
        // These four mirror what both live editors can already do to a text layer — without
        // them, a template built with e.g. glowing or curved text loses that effect the moment
        // it's saved, because there was nowhere to put it on the way out.
        val gradient: Boolean = false,
        val gradientColor2: String? = null,
        val shadow: Boolean = false,
        val shadowColor: String? = null,
        val shadowBlur: Float = 6f,
        val glow: Boolean = false,
        val glowColor: String? = null,
        val skewX: Float = 0f,
        val bend: Float = 0f,
        /** Width-only / height-only stretch (1 = none), on top of the uniform size. */
        val stretchX: Float = 1f,
        val stretchY: Float = 1f,
    ) : TemplateLayer

    /**
     * A photo layer — double-tap (or "Replace photo") swaps it for the user's own. [size] is
     * its reference-canvas box side, the LONG edge — older templates (or a layer whose photo's
     * own aspect ratio isn't known) draw into a plain [size]×[size] square. [boxWidth]/[boxHeight]
     * are the real box the admin's web editor resized it to (aspect-correct, independently
     * resizable via that editor's corner handles); when both are present they win over [size]
     * for the actual on-canvas box, same as the app's own editor's `ImageItem.widthDp/heightDp`.
     * Null on any template saved before that resize feature existed.
     */
    data class Image(
        override val xr: Float,
        override val yr: Float,
        override val scale: Float,
        override val rotation: Float,
        override val alpha: Float,
        val url: String,
        val size: Float = 220f,
        val boxWidth: Float? = null,
        val boxHeight: Float? = null,
        // Sticker-style outline around the image's own cut-out, set in the admin editor.
        // In reference-canvas px, same as [size] — 0 = no stroke.
        val strokeWidth: Float = 0f,
        val strokeColor: String? = null,
    ) : TemplateLayer

    /** A solid decorative shape drawn on-device (no image fetch needed). */
    data class Shape(
        override val xr: Float,
        override val yr: Float,
        override val scale: Float,
        override val rotation: Float,
        override val alpha: Float,
        val kind: String,
        val color: String? = null,
        val size: Float = 200f,
    ) : TemplateLayer
}
