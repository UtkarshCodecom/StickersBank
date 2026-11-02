@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.stickers.bank.ui.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Size
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

/** What happens to the sound when an audio file is added. */
enum class AudioMode(val label: String) {
    KEEP("Keep video sound"),
    REPLACE("Replace with audio"),
    MIX("Mix both"),
}

/**
 * Burns captions into a video and writes an MP4 — on the phone, with Media3 Transformer. The
 * captions are drawn by [CaptionRenderer] onto a transparent frame-sized overlay, so they look
 * exactly like the preview. An added audio file can replace or be mixed with the video's own
 * sound (looped if it's shorter than the video).
 */
object VideoCaptionExporter {

    /** True if [uri] has a sound track at all. */
    fun hasAudio(context: Context, uri: Uri): Boolean = runCatching {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(context, uri, null)
            (0 until ex.trackCount).any { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
        } finally { ex.release() }
    }.getOrDefault(false)

    /**
     * Exports to [outFile]. Must be called from the main thread's coroutine (Transformer lives on
     * the thread that builds it). [onProgress] 0–1. Throws on failure.
     */
    suspend fun export(
        context: Context,
        videoUri: Uri,
        audioUri: Uri?,
        audioMode: AudioMode,
        cues: List<CaptionCue>,
        style: CaptionStyle,
        outFile: File,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.Main) {
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val display = runCatching { Typeface.createFromAsset(context.assets, "go3v2.ttf") }.getOrDefault(bold)
        val overlay = CaptionOverlay(cues.toList(), style, bold, display)

        val videoHasAudio = withContext(Dispatchers.IO) { hasAudio(context, videoUri) }
        val extraAudio = audioUri != null && audioMode != AudioMode.KEEP
        val keepVideoSound = videoHasAudio && (audioMode != AudioMode.REPLACE || audioUri == null)

        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(videoUri))
            .setRemoveAudio(!keepVideoSound)
            .setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(overlay)))))
            .build()
        val sequences = mutableListOf(
            if (keepVideoSound) EditedMediaItemSequence.withAudioAndVideoFrom(listOf(videoItem))
            else EditedMediaItemSequence.withVideoFrom(listOf(videoItem)),
        )
        if (extraAudio) {
            val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(audioUri!!)).setRemoveVideo(true).build()
            sequences += EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
                .addItem(audioItem)
                .setIsLooping(true) // a short song repeats to the end of the video
                .build()
        }
        val composition = Composition.Builder(sequences).build()

        val done = CompletableDeferred<Unit>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) { done.complete(Unit) }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    done.completeExceptionally(exportException)
                }
            })
            .build()
        outFile.parentFile?.mkdirs()
        outFile.delete()
        transformer.start(composition, outFile.absolutePath)
        try {
            coroutineScope {
                val poll = launch {
                    val holder = ProgressHolder()
                    while (true) {
                        if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress / 100f)
                        delay(200)
                    }
                }
                try { done.await() } finally { poll.cancel() }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            transformer.cancel()
            throw e
        }
        onProgress(1f)
    }

    /** Copies [file] into the gallery (Movies/Stickers Bank). The saved video's Uri, or null. */
    fun saveToGallery(context: Context, file: File, displayName: String): Uri? = runCatching {
        val cr = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Stickers Bank")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val dest = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            cr.openOutputStream(dest)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return null
            cr.update(dest, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            dest
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Stickers Bank").apply { mkdirs() }
            val dest = File(dir, "$displayName.mp4")
            file.copyTo(dest, overwrite = true)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("video/mp4"), null)
            Uri.fromFile(dest)
        }
    }.getOrNull()
}

/**
 * The caption layer Transformer draws over every frame: a transparent bitmap the size of the
 * frame (told to us through [configure]) with the current caption on it. It's only redrawn when
 * the caption actually changes; two bitmaps take turns so each change is a new object, which is
 * what makes the overlay upload it again.
 */
private class CaptionOverlay(
    private val cues: List<CaptionCue>,
    private val style: CaptionStyle,
    private val bold: Typeface,
    private val display: Typeface,
) : BitmapOverlay() {
    private var frameSize = Size(1080, 1920)
    private val buffers = arrayOfNulls<Bitmap>(2)
    private var current = 0
    private var lastKey: Any? = null

    override fun configure(videoSize: Size) {
        super.configure(videoSize)
        frameSize = videoSize
        buffers.fill(null)
        lastKey = null
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val t = presentationTimeUs / 1000L
        val key = CaptionRenderer.stateKey(cues, t, style)
        buffers[current]?.let { if (key == lastKey) return it }
        current = 1 - current
        val w = frameSize.width.coerceAtLeast(2)
        val h = frameSize.height.coerceAtLeast(2)
        val bmp = buffers[current]?.takeIf { it.width == w && it.height == h }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { buffers[current] = it }
        bmp.eraseColor(Color.TRANSPARENT)
        CaptionRenderer.draw(Canvas(bmp), w.toFloat(), h.toFloat(), cues, t, style, bold, display)
        lastKey = key
        return bmp
    }
}
