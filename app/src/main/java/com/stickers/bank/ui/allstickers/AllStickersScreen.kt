package com.stickers.bank.ui.allstickers

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.AddToBankDialog
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.SelectableStickerGrid
import com.stickers.bank.ui.components.SelectionActionBar
import com.stickers.bank.ui.components.rememberStickerSelection
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.rememberGifShare
import com.stickers.bank.ui.rememberPackExporter
import com.stickers.bank.ui.rememberTelegramPackShare
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.util.StickerTime

/**
 * NEW ARRIVAL — only stickers uploaded in the last [StickerTime.NEW_ARRIVAL_DAYS] calendar
 * days. The cutoff is snapped to midnight, so the list only rolls over at 12am (the upload
 * *time* never affects whether a sticker is here — only its date). Each sticker shows its
 * upload date and time.
 */
@Composable
fun AllStickersScreen(nav: NavHostController) {
    val context = LocalContext.current
    val catalogState by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val loading by ServiceLocator.catalogRepository.loading.collectAsStateSafe()

    // NEW ARRIVAL shows only admin-uploaded (catalog) stickers — never the user's own
    // creations from the editor. Those live in Settings › My Stickers.
    val all = remember(catalogState) {
        catalogState.allStickers().distinctBy { it.id }
    }
    val categoryTags = remember(catalogState) { catalogState.categories.map { it.name.lowercase() } }

    // The same sort/filter as Featured (from the top-bar button) applies here too, on top
    // of the last-N-days window (snapped to midnight).
    val filter by ServiceLocator.filterPreferences.filter.collectAsStateSafe()
    val cutoff = remember(all) { StickerTime.newArrivalCutoff() }
    val recent = remember(all, cutoff, filter, categoryTags) {
        val catInclude = filter.includeTags.filter { it in categoryTags }
        val tagInclude = filter.includeTags.filter { it !in categoryTags }
        all.filter { s ->
            s.createdAt >= cutoff &&
                filter.allowsType(s.animated) &&
                (catInclude.isEmpty() || catInclude.any { it in s.tags }) &&
                (tagInclude.isEmpty() || tagInclude.any { it in s.tags }) &&
                filter.excludeTags.none { it in s.tags }
        }.sortedWith(filter.sort.comparator())
    }

    val parentNames = remember(catalogState) { catalogState.stickerParentNames() }
    var detail by remember { mutableStateOf<Sticker?>(null) }
    val selection = rememberStickerSelection()
    val gridState = com.stickers.bank.ui.components.rememberSavedGridState("new_arrival")
    var showBankDialog by remember { mutableStateOf(false) }
    val exportPack = rememberPackExporter()
    val gifShare = rememberGifShare()
    val telegramSend = rememberTelegramPackShare()

    Column(Modifier.fillMaxSize()) {
        Text(
            "FRESH DROPS",
            color = Gold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
        )
        Text(
            "Uploaded in the last ${StickerTime.NEW_ARRIVAL_DAYS} days · ${recent.size} stickers",
            color = AppOnSurfaceMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                recent.isEmpty() && loading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Gold) }

                recent.isEmpty() ->
                    // Centred in what's visible above the floating nav, not in the full height
                    // that runs down behind it (which sat the text noticeably low).
                    EmptyState(
                        "NO NEW VIBES… YET 👀",
                        "Fresh stickers are loading.\nMeanwhile, go raid VIBE STICKERS 🔥",
                        Modifier.padding(bottom = com.stickers.bank.ui.components.SbBottomNavClearance),
                    )

                else -> SelectableStickerGrid(
                    stickers = recent,
                    selection = selection,
                    onStickerClick = { detail = it },
                    gridState = gridState,
                    // Where each one came from (its collection, e.g. "xyz") — more useful than
                    // its upload time, since everything here is recent anyway.
                    labelFor = { parentNames[it.id] },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 100.dp),
                )
            }
        }

        if (selection.active) {
            val chosen = recent.filter { it.id in selection.selected }
            // Only legacy .mp4 clips use WhatsApp's GIF flow; animated-webp stickers install
            // as a real (animated) sticker pack, same path as static.
            val allVideo = chosen.isNotEmpty() && chosen.all { it.isVideoClip }
            // Lifted above the floating nav, which would otherwise cover it.
            androidx.compose.foundation.layout.Box(
                Modifier.padding(bottom = com.stickers.bank.ui.components.SbBottomNavClearance),
            ) {
                SelectionActionBar(
                    count = selection.selected.size,
                    onAddToBank = { showBankDialog = true },
                    onWhatsApp = {
                        if (allVideo) gifShare(chosen) else exportPack("Fresh Drops", chosen)
                    },
                    onTelegram = { telegramSend("Fresh Drops", chosen) },
                    whatsAppLabel = if (allVideo) "GIF" else "WhatsApp",
                    onClose = { selection.clear() },
                )
            }
        }
    }

    if (showBankDialog) {
        AddToBankDialog(
            count = selection.selected.size,
            onDismiss = { showBankDialog = false },
            onAdd = { bank ->
                val chosen = recent.filter { it.id in selection.selected }
                chosen.forEach { ServiceLocator.bankRepository.addSticker(bank.id, it) }
                Toast.makeText(context, "Added ${chosen.size} to ${bank.name}", Toast.LENGTH_SHORT).show()
                selection.clear(); showBankDialog = false
            },
        )
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = recent,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }
}
