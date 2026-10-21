package com.stickers.bank.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.CanvasTemplate
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.HScrollHint
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurfaceElevated
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.ui.theme.parseHexColor

/** Card shape when a template (or its category) hasn't set one. Matches the Trending tile.
 * `internal` (not `private`) so the editor screen can fall back to the exact same shape a
 * "Default" (ratio 0) template already shows as a card, instead of its own square default. */
internal const val DEFAULT_TEMPLATE_RATIO = 3.2f / 5f

/** A template's own ratio if the admin set one, else the shared default. */
private fun CanvasTemplate.effectiveRatio() = ratio.takeIf { it > 0f } ?: DEFAULT_TEMPLATE_RATIO

/**
 * Every template in a category shares one shape, so a row lines up. The ratio is stored on the
 * templates themselves (the admin sets it per category and it's written to each one), so take
 * the first non-zero value in the row.
 */
fun rowRatio(list: List<CanvasTemplate>): Float =
    list.firstOrNull { it.ratio > 0f }?.ratio ?: DEFAULT_TEMPLATE_RATIO

/**
 * The shape a browse-grid THUMBNAIL displays at — not necessarily [rowRatio]'s real value.
 * Categories span everything from Portrait 1:2 (0.5) to Wide 21:9 (2.33; see the admin's own
 * CANVAS_RATIOS), and a fixed-width card taken straight to that ratio comes out either a sliver
 * (extreme landscape) or a skyscraper (extreme portrait) next to a normal row's cards — the
 * inconsistency is what actually reads as "absurd," not any one row in isolation. Clamping only
 * the DISPLAYED thumbnail keeps every row's cards in the same rough size class; the real canvas
 * a template opens into still uses its true, unclamped ratio (CanvasTemplateEditorScreen reads
 * that straight from the template, never through this).
 */
private fun cardDisplayRatio(ratio: Float): Float = ratio.coerceIn(0.55f, 1.8f)

/** Group templates into rows, preserving admin order; uncategorised ones fall into "More". */
fun templateRows(templates: List<CanvasTemplate>): List<Pair<String, List<CanvasTemplate>>> {
    val byCategory = LinkedHashMap<String, MutableList<CanvasTemplate>>()
    for (t in templates) byCategory.getOrPut(t.category.trim()) { mutableListOf() }.add(t)
    val uncategorised = byCategory.remove("")
    return buildList {
        byCategory.forEach { (name, list) -> add(name to list.toList()) }
        uncategorised?.let { add("More" to it.toList()) }
    }
}

/**
 * TEMPLATES = admin-built starting canvases. Tapping one opens the dedicated template editor
 * (CanvasTemplateEditorScreen) with that canvas already set up.
 *
 * Pure browse, on purpose: no search box, no favorites here — searching for a template happens
 * from the Featured tab's own search bar (which surfaces matching templates alongside
 * stickers), so this screen stays a straightforward category-chip browsing gallery.
 */
@Composable
fun CanvasTemplatesScreen(nav: NavHostController) {
    val showAd = com.stickers.bank.ads.rememberInterstitial()
    val templates by ServiceLocator.catalogRepository.canvasTemplates.collectAsStateSafe()
    val loading by ServiceLocator.catalogRepository.loading.collectAsStateSafe()
    val rows = remember(templates) { templateRows(templates) }

    var chip by remember { mutableStateOf<String?>(null) } // null = "All"

    Column(Modifier.fillMaxSize()) {
        Text(
            "CANVAS TEMPLATES", color = Gold, fontSize = 11.sp, letterSpacing = 2.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
        )
        // Start from nothing instead of a template: a quick shape choice, then an empty canvas.
        var showNewCanvas by remember { mutableStateOf(false) }
        var showAllSizes by remember { mutableStateOf(false) }
        CreateCanvasButton(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) { showNewCanvas = true }
        fun openBlank(ratio: Float) {
            // Own canvas → ad after the shape is chosen. Admin templates open ad-free.
            showAd { nav.navigate(Routes.templateEditor(blankCanvasId(ratio))) }
        }
        if (showNewCanvas) {
            CanvasRatioPopup(
                onDismiss = { showNewCanvas = false },
                onPick = { ratio -> showNewCanvas = false; openBlank(ratio) },
                onMoreSizes = { showNewCanvas = false; showAllSizes = true },
            )
        }
        if (showAllSizes) {
            NewCanvasPicker(
                onDismiss = { showAllSizes = false },
                onPick = { ratio, _ -> showAllSizes = false; openBlank(ratio) },
            )
        }
        when {
            templates.isEmpty() && loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Gold) }

            templates.isEmpty() ->
                // Centred in what's visible above the floating nav, not in the full height that
                // runs down behind it (which sat the text noticeably low).
                EmptyState(
                    "NOT YET… BUT SOON 👀", "Fresh templates are cooking.\nCome back and make something awesome. 🔥",
                    Modifier.padding(bottom = com.stickers.bank.ui.components.SbBottomNavClearance),
                )

            chip == null -> BrowsingRows(rows, nav)

            else -> {
                val list = rows.firstOrNull { it.first == chip }?.second.orEmpty()
                val ratio = cardDisplayRatio(rowRatio(list))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    gridItems(list, key = { it.id }) { t ->
                        TemplateCard(t, ratio, Modifier.fillMaxWidth()) {
                            nav.navigate(Routes.templateEditor(t.id))
                        }
                    }
                }
            }
        }
    }
}

/** The gold "Create your canvas" button at the top of Canvas — the Create tab's hero, in a
 * slimmer row so the templates below stay in view. */
@Composable
private fun CreateCanvasButton(modifier: Modifier, onClick: () -> Unit) {
    val display = com.stickers.bank.ui.theme.rememberDisplayFont()
    val shape = remember { com.stickers.bank.ui.components.BucketShape(topInset = 14.dp, corner = 22.dp) }
    Row(
        modifier.fillMaxWidth().clip(shape).background(Gold).clickable(onClick = onClick)
            .padding(start = 16.dp, end = 14.dp, top = 22.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(AppBackground),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = Gold, modifier = Modifier.size(28.dp)) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text("CREATE YOUR CANVAS", color = OnGold, fontFamily = display, fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp)
            Text("Pick a shape, start from a blank canvas.", color = OnGold.copy(alpha = 0.72f), fontSize = 12.sp)
        }
        Box(
            Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AppBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Gold, modifier = Modifier.size(18.dp))
        }
    }
}

/** One quick canvas shape: its name, its ratio label, a hint of what it suits, and width/height. */
private data class QuickRatio(val name: String, val label: String, val hint: String, val ratio: Float)

private val QUICK_RATIOS = listOf(
    QuickRatio("Square", "1:1", "Posts, profile pictures", 1f),
    QuickRatio("Portrait", "4:5", "Instagram portrait post", 4f / 5f),
    QuickRatio("Portrait", "3:4", "Photos, cards", 3f / 4f),
    QuickRatio("Landscape", "16:9", "Thumbnails, wide banners", 16f / 9f),
)

/**
 * "What shape?" — the four everyday canvas shapes as cards, each drawn at its own ratio so the
 * difference is visible at a glance. "More sizes" opens the full catalog ([NewCanvasPicker]).
 */
@Composable
private fun CanvasRatioPopup(onDismiss: () -> Unit, onPick: (Float) -> Unit, onMoreSizes: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().neuRaised(cornerRadius = 26.dp, elevation = 9.dp).padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("CHOOSE A SHAPE", color = Gold, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
            Text(
                "Your canvas opens blank in this shape.", color = AppOnSurfaceMuted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            QUICK_RATIOS.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { r ->
                        Column(
                            Modifier.weight(1f).neuRaised(cornerRadius = 16.dp, elevation = 6.dp)
                                .clip(RoundedCornerShape(16.dp)).clickable { onPick(r.ratio) }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // The shape itself, fitted into the same square slot on every card.
                            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier.then(if (r.ratio >= 1f) Modifier.fillMaxWidth() else Modifier.height(64.dp))
                                        .aspectRatio(r.ratio)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(2.dp, Gold, RoundedCornerShape(6.dp))
                                        .background(Gold.copy(alpha = 0.12f)),
                                )
                            }
                            Text(r.name, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
                            Text(r.label, color = Gold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                r.hint, color = AppOnSurfaceMuted, fontSize = 10.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
            Text(
                "More sizes (YouTube, print, stories…)", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).clickable(onClick = onMoreSizes)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * Full-screen shape picker for a blank canvas — every category and named preset from
 * CANVAS_SIZE_CATEGORIES, searchable by name, browsable by category chip. Only the tapped
 * preset's ratio (width/height) reaches the canvas; the size text is display-only.
 *
 * Shared with the admin app's own "new template row" flow (see AdminScreens.kt) — a template's
 * canvas shape is the exact same choice a user picking their own blank canvas makes, so this is
 * the one place that list is defined, not duplicated. [title]/[subtitle] let a caller adjust the
 * header copy for its own context without forking the rest of the picker.
 */
@Composable
fun NewCanvasPicker(
    onDismiss: () -> Unit,
    /** (ratio, preset name) — the name is the preset's own exact label ("YouTube Thumbnail"),
     * for a caller that wants to remember which one was actually picked rather than re-guessing
     * it later from the bare ratio (which several presets share). */
    onPick: (Float, String) -> Unit,
    title: String = "NEW CANVAS",
    subtitle: String = "Pick a shape — from anywhere: social, print, business, events…",
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var query by remember { mutableStateOf("") }
        // null = show every category, one after another, with its own header.
        var activeCategory by remember { mutableStateOf<String?>(null) }

        val searching = query.isNotBlank()
        val filtered = remember(query, activeCategory) {
            val q = query.trim().lowercase()
            CANVAS_SIZE_CATEGORIES.mapNotNull { cat ->
                if (!searching && activeCategory != null && activeCategory != cat.name) return@mapNotNull null
                val presets = if (searching) cat.presets.filter { it.name.lowercase().contains(q) } else cat.presets
                if (presets.isEmpty()) null else cat.copy(presets = presets)
            }
        }

        Column(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, start = 6.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).neuRaised(cornerRadius = 20.dp, elevation = 6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Rounded.Close, "Close",
                        tint = AppOnSurface, modifier = Modifier.size(18.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, color = Gold, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = AppOnSurfaceMuted, fontSize = 11.sp)
                }
            }

            Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
                var focused by remember { mutableStateOf(false) }
                androidx.compose.material3.TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search sizes… (e.g. Instagram, A4, business card)", color = AppOnSurfaceMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Rounded.Search, null, tint = AppOnSurfaceMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                androidx.compose.material.icons.Icons.Rounded.Close, "Clear", tint = AppOnSurfaceMuted,
                                modifier = Modifier.size(16.dp).clickable { query = "" },
                            )
                        }
                    },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Gold, focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .then(
                            if (focused) Modifier.neuInset(cornerRadius = 12.dp, elevation = 6.dp)
                            else Modifier.neuRaised(cornerRadius = 12.dp, elevation = 6.dp),
                        )
                        .onFocusChanged { focused = it.isFocused },
                )
            }

            // Category chips — hidden while searching, since a search already cuts across all
            // of them and a chip filter on top would just be a second, redundant narrowing.
            if (!searching) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(Modifier.width(8.dp))
                    SizeCategoryChip("All", null, activeCategory) { activeCategory = null }
                    CANVAS_SIZE_CATEGORIES.forEach { cat ->
                        SizeCategoryChip("${cat.emoji} ${cat.name}", cat.name, activeCategory) { activeCategory = cat.name }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sizes match \"$query\"", color = AppOnSurfaceMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 40.dp),
                ) {
                    filtered.forEach { cat ->
                        item(key = "hdr-${cat.name}") {
                            Text(
                                "${cat.emoji}  ${cat.name.uppercase()}",
                                color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                            )
                        }
                        items(cat.presets, key = { "${cat.name}-${it.name}" }) { preset ->
                            SizePresetRow(preset) { onPick(preset.ratio, preset.name) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeCategoryChip(label: String, value: String?, active: String?, onClick: () -> Unit) {
    val selected = active == value
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Gold else AppSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (selected) com.stickers.bank.ui.theme.OnGold else AppOnSurface,
        )
    }
}

@Composable
private fun SizePresetRow(preset: CanvasSizePreset, onClick: () -> Unit) {
    val ratio = preset.ratio
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail of the shape itself, long side pinned so every row's swatch reads at a
        // glance without the eye having to compare exact numbers.
        val w = if (ratio >= 1f) 36.dp else 36.dp * ratio
        val h = if (ratio >= 1f) 36.dp / ratio else 36.dp
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.width(w).height(h)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Gold.copy(alpha = 0.75f)),
            )
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(preset.name, color = AppOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(preset.sizeText, color = AppOnSurfaceMuted, fontSize = 11.sp)
        }
    }
}

/**
 * The default "All" view — one horizontal strip per category, Trending-style.
 *
 * Cards share a fixed HEIGHT per row, not a fixed width — a landscape row's cards derive their
 * width from that height and their own ratio, same as a portrait row's do. A fixed-width scheme
 * made every row's cards the same width regardless of shape, so a landscape template (wide,
 * short) ended up with barely any height and read as "small" next to a portrait row's tall
 * cards; deriving width from a shared height instead makes a landscape card properly wide, the
 * way an actual landscape design should look.
 */
@Composable
private fun BrowsingRows(rows: List<Pair<String, List<CanvasTemplate>>>, nav: NavHostController) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
    ) {
        items(rows, key = { it.first }) { (category, list) ->
            val ratio = cardDisplayRatio(rowRatio(list))
            // Portrait rows get extra height. Every row sharing one height made a portrait card
            // (which derives its width from that height) come out narrow and stubby next to a
            // landscape row's, underselling designs that are mostly tall.
            val rowHeight = if (ratio < 1f) 215.dp else 170.dp
            Row(
                Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    category.uppercase(), color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                )
                Text(
                    "View all >", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { nav.navigate(Routes.templateCategory(category)) }
                        .padding(start = 8.dp),
                )
            }
            val scroll = rememberScrollState()
            HScrollHint(scroll) {
                Row(
                    Modifier.horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    list.forEach { t ->
                        TemplateCard(t, ratio, Modifier.height(rowHeight)) {
                            nav.navigate(Routes.templateEditor(t.id))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun TemplateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .neuRaised(cornerRadius = 50.dp, elevation = 5.dp, background = if (selected) Gold else AppSurfaceVariant)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label.uppercase(),
            color = if (selected) AppBackground else AppOnSurfaceMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One category's templates in full, opened from a row's "View all". */
@Composable
fun TemplateCategoryScreen(nav: NavHostController, category: String) {
    val templates by ServiceLocator.catalogRepository.canvasTemplates.collectAsStateSafe()
    val list = remember(templates, category) {
        templateRows(templates).firstOrNull { it.first.equals(category, ignoreCase = true) }?.second.orEmpty()
    }
    val ratio = remember(list) { cardDisplayRatio(rowRatio(list)) }
    Column(Modifier.fillMaxSize()) {
        SbBackHeader(category, { nav.popBackStack() })
        if (list.isEmpty()) {
            EmptyState("Nothing here", "No templates in this category yet.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                gridItems(list, key = { it.id }) { t ->
                    TemplateCard(t, ratio, Modifier.fillMaxWidth()) {
                        nav.navigate(Routes.templateEditor(t.id))
                    }
                }
            }
        }
    }
}

/**
 * Canva-style template card: full-bleed artwork fills the card, no caption on top of it — the
 * admin's own title for the template (often just an internal label like "portrait-4.5") isn't
 * meant for end users, so it doesn't render here; the category row above a strip of these is
 * the only label shown. Takes its aspect [ratio] from the category it sits in.
 */
@Composable
fun TemplateCard(
    template: CanvasTemplate,
    ratio: Float = DEFAULT_TEMPLATE_RATIO,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val panelColor = remember(template.bgColor) { parseHexColor(template.bgColor) ?: AppSurfaceElevated }
    Box(
        modifier
            .aspectRatio(ratio)
            .neuRaised(cornerRadius = 14.dp, elevation = 7.dp)
            // neuRaised paints the visible card 7dp (its elevation) inside these bounds, leaving
            // room for its shadow — clip the art to that same inset shape, or a thumbnail with
            // see-through areas shows its art spilling past the card's edges into the shadow.
            .padding(7.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        if (template.image != null) {
            AsyncImage(
                model = template.image,
                contentDescription = template.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(panelColor))
        }
    }
}
