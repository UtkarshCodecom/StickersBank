package com.stickers.bank.ui.featured

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawWithCache
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Banner
import com.stickers.bank.data.model.Category
import com.stickers.bank.data.model.PopularTag
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.data.model.TrendingCard
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.allstickers.comparator
import com.stickers.bank.ui.components.AddToBankDialog
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.HScrollHint
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.components.SelectableStickerGrid
import com.stickers.bank.ui.components.SelectionActionBar
import com.stickers.bank.ui.components.StickerThumb
import com.stickers.bank.ui.components.rememberStickerSelection
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.rememberGifShare
import com.stickers.bank.ui.rememberPackExporter
import com.stickers.bank.ui.rememberTelegramPackShare
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceElevated
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.ui.theme.parseHexColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Search state lives outside the composable so that re-selecting the FEATURED tab can clear
 * a stale query — re-selecting a tab doesn't recreate the screen, so local `remember` state
 * would survive and a leftover search term would stick around.
 */
object FeaturedSearch {
    var query by mutableStateOf("")
    var gridCategories by mutableStateOf(false) // false = full-width rows, true = 2-col boxes
    fun clear() {
        query = ""
    }
}

/**
 * FEATURED = browse by category. The search bar sits inline at the top, next to the wordmark,
 * always visible — no tap-to-reveal step. Categories stay on screen until you actually type,
 * then the results grid takes over.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FeaturedScreen(nav: NavHostController) {
    val context = LocalContext.current
    val catalogState by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val loading by ServiceLocator.catalogRepository.loading.collectAsStateSafe()
    val loadError by ServiceLocator.catalogRepository.error.collectAsStateSafe()
    val custom by ServiceLocator.customStickerRepository.stickers.collectAsStateSafe()
    val filter by ServiceLocator.filterPreferences.filter.collectAsStateSafe()
    val banners by ServiceLocator.catalogRepository.banners.collectAsStateSafe()
    val bannerDisplayMode by ServiceLocator.catalogRepository.bannerDisplayMode.collectAsStateSafe()
    val trending by ServiceLocator.catalogRepository.trending.collectAsStateSafe()
    val popularTags by ServiceLocator.catalogRepository.popularTags.collectAsStateSafe()

    val categories = catalogState.categories
    val categoryTags = remember(catalogState) { categories.map { it.name.lowercase() } }

    // The category picks from the filter sheet narrow THIS list too: select Hindi + English
    // and Featured shows only those two.
    val visibleCategories = remember(categories, filter, categoryTags) {
        val names = categoryTags.toSet()
        val include = filter.includeTags.filter { it in names }
        val exclude = filter.excludeTags.filter { it in names }
        categories.filter { c ->
            val n = c.name.lowercase()
            (include.isEmpty() || n in include) && n !in exclude
        }
    }

    val query = FeaturedSearch.query
    val searching = query.isNotBlank()
    // Counted once the user pauses typing — not once per keystroke.
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(1500)
        if (query.isNotBlank()) ServiceLocator.analytics.searched(query)
    }

    val all = remember(custom, catalogState) {
        custom.reversed() + catalogState.allStickers().distinctBy { it.id }
    }
    val results = remember(all, query, filter, categoryTags, searching) {
        if (!searching) {
            emptyList()
        } else {
            val catInclude = filter.includeTags.filter { it in categoryTags }
            val tagInclude = filter.includeTags.filter { it !in categoryTags }
            all.filter { s ->
                s.matches(query) &&
                    filter.allowsType(s.animated) &&
                    (catInclude.isEmpty() || catInclude.any { it in s.tags }) &&
                    (tagInclude.isEmpty() || tagInclude.any { it in s.tags }) &&
                    filter.excludeTags.none { it in s.tags }
            }.sortedWith(filter.sort.comparator())
        }
    }

    // Templates are searchable from the same bar — a query can hit a template's title,
    // its category or one of its tags.
    val canvasTemplates by ServiceLocator.catalogRepository.canvasTemplates.collectAsStateSafe()
    val templateHits = remember(canvasTemplates, query, searching) {
        if (!searching) emptyList() else canvasTemplates.filter { it.matchesQuery(query) }
    }

    var detail by remember { mutableStateOf<Sticker?>(null) }
    val selection = rememberStickerSelection()
    var showBankDialog by remember { mutableStateOf(false) }
    val exportPack = rememberPackExporter()
    val gifShare = rememberGifShare()
    val telegramSend = rememberTelegramPackShare()
    val showAd = com.stickers.bank.ads.rememberInterstitial()

    // Drop any selection when you leave the results grid.
    LaunchedEffect(searching) { if (!searching) selection.clear() }

    // Search results split into two swipeable tabs: stills, and animations.
    val stillResults = remember(results) { results.filterNot { it.animated } }
    val animResults = remember(results) { results.filter { it.animated } }
    val kindPager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val kindScope = rememberCoroutineScope()
    // A new search that only matches the other kind jumps there instead of opening on an empty tab.
    LaunchedEffect(stillResults.isEmpty(), animResults.isEmpty()) {
        val page = kindPager.currentPage
        if (page == 0 && stillResults.isEmpty() && animResults.isNotEmpty()) kindPager.scrollToPage(1)
        if (page == 1 && animResults.isEmpty() && stillResults.isNotEmpty()) kindPager.scrollToPage(0)
    }

    // Persisted so the scroll position survives opening a category and coming back.
    val listState = com.stickers.bank.ui.components.rememberSavedListState("featured_categories_list")
    val gridState = com.stickers.bank.ui.components.rememberSavedGridState("featured_categories_grid")

    // Pad by the keyboard height, but subtract the nav-bar inset the Scaffold already applied
    // (otherwise the bar floats a nav-bar's height above the keyboard).
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))) {
        // Pull down anywhere in the feed to re-fetch the catalog — admin changes (new
        // categories, stickers, banners…) otherwise only showed up on ON_RESUME (see
        // StickersBankApp), i.e. only after actually leaving and reopening the app.
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { ServiceLocator.catalogRepository.refresh() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            when {
                searching && results.isEmpty() && templateHits.isEmpty() ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No matches", color = AppOnSurfaceMuted) }

                searching -> Column(Modifier.fillMaxSize()) {
                    if (templateHits.isNotEmpty()) {
                        Text(
                            "TEMPLATES", color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 6.dp),
                        )
                        val tScroll = rememberScrollState()
                        HScrollHint(tScroll) {
                            Row(
                                Modifier.horizontalScroll(tScroll).padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val w = (LocalConfiguration.current.screenWidthDp.dp - 32.dp) / 3.5f
                                templateHits.forEach { t ->
                                    com.stickers.bank.ui.templates.TemplateCard(
                                        t,
                                        if (t.ratio > 0f) t.ratio else 3.2f / 5f,
                                        Modifier.width(w),
                                    ) { nav.navigate(Routes.templateEditor(t.id)) }
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                    }
                    // Counts live in the tabs themselves, up top — the old count line at the
                    // bottom ended up hidden behind the floating nav.
                    SearchKindTabs(kindPager.currentPage, stillResults.size, animResults.size) { page ->
                        kindScope.launch { kindPager.animateScrollToPage(page) }
                    }
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = kindPager,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) { page ->
                        val list = if (page == 0) stillResults else animResults
                        if (list.isEmpty()) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text(if (page == 0) "No stickers match" else "No animations match", color = AppOnSurfaceMuted)
                            }
                        } else {
                            SelectableStickerGrid(
                                stickers = list,
                                selection = selection,
                                onStickerClick = { detail = it },
                                modifier = Modifier.fillMaxSize(),
                                columns = 3,
                                // Room for the last row to scroll clear of the floating nav.
                                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 100.dp),
                            )
                        }
                    }
                }

                categories.isEmpty() && loading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Gold) }

                categories.isEmpty() && loadError != null ->
                    EmptyState(
                        "Couldn't load catalog",
                        "Firestore error: $loadError\n\nIf this says permission denied, open " +
                            "Firestore rules for this project and allow reads.",
                    )

                categories.isEmpty() ->
                    EmptyState(
                        "No categories yet",
                        "Add categories from the admin panel, then pull down here to refresh.",
                    )

                visibleCategories.isEmpty() ->
                    EmptyState(
                        "No categories match",
                        "Your filter hides every category. Open the filter button up top and clear it.",
                    )

                else -> {
                    // Everything scrolls together as one feed: banner → trending → tags → categories.
                    val header: @Composable () -> Unit = {
                        FeaturedHeader(
                            banners = banners,
                            bannerDisplayMode = bannerDisplayMode,
                            trending = trending,
                            popularTags = popularTags,
                            onTrendingClick = { card -> showAd { nav.navigate(Routes.trending(card.id)) } },
                            onTagClick = { tag -> nav.navigate(Routes.tag(tag)) },
                            onSeeAllTrending = { nav.navigate(Routes.TRENDING_ALL) },
                        )
                    }
                    if (FeaturedSearch.gridCategories) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { header() }
                            gridItems(visibleCategories, key = { it.id }) { category ->
                                CategoryBox(category) { nav.navigate(Routes.category(category.id)) }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            // No spacedBy here — each LadderCategoryRow owns its own gap-to-the-
                            // next-rung, so its two rails can run through that gap uninterrupted
                            // instead of the list's own spacing cutting a visible break in them.
                        ) {
                            item {
                                header()
                                Spacer(Modifier.height(12.dp))
                            }
                            itemsIndexed(visibleCategories, key = { _, c -> c.id }) { index, category ->
                                LadderCategoryRow(
                                    category,
                                    isLast = index == visibleCategories.lastIndex,
                                ) { nav.navigate(Routes.category(category.id)) }
                            }
                        }
                    }
                }
            }

            // (Search now opens from the search icon at the top-right of the wordmark.)
        }

        if (searching && selection.active) {
            val chosen = results.filter { it.id in selection.selected }
            val allVideo = chosen.isNotEmpty() && chosen.all { it.isVideoClip }
            // Lifted above the floating nav (it used to sit underneath it); with the keyboard up
            // the nav is hidden behind the keyboard anyway, so no lift is needed then.
            val lift = if (WindowInsets.isImeVisible) 0.dp else com.stickers.bank.ui.components.SbBottomNavClearance
            // Named after what was searched ("Funny"), so each search's picks get their own pack
            // in WhatsApp instead of all landing in one shared generic one.
            val packName = query.trim().replaceFirstChar { it.uppercaseChar() }.ifBlank { "Search" }
            Box(Modifier.padding(bottom = lift)) {
                SelectionActionBar(
                    count = selection.selected.size,
                    onAddToBank = { showBankDialog = true },
                    onWhatsApp = { if (allVideo) gifShare(chosen) else exportPack(packName, chosen) },
                    onTelegram = { telegramSend(packName, chosen) },
                    whatsAppLabel = if (allVideo) "GIF" else "WhatsApp",
                    onClose = { selection.clear() },
                )
            }
        }

        // (Search bar itself now lives at the TOP, inline with "STICKERS BANK" — see SbHomeTopBar.)
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = if (searching) (if (s.animated) animResults else stillResults) else listOf(s),
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }

    if (showBankDialog) {
        AddToBankDialog(
            count = selection.selected.size,
            onDismiss = { showBankDialog = false },
            onAdd = { bank ->
                val chosen = results.filter { it.id in selection.selected }
                chosen.forEach { ServiceLocator.bankRepository.addSticker(bank.id, it) }
                Toast.makeText(context, "Added ${chosen.size} to ${bank.name}", Toast.LENGTH_SHORT).show()
                selection.clear(); showBankDialog = false
            },
        )
    }
}

/**
 * Auto-rotating 5:1 promo banners (managed from the web admin). Hidden when there are none.
 * "carousel" mode slides between banners with dots (the original behaviour); "fade" crossfades
 * with no dots. Either way, a video banner holds on screen until it finishes playing instead
 * of advancing on the usual timer — [BannerMedia] reports that back via [onVideoComplete].
 */
/**
 * Stickers / Animations switch above the search results. The open tab is pressed into the
 * surface (neuInset); the other floats (neuRaised) — and they trade places on tap or swipe.
 * Both draw their visible pill the same size: neuRaised already paints inside its bounds by its
 * elevation, so the pressed one gets that same padding first.
 */
@Composable
private fun SearchKindTabs(selected: Int, stillCount: Int, animCount: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("Stickers" to stillCount, "Animations" to animCount).forEachIndexed { i, (label, count) ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).height(52.dp)
                    .then(
                        if (sel) Modifier.padding(7.dp).neuInset(cornerRadius = 19.dp, elevation = 5.dp)
                        else Modifier.neuRaised(cornerRadius = 19.dp, elevation = 7.dp).padding(7.dp),
                    )
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$label · $count",
                    color = if (sel) Gold else AppOnSurface,
                    fontWeight = if (sel) FontWeight.Black else FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** What the banner carousel rotates through: today's day card, then the admin's own banners. */
private sealed interface BannerSlide {
    data object Today : BannerSlide
    data class Media(val banner: Banner) : BannerSlide
}

private val BannerSlide.isVideo: Boolean get() = this is BannerSlide.Media && banner.video

@Composable
private fun BannerCarousel(banners: List<Banner>, displayMode: String) {
    val slides = remember(banners) { listOf<BannerSlide>(BannerSlide.Today) + banners.map { BannerSlide.Media(it) } }
    if (displayMode == "fade") FadeBannerCarousel(slides) else SlideBannerCarousel(slides)
}

@Composable
private fun BannerSlideContent(slide: BannerSlide, modifier: Modifier, onVideoComplete: () -> Unit) {
    when (slide) {
        BannerSlide.Today -> TodayBanner(modifier)
        is BannerSlide.Media -> BannerMedia(slide.banner, modifier, onVideoComplete)
    }
}

@Composable
private fun SlideBannerCarousel(slides: List<BannerSlide>) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { slides.size })
    // Bumped whenever a video banner finishes, so the rotation below can move on immediately
    // instead of sitting out the usual timer.
    var videoDone by remember { mutableStateOf(0) }

    // This carousel lives in a LazyColumn item, so scrolling it off-screen DISPOSES it and
    // kills any slide that was still animating. rememberPagerState is saveable, so the
    // half-way scroll offset gets restored when the item comes back — which is what left the
    // banner frozen between two pages. Snap to the nearest page whenever we (re)enter.
    LaunchedEffect(Unit) {
        if (pagerState.currentPageOffsetFraction != 0f) {
            runCatching { pagerState.scrollToPage(pagerState.currentPage) }
        }
    }

    // One long-lived loop drives the rotation. The previous version re-keyed this effect on
    // the current page, which meant every page change cancelled the effect *and* the
    // animateScrollToPage it was awaiting — stranding the pager mid-slide. Keying only on
    // slides.size keeps a single coroutine alive across transitions; it waits for the pager
    // to be idle before timing the next advance, so a manual swipe just resets the clock.
    LaunchedEffect(slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            snapshotFlow { pagerState.isScrollInProgress }.first { !it }
            if (slides.getOrNull(pagerState.currentPage)?.isVideo == true) {
                val seen = videoDone
                snapshotFlow { videoDone }.first { it != seen } // hold until the clip ends
            } else {
                kotlinx.coroutines.delay(4000)
            }
            runCatching { pagerState.animateScrollToPage((pagerState.currentPage + 1) % slides.size) }
        }
    }
    // ~95% width — just enough side gap to read as inset, not the old wide margins.
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp)) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            pageSpacing = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            BannerSlideContent(
                slides[page],
                Modifier.fillMaxWidth().aspectRatio(5f).neuRaised(cornerRadius = 16.dp, elevation = 8.dp).clip(RoundedCornerShape(16.dp)),
                onVideoComplete = { videoDone++ },
            )
        }
        if (slides.size > 1) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                repeat(slides.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape).background(if (active) Gold else AppOnSurfaceMuted.copy(alpha = 0.4f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun FadeBannerCarousel(slides: List<BannerSlide>) {
    var index by remember(slides) { mutableStateOf(0) }
    val safeIndex = index.coerceIn(0, slides.lastIndex)
    LaunchedEffect(safeIndex, slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        if (slides.getOrNull(safeIndex)?.isVideo == true) return@LaunchedEffect // waits for the video instead
        kotlinx.coroutines.delay(4000)
        index = (safeIndex + 1) % slides.size
    }
    androidx.compose.animation.Crossfade(
        targetState = safeIndex,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "bannerFade",
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp)
            .aspectRatio(5f).neuRaised(cornerRadius = 16.dp, elevation = 8.dp).clip(RoundedCornerShape(16.dp)),
    ) { i ->
        val slide = slides.getOrNull(i) ?: return@Crossfade
        BannerSlideContent(slide, Modifier.fillMaxSize(), onVideoComplete = { index = (safeIndex + 1) % slides.size })
    }
}

/**
 * The built-in first slide: today's day on black — "Happy" in script over the day name in the
 * splash's Gang of Three face, the date on the right, a warm gold glow behind. Same inset mat
 * as the photo banners so it sits in the carousel like one of them.
 */
@Composable
private fun TodayBanner(modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Recomputed if the app is still open when the date rolls over (keyed on the date itself).
    val dateKey = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
    val (day, dayOfMonth, month) = remember(dateKey) {
        val now = java.util.Date()
        Triple(
            java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(now).uppercase(),
            java.text.SimpleDateFormat("dd", java.util.Locale.ENGLISH).format(now),
            java.text.SimpleDateFormat("MMMM", java.util.Locale.ENGLISH).format(now).uppercase(),
        )
    }
    // Drawn straight onto a canvas from the fonts' real letter shapes (ink bounds), not text
    // line boxes: line boxes carry a lot of empty ascent/descent — the script face's especially
    // — which is what kept the greeting small and off-centre however it was nudged. Measured
    // ink is what lets "Happy TUESDAY" and the date grow until they genuinely fill the strip.
    val displayFace = remember {
        runCatching { android.graphics.Typeface.createFromAsset(context.assets, "go3v2.ttf") }
            .getOrDefault(android.graphics.Typeface.DEFAULT_BOLD)
    }
    val scriptFace = remember {
        runCatching { android.graphics.Typeface.createFromAsset(context.assets, "fonts/font_1.ttf") }
            .getOrDefault(android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC))
    }
    val warm = Color(0xFFF5B301)
    Box(
        modifier.padding(5.dp).clip(RoundedCornerShape(11.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF050505), Color(0xFF15120A))))
            .drawBehind {
                // A soft gold glow rising from the bottom-right, behind the date.
                drawCircle(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(warm.copy(alpha = 0.32f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 1.05f),
                        radius = size.height * 1.6f,
                    ),
                    radius = size.height * 1.6f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 1.05f),
                )
            }
            .border(1.dp, warm.copy(alpha = 0.25f), RoundedCornerShape(11.dp))
            .drawWithCache {
                val w = size.width
                val h = size.height
                fun paint(face: android.graphics.Typeface, color: Color, spacing: Float = 0f) =
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = face
                        this.color = color.toArgb()
                        letterSpacing = spacing
                    }
                fun ink(p: android.graphics.Paint, text: String, sizePx: Float) =
                    android.graphics.Rect().also { p.textSize = sizePx; p.getTextBounds(text, 0, text.length, it) }

                val padX = h * 0.22f
                val padY = h * 0.13f
                val innerH = h - 2 * padY

                // Date, right: the day-of-month fills ~2/3 of the height, the month sits under it.
                val numPaint = paint(displayFace, warm)
                val monthPaint = paint(android.graphics.Typeface.DEFAULT_BOLD, Color.White.copy(alpha = 0.65f), 0.22f)
                val numRef = ink(numPaint, dayOfMonth, 100f)
                val numSize = 100f * innerH * 0.64f / numRef.height().coerceAtLeast(1)
                val numInk = ink(numPaint, dayOfMonth, numSize)
                val monthRef = ink(monthPaint, month, 100f)
                val monthSize = 100f * innerH * 0.17f / monthRef.height().coerceAtLeast(1)
                val monthInk = ink(monthPaint, month, monthSize)
                val dateW = maxOf(numInk.width(), monthInk.width()).toFloat()
                val numBaseline = padY - numInk.top
                val monthBaseline = padY + numInk.height() + innerH * 0.19f - monthInk.top

                // Greeting, left: "Happy" + DAY on one shared baseline, scaled as one piece to
                // the largest size that fits BOTH the width left of the date and the full height.
                val happyPaint = paint(scriptFace, warm)
                val dayPaint = paint(displayFace, Color.White, 0.04f)
                val happyRef = ink(happyPaint, "Happy", 100f * HAPPY_TO_DAY)
                val dayRef = ink(dayPaint, day, 100f)
                val gapRef = 100f * GAP_TO_DAY
                val widthRef = happyRef.width() + gapRef + dayRef.width()
                val topRef = minOf(happyRef.top, dayRef.top).toFloat()
                val heightRef = maxOf(happyRef.bottom, dayRef.bottom) - topRef
                val room = w - 2 * padX - dateW - h * 0.25f
                val k = minOf(room / widthRef, innerH / heightRef).coerceAtLeast(0.05f)
                happyPaint.textSize = 100f * HAPPY_TO_DAY * k
                dayPaint.textSize = 100f * k
                val baseline = (h - heightRef * k) / 2f - topRef * k
                val startX = padX + (room - widthRef * k) / 2f
                val happyX = startX - happyRef.left * k
                val dayX = startX + (happyRef.width() + gapRef) * k - dayRef.left * k

                onDrawWithContent {
                    drawContent()
                    drawIntoCanvas { c ->
                        val nc = c.nativeCanvas
                        nc.drawText("Happy", happyX, baseline, happyPaint)
                        nc.drawText(day, dayX, baseline, dayPaint)
                        nc.drawText(dayOfMonth, w - padX - numInk.width() - numInk.left, numBaseline, numPaint)
                        nc.drawText(month, w - padX - monthInk.width() - monthInk.left, monthBaseline, monthPaint)
                    }
                }
            },
    )
}

/** The today banner's greeting proportions: "Happy" and the gap after it, relative to the day's size. */
private const val HAPPY_TO_DAY = 0.78f
private const val GAP_TO_DAY = 0.28f

/** One banner slot — an image, or a playing video that reports completion so the rotation can advance. */
@Composable
private fun BannerMedia(banner: Banner, modifier: Modifier, onVideoComplete: () -> Unit) {
    // Inset with a visible mat, same as the other photo cards — `modifier` already carries the
    // card's own outer neuRaised+clip, this just insets and re-clips the media itself.
    // Smaller inset than the taller cards: at a 5:1 aspect a 7dp mat would eat a much bigger
    // share of the card's short height than its long width.
    val context = androidx.compose.ui.platform.LocalContext.current
    val linked = modifier.then(
        if (banner.link.isNotBlank()) {
            Modifier.clickable { openBannerLink(context, banner.link) }
        } else Modifier,
    )
    val framed = linked.padding(5.dp).clip(RoundedCornerShape(11.dp))
    if (banner.video) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoURI(android.net.Uri.parse(banner.image))
                    setOnPreparedListener { mp -> mp.isLooping = false; start() }
                    setOnCompletionListener { onVideoComplete() }
                    // A clip that can't play (offline, bad upload) just moves the banner on.
                    // Without this VideoView pops up its own "Can't play this video" dialog.
                    setOnErrorListener { _, _, _ -> onVideoComplete(); true }
                }
            },
            onRelease = { it.stopPlayback() },
            modifier = framed,
        )
    } else {
        coil.compose.AsyncImage(
            model = banner.image,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = framed,
        )
    }
}

/**
 * Opens a banner's admin-set link in the browser. Anything without a scheme is assumed https,
 * so an admin can type "example.com" and still get a working link; a URL no app can handle is
 * swallowed rather than crashing the Featured tab.
 */
private fun openBannerLink(context: android.content.Context, link: String) {
    val url = if (link.startsWith("http://") || link.startsWith("https://")) link else "https://$link"
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun LayoutToggle(grid: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.neuRaised(cornerRadius = 50.dp, elevation = 6.dp).clip(RoundedCornerShape(50)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToggleIcon(Icons.Rounded.ViewAgenda, !grid) { onChange(false) }
        ToggleIcon(Icons.Rounded.GridView, grid) { onChange(true) }
    }
}

@Composable
private fun ToggleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(if (selected) Gold else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (selected) AppBackground else AppOnSurfaceMuted, modifier = Modifier.size(21.dp)) }
}

@Composable
private fun CategoryBox(category: Category, onClick: () -> Unit) {
    // Grid mode → the admin's SQUARE image as background (falls back to the gradient card).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .neuRaised(cornerRadius = 20.dp, elevation = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        if (category.squareImages.isNotEmpty()) {
            // Inset with a visible mat of the card's own surface color on all sides, instead of
            // full-bleed — that mat is what actually reads as "floating," since the raised
            // shadow drawn behind the card is otherwise fully covered by an edge-to-edge photo.
            CrossfadingCategoryImage(
                category.squareImages, category.name,
                Modifier.padding(7.dp).clip(RoundedCornerShape(14.dp)).fillMaxSize(),
            )
        }
        // No scrim over the artwork and no shadow on the text — plain label, unstyled.
        Column(Modifier.align(Alignment.BottomStart).padding(start = 17.dp, end = 11.dp, bottom = 11.dp)) {
            Text(
                category.name.uppercase(), color = Color.Black, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${category.stickerCount} stickers", color = Color.Black, fontSize = 11.sp,
            )
        }
    }
}

// Card stays its usual height; the row itself is a bit taller so the two rails below have the
// 12dp gap-to-the-next-rung to run through too — otherwise the list's own item spacing would
// cut a visible break in an otherwise continuous line.
private val LADDER_CARD_HEIGHT = 78.dp
private val LADDER_ROW_HEIGHT = 90.dp
private val LADDER_RAIL_WIDTH = 8.dp
// Was 24dp — pulled in close enough to the card's own edges that the rail-to-rail span read as
// a narrow strip floating in the middle of a much wider card instead of the ladder's own width.
// Must clear CategoryPill's own 16dp corner radius (below) — inset any smaller than that puts
// the rail's straight edge inside the zone where the card's corner curves away from the edge,
// exposing a sliver of rail right at the top/bottom corners of every card instead of it staying
// fully hidden behind the card wherever they overlap.
private val LADDER_RAIL_INSET = 20.dp

/**
 * "Ladder" category list (list-mode only, not the grid) — two thin rails run continuously down
 * both sides, BEHIND the full-width cards (drawn first, so the card — composed after, i.e. on
 * top — hides the rail wherever it overlaps), peeking out only in the gap between one card and
 * the next. Reads as one continuous pair of rails threading behind the whole stack, like the
 * side rails of a real ladder, rather than a decoration floating beside the cards.
 */
@Composable
private fun LadderCategoryRow(category: Category, isLast: Boolean = false, onClick: () -> Unit) {
    // The trailing reveal (the row's extra height below the card) exists so the rail keeps
    // running into the NEXT row's card — the last row has no next card to run into, so its
    // rail stops at the card's own height instead of dangling past it with nothing to hide it.
    val railHeight = if (isLast) LADDER_CARD_HEIGHT else LADDER_ROW_HEIGHT
    Box(Modifier.fillMaxWidth().height(LADDER_ROW_HEIGHT)) {
        Box(
            Modifier.align(Alignment.TopStart).offset(x = LADDER_RAIL_INSET)
                .width(LADDER_RAIL_WIDTH).height(railHeight).background(Color.Black),
        )
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = -LADDER_RAIL_INSET)
                .width(LADDER_RAIL_WIDTH).height(railHeight).background(Color.Black),
        )
        CategoryPill(
            category,
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(bottom = LADDER_ROW_HEIGHT - LADDER_CARD_HEIGHT),
            onClick,
        )
    }
}

@Composable
private fun CategoryPill(category: Category, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // List mode → the admin's 5:1 BANNER image as background (falls back to the gradient card).
    // Fixed height so every card matches exactly, whether or not it has a description line.
    Box(
        modifier
            .fillMaxWidth()
            .height(78.dp)
            .neuRaised(cornerRadius = 16.dp, elevation = 7.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        if (category.bannerImages.isNotEmpty()) {
            // Same visible-mat framing as CategoryBox — see its comment.
            CrossfadingCategoryImage(
                category.bannerImages, null,
                Modifier.padding(6.dp).clip(RoundedCornerShape(11.dp)).fillMaxSize(),
            )
        }
        // The count is the only text on the card — no name, no description, no accent bar.
        // Sits low in the card rather than centred, so it reads as a caption under the artwork.
        Row(
            Modifier.fillMaxSize().padding(start = 20.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "${category.stickerCount} stickers", color = Color.Black, fontSize = 10.sp, lineHeight = 11.sp,
            )
        }
    }
}

/**
 * A category card's background image(s). One image just renders; more than one auto-rotates —
 * plain crossfade every second, no swipe/dots carousel — so admins can set a few images per
 * category and have the card cycle through them on its own.
 */
@Composable
private fun CrossfadingCategoryImage(images: List<String>, contentDescription: String?, modifier: Modifier = Modifier) {
    if (images.size <= 1) {
        coil.compose.AsyncImage(
            model = images.firstOrNull(), contentDescription = contentDescription,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier,
        )
        return
    }
    var index by remember(images) { mutableStateOf(0) }
    LaunchedEffect(images) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            index = (index + 1) % images.size
        }
    }
    androidx.compose.animation.Crossfade(
        targetState = index,
        animationSpec = androidx.compose.animation.core.tween(500),
        modifier = modifier,
        label = "categoryImage",
    ) { i ->
        coil.compose.AsyncImage(
            model = images.getOrNull(i), contentDescription = contentDescription,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
    }
}

// ---------------- Featured feed header: Trending + Popular tags ----------------

@Composable
private fun FeaturedHeader(
    banners: List<Banner>,
    bannerDisplayMode: String,
    trending: List<TrendingCard>,
    popularTags: List<PopularTag>,
    onTrendingClick: (TrendingCard) -> Unit,
    onTagClick: (String) -> Unit,
    onSeeAllTrending: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        BannerCarousel(banners, bannerDisplayMode)
        if (trending.isNotEmpty()) {
            // Full screen width shows ~3.5 cards, so the row visibly continues off-edge.
            val cardWidth = (LocalConfiguration.current.screenWidthDp.dp - 32.dp) / 3.5f
            // Tighter than other sections — sits right under the banner.
            FeaturedSectionLabelRow("TRENDING", "See all >", onSeeAllTrending, top = 8.dp)
            val scroll = rememberScrollState()
            val density = LocalDensity.current
            val cardPx = with(density) { cardWidth.toPx() }
            val stepPx = cardPx + with(density) { 10.dp.toPx() }
            var viewportPx by remember { mutableFloatStateOf(0f) }
            HScrollHint(scroll) {
                Row(
                    Modifier.onSizeChanged { viewportPx = it.width.toFloat() }.horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    trending.forEachIndexed { i, card ->
                        TrendingTile(
                            card,
                            Modifier.width(cardWidth).graphicsLayer {
                                // 3D carousel: how far this card sits from the row's middle, −1
                                // (left edge) … 0 (centre) … +1 (right edge). Read here, in the
                                // layer, so scrolling only re-draws instead of recomposing.
                                val half = viewportPx / 2f
                                val f = if (half <= 0f) 0f
                                else ((i * stepPx + cardPx / 2f - scroll.value - half) / half).coerceIn(-1.2f, 1.2f)
                                // Positive rotationY brings a layer's left edge toward the viewer,
                                // so cards on the left turn their outer (left) side out of the screen
                                // and their inner side in; the right-hand cards mirror that.
                                rotationY = -f * 26f
                                val shrink = 1f - 0.08f * kotlin.math.abs(f)
                                scaleX = shrink; scaleY = shrink
                                // Further-back camera, so the turn reads as depth, not distortion.
                                cameraDistance = 14f * density.density
                            },
                            onTrendingClick,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
        if (popularTags.isNotEmpty()) {
            // Tighter than the other sections: the pills are short, so the usual 16/8 rhythm
            // left them floating in dead space. No vertical padding on the row itself either —
            // shadows/glows in Compose aren't clipped by a loose parent, so it wasn't buying
            // anything but extra gap.
            FeaturedSectionLabel("POPULAR TAGS", top = 4.dp, bottom = 2.dp)
            val scroll = rememberScrollState()
            HScrollHint(scroll) {
                Row(
                    Modifier.horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    popularTags.forEach { pt ->
                        // Admin-picked color wins; otherwise fall back to a deterministic theme.
                        val theme = pt.color?.let { parseHexColor(it) } ?: tagTheme(pt.tag)
                        Box(
                            Modifier
                                // Coloured shadow = the pill glows in its own theme colour.
                                // Must come before clip/background so it's cast, not clipped away.
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = theme,
                                    spotColor = theme,
                                )
                                // The picked colour, solid — no wash, no tint, no border, so the
                                // pill reads as exactly the swatch the admin chose, white text on top.
                                .clip(RoundedCornerShape(50)).background(theme)
                                .clickable { onTagClick(pt.tag) }.padding(horizontal = 13.dp, vertical = 6.dp),
                        ) {
                            // Any colour can be picked in the admin now, not just the vivid
                            // presets — so a pale one gets dark text instead of unreadable white.
                            val ink = if (theme.luminance() > 0.7f) Color(0xFF1C1C1C) else Color.White
                            Text("#${pt.tag}", color = ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("CATEGORIES", color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            LayoutToggle(FeaturedSearch.gridCategories) { FeaturedSearch.gridCategories = it }
        }
    }
}

// Deterministic per-tag color so the same tag always gets the same theme, and different tags
// spread across a varied, vivid palette (purple/green/gold/pink/blue/orange/teal/red).
private val TagPalette = listOf(
    Color(0xFF9B5DE5), Color(0xFF00C853), Color(0xFFFFC400), Color(0xFFFF4D8D),
    Color(0xFF3B82F6), Color(0xFFFF8A00), Color(0xFF14B8A6), Color(0xFFEF4444),
)
private fun tagTheme(tag: String): Color =
    TagPalette[(tag.lowercase().hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % TagPalette.size]

@Composable
private fun FeaturedSectionLabel(
    text: String,
    top: androidx.compose.ui.unit.Dp = 16.dp,
    bottom: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Text(
        text, color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = top, bottom = bottom),
    )
}

@Composable
private fun FeaturedSectionLabelRow(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    top: androidx.compose.ui.unit.Dp = 16.dp,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = top, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            actionLabel, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

// Portrait 4:5 (width:height) card — the admin's own title + image, not a real collection.
// The photo is inset on all four sides, leaving a mat of the card's own surface visible around
// it (that mat is what the neumorphic shadow reads against). The admin-picked panel colour is a
// SOLID strip pinned to the bottom of that inset photo — left/right edges and bottom corner
// radius all matching the photo exactly, so the two read as one block rather than a narrow
// image with a wider bar under it.
@Composable
private fun TrendingTile(card: TrendingCard, modifier: Modifier = Modifier, onClick: (TrendingCard) -> Unit) {
    val panelColor = remember(card.bgColor) { parseHexColor(card.bgColor) ?: AppSurfaceElevated }
    Box(
        modifier.aspectRatio(3.2f / 5f).neuRaised(cornerRadius = 16.dp, elevation = 7.dp).clip(RoundedCornerShape(16.dp))
            .clickable { onClick(card) },
    ) {
        coil.compose.AsyncImage(
            model = card.image, contentDescription = card.title,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.padding(7.dp).clip(RoundedCornerShape(11.dp)).fillMaxSize(),
        )
        TrendingBadge(card.badge, Modifier.align(Alignment.TopStart).padding(11.dp))
        Row(
            // Same 7dp inset as the photo above, so the panel's left/right edges line up with
            // the photo's instead of running the full card width.
            Modifier.align(Alignment.BottomCenter)
                .padding(start = 7.dp, end = 7.dp, bottom = 7.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 11.dp, bottomEnd = 11.dp))
                .background(panelColor)
                .padding(start = 9.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // No chevron here on purpose: these tiles are only ~1/3.5 of the screen wide, and a
            // glass chevron ate enough of that to truncate most titles ("Tom &…"). The wide
            // category rows have room for one; this doesn't.
            Column(Modifier.weight(1f)) {
                Text(
                    card.title, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text("${card.stickerIds.size} Stickers", color = Color.White.copy(alpha = 1f), fontSize = 10.sp, lineHeight = 11.sp, modifier = Modifier.padding(top = 1.dp))
            }
        }
    }
}

@Composable
private fun TrendingBadge(badge: String, modifier: Modifier = Modifier) {
    val label: String
    val bg: Color
    val fire: Boolean
    when (badge.lowercase()) {
        "new" -> { label = "NEW"; bg = Color(0xFF2ECC71); fire = false }
        "hot" -> { label = "HOT"; bg = Color(0xFFFF5A36); fire = true }
        else -> return
    }
    // The fire is deliberately NOT inline with the label — it's a bigger, separately laid out
    // glyph pinned to the pill's top-left corner so it visibly pokes out above/left of it,
    // instead of being squeezed down to the badge's own tiny text size.
    Box(modifier) {
        Box(
            Modifier.clip(RoundedCornerShape(5.dp)).background(bg)
                .padding(start = if (fire) 12.dp else 6.dp, end = 6.dp, top = 1.dp, bottom = 1.dp),
        ) {
            Text(label, color = Color.White, fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp)
        }
        if (fire) {
            Text(
                "🔥",
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = (-9).dp, y = (-7).dp),
            )
        }
    }
}

// ---------------- Tag results (opened from a popular-tag pill) ----------------

@Composable
fun TagResultsScreen(nav: NavHostController, tag: String) {
    val catalog by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val custom by ServiceLocator.customStickerRepository.stickers.collectAsStateSafe()
    var detail by remember { mutableStateOf<Sticker?>(null) }
    val results = remember(catalog, custom, tag) {
        (custom + catalog.allStickers()).distinctBy { it.id }.filter { s -> s.tags.any { it.equals(tag, ignoreCase = true) } }
    }
    Column(Modifier.fillMaxSize()) {
        SbBackHeader("#$tag", { nav.popBackStack() })
        Text("${results.size} stickers", color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
        if (results.isEmpty()) {
            EmptyState("No stickers", "Nothing tagged \"$tag\" yet.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gridItems(results, key = { it.id }) { s -> StickerThumb(sticker = s, onClick = { detail = s }) }
            }
        }
    }
    detail?.let { s ->
        StickerDetailSheet(
            stickers = results,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }
}

// ---------------- All trending cards at once (opened from the "See all" button) ----------------

@Composable
fun TrendingAllScreen(nav: NavHostController) {
    val showAd = com.stickers.bank.ads.rememberInterstitial()
    val trending by ServiceLocator.catalogRepository.trending.collectAsStateSafe()
    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Trending", { nav.popBackStack() })
        if (trending.isEmpty()) {
            EmptyState("No trending cards", "Nothing curated here yet.")
        } else {
            // 3 columns, not 2 — at 2 the cards came out roughly twice the size they are in
            // the Featured row (which fits ~3.5 across), which read as oversized here.
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                gridItems(trending, key = { it.id }) { card ->
                    TrendingTile(card, Modifier.fillMaxWidth()) { c -> showAd { nav.navigate(Routes.trending(c.id)) } }
                }
            }
        }
    }
}

// ---------------- Trending results (opened from a Trending card) ----------------

@Composable
fun TrendingResultsScreen(nav: NavHostController, trendingId: String) {
    // Resolved against the flat sticker set (every doc, regardless of category nesting) so a
    // sticker whose parent got restructured/deleted still shows up here — otherwise the card's
    // "N stickers" count (from stickerIds.size) can end up larger than what actually renders.
    val allStickersFlat by ServiceLocator.catalogRepository.allStickersFlat.collectAsStateSafe()
    val trendingList by ServiceLocator.catalogRepository.trending.collectAsStateSafe()
    val card = trendingList.firstOrNull { it.id == trendingId }
    var detail by remember { mutableStateOf<Sticker?>(null) }
    val results = remember(allStickersFlat, card) {
        card?.let { ServiceLocator.catalogRepository.stickersForIds(it.stickerIds) } ?: emptyList()
    }
    // This card's own backdrop, set per card in the Trending admin — it wins over the single
    // app-wide "trending" page background, so each trending page can look different.
    val cardBg = remember(card?.pageBgColor, card?.pageBgImage) {
        if (card?.pageBgColor == null && card?.pageBgImage == null) null
        else com.stickers.bank.data.model.PageBackground(card.pageBgColor, card.pageBgImage)
    }
    com.stickers.bank.ui.ProvidePageBackdrop(cardBg)

    Column(Modifier.fillMaxSize()) {
        SbBackHeader(card?.title ?: "Trending", { nav.popBackStack() })
        Text("${results.size} stickers", color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
        if (results.isEmpty()) {
            EmptyState("No stickers", "Nothing here yet.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gridItems(results, key = { it.id }) { s -> StickerThumb(sticker = s, onClick = { detail = s }) }
            }
        }
    }
    detail?.let { s ->
        StickerDetailSheet(
            stickers = results,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }
}
