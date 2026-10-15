package com.stickers.bank.ui.featured

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Category
import com.stickers.bank.data.model.Collection
import com.stickers.bank.data.model.Group
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.BottomActionBar
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.components.StickerThumb
import com.stickers.bank.ui.components.checkerboard
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.rememberBankAdder
import com.stickers.bank.ui.rememberPackExporter
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised

private fun Group.flatStickers(): List<Sticker> = stickers + collections.flatMap { it.stickers }
private fun Category.flatStickers(): List<Sticker> = stickers + groups.flatMap { it.flatStickers() }

@Composable
fun CategoryScreen(nav: NavHostController, categoryId: String) {
    LaunchedEffect(categoryId) { ServiceLocator.analytics.categoryOpened(categoryId) }
    val catalog by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val category = catalog.findCategory(categoryId)
    if (category == null) {
        SbBackHeader("Not found", { nav.popBackStack() })
        return
    }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    // Persisted so returning from a collection keeps the group tab you were on (e.g. Hollywood),
    // not snapping back to the first group.
    var selectedGroup by rememberSaveable(categoryId) { mutableStateOf(0) }
    var detail by remember { mutableStateOf<Sticker?>(null) }
    // Persisted so scroll position survives opening a collection/sticker and coming back.
    val stickerGridState = com.stickers.bank.ui.components.rememberSavedGridState("cat_stickers_$categoryId")
    // Stickers placed directly on a leaf category/group (no Collections underneath) skipped
    // multi-select entirely — only CollectionScreen's grid had it. Same selection state, wired
    // the same way, so a leaf category behaves exactly like a collection.
    val selection = com.stickers.bank.ui.components.rememberStickerSelection()

    val exportPack = rememberPackExporter()
    val context = androidx.compose.ui.platform.LocalContext.current
    var bankTarget by remember { mutableStateOf<List<Sticker>>(emptyList()) }
    var showBankDialog by remember { mutableStateOf(false) }

    val categoryBg = remember(category.bgColor, category.bgImage) {
        if (category.bgColor == null && category.bgImage == null) null
        else com.stickers.bank.data.model.PageBackground(category.bgColor, category.bgImage)
    }

    // Painted at the app root (behind the status bar and header) rather than inside this
    // screen's own bounds, so a category's backdrop covers the full screen like a tab's does.
    com.stickers.bank.ui.ProvidePageBackdrop(categoryBg)

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        SbBackHeader(category.name, { if (selection.active) selection.clear() else nav.popBackStack() })
        // Search is hidden behind a bottom-right button to save space; it drops in only when opened.
        if (searchOpen) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchField(query, { query = it }, hint = category.name.uppercase(), modifier = Modifier.weight(1f))
                Box(
                    Modifier.padding(start = 8.dp).size(44.dp).neuRaised(cornerRadius = 22.dp, elevation = 6.dp).clip(CircleShape)
                        .clickable { searchOpen = false; query = "" },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Close search", tint = AppOnSurface, modifier = Modifier.size(20.dp)) }
            }
        }

        val group = if (category.isLeaf) null
        else category.groups.getOrElse(selectedGroup) { category.groups.first() }
        val currentStickers = when {
            category.isLeaf -> category.stickers
            group!!.isLeaf -> group.stickers
            else -> group.flatStickers()
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (category.isLeaf) {
                // No Types under this category at all — stickers placed directly on it are
                // the only content, shown exactly like a leaf always has been.
                com.stickers.bank.ui.components.SelectableStickerGrid(
                    stickers = category.stickers.filter { it.matches(query) },
                    selection = selection,
                    onStickerClick = { detail = it },
                    gridState = stickerGridState,
                    columns = 3,
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    // Stickers placed directly on the CATEGORY (skipping Type & Name) show as a
                    // horizontal strip above the Type tabs — doesn't require the category to be
                    // a leaf, so both can coexist.
                    if (category.stickers.isNotEmpty()) {
                        DirectStickerStrip(category.stickers.filter { it.matches(query) }) { detail = it }
                    }
                    GroupTabs(category.groups, selectedGroup) { selectedGroup = it }
                    if (group!!.isLeaf) {
                        com.stickers.bank.ui.components.SelectableStickerGrid(
                            stickers = group.stickers.filter { it.matches(query) },
                            selection = selection,
                            onStickerClick = { detail = it },
                            gridState = stickerGridState,
                            columns = 3,
                        )
                    } else {
                        // Per-group key so each group tab keeps its own collection scroll.
                        val collectionGridState = com.stickers.bank.ui.components
                            .rememberSavedGridState("cat_colls_${categoryId}_${group.id}")
                        Column(Modifier.fillMaxSize()) {
                            // Same idea one level down: stickers placed directly on the GROUP
                            // (skipping Name) show above the Name/collection grid.
                            if (group.stickers.isNotEmpty()) {
                                DirectStickerStrip(group.stickers.filter { it.matches(query) }) { detail = it }
                            }
                            CollectionGrid(collectionGridState, group.collections, query) { coll ->
                                nav.navigate(Routes.collection(category.id, group.id, coll.id))
                            }
                        }
                    }
                }
            }
            // Floating search button (bottom-right), hidden while the search bar is open.
            if (!searchOpen) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(18.dp).size(54.dp)
                        .neuRaised(cornerRadius = 27.dp, elevation = 8.dp, background = Gold).clip(CircleShape)
                        .clickable { searchOpen = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Search, "Search", tint = AppBackground, modifier = Modifier.size(24.dp)) }
            }
        }

        if (selection.active) {
            com.stickers.bank.ui.components.SelectionActionBar(
                count = selection.selected.size,
                onAddToBank = { bankTarget = currentStickers.filter { it.id in selection.selected }; showBankDialog = true },
                onWhatsApp = { exportPack(category.name, currentStickers.filter { it.id in selection.selected }) },
                onClose = { selection.clear() },
            )
        } else {
            BottomActionBar(
                onBank = { bankTarget = currentStickers; showBankDialog = true },
                onWhatsApp = { exportPack(category.name, currentStickers) },
            )
        }
    }
    }

    if (showBankDialog) {
        com.stickers.bank.ui.components.AddToBankDialog(
            count = bankTarget.size,
            onDismiss = { showBankDialog = false },
            onAdd = { bank ->
                bankTarget.forEach { ServiceLocator.bankRepository.addSticker(bank.id, it) }
                android.widget.Toast.makeText(context, "Added ${bankTarget.size} to ${bank.name}", android.widget.Toast.LENGTH_SHORT).show()
                selection.clear(); showBankDialog = false
            },
        )
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = category.flatStickers(),
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }
}

@Composable
fun CollectionScreen(
    nav: NavHostController,
    categoryId: String,
    groupId: String,
    collectionId: String,
) {
    val catalog by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val category = catalog.findCategory(categoryId)
    val group = category?.groups?.firstOrNull { it.id == groupId }
    val collection = group?.collections?.firstOrNull { it.id == collectionId }
    if (collection == null) {
        SbBackHeader("Not found", { nav.popBackStack() })
        return
    }
    var detail by remember { mutableStateOf<Sticker?>(null) }
    val selection = com.stickers.bank.ui.components.rememberStickerSelection()
    val collectionStickerGridState = com.stickers.bank.ui.components.rememberSavedGridState("coll_$collectionId")
    var showBankDialog by remember { mutableStateOf(false) }
    var bankTarget by remember { mutableStateOf<List<Sticker>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val exportPack = rememberPackExporter()
    val gifShare = com.stickers.bank.ui.rememberGifShare()
    val telegramSend = com.stickers.bank.ui.rememberTelegramPackShare()
    // Only legacy .mp4 clips take the GIF path; animated-webp collections export as an
    // animated sticker pack (same route as static).
    val isVideoCollection = collection.stickers.any { it.isVideoClip }

    Column(Modifier.fillMaxSize()) {
        SbBackHeader(collection.name, { if (selection.active) selection.clear() else nav.popBackStack() })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            com.stickers.bank.ui.components.SelectableStickerGrid(
                stickers = collection.stickers,
                selection = selection,
                onStickerClick = { detail = it },
                gridState = collectionStickerGridState,
                columns = 3,
            )
        }
        if (selection.active) {
            com.stickers.bank.ui.components.SelectionActionBar(
                count = selection.selected.size,
                onAddToBank = { bankTarget = collection.stickers.filter { it.id in selection.selected }; showBankDialog = true },
                onWhatsApp = {
                    val chosen = collection.stickers.filter { it.id in selection.selected }
                    if (isVideoCollection) gifShare(chosen)
                    else exportPack(collection.name, chosen)
                },
                onTelegram = {
                    telegramSend(collection.name, collection.stickers.filter { it.id in selection.selected })
                },
                whatsAppLabel = if (isVideoCollection) "GIF" else "WhatsApp",
                onClose = { selection.clear() },
            )
        } else {
            BottomActionBar(
                actionLabel = if (isVideoCollection) "GIF" else "WHATSAPP",
                onBank = { bankTarget = collection.stickers; showBankDialog = true },
                onWhatsApp = {
                    if (isVideoCollection) gifShare(collection.stickers)
                    else exportPack(collection.name, collection.stickers)
                },
            )
        }
    }

    if (showBankDialog) {
        com.stickers.bank.ui.components.AddToBankDialog(
            count = bankTarget.size,
            onDismiss = { showBankDialog = false },
            onAdd = { bank ->
                bankTarget.forEach { ServiceLocator.bankRepository.addSticker(bank.id, it) }
                android.widget.Toast.makeText(context, "Added ${bankTarget.size} to ${bank.name}", android.widget.Toast.LENGTH_SHORT).show()
                selection.clear(); showBankDialog = false
            },
        )
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = collection.stickers,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    // Idle = raised, presses to inset while focused — same as SbSearchBar.
    var focused by remember { mutableStateOf(false) }
    androidx.compose.material3.TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(hint, color = AppOnSurfaceMuted, letterSpacing = 2.sp, fontSize = 13.sp) },
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            cursorColor = Gold,
            focusedTextColor = AppOnSurface,
            unfocusedTextColor = AppOnSurface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focused) Modifier.neuInset(cornerRadius = 12.dp, elevation = 6.dp)
                else Modifier.neuRaised(cornerRadius = 12.dp, elevation = 6.dp),
            )
            .onFocusChanged { focused = it.isFocused },
    )
}

@Composable
private fun GroupTabs(groups: List<Group>, selected: Int, onSelect: (Int) -> Unit) {
    val scroll = rememberScrollState()
    com.stickers.bank.ui.components.HScrollHint(scroll) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEachIndexed { i, g ->
            val sel = i == selected
            // The active tab reads as pressed IN (neuInset) — it's the one currently "held
            // down" — while the rest float above the page (neuRaised), same embossed-button
            // language as everywhere else selection is shown as a physical state, not just a
            // color change.
            Box(
                Modifier
                    .then(
                        if (sel) Modifier.neuInset(cornerRadius = 50.dp, elevation = 5.dp)
                        else Modifier.neuRaised(cornerRadius = 50.dp, elevation = 5.dp),
                    )
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    g.name.uppercase(),
                    color = if (sel) Gold else AppOnSurfaceMuted,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    }
}

/**
 * Stickers placed directly on a Category or Group (admin skipped Type and/or Name entirely) —
 * shown as a horizontal strip so they can sit alongside the Type tabs / collection grid below
 * them, instead of taking over the whole screen the way a true leaf's grid does.
 */
@Composable
private fun DirectStickerStrip(stickers: List<Sticker>, onClick: (Sticker) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            "STICKERS", color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        )
        val scroll = rememberLazyListState()
        LazyRow(
            state = scroll,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rowItems(stickers, key = { it.id }) { s ->
                StickerThumb(sticker = s, onClick = { onClick(s) }, modifier = Modifier.size(84.dp))
            }
        }
    }
}

@Composable
private fun CollectionGrid(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    collections: List<Collection>,
    query: String,
    onClick: (Collection) -> Unit,
) {
    val list = collections.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(list, key = { it.id }) { coll ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // The cover photo (or, with no cover, the collection's first sticker) is inset
                // inside the card rather than filling it, so a band of the card's own surface
                // stays visible around it — that band is what the neumorphic shadow reads
                // against, which is what makes the card look raised.
                val cover = coll.cover?.takeIf { it.isNotBlank() }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatioSquare()
                        .neuRaised(cornerRadius = 14.dp, elevation = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onClick(coll) },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = cover ?: coll.stickers.firstOrNull()?.coilModel(),
                        contentDescription = coll.name,
                        contentScale = if (cover != null) ContentScale.Crop else ContentScale.Fit,
                        // A cover photo gets the same visible-mat framing as the other photo
                        // cards; the no-cover sticker case already had its own breathing room.
                        modifier = Modifier
                            .padding(if (cover != null) 7.dp else 10.dp)
                            .then(if (cover != null) Modifier.clip(RoundedCornerShape(9.dp)) else Modifier)
                            .fillMaxSize(),
                    )
                }
                // The name gets its own background chip (instead of the card).
                Text(
                    coll.name,
                    color = AppOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .neuRaised(cornerRadius = 50.dp, elevation = 5.dp)
                        .clip(RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

private fun Modifier.aspectRatioSquare() = this.aspectRatio(1f)
