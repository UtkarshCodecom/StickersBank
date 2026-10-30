package com.stickers.bank.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Bank
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.neuClickable
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised

/** Holds the current multi-selection of sticker ids. */
class StickerSelectionState {
    var active by mutableStateOf(false)
    val selected = mutableStateListOf<String>()

    fun clear() { active = false; selected.clear() }

    /**
     * Add without duplicating. `selected` is a list (Compose needs an observable one), and the
     * drag handlers fire many times over the same cell — unguarded adds put the same id in
     * dozens of times, which inflated the "N selected" count and meant a tap had to be
     * repeated once per copy before the sticker actually came unselected.
     */
    fun select(id: String) {
        if (id !in selected) selected.add(id)
        active = true
    }

    fun toggle(id: String) {
        // removeAll, not remove: clears every copy left by an earlier drag.
        if (id in selected) selected.removeAll { it == id } else selected.add(id)
        active = selected.isNotEmpty()
    }
}

@Composable
fun rememberStickerSelection(): StickerSelectionState = remember { StickerSelectionState() }

private fun LazyGridState.itemIndexAt(offset: Offset): Int? =
    layoutInfo.visibleItemsInfo.firstOrNull { item ->
        val x = item.offset.x
        val y = item.offset.y
        offset.x >= x && offset.x < x + item.size.width &&
            offset.y >= y && offset.y < y + item.size.height
    }?.index

/**
 * Sticker grid with tap-to-open and **long-press + drag to multi-select**. Long-press a
 * cell to start a selection, then drag over cells to range-select from the anchor.
 */
@Composable
fun SelectableStickerGrid(
    stickers: List<Sticker>,
    selection: StickerSelectionState,
    onStickerClick: (Sticker) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    /** Hoist this from the screen so scroll position survives navigating away and back. */
    gridState: LazyGridState = rememberLazyGridState(),
    /** Optional caption under each cell (Fresh Drops uses it for the sticker's collection name). */
    labelFor: ((Sticker) -> String?)? = null,
) {
    // Back exits the selection instead of leaving the screen — a selection is local, in-place
    // state the user very likely wants to back out of on its own, not a reason to navigate away.
    BackHandler(enabled = selection.active) { selection.clear() }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(stickers) {
                detectTapGestures(
                    // Must be non-null. With no onLongPress, Compose reports a long hold as a
                    // TAP on release — so the long-press that starts a selection (handled by the
                    // drag detector below) was immediately followed by onTap toggling that same
                    // sticker back off. Dragging masked it, because onDrag consumes the event and
                    // cancels the tap. Claiming the long press here keeps onTap for real taps only.
                    onLongPress = { /* selection start is the drag detector's job */ },
                    onTap = { offset ->
                        val idx = gridState.itemIndexAt(offset) ?: return@detectTapGestures
                        val s = stickers.getOrNull(idx) ?: return@detectTapGestures
                        if (selection.active) selection.toggle(s.id) else onStickerClick(s)
                    },
                )
            }
            // When already selecting, a plain slide adds every cell it passes over. Otherwise a
            // long-press starts a selection and dragging range-selects from the anchor.
            .pointerInput(stickers, selection.active) {
                var anchor = -1
                if (selection.active) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val idx = gridState.itemIndexAt(offset) ?: -1
                            if (idx in stickers.indices) { anchor = idx; selection.select(stickers[idx].id) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val idx = gridState.itemIndexAt(change.position) ?: -1
                            if (idx in stickers.indices) selection.select(stickers[idx].id)
                        },
                        onDragEnd = { anchor = -1 },
                        onDragCancel = { anchor = -1 },
                    )
                } else {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val idx = gridState.itemIndexAt(offset) ?: -1
                            if (idx in stickers.indices) {
                                anchor = idx
                                selection.active = true
                                selection.selected.clear()
                                selection.selected.add(stickers[idx].id)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val idx = gridState.itemIndexAt(change.position) ?: -1
                            if (idx in stickers.indices && anchor >= 0) {
                                val range = minOf(anchor, idx)..maxOf(anchor, idx)
                                selection.selected.clear()
                                range.forEach { selection.selected.add(stickers[it].id) }
                            }
                        },
                        onDragEnd = { anchor = -1 },
                        onDragCancel = { anchor = -1 },
                    )
                }
            },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(stickers, key = { it.id }) { s ->
            val isSelected = s.id in selection.selected
            Column {
                Box(
                    // Raised (floating) by default like every other sticker cell; only dents
                    // inward once it's actually part of the selection, so "picked" reads the
                    // same way a held-down press does elsewhere.
                    Modifier.aspectRatio(1f).then(
                        if (isSelected) Modifier.neuInset(cornerRadius = 12.dp, elevation = 5.dp)
                        else Modifier.neuRaised(cornerRadius = 12.dp, elevation = 5.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = rememberStickerModel(s),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        // Clear of the raised surface (painted 5dp inside the cell) and of the
                        // pressed state's inner shadows, so both read round every sticker.
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                    )
                    if (selection.active) {
                        // A strong dark shade (fading in, not snapping) so picked stickers stand
                        // out at a glance across the whole grid — the old light tint was too
                        // faint to tell which ones were chosen.
                        val shade by animateFloatAsState(if (isSelected) 0.55f else 0f, tween(160), label = "selectedShade")
                        if (shade > 0f) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = shade)))
                        }
                        if (isSelected) {
                            // White disc behind the check so it reads on the dark shade and on
                            // any sticker colour underneath.
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                            }
                        } else {
                            Icon(
                                Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = AppOnSurface.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp),
                            )
                        }
                    }
                }
                labelFor?.invoke(s)?.let { label ->
                    Text(
                        label,
                        color = AppOnSurfaceMuted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                    )
                }
            }
        }
    }
}

/** Bottom bar shown while a selection is active: Bank + WhatsApp + Telegram. */
@Composable
fun SelectionActionBar(
    count: Int,
    onAddToBank: () -> Unit,
    onWhatsApp: () -> Unit,
    onClose: () -> Unit,
    onTelegram: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    whatsAppLabel: String = "WhatsApp",
) {
    val enabled = count > 0
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(AppSurface)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Close, "Cancel", tint = AppOnSurfaceMuted,
            modifier = Modifier
                .clip(CircleShape).clickable(onClick = onClose).padding(8.dp).size(22.dp),
        )
        Text(
            "$count",
            color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp,
            modifier = Modifier.padding(start = 2.dp),
        )
        Text(
            "selected",
            color = AppOnSurfaceMuted, fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, end = 10.dp),
        )
        // Buttons fill the remaining width equally so they're big and reach the edge.
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SelBtn("Bank", Icons.Rounded.Add, enabled = enabled, modifier = Modifier.weight(1f), onClick = onAddToBank)
            if (onTelegram != null) {
                SelBtn("Telegram", Icons.AutoMirrored.Rounded.Send, enabled = enabled, accent = TelegramBlue, modifier = Modifier.weight(1f), onClick = onTelegram)
            }
            SelBtn(whatsAppLabel, Icons.AutoMirrored.Rounded.Send, enabled = enabled, accent = Gold, modifier = Modifier.weight(1f), onClick = onWhatsApp)
            if (onDelete != null) {
                SelBtn("Delete", Icons.Rounded.Delete, enabled = enabled, accent = Danger, modifier = Modifier.weight(1f), onClick = onDelete)
            }
        }
    }
}

private val TelegramBlue = Color(0xFF29A9EB)

@Composable
private fun SelBtn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: () -> Unit,
) {
    val bg = if (!enabled) AppSurfaceVariant else accent ?: AppSurfaceVariant
    val fg = when {
        !enabled -> AppOnSurfaceMuted
        accent != null -> AppBackground
        else -> AppOnSurface
    }
    Column(
        modifier
            .neuClickable(cornerRadius = 14.dp, elevation = 6.dp, background = bg, enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
    }
}

/**
 * Pick an EXISTING bank to add the selection to, or create a new one — so users don't end
 * up with a new bank every time.
 */
@Composable
fun AddToBankDialog(
    count: Int,
    onDismiss: () -> Unit,
    onAdd: (Bank) -> Unit,
) {
    val banks by ServiceLocator.bankRepository.banks.collectAsStateSafe()
    var creating by remember { mutableStateOf(banks.isEmpty()) }
    var newName by remember { mutableStateOf("") }
    val duplicate = newName.isNotBlank() && ServiceLocator.bankRepository.bankNameExists(newName)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Add $count sticker${if (count == 1) "" else "s"} to…", color = AppOnSurface) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                banks.forEach { bank ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            .neuClickable(cornerRadius = 10.dp, elevation = 5.dp, onClick = { onAdd(bank) })
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(bank.name, color = AppOnSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${bank.stickers.size}", color = AppOnSurfaceMuted, fontSize = 12.sp)
                    }
                }
                if (creating) {
                    TextField(
                        value = newName, onValueChange = { newName = it }, singleLine = true,
                        isError = duplicate,
                        placeholder = { Text("New bank name", color = AppOnSurfaceMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppSurfaceVariant, unfocusedContainerColor = AppSurfaceVariant,
                            focusedIndicatorColor = Gold, unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Gold, focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (duplicate) {
                        Text(
                            "A bank with this name already exists.",
                            color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth()
                            .neuClickable(cornerRadius = 10.dp, elevation = 5.dp, onClick = { creating = true })
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Gold, modifier = Modifier.size(18.dp))
                        Text("  New bank", color = Gold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                val ok = newName.isNotBlank() && !duplicate
                TextButton(
                    enabled = ok,
                    onClick = { onAdd(ServiceLocator.bankRepository.createBank(newName.trim())) },
                ) { Text("Create & add", color = if (ok) Gold else AppOnSurfaceMuted, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) } },
    )
}

/**
 * Bank-name dialog. If [allowExisting] and the name already exists, adding merges into
 * that bank; otherwise a duplicate name is rejected inline.
 */
@Composable
fun BankNameDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    allowExisting: Boolean = true,
) {
    var name by remember { mutableStateOf("") }
    val exists = name.isNotBlank() && ServiceLocator.bankRepository.bankNameExists(name)
    val blockedDuplicate = exists && !allowExisting
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text(title, color = AppOnSurface) },
        text = {
            Column {
                TextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    placeholder = { Text("Bank name", color = AppOnSurfaceMuted) },
                    isError = blockedDuplicate,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppSurfaceVariant, unfocusedContainerColor = AppSurfaceVariant,
                        focusedIndicatorColor = Gold, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Gold, focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface,
                    ),
                )
                if (exists) {
                    Text(
                        if (allowExisting) "Will be added to your existing “${name.trim()}” bank."
                        else "A bank named “${name.trim()}” already exists.",
                        color = if (allowExisting) AppOnSurfaceMuted else Danger,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !blockedDuplicate,
                onClick = { onConfirm(name.trim()) },
            ) { Text("Save", color = if (name.isNotBlank() && !blockedDuplicate) Gold else AppOnSurfaceMuted, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) } },
    )
}
