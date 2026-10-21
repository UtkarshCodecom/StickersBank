package com.stickers.bank.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.ui.allstickers.AllStickersScreen
import com.stickers.bank.ui.bank.BankDetailScreen
import com.stickers.bank.ui.bank.BankScreen
import com.stickers.bank.ui.components.SbBottomNav
import com.stickers.bank.ui.components.SbHomeTopBar
import com.stickers.bank.ui.create.CreateScreen
import com.stickers.bank.ui.create.EditorScreen
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.featured.CategoryScreen
import com.stickers.bank.ui.featured.CollectionScreen
import com.stickers.bank.ui.featured.FeaturedScreen
import com.stickers.bank.ui.featured.FeaturedSearch
import com.stickers.bank.ui.settings.SettingsScreen

@Composable
fun StickersBankApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isRoot = route in Routes.roots
    LaunchedEffect(route) { analyticsScreenName(route)?.let { ServiceLocator.analytics.screenOpened(it) } }
    val showFilterButton = route == Routes.FEATURED || route == Routes.NEW_ARRIVAL

    val filter by ServiceLocator.filterPreferences.filter.collectAsStateSafe()
    val catalog by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val popularTags by ServiceLocator.catalogRepository.popularTags.collectAsStateSafe()
    val pageBackgrounds by ServiceLocator.catalogRepository.pageBackgrounds.collectAsStateSafe()
    var showFilterSheet by remember { mutableStateOf(false) }

    // Reload the catalog from Firestore whenever we come back to the foreground
    // (e.g. after uploading stickers in the admin panel).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ServiceLocator.catalogRepository.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Backdrop first and OUTSIDE the Scaffold's inset padding, so an admin-set page image runs
    // the full height of the screen — behind the status bar and the STICKERS BANK row too, not
    // just the content area below them.
    Box(Modifier.fillMaxSize()) {
    com.stickers.bank.ui.components.PageBackdrop(
        PageBackdropHost.override ?: pageBackgroundKeyFor(route)?.let { pageBackgrounds[it] },
        Modifier.fillMaxSize(),
    )
    Scaffold(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        // Transparent: the backdrop above shows through everything.
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (isRoot) {
                SbHomeTopBar(
                    showFilter = showFilterButton,
                    filterActiveCount = filter.activeCount,
                    onOpenFilter = { showFilterSheet = true },
                    showSearch = route == Routes.FEATURED,
                )
            }
        },
    ) { padding ->
        // No bottomBar slot: NavHost gets the FULL remaining height and SbBottomNav is overlaid
        // on top, so scrolling page content genuinely passes underneath the floating pill instead
        // of stopping at a Scaffold-reserved gap.
        Box(Modifier.padding(padding).fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = Routes.FEATURED,
                modifier = Modifier.fillMaxSize(),
            ) {
                appGraph(nav)
            }
            if (isRoot) {
                SbBottomNav(
                    current = route ?: Routes.FEATURED,
                    onTabSelected = { tab ->
                        when (tab.route) {
                            // CREATE opens the editor directly (the Sticker/Animation switch is in there).
                            Routes.CREATE -> nav.navigate(Routes.editor())
                            Routes.FEATURED -> { FeaturedSearch.clear(); nav.navigateToTab(tab.route) }
                            // Canvas opens straight onto "New Project" (closing it shows the templates).
                            Routes.CANVAS_TEMPLATES -> {
                                com.stickers.bank.ui.templates.CanvasLaunch.newProject = true
                                nav.navigateToTab(tab.route)
                            }
                            else -> nav.navigateToTab(tab.route)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
    } // backdrop Box

    if (showFilterSheet) {
        val categoryTags = catalog.categories.map { it.name.lowercase() }
        com.stickers.bank.ui.allstickers.FilterSortSheet(
            state = filter,
            categories = categoryTags,
            popularTags = popularTags,
            onChange = { ServiceLocator.filterPreferences.setFilter(it) },
            onDismiss = { showFilterSheet = false },
        )
    }
}

/** Maps a nav route to its `pageBackgrounds` key (see [com.stickers.bank.data.PAGE_BACKGROUND_KEYS]).
 * Null for anything not admin-customisable this way (CREATE opens the editor directly, category/
 * collection/etc. detail routes have their own per-category background instead). */
private fun pageBackgroundKeyFor(route: String?): String? = when (route) {
    Routes.FEATURED -> "featured"
    Routes.CANVAS_TEMPLATES -> "templates"
    Routes.NEW_ARRIVAL -> "newArrival"
    Routes.BANK -> "bank"
    Routes.FAVORITES -> "favorites"
    Routes.TRENDING_ALL -> "trendingAll"
    Routes.TRENDING -> "trending"
    else -> null
}

private fun NavGraphBuilder.appGraph(nav: NavHostController) {
    composable(Routes.FEATURED) { FeaturedScreen(nav) }
    composable(Routes.CANVAS_TEMPLATES) { com.stickers.bank.ui.templates.CanvasTemplatesScreen(nav) }
    composable(Routes.NEW_ARRIVAL) { AllStickersScreen(nav) }
    composable(Routes.FAVORITES) { com.stickers.bank.ui.favorites.FavoritesScreen(nav) }
    composable(Routes.CREATE) { CreateScreen(nav) }
    composable(Routes.BANK) { BankScreen(nav) }

    composable(Routes.CATEGORY) { entry ->
        CategoryScreen(nav, entry.arguments?.getString("id").orEmpty())
    }
    composable(Routes.COLLECTION) { entry ->
        val a = entry.arguments!!
        CollectionScreen(
            nav,
            a.getString("categoryId").orEmpty(),
            a.getString("groupId").orEmpty(),
            a.getString("collectionId").orEmpty(),
        )
    }
    composable(Routes.BANK_DETAIL) { entry ->
        BankDetailScreen(nav, entry.arguments?.getString("id").orEmpty())
    }
    composable(Routes.SETTINGS) { SettingsScreen(nav) }
    composable(Routes.STATUS) { com.stickers.bank.ui.status.StatusScreen() }
    composable(Routes.TAG) { entry ->
        com.stickers.bank.ui.featured.TagResultsScreen(nav, android.net.Uri.decode(entry.arguments?.getString("tag").orEmpty()))
    }
    composable(Routes.TRENDING) { entry ->
        com.stickers.bank.ui.featured.TrendingResultsScreen(nav, entry.arguments?.getString("id").orEmpty())
    }
    composable(Routes.TRENDING_ALL) { com.stickers.bank.ui.featured.TrendingAllScreen(nav) }
    composable(Routes.TEMPLATE_CATEGORY) { entry ->
        com.stickers.bank.ui.templates.TemplateCategoryScreen(
            nav,
            android.net.Uri.decode(entry.arguments?.getString("name").orEmpty()),
        )
    }
    composable(Routes.MY_STICKERS) { com.stickers.bank.ui.settings.MyStickersScreen(nav) }
    composable(
        Routes.EDITOR,
        arguments = listOf(
            navArgument("stickerId") { type = NavType.StringType; defaultValue = "" },
            navArgument("animated") { type = NavType.StringType; defaultValue = "false" },
        ),
    ) { entry ->
        EditorScreen(
            nav,
            stickerId = entry.arguments?.getString("stickerId").orEmpty().ifBlank { null },
            animated = entry.arguments?.getString("animated") == "true",
        )
    }
    composable(Routes.TEMPLATE_EDITOR) { entry ->
        com.stickers.bank.ui.templates.CanvasTemplateEditorScreen(
            nav,
            entry.arguments?.getString("templateId").orEmpty(),
        )
    }
}

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.FEATURED) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The Analytics name for each destination (tabs use the names shown in the bottom nav). */
private fun analyticsScreenName(route: String?): String? = when (route) {
    Routes.FEATURED -> "vibe"
    Routes.CANVAS_TEMPLATES -> "canvas"
    Routes.NEW_ARRIVAL -> "freshDrops"
    Routes.BANK -> "bank"
    Routes.SETTINGS -> "settings"
    Routes.STATUS -> "status"
    Routes.CATEGORY -> "category"
    Routes.COLLECTION -> "collection"
    Routes.EDITOR -> "stickerEditor"
    Routes.TEMPLATE_EDITOR -> "canvasEditor"
    Routes.TEMPLATE_CATEGORY -> "templateCategory"
    Routes.BANK_DETAIL -> "bankDetail"
    Routes.TAG -> "tagPage"
    Routes.TRENDING -> "trendingCard"
    Routes.TRENDING_ALL -> "trendingAll"
    Routes.FAVORITES -> "favorites"
    Routes.MY_STICKERS -> "myStickers"
    else -> null
}
