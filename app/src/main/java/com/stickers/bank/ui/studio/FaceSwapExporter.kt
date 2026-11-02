@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.stickers.bank.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Size
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.stickers.bank.util.VideoToWebp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes the face-swapped video as an MP4 (the video's own sound kept), on the phone with Media3
 * Transformer. The swapped face is drawn by [FaceSwapPainter] — the same code as the preview —
 * onto a transparent overlay laid over every frame. Only the first [SWAP_MAX_MS] is kept.
 */
object FaceSwapExporter {

    suspend fun export(
        context: Context,
        videoUri: Uri,
        source: SourceFace,
        track: FaceTrack,
        options: SwapOptions,
        outFile: File,
        onProgress: (Float) -> Unit,
    ) {
        val hasAudio = withContext(Dispatchers.IO) { VideoCaptionExporter.hasAudio(context, videoUri) }
        val tooLong = withContext(Dispatchers.IO) { VideoToWebp.durationMs(context, videoUri) } > SWAP_MAX_MS
        val media = MediaItem.Builder().setUri(videoUri).apply {
            if (tooLong) setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setEndPositionMs(SWAP_MAX_MS).build())
        }.build()
        val item = EditedMediaItem.Builder(media)
            .setRemoveAudio(!hasAudio)
            .setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(SwapOverlay(source, track, options))))))
            .build()
        val sequence = if (hasAudio) EditedMediaItemSequence.withAudioAndVideoFrom(listOf(item))
        else EditedMediaItemSequence.withVideoFrom(listOf(item))
        VideoCaptionExporter.run(context, Composition.Builder(listOf(sequence)).build(), outFile, onProgress)
    }
}

/**
 * The swapped face over one frame: a transparent frame-sized bitmap with the face painted where
 * the track puts it at that moment. Two bitmaps take turns so each frame is a new object (which
 * is what makes the overlay upload it again); frames with no face share one empty bitmap.
 */
private class SwapOverlay(
    source: SourceFace,
    private val track: FaceTrack,
    private val options: SwapOptions,
) : BitmapOverlay() {
    private val painter = FaceSwapPainter(source)
    private var frameSize = Size(1080, 1920)
    private val buffers = arrayOfNulls<Bitmap>(2)
    private var current = 0
    private var empty: Bitmap? = null

    override fun configure(videoSize: Size) {
        super.configure(videoSize)
        frameSize = videoSize
        buffers.fill(null)
        empty = null
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val w = frameSize.width.coerceAtLeast(2)
        val h = frameSize.height.coerceAtLeast(2)
        val pose = track.poseAt(presentationTimeUs / 1000L)
            ?: return empty?.takeIf { it.width == w && it.height == h }
                ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { empty = it }
        current = 1 - current
        val bmp = buffers[current]?.takeIf { it.width == w && it.height == h }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { buffers[current] = it }
        bmp.eraseColor(Color.TRANSPARENT)
        painter.draw(Canvas(bmp), w.toFloat(), h.toFloat(), pose, options)
        return bmp
    }
}
