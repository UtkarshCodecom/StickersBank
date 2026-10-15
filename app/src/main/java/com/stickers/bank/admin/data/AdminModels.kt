package com.stickers.bank.admin.data

import com.google.firebase.firestore.DocumentId

/**
 * Flat Firestore collections so the catalog scales and tags are queryable
 * (stickers.tags via array-contains). Hierarchy: category -> group -> collection -> sticker.
 */
data class AdminCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val order: Int = 0,
    val createdAt: Long = 0,
    val description: String = "",
    // Legacy single-image fields — still read as a fallback when the *Images lists are empty,
    // so categories saved before multi-image support keep showing their one image.
    val bannerImage: String = "",
    val squareImage: String = "",
    val bannerImages: List<String> = emptyList(),
    val squareImages: List<String> = emptyList(),
    // The screen this category opens into — not the card above. Both optional.
    val bgColor: String = "",
    val bgImage: String = "",
)

data class AdminGroup(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val name: String = "",
    val order: Int = 0,
    val createdAt: Long = 0,
    val description: String = "",
    val bannerImage: String = "",
    val squareImage: String = "",
    val bannerImages: List<String> = emptyList(),
    val squareImages: List<String> = emptyList(),
    val bgColor: String = "",
    val bgImage: String = "",
)

data class AdminCollection(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val groupId: String = "",
    val name: String = "",
    val cover: String = "",
    val animated: Boolean = false,
    val order: Int = 0,
    val createdAt: Long = 0,
)

data class AdminSticker(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val groupId: String = "",
    val collectionId: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val caption: String = "",
    val animated: Boolean = false,
    val createdAt: Long = 0,
    /** Admin-arranged place within its collection/group/category (web admin → Arrange order);
     * -1 = never arranged, so it follows the arranged ones in upload order. */
    val position: Long = -1,
)

/**
 * A named row on the app's Templates tab, with its own locked-in canvas shape — every template
 * filed under this name opens onto the same width/height ratio. Mirrors the web admin's
 * `canvasTemplateCategories` collection (see admin-web/src/db.js) so either admin surface can
 * manage the same data.
 */
data class AdminTemplateCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val ratio: Float = 0f,
    // The exact name of the shape picked for this row (e.g. "YouTube Thumbnail"), stored as-is
    // instead of re-derived from [ratio] later — several named presets share the same ratio
    // (a YouTube Thumbnail and a plain 16:9 Landscape are both 1.78), so guessing backwards from
    // the number alone can't reliably tell them apart. Blank on a row created before this field
    // existed; callers fall back to the old ratio-guessing (see AdminScreens.ratioLabel) then.
    val shapeLabel: String = "",
    val order: Int = 0,
    val createdAt: Long = 0,
)

/** Listing-only view of a `canvasTemplates` doc — enough to show/delete a card; the full
 * layer `config` is written at save time but never round-tripped back through this class. */
data class AdminCanvasTemplate(
    @DocumentId val id: String = "",
    val title: String = "",
    val image: String = "",
    val category: String = "",
    val ratio: Float = 0f,
    val order: Int = 0,
    val createdAt: Long = 0,
)
