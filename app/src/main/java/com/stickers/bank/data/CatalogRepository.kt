package com.stickers.bank.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stickers.bank.admin.data.AdminCategory
import com.stickers.bank.admin.data.AdminCollection
import com.stickers.bank.admin.data.AdminGroup
import com.stickers.bank.admin.data.AdminSticker
import com.stickers.bank.data.model.Banner
import com.stickers.bank.data.model.CanvasTemplate
import com.stickers.bank.data.model.Catalog
import com.stickers.bank.data.model.Category
import com.stickers.bank.data.model.Collection
import com.stickers.bank.data.model.CustomFont
import com.stickers.bank.data.model.Decoration
import com.stickers.bank.data.model.DecorationCategory
import com.stickers.bank.data.model.Group
import com.stickers.bank.data.model.PageBackground
import com.stickers.bank.data.model.PopularTag
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.data.model.TemplateConfig
import com.stickers.bank.data.model.TemplateLayer
import com.stickers.bank.data.model.TrendingCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

/**
 * Loads the live catalog from Firestore (categories → groups → collections → stickers)
 * and exposes it as the UI [Catalog] tree. Sticker images are R2 public URLs.
 */
/** Matches the web admin's `PAGE_BACKGROUND_KEYS` in db.js exactly — keep both in sync. */
val PAGE_BACKGROUND_KEYS = listOf("featured", "templates", "newArrival", "bank", "favorites", "trendingAll", "trending")

class CatalogRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _catalog = MutableStateFlow(Catalog())
    val catalog: StateFlow<Catalog> = _catalog.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Non-null when the last Firestore load failed (e.g. permission denied). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Emojis for the editor's emoji picker, managed from the web admin ("emojis" collection). */
    private val _emojis = MutableStateFlow<List<String>>(emptyList())
    val emojis: StateFlow<List<String>> = _emojis.asStateFlow()

    /** Promo banners (5:1) shown on Featured, managed from the web admin ("banners"). */
    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    /** How the banner rotation animates: "carousel" (dots, slide) or "fade" (no dots, crossfade). */
    private val _bannerDisplayMode = MutableStateFlow("carousel")
    val bannerDisplayMode: StateFlow<String> = _bannerDisplayMode.asStateFlow()

    /** Admin-uploaded custom fonts, shown in the editor's font picker alongside the built-ins. */
    private val _customFonts = MutableStateFlow<List<CustomFont>>(emptyList())
    val customFonts: StateFlow<List<CustomFont>> = _customFonts.asStateFlow()

    /** Admin-set font-picker order (appSettings/fonts.order), first to last. Keys: "default", a
     * built-in's asset path, or "custom:<docId>"; fonts not listed follow in their natural order. */
    private val _fontOrder = MutableStateFlow<List<String>>(emptyList())
    val fontOrder: StateFlow<List<String>> = _fontOrder.asStateFlow()

    /** Built-in font asset paths the admin has hidden from the picker (still bundled, just excluded). */
    private val _hiddenFontPaths = MutableStateFlow<Set<String>>(emptySet())
    val hiddenFontPaths: StateFlow<Set<String>> = _hiddenFontPaths.asStateFlow()

    /** Standalone Trending cards the admin built (own title/image/sticker picks), in order. */
    private val _trending = MutableStateFlow<List<TrendingCard>>(emptyList())
    val trending: StateFlow<List<TrendingCard>> = _trending.asStateFlow()

    /** Admin-curated tags shown as pills on Featured ("popularTags" collection). */
    private val _popularTags = MutableStateFlow<List<PopularTag>>(emptyList())
    val popularTags: StateFlow<List<PopularTag>> = _popularTags.asStateFlow()

    /** Admin-curated transparent decoration overlays for the editor's Decorator. */
    private val _decorations = MutableStateFlow<List<Decoration>>(emptyList())
    val decorations: StateFlow<List<Decoration>> = _decorations.asStateFlow()

    /** The Decorator's category toggles, in admin order ("decorationCategories" collection). */
    private val _decorationCategories = MutableStateFlow<List<DecorationCategory>>(emptyList())
    val decorationCategories: StateFlow<List<DecorationCategory>> = _decorationCategories.asStateFlow()

    /**
     * Per-page custom backdrops (solid color and/or image) keyed by page id — "featured",
     * "templates", "newArrival", "bank", "favorites" — read from the single
     * `appSettings/pageBackgrounds` doc. A page missing from the map (or an empty
     * [PageBackground]) just shows the app's plain flat background.
     */
    private val _pageBackgrounds = MutableStateFlow<Map<String, PageBackground>>(emptyMap())
    val pageBackgrounds: StateFlow<Map<String, PageBackground>> = _pageBackgrounds.asStateFlow()

    /** Ready-made canvases for the Templates tab ("canvasTemplates" collection). */
    private val _canvasTemplates = MutableStateFlow<List<CanvasTemplate>>(emptyList())
    val canvasTemplates: StateFlow<List<CanvasTemplate>> = _canvasTemplates.asStateFlow()

    /**
     * Every sticker doc in Firestore, independent of whether it's actually reachable through a
     * valid category → group → collection chain right now. [Catalog.allStickers] only walks that
     * tree, so a sticker whose parent was later deleted/restructured silently vanishes from it —
     * but its id can still be sitting in a hand-picked list like a Trending card's stickerIds.
     * Resolving against this flat list instead is what keeps Trending's "N stickers" count
     * matching what actually renders.
     */
    private val _allStickersFlat = MutableStateFlow<List<Sticker>>(emptyList())
    val allStickersFlat: StateFlow<List<Sticker>> = _allStickersFlat.asStateFlow()

    /** Snapshot of all catalog stickers (current value). */
    val catalogStickers: List<Sticker>
        get() = _catalog.value.allStickers().distinctBy { it.id }

    /** Resolve hand-picked sticker ids (e.g. a Trending card) against the flat sticker set. */
    fun stickersForIds(ids: List<String>): List<Sticker> {
        val byId = _allStickersFlat.value.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _loading.value = true
            runCatching { buildFromFirestore() }
                .onSuccess { _catalog.value = it; _error.value = null }
                .onFailure { _error.value = it.message ?: it.toString() }
            _loading.value = false
        }
        // Emojis load independently so a failure here never blocks the catalog.
        scope.launch {
            runCatching {
                db.collection("emojis").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { it.getString("value")?.takeIf { v -> v.isNotBlank() } }
            }.onSuccess { if (it.isNotEmpty()) _emojis.value = it }
        }
        // Promo banners (independent too). Set even when empty so admin deletions show up.
        scope.launch {
            runCatching {
                db.collection("banners").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { d ->
                        val image = d.getString("image")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        Banner(
                            id = d.id,
                            image = image,
                            video = d.getBoolean("video") ?: false,
                            link = d.getString("link").orEmpty(),
                        )
                    }
            }.onSuccess { _banners.value = it }
        }
        // The banner rotation's display mode — a single app-wide setting.
        scope.launch {
            runCatching {
                db.collection("appSettings").document("banners").get().await().getString("displayMode")
            }.onSuccess { _bannerDisplayMode.value = it?.takeIf { m -> m.isNotBlank() } ?: "carousel" }
        }
        // Per-page custom backgrounds — one flat doc, "<page>Color"/"<page>Image" fields.
        scope.launch {
            runCatching {
                val data = db.collection("appSettings").document("pageBackgrounds").get().await().data.orEmpty()
                PAGE_BACKGROUND_KEYS.associateWith { key ->
                    PageBackground(
                        color = (data["${key}Color"] as? String)?.takeIf { it.isNotBlank() },
                        image = (data["${key}Image"] as? String)?.takeIf { it.isNotBlank() },
                    )
                }
            }.onSuccess { _pageBackgrounds.value = it }
        }
        // Admin-uploaded custom fonts for the editor's font picker.
        scope.launch {
            runCatching {
                db.collection("fonts").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { d ->
                        val name = d.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val url = d.getString("url")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        CustomFont(id = d.id, name = name, url = url)
                    }
            }.onSuccess { _customFonts.value = it }
        }
        scope.launch {
            runCatching {
                (db.collection("appSettings").document("fonts").get().await().get("order") as? List<*>)
                    .orEmpty().filterIsInstance<String>()
            }.onSuccess { _fontOrder.value = it }
        }
        // Built-in fonts the admin has hidden from the picker.
        scope.launch {
            runCatching {
                db.collection("hiddenFonts").get().await().documents
                    .mapNotNull { it.getString("path")?.takeIf { p -> p.isNotBlank() } }
                    .toSet()
            }.onSuccess { _hiddenFontPaths.value = it }
        }
        // Trending cards (standalone — own title/image/sticker picks, not tied to a collection).
        scope.launch {
            runCatching {
                db.collection("trending").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { d ->
                        val title = d.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        @Suppress("UNCHECKED_CAST")
                        val ids = (d.get("stickerIds") as? List<Any?>)?.filterIsInstance<String>() ?: emptyList()
                        TrendingCard(
                            id = d.id,
                            title = title,
                            image = d.getString("image")?.takeIf { it.isNotBlank() },
                            badge = d.getString("badge") ?: "none",
                            stickerIds = ids,
                            bgColor = d.getString("bgColor")?.takeIf { it.isNotBlank() },
                            pageBgColor = d.getString("pageBgColor")?.takeIf { it.isNotBlank() },
                            pageBgImage = d.getString("pageBgImage")?.takeIf { it.isNotBlank() },
                        )
                    }
            }.onSuccess { _trending.value = it }
        }
        // Popular tags (admin-curated).
        scope.launch {
            runCatching {
                db.collection("popularTags").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { d ->
                        val tag = d.getString("tag")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        PopularTag(tag = tag, color = d.getString("color")?.takeIf { it.isNotBlank() })
                    }
            }.onSuccess { _popularTags.value = it }
        }
        // Admin-curated decoration overlays (glasses, caps, etc.) for the editor.
        scope.launch {
            runCatching {
                db.collection("decorations").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { d ->
                        val image = d.getString("image")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        Decoration(id = d.id, image = image, categoryId = d.getString("categoryId").orEmpty())
                    }
            }.onSuccess { _decorations.value = it }
        }
        scope.launch {
            runCatching {
                db.collection("decorationCategories").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { d ->
                        val name = d.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        DecorationCategory(id = d.id, name = name)
                    }
            }.onSuccess { _decorationCategories.value = it }
        }
        // Canvas templates (admin-built starting canvases for the Templates tab).
        scope.launch {
            runCatching {
                db.collection("canvasTemplates").get().await().documents
                    .sortedBy { it.getLong("order") ?: 0L }
                    .mapNotNull { d ->
                        val title = d.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        CanvasTemplate(
                            id = d.id,
                            title = title,
                            image = d.getString("image")?.takeIf { it.isNotBlank() },
                            textColor = d.getString("textColor")?.takeIf { it.isNotBlank() },
                            bgColor = d.getString("bgColor")?.takeIf { it.isNotBlank() },
                            config = parseTemplateConfig(d.get("config")),
                            category = d.getString("category").orEmpty().trim(),
                            tags = (d.get("tags") as? List<*>)?.mapNotNull { t ->
                                (t as? String)?.trim()?.takeIf { it.isNotBlank() }
                            }.orEmpty(),
                            ratio = (d.getDouble("ratio") ?: 0.0).toFloat(),
                        )
                    }
            }.onSuccess { _canvasTemplates.value = it }
        }
    }

    private suspend fun buildFromFirestore(): Catalog {
        val cats = db.collection("categories").orderBy("order", Query.Direction.ASCENDING)
            .get().await().toObjects(AdminCategory::class.java)
        val groups = db.collection("groups").get().await().toObjects(AdminGroup::class.java)
        val colls = db.collection("collections").get().await().toObjects(AdminCollection::class.java)
        val stks = db.collection("stickers").get().await().toObjects(AdminSticker::class.java)
        _allStickersFlat.value = stks.map { it.toSticker() }

        val stByColl = stks.filter { it.collectionId.isNotBlank() }.groupBy { it.collectionId }
        // Stickers placed directly on a Group or Category (admin skipped Name and/or Type
        // entirely) — collectionId blank marks "direct", groupId blank further narrows it to
        // the category itself rather than a group.
        val stDirectByGroup = stks.filter { it.collectionId.isBlank() && it.groupId.isNotBlank() }.groupBy { it.groupId }
        val stDirectByCategory = stks.filter { it.collectionId.isBlank() && it.groupId.isBlank() }.groupBy { it.categoryId }
        val collsByGroup = colls.groupBy { it.groupId }
        val groupsByCat = groups.groupBy { it.categoryId }

        val categories = cats.map { c ->
            val gs = (groupsByCat[c.id] ?: emptyList()).sortedBy { it.order }.map { g ->
                val cs = (collsByGroup[g.id] ?: emptyList()).sortedBy { it.order }.map { col ->
                    val st = (stByColl[col.id] ?: emptyList()).sortedWith(stickerOrder).map { it.toSticker() }
                    Collection(
                        id = col.id,
                        name = col.name,
                        cover = col.cover.ifBlank { null }, // R2 URL set from the web admin
                        animated = col.animated,
                        stickers = st,
                    )
                }
                val directSt = (stDirectByGroup[g.id] ?: emptyList()).sortedWith(stickerOrder).map { it.toSticker() }
                Group(
                    id = g.id, name = g.name, collections = cs, stickers = directSt,
                    description = g.description,
                    bannerImages = g.bannerImages.filter { it.isNotBlank() }
                        .ifEmpty { listOfNotNull(g.bannerImage.ifBlank { null }) },
                    squareImages = g.squareImages.filter { it.isNotBlank() }
                        .ifEmpty { listOfNotNull(g.squareImage.ifBlank { null }) },
                    bgColor = g.bgColor.ifBlank { null },
                    bgImage = g.bgImage.ifBlank { null },
                )
            }
            val directCatSt = (stDirectByCategory[c.id] ?: emptyList()).sortedWith(stickerOrder).map { it.toSticker() }
            Category(
                id = c.id, name = c.name, groups = gs, stickers = directCatSt,
                description = c.description,
                bannerImages = c.bannerImages.filter { it.isNotBlank() }
                    .ifEmpty { listOfNotNull(c.bannerImage.ifBlank { null }) },
                squareImages = c.squareImages.filter { it.isNotBlank() }
                    .ifEmpty { listOfNotNull(c.squareImage.ifBlank { null }) },
                bgColor = c.bgColor.ifBlank { null },
                bgImage = c.bgImage.ifBlank { null },
            )
        }
        return Catalog(categories)
    }

    /**
     * Firestore hands the saved canvas back as plain maps/lists. Everything is read
     * defensively — a template with a malformed layer should lose that layer, not the
     * whole template.
     */
    private fun parseTemplateConfig(raw: Any?): TemplateConfig? {
        val map = raw as? Map<*, *> ?: return null
        val layers = (map["layers"] as? List<*>).orEmpty().mapNotNull { parseTemplateLayer(it) }
        val base = map["base"] as? Map<*, *>
        return TemplateConfig(
            bgColor = (map["bgColor"] as? String)?.takeIf { it.isNotBlank() },
            baseImage = (map["baseImage"] as? String)?.takeIf { it.isNotBlank() },
            baseOffsetX = (base?.get("x") as? Number)?.toFloat() ?: 0f,
            baseOffsetY = (base?.get("y") as? Number)?.toFloat() ?: 0f,
            baseScale = (base?.get("scale") as? Number)?.toFloat() ?: 1f,
            baseScaleY = (base?.get("scaleY") as? Number)?.toFloat()
                ?: (base?.get("scale") as? Number)?.toFloat() ?: 1f,
            baseRotation = (base?.get("rotation") as? Number)?.toFloat() ?: 0f,
            baseFlipX = (base?.get("flipX") as? Boolean) ?: false,
            baseAlpha = (base?.get("alpha") as? Number)?.toFloat() ?: 1f,
            layers = layers,
        )
    }

    private fun parseTemplateLayer(raw: Any?): TemplateLayer? {
        val m = raw as? Map<*, *> ?: return null
        fun num(key: String, fallback: Float) = (m[key] as? Number)?.toFloat() ?: fallback
        fun str(key: String) = (m[key] as? String)?.takeIf { it.isNotBlank() }
        fun bool(key: String, fallback: Boolean) = (m[key] as? Boolean) ?: fallback

        val xr = num("xr", 0.5f)
        val yr = num("yr", 0.5f)
        val scale = num("scale", 1f)
        val rotation = num("rotation", 0f)
        val alpha = num("alpha", 1f)

        return when (m["type"] as? String) {
            // Emoji are just text layers carrying a glyph.
            "text", "emoji" -> {
                val text = (m["text"] as? String).orEmpty()
                if (text.isBlank()) null else TemplateLayer.Text(
                    xr, yr, scale, rotation, alpha,
                    text = text,
                    color = str("color"),
                    size = num("size", 56f),
                    bold = bool("bold", true),
                    italic = bool("italic", false),
                    align = (m["align"] as? Number)?.toInt() ?: 1,
                    outline = bool("outline", false),
                    outlineColor = str("outlineColor"),
                    outlineWidth = num("outlineWidth", 8f),
                    letterSpacing = num("letterSpacing", 0f),
                    lineHeight = num("lineHeight", 1.1f),
                    highlight = str("highlight"),
                    gradient = bool("gradient", false),
                    gradientColor2 = str("gradientColor2"),
                    shadow = bool("shadow", false),
                    shadowColor = str("shadowColor"),
                    shadowBlur = num("shadowBlur", 6f),
                    glow = bool("glow", false),
                    glowColor = str("glowColor"),
                    skewX = num("skewX", 0f),
                    bend = num("bend", 0f),
                    stretchX = num("stretchX", 1f),
                    stretchY = num("stretchY", 1f),
                )
            }
            "image" -> str("url")?.let {
                TemplateLayer.Image(
                    xr, yr, scale, rotation, alpha, it,
                    size = num("baseSize", 220f),
                    boxWidth = (m["baseW"] as? Number)?.toFloat(),
                    boxHeight = (m["baseH"] as? Number)?.toFloat(),
                    strokeWidth = num("strokeW", 0f),
                    strokeColor = str("strokeColor"),
                )
            }
            "shape" -> TemplateLayer.Shape(
                xr, yr, scale, rotation, alpha,
                kind = (m["shapeKind"] as? String).orEmpty().ifBlank { "circle" },
                color = str("shapeColor"),
                size = num("baseSize", 200f),
            )
            else -> null
        }
    }

    /** Admin-arranged stickers (`position` >= 0) first, in that order; the rest follow by upload
     * time. Mirrors byStickerOrder in the web admin's db.js. */
    private val stickerOrder = compareBy<AdminSticker>(
        { if (it.position >= 0) 0 else 1 },
        { if (it.position >= 0) it.position else 0L },
        { it.createdAt },
    )

    private fun AdminSticker.toSticker() = Sticker(
        id = id,
        image = imageUrl,
        caption = caption,
        tags = tags,
        animated = animated,
        custom = false,
        createdAt = createdAt,
    )
}

/** Shared, lenient JSON used across repositories. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}
