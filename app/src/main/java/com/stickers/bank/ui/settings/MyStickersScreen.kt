package com.stickers.bank.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickers.bank.ui.components.PreparingPopup
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.util.StickerBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.AddToBankDialog
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.components.SelectableStickerGrid
import com.stickers.bank.ui.components.SelectionActionBar
import com.stickers.bank.ui.components.rememberStickerSelection
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.rememberGifShare
import com.stickers.bank.ui.rememberPackExporter
import com.stickers.bank.ui.rememberTelegramPackShare

/**
 * MY STICKERS — the auto-folder of everything the user made in the editor. Each tile shows the
 * sticker plus its name (caption). "Add all to WhatsApp" sends the lot; long-press to
 * multi-select and add a batch to a bank or export; tap one to open the detail sheet (share /
 * add to bank / edit / delete). Whatever leaves here for WhatsApp or a bank carries the small
 * Stickers Bank badge (see StickerBadge) — the stickers here stay clean.
 */
@Composable
fun MyStickersScreen(nav: NavHostController) {
    val context = LocalContext.current
    val custom by ServiceLocator.customStickerRepository.stickers.collectAsStateSafe()
    val ordered = remember(custom) { custom.reversed() }

    var detail by remember { mutableStateOf<Sticker?>(null) }
    val selection = rememberStickerSelection()
    val gridState = com.stickers.bank.ui.components.rememberSavedGridState("my_stickers_folder")
    var showBankDialog by remember { mutableStateOf(false) }
    val exportPack = rememberPackExporter()
    val gifShare = rememberGifShare()
    val telegramSend = rememberTelegramPackShare()
    val scope = rememberCoroutineScope()
    var badging by remember { mutableStateOf(false) }
    if (badging) PreparingPopup("Adding your stickers…", "Putting the Stickers Bank logo on them")

    /** WhatsApp for these (badged by the exporter itself); old .mp4 clips go as GIFs. */
    fun toWhatsApp(chosen: List<Sticker>) {
        if (chosen.isNotEmpty() && chosen.all { it.isVideoClip }) gifShare(chosen) else exportPack("My Stickers", chosen)
    }

    Column(Modifier.fillMaxSize()) {
        SbBackHeader("My Stickers", { if (selection.active) selection.clear() else nav.popBackStack() })
        if (ordered.isNotEmpty() && !selection.active) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Long-press to pick some, or send them all.",
                    color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.weight(1f),
                )
                Row(
                    Modifier.neuRaised(cornerRadius = 50.dp, elevation = 6.dp, background = Gold)
                        .clip(RoundedCornerShape(50))
                        .clickable { toWhatsApp(ordered) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = OnGold, modifier = Modifier.size(16.dp))
                    Text(
                        "Add all to WhatsApp", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (ordered.isEmpty()) {
                EmptyState(
                    "No stickers yet",
                    "Stickers you make in the Create tab land here. Each one keeps the name you gave it.",
                )
            } else {
                SelectableStickerGrid(
                    stickers = ordered,
                    selection = selection,
                    onStickerClick = { detail = it },
                    gridState = gridState,
                    labelFor = { it.caption.ifBlank { "Untitled" } },
                )
            }
        }

        if (selection.active) {
            val chosen = ordered.filter { it.id in selection.selected }
            val allVideo = chosen.isNotEmpty() && chosen.all { it.isVideoClip }
            SelectionActionBar(
                count = selection.selected.size,
                onAddToBank = { showBankDialog = true },
                onWhatsApp = { toWhatsApp(chosen) },
                onTelegram = { telegramSend("My Stickers", chosen) },
                whatsAppLabel = if (allVideo) "GIF" else "WhatsApp",
                onClose = { selection.clear() },
            )
        }
    }

    if (showBankDialog) {
        AddToBankDialog(
            count = selection.selected.size,
            onDismiss = { showBankDialog = false },
            onAdd = { bank ->
                val chosen = ordered.filter { it.id in selection.selected }
                selection.clear(); showBankDialog = false
                scope.launch {
                    // The bank gets the badged copies (same ids), made off the main thread.
                    badging = true
                    val stickers = withContext(Dispatchers.IO) { StickerBadge.branded(context, chosen) }
                    badging = false
                    stickers.forEach { ServiceLocator.bankRepository.addSticker(bank.id, it) }
                    Toast.makeText(context, "Added ${stickers.size} to ${bank.name}", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = ordered,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
            onRemove = { st -> ServiceLocator.customStickerRepository.remove(st.id) },
        )
    }
}
