package com.stickers.bank.ui.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.BottomActionBar
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.components.StickerThumb
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.navigateToTab
import com.stickers.bank.ui.rememberPackExporter
import com.stickers.bank.ui.rememberTelegramPackShare
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.neuRaised

@Composable
fun BankDetailScreen(nav: NavHostController, bankId: String) {
    val banks by ServiceLocator.bankRepository.banks.collectAsStateSafe()
    val bank = banks.firstOrNull { it.id == bankId }
    var detail by remember { mutableStateOf<Sticker?>(null) }
    val gridState = com.stickers.bank.ui.components.rememberSavedGridState("bankdetail_$bankId")

    val exportPack = rememberPackExporter()
    val gifShare = com.stickers.bank.ui.rememberGifShare()
    val telegramShare = rememberTelegramPackShare()

    if (bank == null) {
        Column(Modifier.fillMaxSize()) {
            SbBackHeader("Bank", { nav.popBackStack() })
            EmptyState("Bank not found", "It may have been deleted.")
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        SbBackHeader(bank.name, { nav.popBackStack() })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (bank.stickers.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("This bank is empty", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Browse stickers and add them to fill it.",
                        color = AppOnSurfaceMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
                    )
                    Box(
                        Modifier.size(64.dp).neuRaised(cornerRadius = 32.dp, elevation = 8.dp, background = Gold)
                            .clip(CircleShape)
                            .clickable { nav.navigateToTab(Routes.FEATURED) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, "Browse stickers", tint = AppBackground, modifier = Modifier.size(34.dp)) }
                    Text("Browse stickers", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(bank.stickers, key = { it.id }) { s ->
                        StickerThumb(sticker = s, onClick = { detail = s })
                    }
                }
            }
        }
        // Only legacy .mp4 clips use the GIF flow; animated-webp installs as an animated pack.
        val videoBank = bank.stickers.isNotEmpty() && bank.stickers.all { it.isVideoClip }
        BottomActionBar(
            bankLabel = "TELEGRAM",
            actionLabel = if (videoBank) "GIF" else "WHATSAPP",
            onBank = { telegramShare(bank.name, bank.stickers) },
            onWhatsApp = {
                if (videoBank) gifShare(bank.stickers)
                else exportPack(bank.name, bank.stickers)
            },
        )
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = bank.stickers,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
            onRemove = { st -> ServiceLocator.bankRepository.removeSticker(bank.id, st.id) },
        )
    }
}
