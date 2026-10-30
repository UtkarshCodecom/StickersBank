package com.stickers.bank.ui.packs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Pack
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.AddToBankDialog
import com.stickers.bank.ui.components.BottomActionBar
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.components.SbSearchBar
import com.stickers.bank.ui.components.SelectableStickerGrid
import com.stickers.bank.ui.components.SelectionActionBar
import com.stickers.bank.ui.components.rememberStickerModel
import com.stickers.bank.ui.components.rememberStickerSelection
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.featured.SearchKindTabs
import com.stickers.bank.ui.rememberGifShare
import com.stickers.bank.ui.rememberPackExporter
import com.stickers.bank.ui.rememberTelegramPackShare
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import kotlinx.coroutines.launch

/** The name every admin pack goes out under (shown on each card, like a publisher). */
private const val PACK_PUBLISHER = "Stickers Bank"

/** Every admin pack with its stickers resolved (a pack whose stickers are all gone is dropped). */
@Composable
internal fun rememberResolvedPacks(): List<Pair<Pack, List<Sticker>>> {
    val packs by ServiceLocator.catalogRepository.packs.collectAsStateSafe()
    val allStickers by ServiceLocator.catalogRepository.allStickersFlat.collectAsStateSafe()
    return remember(packs, allStickers) {
        packs.map { it to ServiceLocator.catalogRepository.stickersForIds(it.stickerIds) }
            .filter { it.second.isNotEmpty() }
    }
}

/** A pack matches a search if every word is in its name or in one of its stickers' tags. */
internal fun Pair<Pack, List<Sticker>>.matchesQuery(query: String): Boolean {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return true
    val name = first.name.lowercase()
    return words.all { w -> w in name || second.any { s -> s.tags.any { it.lowercase().contains(w) } } }
}

/**
 * PACKS — the admin's ready-made packs, opened from the Packs button on Featured. A search box,
 * then two tabs like the search results — Stickers and Animations — each a list of pack cards,
 * the user's favourite packs first.
 */
@Composable
fun PacksScreen(nav: NavHostController) {
    val packs by ServiceLocator.catalogRepository.packs.collectAsStateSafe()
    val allStickers by ServiceLocator.catalogRepository.allStickersFlat.collectAsStateSafe()
    val loading by ServiceLocator.catalogRepository.loading.collectAsStateSafe()
    val favorites by ServiceLocator.favoritesRepository.packIds.collectAsStateSafe()
    val resolved = rememberResolvedPacks()
    var query by remember { mutableStateOf("") }
    val shown = remember(resolved, query, favorites) {
        resolved.filter { it.matchesQuery(query) }.sortedBy { if (it.first.id in favorites) 0 else 1 }
    }
    val still = shown.filter { !it.first.animated }
    val animated = shown.filter { it.first.animated }
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val exportPack = rememberPackExporter()
    val gifShare = rememberGifShare()

    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Packs", { nav.popBackStack() })
        SbSearchBar(
            value = query,
            onChange = { query = it },
            placeholder = "Search packs",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        )
        SearchKindTabs(pager.currentPage, still.size, animated.size) { page ->
            scope.launch { pager.animateScrollToPage(page) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val list = if (page == 0) still else animated
            when {
                list.isEmpty() && query.isBlank() && (loading || (packs.isNotEmpty() && allStickers.isEmpty())) ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Gold) }
                list.isEmpty() && query.isNotBlank() -> EmptyState("No packs match", "Try another word.")
                list.isEmpty() -> EmptyState(
                    if (page == 0) "No sticker packs yet" else "No animated packs yet",
                    "New packs are on the way — check back soon.",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(list, key = { it.first.id }) { (pack, stickers) ->
                        PackCard(
                            pack, stickers,
                            favorite = pack.id in favorites,
                            onToggleFavorite = { ServiceLocator.favoritesRepository.togglePack(pack.id) },
                            onWhatsApp = {
                                if (stickers.all { it.isVideoClip }) gifShare(stickers) else exportPack(pack.name, stickers)
                            },
                            onOpen = { nav.navigate(Routes.pack(pack.id)) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A pack card, in the Bank card style: its thumbnail on the left with the name, "Stickers Bank"
 * and the count beside it (and a heart to favourite it), four stickers as a preview, and an
 * "Add to WhatsApp" button for the whole pack.
 */
@Composable
internal fun PackCard(
    pack: Pack,
    stickers: List<Sticker>,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onWhatsApp: () -> Unit,
    onOpen: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .neuRaised(cornerRadius = 16.dp, elevation = 7.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StickerWell(stickers.firstOrNull(), Modifier.size(62.dp), padding = 7.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(pack.name, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(PACK_PUBLISHER, color = AppOnSurfaceMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    if (pack.animated) Icon(Icons.Rounded.Movie, "Animated", tint = Gold, modifier = Modifier.padding(end = 4.dp).size(14.dp))
                    Text(
                        "${stickers.size} ${if (pack.animated) "animations" else "stickers"}",
                        color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    )
                }
            }
            FavoriteHeart(favorite, onToggleFavorite)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stickers.take(4).forEach { s -> StickerWell(s, Modifier.weight(1f).aspectRatio(1f)) }
            repeat(4 - stickers.take(4).size) { Box(Modifier.weight(1f)) }
        }
        Row(
            Modifier.padding(top = 12.dp).fillMaxWidth().height(44.dp)
                .clip(RoundedCornerShape(50)).background(Gold).clickable(onClick = onWhatsApp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Rounded.Send, null, tint = OnGold, modifier = Modifier.size(16.dp))
            Text("Add to WhatsApp", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * A small pack card for horizontal rows (search results, Favorites): a 2×2 of its stickers, then
 * the name and "Stickers Bank".
 */
@Composable
internal fun PackMiniCard(pack: Pack, stickers: List<Sticker>, width: Dp, onOpen: () -> Unit) {
    Column(
        Modifier.width(width).neuRaised(cornerRadius = 14.dp, elevation = 6.dp)
            .clip(RoundedCornerShape(14.dp)).clickable(onClick = onOpen).padding(10.dp),
    ) {
        stickers.take(4).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { s -> StickerWell(s, Modifier.weight(1f).aspectRatio(1f), padding = 5.dp) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
        Text(pack.name, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "$PACK_PUBLISHER · ${stickers.size}", color = AppOnSurfaceMuted, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One sticker in an inset well (or an empty well). */
@Composable
private fun StickerWell(sticker: Sticker?, modifier: Modifier, padding: Dp = 8.dp) {
    Box(modifier.neuInset(cornerRadius = 10.dp, elevation = 4.dp), contentAlignment = Alignment.Center) {
        if (sticker != null) {
            AsyncImage(
                model = rememberStickerModel(sticker),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(padding), // clear of the well's inner shadow
            )
        }
    }
}

@Composable
private fun FavoriteHeart(favorite: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (favorite) "Remove from favourites" else "Add to favourites",
            tint = if (favorite) Gold else AppOnSurfaceMuted, modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * One pack's stickers — the whole pack to WhatsApp or a bank from the bottom bar, or long-press
 * to pick some, the same as a category's own page. The heart favourites the pack.
 */
@Composable
fun PackDetailScreen(nav: NavHostController, packId: String) {
    val context = LocalContext.current
    val packs by ServiceLocator.catalogRepository.packs.collectAsStateSafe()
    val allStickers by ServiceLocator.catalogRepository.allStickersFlat.collectAsStateSafe()
    val favorites by ServiceLocator.favoritesRepository.packIds.collectAsStateSafe()
    val pack = packs.firstOrNull { it.id == packId }
    val stickers = remember(pack, allStickers) {
        pack?.let { ServiceLocator.catalogRepository.stickersForIds(it.stickerIds) } ?: emptyList()
    }
    val name = pack?.name ?: "Pack"

    var detail by remember { mutableStateOf<Sticker?>(null) }
    val selection = rememberStickerSelection()
    var showBankDialog by remember { mutableStateOf(false) }
    var bankTarget by remember { mutableStateOf<List<Sticker>>(emptyList()) }
    val exportPack = rememberPackExporter()
    val gifShare = rememberGifShare()
    val telegramSend = rememberTelegramPackShare()

    /** WhatsApp for these; old .mp4 clips go as GIFs. */
    fun toWhatsApp(chosen: List<Sticker>) {
        if (chosen.isNotEmpty() && chosen.all { it.isVideoClip }) gifShare(chosen) else exportPack(name, chosen)
    }

    Column(Modifier.fillMaxSize()) {
        SbBackHeader(name, { if (selection.active) selection.clear() else nav.popBackStack() })
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$PACK_PUBLISHER · ${stickers.size} ${if (pack?.animated == true) "animations" else "stickers"}",
                color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.weight(1f),
            )
            if (pack != null) FavoriteHeart(pack.id in favorites) { ServiceLocator.favoritesRepository.togglePack(pack.id) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (stickers.isEmpty()) {
                EmptyState("Nothing here yet", "This pack is empty for now.")
            } else {
                SelectableStickerGrid(
                    stickers = stickers,
                    selection = selection,
                    onStickerClick = { detail = it },
                    modifier = Modifier.fillMaxSize(),
                    columns = 3,
                )
            }
        }
        if (selection.active) {
            val chosen = stickers.filter { it.id in selection.selected }
            SelectionActionBar(
                count = selection.selected.size,
                onAddToBank = { bankTarget = chosen; showBankDialog = true },
                onWhatsApp = { toWhatsApp(chosen) },
                onTelegram = { telegramSend(name, chosen) },
                whatsAppLabel = if (chosen.isNotEmpty() && chosen.all { it.isVideoClip }) "GIF" else "WhatsApp",
                onClose = { selection.clear() },
            )
        } else if (stickers.isNotEmpty()) {
            BottomActionBar(
                onBank = { bankTarget = stickers; showBankDialog = true },
                onWhatsApp = { toWhatsApp(stickers) },
            )
        }
    }

    if (showBankDialog) {
        AddToBankDialog(
            count = bankTarget.size,
            onDismiss = { showBankDialog = false },
            onAdd = { bank ->
                bankTarget.forEach { ServiceLocator.bankRepository.addSticker(bank.id, it) }
                Toast.makeText(context, "Added ${bankTarget.size} to ${bank.name}", Toast.LENGTH_SHORT).show()
                selection.clear(); showBankDialog = false
            },
        )
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = stickers,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }
}
