@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.stickers.bank.ui.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.stickers.bank.ui.components.PolygonProgress
import com.stickers.bank.ui.components.PreparingPopup
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppOutline
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AUTO SUBTITLES — pick a video, its speech turns into captions on the phone (Vosk), restyle
 * and fix them, keep / replace / mix the sound, and export an MP4 with the captions burned in
 * (Media3 Transformer) to the gallery. Free and offline once the language model is downloaded.
 */
@Composable
fun SubtitleStudioScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var aspect by remember { mutableStateOf(9f / 16f) }
    var videoHasAudio by remember { mutableStateOf(true) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioName by remember { mutableStateOf("") }
    var audioMode by remember { mutableStateOf(AudioMode.REPLACE) }
    var language by remember { mutableStateOf(CaptionLanguage.ENGLISH) }
    val cues = remember { mutableStateListOf<CaptionCue>() }
    var style by remember { mutableStateOf(CaptionStyle.KARAOKE) }
    var modelProgress by remember { mutableStateOf<Float?>(null) }
    var transcribeProgress by remember { mutableStateOf<Float?>(null) }
    var exportProgress by remember { mutableStateOf<Float?>(null) }
    var askDownload by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var posMs by remember { mutableLongStateOf(0L) }

    val bold = remember { Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val display = remember { runCatching { Typeface.createFromAsset(context.assets, "go3v2.ttf") }.getOrDefault(bold) }

    // ---- Preview players: the video, and (when added) the music alongside it ----
    val player = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }
    val music = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }
    val musicOn = audioUri != null && audioMode != AudioMode.KEEP
    DisposableEffect(Unit) { onDispose { player.release(); music.release() } }
    LaunchedEffect(videoUri) {
        val v = videoUri ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(v)); player.prepare()
    }
    LaunchedEffect(audioUri) {
        val a = audioUri ?: return@LaunchedEffect
        music.setMediaItem(MediaItem.fromUri(a)); music.prepare()
    }
    // The video's own sound is muted while "Replace" is on, so the preview sounds like the export.
    LaunchedEffect(audioMode, audioUri) { player.volume = if (audioUri != null && audioMode == AudioMode.REPLACE) 0f else 1f }
    DisposableEffect(player, musicOn) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { music.playWhenReady = isPlaying && musicOn }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (musicOn) music.seekTo(newPosition.positionMs)
            }
        }
        player.addListener(listener)
        music.playWhenReady = player.isPlaying && musicOn
        onDispose { player.removeListener(listener) }
    }
    // The caption overlay follows the player frame by frame.
    LaunchedEffect(player) { while (true) { withFrameNanos { }; posMs = player.currentPosition } }

    // ---- Picking ----
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val meta = withContext(Dispatchers.IO) { readVideo(context, uri) }
            videoUri = uri
            aspect = meta.first
            videoHasAudio = meta.second
            cues.clear()
            exportedFile = null
        }
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            audioUri = uri
            audioName = queryName(context, uri) ?: "Audio"
            if (audioMode == AudioMode.KEEP) audioMode = AudioMode.REPLACE
        }
    }

    // ---- Captions: speech → words → cues ----
    fun transcribe() {
        val source = when {
            videoUri == null -> return
            videoHasAudio -> videoUri!!
            audioUri != null -> audioUri!!
            else -> { toast("This video has no sound. Add captions by hand, or add an audio file."); return }
        }
        scope.launch {
            transcribeProgress = 0f
            player.pause()
            val words = runCatching { SpeechToText.transcribe(context, source, language) { transcribeProgress = it } }
                .getOrElse { e ->
                    transcribeProgress = null
                    toast(if (e is NoAudioException) e.message!! else "Couldn't read the speech: ${e.message}")
                    return@launch
                }
            transcribeProgress = null
            cues.clear()
            cues.addAll(groupIntoCues(words))
            if (cues.isEmpty()) toast("No speech found. You can add captions by hand.")
        }
    }
    fun generate() {
        if (!SpeechToText.isModelReady(context, language)) askDownload = true else transcribe()
    }
    fun download() {
        scope.launch {
            modelProgress = 0f
            val ok = SpeechToText.downloadModel(context, language) { modelProgress = it }
            modelProgress = null
            if (ok) transcribe() else toast("Couldn't download the speech model. Check your connection and try again.")
        }
    }

    // ---- Export ----
    fun doExport() {
        val v = videoUri ?: return
        scope.launch {
            player.pause()
            exportProgress = 0f
            val stamp = System.currentTimeMillis()
            val out = File(context.cacheDir, "studio/captioned_$stamp.mp4")
            runCatching {
                VideoCaptionExporter.export(context, v, audioUri, audioMode, cues.toList(), style, out) { exportProgress = it * 0.97f }
            }.onSuccess {
                val saved = withContext(Dispatchers.IO) { VideoCaptionExporter.saveToGallery(context, out, "StickersBank_$stamp") }
                exportProgress = null
                exportedFile = out
                toast(if (saved != null) "Saved to your gallery (Movies › Stickers Bank)" else "Video made — tap Share to send it")
            }.onFailure { e ->
                exportProgress = null
                toast("Couldn't make the video: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }
    // Android 9 and older need storage permission to put the video in the gallery.
    val askStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { doExport() }
    fun export() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) askStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else doExport()
    }
    fun share(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share video").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure { toast("Nothing available to share to.") }
    }

    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Auto Subtitles", { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (videoUri == null) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp).neuRaised(cornerRadius = 22.dp, elevation = 8.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.VideoLibrary, null, tint = OnGold, modifier = Modifier.size(30.dp))
                    }
                    Text("Pick a video", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        "Its speech becomes captions on your phone — free, nothing uploaded.",
                        color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                return@Column
            }

            // Player + live captions on top (drawn exactly as they'll be burned in).
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.heightIn(max = 420.dp).aspectRatio(aspect, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp)).background(Color.Black),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setShowNextButton(false)
                                setShowPreviousButton(false)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        drawIntoCanvas {
                            CaptionRenderer.draw(it.nativeCanvas, size.width, size.height, cues, posMs, style, bold, display)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Change video", Modifier.weight(1f)) {
                    pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }
                SmallButton(if (audioUri == null) "＋ Add audio" else "Change audio", Modifier.weight(1f)) { pickAudio.launch("audio/*") }
            }
            if (audioUri != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.MusicNote, null, tint = Gold, modifier = Modifier.size(16.dp))
                    Text(audioName, color = AppOnSurface, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    Text(
                        "Remove", color = AppOnSurfaceMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { audioUri = null; music.stop() }.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                ChipRow(AudioMode.entries.filter { videoHasAudio || it != AudioMode.MIX }, audioMode, { it.label }) { audioMode = it }
            }

            Label("CAPTIONS")
            ChipRow(CaptionLanguage.entries, language, { it.label }) { language = it }
            Box(
                Modifier.padding(top = 10.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(50)).background(Gold)
                    .clickable(enabled = transcribeProgress == null && modelProgress == null) { generate() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (cues.isEmpty()) "Auto-generate captions" else "Generate again",
                    color = OnGold, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                )
            }

            Label("STYLE")
            ChipRow(CaptionStyle.entries, style, { it.label }) { style = it }

            if (cues.isNotEmpty()) Label("EDIT CAPTIONS (${cues.size})")
            cues.forEachIndexed { i, cue ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp).neuRaised(cornerRadius = 12.dp, elevation = 4.dp)
                        .clip(RoundedCornerShape(12.dp)).clickable { player.seekTo(cue.startMs) }
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(formatTime(cue.startMs), color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(40.dp))
                    Text(cue.text, color = AppOnSurface, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Edit, "Edit", tint = AppOnSurfaceMuted, modifier = Modifier.size(36.dp).clip(CircleShape).clickable { editing = i }.padding(8.dp))
                    Icon(Icons.Rounded.Delete, "Delete", tint = AppOnSurfaceMuted, modifier = Modifier.size(36.dp).clip(CircleShape).clickable { cues.removeAt(i) }.padding(8.dp))
                }
            }
            Text(
                "＋ Add a caption at ${formatTime(posMs)}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(50)).clickable {
                    val start = posMs
                    val cue = CaptionCue(listOf(CaptionWord("New caption", start, start + 2000)))
                    val at = cues.indexOfFirst { it.startMs > start }.let { if (it < 0) cues.size else it }
                    cues.add(at, cue)
                    editing = at
                }.padding(horizontal = 4.dp, vertical = 6.dp),
            )

            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1.4f).height(52.dp).clip(RoundedCornerShape(50)).background(Gold)
                        .clickable(enabled = exportProgress == null) { export() },
                    contentAlignment = Alignment.Center,
                ) { Text("Save video", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                exportedFile?.let { f ->
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(50)).border(1.5.dp, Gold, RoundedCornerShape(50))
                            .clickable { share(f) },
                        contentAlignment = Alignment.Center,
                    ) { Text("Share", color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }
            Box(Modifier.height(48.dp))
        }
    }

    if (askDownload) {
        AlertDialog(
            onDismissRequest = { askDownload = false },
            containerColor = AppSurface,
            title = { Text("Download ${language.label} speech model?", color = AppOnSurface) },
            text = {
                Text(
                    "A free, one-time download of about ${language.sizeMb} MB. After that, captions are made on your phone — offline, nothing uploaded.",
                    color = AppOnSurfaceMuted,
                )
            },
            confirmButton = { TextButton(onClick = { askDownload = false; download() }) { Text("Download", color = Gold, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { askDownload = false }) { Text("Not now", color = AppOnSurfaceMuted) } },
        )
    }

    editing?.let { i ->
        val cue = cues.getOrNull(i)
        if (cue == null) { editing = null; return@let }
        var text by remember(i) { mutableStateOf(cue.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = AppSurface,
            title = { Text("Edit caption · ${formatTime(cue.startMs)}", color = AppOnSurface) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold, unfocusedBorderColor = AppOutline,
                        focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface, cursorColor = Gold,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) cues[i] = cue.withText(text)
                    editing = null
                }) { Text("Save", color = Gold, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel", color = AppOnSurfaceMuted) } },
        )
    }

    modelProgress?.let { PreparingPopup("Downloading ${language.label} model…", "One time only — then it works offline.", progress = it) }
    transcribeProgress?.let { PreparingPopup("Listening for speech…", "On your phone — nothing uploaded.", progress = it) }
    exportProgress?.let { PreparingPopup("Making your video…", progress = it) }
}

@Composable
internal fun Label(text: String) {
    Text(
        text, color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 2.dp),
    )
}

@Composable
internal fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.then(
                    if (on) Modifier.neuInset(cornerRadius = 50.dp, elevation = 4.dp, background = Gold)
                    else Modifier.neuRaised(cornerRadius = 50.dp, elevation = 5.dp),
                ).clip(RoundedCornerShape(50)).clickable { onPick(o) }.padding(horizontal = 16.dp, vertical = 9.dp),
            ) { Text(label(o), color = if (on) OnGold else AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@Composable
internal fun SmallButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).neuRaised(cornerRadius = 22.dp, elevation = 5.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** (display aspect ratio, has sound) of a video. */
internal fun readVideo(context: android.content.Context, uri: Uri): Pair<Float, Boolean> {
    val mmr = MediaMetadataRetriever()
    val aspect = try {
        mmr.setDataSource(context, uri)
        val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 9f
        val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 16f
        val rot = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rot % 180 != 0) h / w else w / h
    } catch (e: Exception) {
        9f / 16f
    } finally {
        runCatching { mmr.release() }
    }
    return aspect.coerceIn(0.3f, 3f) to VideoCaptionExporter.hasAudio(context, uri)
}

private fun queryName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
