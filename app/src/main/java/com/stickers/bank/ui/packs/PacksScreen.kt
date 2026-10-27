package com.stickers.bank.ui.packs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.stickers.bank.ui.components.SelectableStickerGrid
import com.stickers.bank.ui.components.SelectionActionBar
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
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import kotlinx.coroutines.launch

/**
 * PACKS — the admin's ready-made packs, opened from the Packs button on Featured. Two tabs like
 * the search results — Stickers and Animations — each a list of pack cards in the Bank tab's
 * card style. A pack whose stickers have all since been deleted isn't shown.
 */
@Composable
fun PacksScreen(nav: NavHostController) {
    val packs by ServiceLocator.catalogRepository.packs.collectAsStateSafe()
    val allStickers by ServiceLocator.catalogRepository.allStickersFlat.collectAsStateSafe()
    val loading by ServiceLocator.catalogRepository.loading.collectAsStateSafe()
    val resolved = remember(packs, allStickers) {
        packs.map { it to ServiceLocator.catalogRepository.stickersForIds(it.stickerIds) }
            .filter { it.second.isNotEmpty() }
    }
    val still = resolved.filter { !it.first.animated }
    val animated = resolved.filter { it.first.animated }
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Packs", { nav.popBackStack() })
        SearchKindTabs(pager.currentPage, still.size, animated.size) { page ->
            scope.launch { pager.animateScrollToPage(page) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val list = if (page == 0) still else animated
            when {
                list.isEmpty() && (loading || (packs.isNotEmpty() && allStickers.isEmpty())) ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Gold) }
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
                        PackCard(pack, stickers) { nav.navigate(Routes.pack(pack.id)) }
                    }
                }
            }
        }
    }
}

/** A pack, drawn like a Bank card: name and count, then the first five stickers in wells. */
@Composable
private fun PackCard(pack: Pack, stickers: List<Sticker>, onOpen: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 156.dp)
            .neuRaised(cornerRadius = 16.dp, elevation = 7.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pack.animated) {
                Icon(Icons.Rounded.Movie, "Animated", tint = Gold, modifier = Modifier.padding(end = 6.dp).size(18.dp))
            }
            Text(
                pack.name, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Text("${stickers.size}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stickers.take(5).forEach { s ->
                Box(
                    Modifier.weight(1f).aspectRatio(1f).neuInset(cornerRadius = 10.dp, elevation = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = s.coilModel(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp), // clear of the well's inner shadow
                    )
                }
            }
            repeat(5 - stickers.take(5).size) { Box(Modifier.weight(1f)) }
        }
    }
}

/**
 * One pack's stickers — the whole pack to WhatsApp or a bank from the bottom bar, or long-press
 * to pick some, the same as a category's own page.
 */
@Composable
fun PackDetailScreen(nav: NavHostController, packId: String) {
    val context = LocalContext.current
    val packs by ServiceLocator.catalogRepository.packs.collectAsStateSafe()
    val allStickers by ServiceLocator.catalogRepository.allStickersFlat.collectAsStateSafe()
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
        Text(
            "${stickers.size} ${if (pack?.animated == true) "animations" else "stickers"}",
            color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
        )
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
