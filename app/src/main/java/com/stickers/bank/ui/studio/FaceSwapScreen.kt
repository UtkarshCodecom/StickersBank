@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.stickers.bank.ui.studio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.PolygonProgress
import com.stickers.bank.ui.components.PreparingPopup
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.rememberSingleStickerToWhatsApp
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.util.BitmapUtils
import com.stickers.bank.util.VideoToWebp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Sticker made from the swapped video: this long, at this frame rate. */
private const val STICKER_MS = 4_000L
private const val STICKER_FRAME_MS = 83

/** What a rendered MP4 was made from — when any of it changes, it has to be made again. */
private data class RenderKey(val source: SourceFace, val video: Uri, val track: FaceTrack, val options: SwapOptions)

/**
 * FACE SWAP — the face from a photo put onto the person in a video, the way Sticker.ly-style
 * apps do it, but entirely on the phone ([FaceSwap]): free, offline, nothing uploaded. The face
 * is tracked once when the video is picked, the preview shows the swap live over the player,
 * and it saves as an MP4 (with the video's sound) or as an animated sticker.
 */
@Composable
fun FaceSwapScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    var source by remember { mutableStateOf<SourceFace?>(null) }
    var findingFace by remember { mutableStateOf(false) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var aspect by remember { mutableStateOf(9f / 16f) }
    var track by remember { mutableStateOf<FaceTrack?>(null) }
    var trackProgress by remember { mutableStateOf<Float?>(null) }
    var trackJob by remember { mutableStateOf<Job?>(null) }
    var faceSize by remember { mutableFloatStateOf(1f) }
    var skinMatch by remember { mutableFloatStateOf(0.7f) }
    var talkingMouth by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf<Float?>(null) }
    var stickerProgress by remember { mutableStateOf<Float?>(null) }
    var rendered by remember { mutableStateOf<Pair<RenderKey, File>?>(null) }
    var savedVideo by remember { mutableStateOf<File?>(null) }
    var savedSticker by remember { mutableStateOf<Sticker?>(null) }
    var posMs by remember { mutableLongStateOf(0L) }
    val options = SwapOptions(faceSize, skinMatch, talkingMouth)
    val sendToWhatsApp = rememberSingleStickerToWhatsApp()

    val player = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(videoUri) {
        val v = videoUri ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(v)); player.prepare(); player.play()
    }
    // The swapped face follows the player frame by frame.
    LaunchedEffect(player) { while (true) { withFrameNanos { }; posMs = player.currentPosition } }
    val painter = remember(source) { source?.let { FaceSwapPainter(it) } }

    // ---- Picking ----
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            findingFace = true
            val photo = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri, 1600) }
            val face = photo?.let { FaceSwap.sourceFrom(it) }
            findingFace = false
            when {
                photo == null -> toast("Couldn't open that photo.")
                face == null -> toast("No clear face in that photo — try one where the face is front-on and well lit.")
                else -> { source = face; savedVideo = null; savedSticker = null }
            }
        }
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            trackJob?.cancel()
            trackJob = scope.launch {
                aspect = withContext(Dispatchers.IO) { readVideo(context, uri) }.first
                videoUri = uri
                track = null
                savedVideo = null
                savedSticker = null
                trackProgress = 0f
                val found = runCatching { FaceSwap.track(context, uri) { trackProgress = it } }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrNull()
                trackProgress = null
                when {
                    found == null -> toast("Couldn't read that video. Try another one.")
                    found.coverage == 0f -> toast("No face found in this video. Pick one where a face is clearly visible.")
                    else -> {
                        track = found
                        if (found.coverage < 0.5f) toast("The face is only visible in part of this video — it's swapped where it can be found.")
                    }
                }
            }
        }
    }
    fun pickPhoto() = pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun pickVideo() = pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

    // ---- Making the video (once per set of choices) ----
    suspend fun renderedVideo(): File? {
        val key = RenderKey(source ?: return null, videoUri ?: return null, track ?: return null, options)
        rendered?.takeIf { it.first == key && it.second.exists() }?.let { return it.second }
        player.pause()
        exportProgress = 0f
        val out = File(context.cacheDir, "studio/faceswap_${System.currentTimeMillis()}.mp4")
        val ok = runCatching {
            FaceSwapExporter.export(context, key.video, key.source, key.track, key.options, out) { exportProgress = it }
        }.onFailure {
            if (it is CancellationException) throw it
            toast("Couldn't make the video: ${it.message ?: it.javaClass.simpleName}")
        }.isSuccess
        exportProgress = null
        if (!ok) return null
        rendered?.second?.delete()
        rendered = key to out
        return out
    }

    fun doSaveVideo() {
        scope.launch {
            val file = renderedVideo() ?: return@launch
            val saved = withContext(Dispatchers.IO) {
                VideoCaptionExporter.saveToGallery(context, file, "StickersBank_FaceSwap_${System.currentTimeMillis()}")
            }
            savedVideo = file
            toast(if (saved != null) "Saved to your gallery (Movies › Stickers Bank)" else "Video made — tap Share to send it")
        }
    }
    // Android 9 and older need storage permission to put the video in the gallery.
    val askStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { doSaveVideo() }
    fun saveVideo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) askStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else doSaveVideo()
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

    // ---- Making a sticker: 4 seconds from where the preview is, framed on the face ----
    fun makeSticker() {
        val t = track ?: return
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val start = posMs.coerceAtMost((duration - STICKER_MS).coerceAtLeast(0L)).coerceAtLeast(0L)
        scope.launch {
            val file = renderedVideo() ?: return@launch
            stickerProgress = 0f
            val bytes = withContext(Dispatchers.Default) {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(file.absolutePath)
                    val count = (STICKER_MS / STICKER_FRAME_MS).toInt()
                    val center = faceCenter(t, start, start + STICKER_MS)
                    VideoToWebp.encodeTransparent(
                        count = count,
                        frameMs = STICKER_FRAME_MS,
                        frameAt = { _, ms ->
                            VideoToWebp.frameAt(mmr, (start + ms) * 1000L, 1024)?.let { frame ->
                                squareAround(frame, center).also { if (it !== frame) frame.recycle() }
                            }
                        },
                        onProgress = { stickerProgress = it * 0.95f },
                    )
                } catch (e: Exception) {
                    null
                } finally {
                    runCatching { mmr.release() }
                }
            }
            if (bytes == null) {
                stickerProgress = null
                toast("Couldn't make the sticker. Try again.")
                return@launch
            }
            savedSticker = withContext(Dispatchers.IO) {
                val repo = ServiceLocator.customStickerRepository
                val out = repo.newImageFile("webp")
                out.writeBytes(bytes)
                repo.add(out, "Face swap", listOf("face", "swap"), animated = true)
            }
            stickerProgress = null
            toast("Saved to My Stickers")
        }
    }

    val ready = source != null && track != null
    val busy = exportProgress != null || stickerProgress != null

    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Face Swap", { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(
                "Put the face from a photo onto the person in a video — on your phone, free, nothing uploaded.",
                color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickCard(
                    title = if (source == null) "Face photo" else "Change face",
                    icon = Icons.Rounded.AddPhotoAlternate,
                    preview = source?.patch,
                    busy = findingFace,
                    modifier = Modifier.weight(1f),
                ) { pickPhoto() }
                PickCard(
                    title = if (videoUri == null) "Video" else "Change video",
                    icon = Icons.Rounded.VideoLibrary,
                    preview = null,
                    busy = false,
                    modifier = Modifier.weight(1f),
                ) { pickVideo() }
            }

            videoUri?.let {
                // Player + the swapped face drawn live on top — the same painter the export uses.
                Box(Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.heightIn(max = 440.dp).aspectRatio(aspect, matchHeightConstraintsFirst = true)
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
                        val p = painter
                        val pose = track?.poseAt(posMs)
                        if (p != null && pose != null) {
                            // Its own layer, so the "talking mouth" cut-out only clears the face.
                            Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                                drawIntoCanvas { p.draw(it.nativeCanvas, size.width, size.height, pose, options) }
                            }
                        }
                    }
                }
            }

            trackProgress?.let { prog ->
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    PolygonProgress(prog, Modifier.size(28.dp))
                    Text(
                        "Following the face through your video… ${(prog * 100).roundToInt()}%",
                        color = AppOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            if (videoUri != null && trackProgress == null && source == null) {
                Text("Now pick a face photo.", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            }

            if (ready) {
                Label("FACE SIZE")
                SwapSlider(faceSize, 0.8f..1.3f) { faceSize = it }
                Label("SKIN TONE MATCH")
                SwapSlider(skinMatch, 0f..1f) { skinMatch = it }
                Label("MOUTH")
                ChipRow(listOf(false, true), talkingMouth, { if (it) "Talking (theirs)" else "From the photo" }) { talkingMouth = it }

                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(50)).background(Gold)
                            .clickable(enabled = !busy) { saveVideo() },
                        contentAlignment = Alignment.Center,
                    ) { Text("Save video", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    Box(
                        Modifier.weight(1f).height(52.dp).neuRaised(cornerRadius = 26.dp, elevation = 6.dp)
                            .clip(RoundedCornerShape(50)).clickable(enabled = !busy) { makeSticker() },
                        contentAlignment = Alignment.Center,
                    ) { Text("Make sticker", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
                Text(
                    "The sticker is 4 seconds from where the video is paused.",
                    color = AppOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
                )
                savedVideo?.let { f ->
                    Box(
                        Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
                            .border(1.5.dp, Gold, RoundedCornerShape(50)).clickable { share(f) },
                        contentAlignment = Alignment.Center,
                    ) { Text("Share video", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                }
                savedSticker?.let { s ->
                    Row(
                        Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
                            .border(1.5.dp, Gold, RoundedCornerShape(50)).clickable { sendToWhatsApp(s) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Gold, modifier = Modifier.size(16.dp))
                        Text("Add sticker to WhatsApp", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        "Open My Stickers", color = AppOnSurfaceMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50))
                            .clickable { nav.navigate(Routes.MY_STICKERS) }.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            Text(
                "Best results: a front-facing, well-lit face photo, and a video where the face is clear and not too small. " +
                    "Only the biggest face is swapped, and only the first minute of a video.",
                color = AppOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 18.dp),
            )
            Box(Modifier.height(48.dp))
        }
    }

    exportProgress?.let { PreparingPopup("Making your video…", "On your phone — nothing uploaded.", progress = it) }
    stickerProgress?.let { PreparingPopup("Making your sticker…", progress = it) }
}

@Composable
private fun PickCard(
    title: String,
    icon: ImageVector,
    preview: Bitmap?,
    busy: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.neuRaised(cornerRadius = 20.dp, elevation = 7.dp).clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !busy, onClick = onClick).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
            when {
                busy -> PolygonProgress(null, Modifier.size(40.dp))
                preview != null -> Image(
                    preview.asImageBitmap(), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                )
                else -> Icon(icon, null, tint = OnGold, modifier = Modifier.size(30.dp))
            }
        }
        Text(
            if (busy) "Finding the face…" else title,
            color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun SwapSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Slider(
        value = value, onValueChange = onChange, valueRange = range,
        colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = AppSurfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Where the face is on average over [fromMs]–[toMs] (fractions of the frame), or the middle. */
private fun faceCenter(track: FaceTrack, fromMs: Long, toMs: Long): Pair<Float, Float> {
    var x = 0f; var y = 0f; var n = 0
    var t = fromMs
    while (t <= toMs) {
        track.poseAt(t)?.let { p ->
            // Between the eyes and the mouth, nudged up a little so the forehead fits too.
            x += (p.points[0] + p.points[2] + p.points[4] + p.points[6]) / 4f
            y += (p.points[1] + p.points[3]) / 2f * 0.7f + (p.points[5] + p.points[7]) / 2f * 0.3f
            n++
        }
        t += 250
    }
    return if (n == 0) 0.5f to 0.5f else x / n to y / n
}

/** The biggest square of [frame] centred (as near as it fits) on [center]. */
private fun squareAround(frame: Bitmap, center: Pair<Float, Float>): Bitmap {
    val side = minOf(frame.width, frame.height)
    val left = (center.first * frame.width - side / 2f).roundToInt().coerceIn(0, frame.width - side)
    val top = (center.second * frame.height - side / 2f).roundToInt().coerceIn(0, frame.height - side)
    val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(out).drawBitmap(
        frame, Rect(left, top, left + side, top + side), android.graphics.RectF(0f, 0f, 512f, 512f),
        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG),
    )
    return out
}
