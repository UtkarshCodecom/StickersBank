package com.stickers.bank.ui.status

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.export.StickerExporter
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.SbBottomNavClearance
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * STATUS = WhatsApp status saver: the statuses the user has watched in WhatsApp (or WhatsApp
 * Business), each savable to the gallery. Reading them needs a one-time folder grant — see
 * [StatusStore] for why it's a folder pick rather than a storage permission.
 */
@Composable
fun StatusScreen() {
    val context = LocalContext.current
    val store = remember { StatusStore(context) }
    val installed = remember {
        val pkgs = StickerExporter.whatsAppPackages(context)
        StatusSource.entries.filter { it.pkg in pkgs }
    }
    var source by rememberSaveable { mutableStateOf(installed.firstOrNull() ?: StatusSource.WHATSAPP) }
    var tree by remember(source) { mutableStateOf(store.grantedTree(source)) }
    var items by remember(source) { mutableStateOf<List<StatusItem>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var wrongFolder by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(store.savedIds()) }
    var preview by remember { mutableStateOf<StatusItem?>(null) }
    val scope = rememberCoroutineScope()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked ->
        if (picked == null) return@rememberLauncherForActivityResult
        if (store.isStatusFolder(picked)) {
            store.keepGrant(source, picked)
            tree = picked
            wrongFolder = false
            reload++
        } else {
            wrongFolder = true
        }
    }

    fun doSave(item: StatusItem) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { store.save(item) }
            if (ok) {
                saved = saved + item.id
                ServiceLocator.analytics.statusSaved()
            }
            Toast.makeText(context, if (ok) "Saved to your gallery" else "Couldn't save this status", Toast.LENGTH_SHORT).show()
        }
    }

    // Android 9 and older only: writing to the public gallery folder needs this, asked on first save.
    var pendingSave by remember { mutableStateOf<StatusItem?>(null) }
    val askWrite = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val item = pendingSave
        pendingSave = null
        if (granted && item != null) doSave(item)
        else if (!granted) Toast.makeText(context, "Storage permission is needed to save on this phone.", Toast.LENGTH_SHORT).show()
    }

    fun save(item: StatusItem) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingSave = item
            askWrite.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        doSave(item)
    }

    // Statuses come and go (WhatsApp clears them after a day), so re-read on every return here.
    LifecycleResumeEffect(Unit) {
        reload++
        onPauseOrDispose { }
    }
    LaunchedEffect(tree, reload) {
        val t = tree ?: return@LaunchedEffect
        items = withContext(Dispatchers.IO) { store.list(t) }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "STATUS SAVER", color = Gold, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
        )
        Text(
            "Photos, videos and voice statuses you've opened in WhatsApp — save any of them. " +
                "WhatsApp only keeps the ones you've watched, so open a status there first.",
            color = AppOnSurfaceMuted, fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
        )
        if (installed.size > 1) {
            Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                installed.forEach { s -> SourceChip(s.label, s == source) { source = s } }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val list = items
            when {
                installed.isEmpty() -> EmptyState(
                    "WhatsApp isn't installed",
                    "Install WhatsApp and watch some statuses — they'll show up here to save.",
                    Modifier.padding(bottom = SbBottomNavClearance),
                )
                tree == null -> AccessCard(source, wrongFolder) { pickFolder.launch(store.initialUri(source)) }
                list == null -> Box(Modifier.fillMaxSize().padding(bottom = SbBottomNavClearance), Alignment.Center) {
                    CircularProgressIndicator(color = Gold)
                }
                list.isEmpty() -> EmptyState(
                    "No statuses yet",
                    "Watch some statuses in ${source.label}, then come back — they'll be here to save.",
                    Modifier.padding(bottom = SbBottomNavClearance),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { item ->
                        StatusTile(item, item.id in saved, onOpen = { preview = item }, onSave = { save(item) })
                    }
                }
            }
        }
    }

    preview?.let { p -> StatusPreview(p, p.id in saved, onSave = { save(p) }, onDismiss = { preview = null }) }
}

/** WhatsApp / WP Business switch (only shown when both are installed). Same press/float as the
 * search tabs: the chosen one is pressed in, the other raised. */
@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(46.dp)
            .then(
                if (selected) Modifier.padding(6.dp).neuInset(cornerRadius = 17.dp, elevation = 5.dp)
                else Modifier.neuRaised(cornerRadius = 17.dp, elevation = 6.dp).padding(6.dp),
            )
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Gold else AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Explains the one-time folder grant and opens the picker straight onto the Status folder. */
@Composable
private fun AccessCard(source: StatusSource, wrongFolder: Boolean, onAllow: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            .neuRaised(cornerRadius = 26.dp, elevation = 9.dp)
            .padding(horizontal = 30.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(66.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FolderOpen, null, tint = OnGold, modifier = Modifier.size(32.dp))
        }
        Text(
            "Allow access to statuses", color = AppOnSurface, fontWeight = FontWeight.Black, fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Android asks once before an app can see ${source.label}'s Status folder. " +
                "On the next screen, tap “Use this folder”, then “Allow”.",
            color = AppOnSurfaceMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (wrongFolder) {
            Text(
                "That wasn't the Status folder. Tap below and choose “Use this folder” straight away, without opening another folder.",
                color = Danger, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Box(
            Modifier.padding(top = 20.dp).fillMaxWidth().height(50.dp)
                .clip(RoundedCornerShape(50)).background(Gold).clickable(onClick = onAllow),
            contentAlignment = Alignment.Center,
        ) { Text("Allow access", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Text(
            "Nothing is uploaded — statuses stay on your phone.",
            color = AppOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun StatusTile(item: StatusItem, saved: Boolean, onOpen: () -> Unit, onSave: () -> Unit) {
    Box(
        Modifier.aspectRatio(0.8f)
            .neuRaised(cornerRadius = 14.dp, elevation = 6.dp)
            .padding(6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111114))
            .clickable(onClick = onOpen),
    ) {
        if (item.isAudio) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.GraphicEq, null, tint = Gold, modifier = Modifier.size(38.dp))
                Text("Voice", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            AsyncImage(model = item.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (item.isVideo) {
            Icon(Icons.Rounded.PlayCircle, "Video", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.align(Alignment.Center).size(34.dp))
        }
        SaveButton(saved, Modifier.align(Alignment.BottomEnd).padding(6.dp), onSave)
    }
}

@Composable
private fun SaveButton(saved: Boolean, modifier: Modifier, onSave: () -> Unit) {
    Box(
        modifier.size(30.dp).clip(CircleShape)
            .background(if (saved) Color(0xFF2E7D32) else Gold)
            .clickable(enabled = !saved, onClick = onSave),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (saved) Icons.Rounded.Check else Icons.Rounded.Download,
            if (saved) "Saved" else "Save",
            tint = if (saved) Color.White else OnGold, modifier = Modifier.size(18.dp),
        )
    }
}

/** Full-screen look at one status — the photo, or the video playing on a loop — with Save. */
@Composable
private fun StatusPreview(item: StatusItem, saved: Boolean, onSave: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (item.isAudio) {
                Icon(Icons.Rounded.GraphicEq, null, tint = Gold, modifier = Modifier.align(Alignment.Center).size(96.dp))
            }
            if (item.isVideo || item.isAudio) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(item.uri)
                            setOnPreparedListener { it.isLooping = true; start() }
                            setOnErrorListener { _, _, _ -> true }
                        }
                    },
                    onRelease = { it.stopPlayback() },
                    // A voice status has no picture — keep the player out of sight behind the icon.
                    modifier = if (item.isAudio) Modifier.size(1.dp) else Modifier.align(Alignment.Center).fillMaxWidth(),
                )
            } else {
                AsyncImage(model = item.uri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Box(
                Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp).size(40.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
            Box(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp)
                    .fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50))
                    .background(if (saved) Color(0xFF2E7D32) else Gold)
                    .clickable(enabled = !saved, onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (saved) "Saved to gallery" else "Save to gallery",
                    color = if (saved) Color.White else OnGold, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                )
            }
        }
    }
}
