package com.stickers.bank.data.model

import kotlinx.serialization.Serializable

/**
 * A single sticker. [image] is either an asset-relative path (custom == false),
 * e.g. "catalog/deepika/s0.webp", or an absolute file path for user-created
 * stickers (custom == true).
 */
@Serializable
data class Sticker(
    val id: String,
    val image: String,
    val caption: String = "",
    val tags: List<String> = emptyList(),
    val animated: Boolean = false,
    val custom: Boolean = false,
    /** Upload time (epoch millis) — decides what shows in Fresh Drops. */
    val createdAt: Long = 0,
) {
    /** Model Coil can load: remote URL (R2), plain file path (custom), or android_asset. */
    fun coilModel(): String = when {
        image.startsWith("http") -> image
        custom -> image
        else -> "file:///android_asset/$image"
    }

    /**
     * A legacy animated clip stored as .mp4 — shared to WhatsApp as a GIF, played in a
     * VideoView. New animated stickers are animated WebP (see [isAnimatedSticker]).
     */
    val isVideoClip: Boolean get() = animated && image.endsWith(".mp4", ignoreCase = true)

    /**
     * An animated *sticker* — an animated WebP that WhatsApp installs as part of an
     * animated sticker pack and plays inline, exactly like a static sticker but moving.
     */
    val isAnimatedSticker: Boolean get() = animated && !image.endsWith(".mp4", ignoreCase = true)

    /**
     * Tags are always single words (admin input splits on space — see splitTags in the web
     * admin), so a query typed as "funny cute" would never literally appear as one substring
     * anywhere; treat it as two separate terms instead, every one of which has to match
     * somewhere (caption, id, or any one tag) for a hit.
     */
    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.all { w ->
            caption.lowercase().contains(w) || id.lowercase().contains(w) || tags.any { it.lowercase().contains(w) }
        }
    }
}

@Serializable
data class Collection(
    val id: String,
    val name: String,
    val cover: String? = null,
    val animated: Boolean = false,
    val stickers: List<Sticker> = emptyList(),
)

/** A second-level tab under a category, e.g. "Actress" or "Comedian". */
@Serializable
data class Group(
    val id: String,
    val name: String,
    val collections: List<Collection> = emptyList(),
    val stickers: List<Sticker> = emptyList(),
    val description: String = "",
    val bannerImages: List<String> = emptyList(),
    val squareImages: List<String> = emptyList(),
    val bgColor: String? = null,
    val bgImage: String? = null,
) {
    val isLeaf: Boolean get() = collections.isEmpty()
}

@Serializable
data class Category(
    val id: String,
    val name: String,
    val groups: List<Group> = emptyList(),
    val stickers: List<Sticker> = emptyList(),
    val description: String = "",
    /**
     * Admin-set images: [bannerImages] are 5:1 wide banners (list view), [squareImages] are
     * 1:1 (grid view). More than one and the card auto-crossfades between them every second.
     */
    val bannerImages: List<String> = emptyList(),
    val squareImages: List<String> = emptyList(),
    /** Custom background (solid color and/or image) for the screen this category opens into —
     * distinct from [bannerImages]/[squareImages], which are the card's own thumbnail. */
    val bgColor: String? = null,
    val bgImage: String? = null,
) {
    /** A category is a leaf when it has no sub-groups (stickers shown directly). */
    val isLeaf: Boolean get() = groups.isEmpty()

    /** Total stickers under this category (across groups + collections). */
    val stickerCount: Int
        get() = stickers.size + groups.sumOf { g -> g.stickers.size + g.collections.sumOf { it.stickers.size } }
}

/** A promo banner (5:1) shown at the top of Featured. [video] means [image] is a video URL, played back instead of shown as a still. */
data class Banner(
    val id: String,
    val image: String,
    val video: Boolean = false,
    /** Admin-set URL a tap opens in the browser. Blank = the banner isn't tappable. */
    val link: String = "",
)

/** An admin-uploaded .ttf/.otf, downloaded and cached on-device for the editor's font picker. */
data class CustomFont(
    val id: String,
    val name: String,
    val url: String,
)

/**
 * A standalone Trending card shown on Featured — NOT tied to a real category/collection. The
 * admin gives it its own title + portrait (3:5) image and hand-picks which stickers it opens.
 */
/**
 * An admin-curated pack (Featured → Packs): a name and a hand-picked set of stickers, kept
 * all-still or all-animated so it goes to WhatsApp as one pack of its kind.
 */
data class Pack(
    val id: String,
    val name: String,
    val animated: Boolean = false,
    val stickerIds: List<String> = emptyList(),
)

data class TrendingCard(
    val id: String,
    val title: String,
    val image: String? = null,
    val badge: String = "none",
    val stickerIds: List<String> = emptyList(),
    // Solid color (hex, e.g. "#7C3AED") behind the title/count panel — picked in admin.
    val bgColor: String? = null,
    /** Backdrop for the PAGE this card opens into — set per card in the Trending admin, so
     * each trending page can have its own artwork. Distinct from [bgColor], which only tints
     * the card itself. */
    val pageBgColor: String? = null,
    val pageBgImage: String? = null,
)

/** A hashtag pill shown on Featured. [color] is admin-picked (hex); null falls back to the app's own deterministic theme. */
data class PopularTag(
    val tag: String,
    val color: String? = null,
)

/** An admin-curated transparent overlay (glasses, a cap, etc.) — pure decorative image, picked
 * straight into the editor's canvas as a new layer. [categoryId] is blank when the admin hasn't
 * filed it under a [DecorationCategory]; it still shows under "All". */
data class Decoration(
    val id: String,
    val image: String,
    val categoryId: String = "",
)

/** An admin-managed group of decorations (Glasses, Hats…) — the toggles along the bottom of the
 * editor's Decorator, in the admin's order. */
data class DecorationCategory(
    val id: String,
    val name: String,
)

/** A whole tab's custom backdrop (solid color and/or image), admin-set per page. Either may be
 * blank, in which case the app's plain flat background shows through for that part. */
data class PageBackground(
    val color: String? = null,
    val image: String? = null,
) {
    val isEmpty: Boolean get() = color.isNullOrBlank() && image.isNullOrBlank()
}

/**
 * A ready-made canvas the user opens straight in the editor — they only swap the picture and
 * the words.
 *
 * [image], [title], [textColor] and [bgColor] are purely the **thumbnail**: how the card looks
 * in the Templates tab. What the editor actually opens is [config] — the canvas the admin
 * built and saved in the web editor.
 */
data class CanvasTemplate(
    val id: String,
    val title: String,
    val image: String? = null,
    val textColor: String? = null,
    val bgColor: String? = null,
    val config: TemplateConfig? = null,
    /** Groups templates into named rows on the Templates tab. Blank = the catch-all row. */
    val category: String = "",
    /** Free-text tags, so a template can be found from the Featured search bar. */
    val tags: List<String> = emptyList(),
    /**
     * Canvas shape as width/height, e.g. 1.0 square, 0.8 for 4:5. Set per CATEGORY in the
     * admin, so every template in a row opens onto the same canvas. 0 = fall back to square.
     */
    val ratio: Float = 0f,
) {
    fun matchesQuery(q: String): Boolean {
        if (q.isBlank()) return true
        val words = q.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.all { w ->
            title.lowercase().contains(w) || category.lowercase().contains(w) || tags.any { it.lowercase().contains(w) }
        }
    }
}

@Serializable
data class Catalog(
    val categories: List<Category> = emptyList(),
) {
    /** Every sticker in the catalog, flattened. */
    fun allStickers(): List<Sticker> {
        val out = ArrayList<Sticker>()
        for (c in categories) {
            out += c.stickers
            for (g in c.groups) {
                out += g.stickers
                for (col in g.collections) out += col.stickers
            }
        }
        return out
    }

    fun findCategory(id: String): Category? = categories.firstOrNull { it.id == id }

    /**
     * Sticker id → the name of the place it lives directly in: its collection (Hindi → Actor →
     * "xyz" gives "xyz"), or its group / category for stickers placed straight on those.
     */
    fun stickerParentNames(): Map<String, String> {
        val out = HashMap<String, String>()
        for (c in categories) {
            c.stickers.forEach { out[it.id] = c.name }
            for (g in c.groups) {
                g.stickers.forEach { out[it.id] = g.name }
                for (col in g.collections) col.stickers.forEach { out[it.id] = col.name }
            }
        }
        return out
    }

    /** Resolve a [TrendingCard]'s hand-picked sticker ids to the actual stickers, in that order. */
    fun stickersForIds(ids: List<String>): List<Sticker> {
        val byId = allStickers().distinctBy { it.id }.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    /** Stickers carrying [tag] (case-insensitive), across the whole catalog. */
    fun stickersForTag(tag: String): List<Sticker> {
        val t = tag.trim().lowercase()
        return allStickers().distinctBy { it.id }.filter { s -> s.tags.any { it.lowercase() == t } }
    }
}
