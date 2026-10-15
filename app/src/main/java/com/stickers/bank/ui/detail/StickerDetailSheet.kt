package com.stickers.bank.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.components.BankNameDialog
import com.stickers.bank.ui.rememberGifShare
import com.stickers.bank.ui.rememberSingleShare
import com.stickers.bank.ui.rememberSingleStickerToWhatsApp
import com.stickers.bank.ui.rememberTelegramPackShare
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.neuClickable
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import kotlinx.coroutines.flow.StateFlow

val TelegramBlue = Color(0xFF29A9EB)

/** Convenience overload for callers that open the sheet on a single sticker. */
@Composable
fun StickerDetailSheet(
    sticker: Sticker,
    onDismiss: () -> Unit,
    onEdit: (Sticker) -> Unit,
    onRemove: (() -> Unit)? = null,
) = StickerDetailSheet(
    stickers = listOf(sticker),
    initialId = sticker.id,
    onDismiss = onDismiss,
    onEdit = onEdit,
    onRemove = onRemove?.let { { _: Sticker -> it() } },
)

/**
 * Bottom popup for a sticker. It is a [Dialog] (not a ModalBottomSheet) on purpose: it must
 * NOT close on an outside tap or a downward drag — only the ✕/back button or the system back
 * gesture closes it. Swiping left/right pages through [stickers] like a carousel.
 */
@Composable
fun StickerDetailSheet(
    stickers: List<Sticker>,
    initialId: String,
    onDismiss: () -> Unit,
    onEdit: (Sticker) -> Unit,
    onRemove: ((Sticker) -> Unit)? = null,
) {
    if (stickers.isEmpty()) return
    val context = LocalContext.current
    val banks by ServiceLocator.bankRepository.banks.collectAsStateSafe()
    val favoriteIds by ServiceLocator.favoritesRepository.ids.collectAsStateSafe()
    val shareSingle = rememberSingleShare()
    val telegramSend = rememberTelegramPackShare()
    val sendSingle = rememberSingleStickerToWhatsApp()
    val gifShare = rememberGifShare()

    var showBanks by remember { mutableStateOf(false) }
    var showNewBank by remember { mutableStateOf(false) }
    // Snapshot the list at open time so paging stays stable if the source updates.
    val pages = remember { stickers }
    val startPage = remember { pages.indexOfFirst { it.id == initialId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = startPage) { pages.size }
    val current = pages[pagerState.currentPage.coerceIn(0, pages.lastIndex)]
    // Every sticker actually looked at — including ones swiped to inside the sheet.
    LaunchedEffect(current.id) { ServiceLocator.analytics.stickerViewed(current.id) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false, // outside tap must NOT close it
            dismissOnBackPress = true, // system back closes it
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(AppSurface)
                    .navigationBarsPadding()
                    .heightIn(max = 660.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    // Extra bottom padding so the last button clears the gesture / nav bar.
                    .padding(top = 10.dp, bottom = 54.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header: back (in bank view) or a small handle, + a clear close button.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (showBanks) {
                        Box(
                            Modifier.size(38.dp).neuClickable(cornerRadius = 19.dp, elevation = 6.dp) { showBanks = false },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = AppOnSurface, modifier = Modifier.size(19.dp)) }
                    } else {
                        Box(Modifier.size(34.dp))
                    }
                    Box(
                        Modifier.weight(1f).padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(AppSurfaceVariant))
                    }
                    if (!showBanks) {
                        val isFav = current.id in favoriteIds
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            if (isFav) "Remove from favorites" else "Add to favorites",
                            tint = if (isFav) Danger else AppOnSurface,
                            modifier = Modifier.neuRaised(cornerRadius = 17.dp, elevation = 5.dp).clip(CircleShape)
                                .clickable { ServiceLocator.favoritesRepository.toggle(current.id) }
                                .padding(7.dp).size(20.dp),
                        )
                        Box(Modifier.size(8.dp))
                    }
                    Icon(
                        Icons.Rounded.Close, "Close", tint = AppOnSurface,
                        modifier = Modifier.neuRaised(cornerRadius = 17.dp, elevation = 5.dp).clip(CircleShape).clickable(onClick = onDismiss).padding(7.dp).size(20.dp),
                    )
                }

                if (!showBanks) {
                    // Swipeable preview carousel.
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 6.dp),
                    ) { page ->
                        val s = pages[page]
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.fillMaxHeight().aspectRatio(1f).neuInset(cornerRadius = 20.dp, elevation = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Inset just past the well's inner shadow (~12dp at this 7dp depth),
                                // so the pressed-in plate shows round every sticker instead of being
                                // covered by stickers that fill their whole square.
                                val clear = Modifier.fillMaxSize().padding(14.dp)
                                if (s.isVideoClip) {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = { ctx ->
                                            android.widget.VideoView(ctx).apply {
                                                // VideoView paints an opaque backdrop of its own, which
                                                // showed as a dark frame around the clip.
                                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                setVideoURI(android.net.Uri.parse(s.image))
                                                setOnPreparedListener { mp -> mp.isLooping = true; start() }
                                                setOnErrorListener { _, _, _ -> true }
                                            }
                                        },
                                        onRelease = { it.stopPlayback() },
                                        modifier = clear.clip(RoundedCornerShape(10.dp)),
                                    )
                                } else {
                                    AsyncImage(
                                        model = s.coilModel(),
                                        contentDescription = s.caption,
                                        modifier = clear,
                                    )
                                }
                            }
                        }
                    }

                    if (pages.size > 1) {
                        Text(
                            "${pagerState.currentPage + 1} / ${pages.size}  ·  swipe for more",
                            color = AppOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (current.caption.isNotBlank()) {
                        Text(current.caption, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
                    }

                    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Add to bank", Icons.Rounded.Add, Modifier.weight(1f)) { showBanks = true }
                        ActionButton("Edit", Icons.Rounded.Edit, Modifier.weight(1f)) { onDismiss(); onEdit(current) }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton(if (current.isVideoClip) "GIF" else "WhatsApp", Icons.AutoMirrored.Rounded.Send, Modifier.weight(1f), filled = true) {
                            if (current.isVideoClip) {
                                gifShare(listOf(current))
                            } else {
                                // Straight into the running "Stickers Bank" pack in WhatsApp — every
                                // single sticker joins that same pack, now or days later.
                                sendSingle(current)
                            }
                        }
                        ActionButton("Telegram", Icons.AutoMirrored.Rounded.Send, Modifier.weight(1f), accent = TelegramBlue) {
                            telegramSend(current.caption.ifBlank { "Stickers Bank" }, listOf(current))
                        }
                    }
                    SlimButton("Share as image", AppOnSurfaceMuted) { shareSingle(current) }
                    if (onRemove != null) {
                        SlimButton("Remove from bank", Danger) { onDismiss(); onRemove(current) }
                    }
                } else {
                    BankPickerBody(
                        sticker = current,
                        banks = banks,
                        onNewBank = { showNewBank = true },
                    )
                }
            }
        }
    }

    if (showNewBank) {
        BankNameDialog(
            title = "New bank",
            allowExisting = true,
            onDismiss = { showNewBank = false },
            onConfirm = { name ->
                val bank = ServiceLocator.bankRepository.createBank(name)
                ServiceLocator.bankRepository.addSticker(bank.id, current)
                Toast.makeText(context, "Added to ${bank.name}", Toast.LENGTH_SHORT).show()
                showNewBank = false
            },
        )
    }
}

@Composable
private fun ColumnScope.BankPickerBody(
    sticker: Sticker,
    banks: List<com.stickers.bank.data.model.Bank>,
    onNewBank: () -> Unit,
) {
    Text(
        "ADD TO BANK", color = Gold, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp).align(Alignment.Start),
    )
    Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
        banks.forEach { bank ->
            val inBank = bank.stickers.any { it.id == sticker.id }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).neuRaised(cornerRadius = 12.dp, elevation = 6.dp).clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (inBank) ServiceLocator.bankRepository.removeSticker(bank.id, sticker.id)
                        else ServiceLocator.bankRepository.addSticker(bank.id, sticker)
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(bank.name, color = AppOnSurface, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("${bank.stickers.size}", color = AppOnSurfaceMuted, fontSize = 12.sp)
                if (inBank) Icon(Icons.Rounded.Check, "In bank", tint = Gold, modifier = Modifier.padding(start = 8.dp).size(18.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().neuRaised(cornerRadius = 12.dp, elevation = 6.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onNewBank).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = Gold, modifier = Modifier.size(18.dp))
            Text("  New bank", color = Gold, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SlimButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(top = 10.dp).neuRaised(cornerRadius = 14.dp, elevation = 6.dp).clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    accent: Color? = null,
    onClick: () -> Unit,
) {
    val bg = accent ?: if (filled) Gold else AppSurfaceVariant
    val fg = if (accent != null || filled) AppBackground else AppOnSurface
    Column(
        modifier = modifier.neuRaised(cornerRadius = 14.dp, elevation = 6.dp, background = bg).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (accent != null || filled) AppBackground else Gold, modifier = Modifier.size(20.dp))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Small helper so screens can collect a StateFlow into Compose state. */
@Composable
fun <T> StateFlow<T>.collectAsStateSafe(): State<T> = collectAsState(value)
