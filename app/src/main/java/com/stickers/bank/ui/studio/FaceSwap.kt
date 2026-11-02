package com.stickers.bank.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.stickers.bank.util.VideoToWebp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Only this much of a video is face-swapped (and exported) — longer clips are cut here. */
const val SWAP_MAX_MS = 60_000L

/**
 * The face taken from the photo: the photo cropped around the face with everything outside the
 * face outline faded to transparent ([patch]), where its eyes and mouth are in that crop
 * ([anchors]: left eye x,y · right eye x,y · mouth centre x,y), and its average skin colour.
 */
class SourceFace(val patch: Bitmap, val anchors: FloatArray, val skin: FloatArray)

/**
 * Where the face is in one video frame. [points] are fractions of the frame (0–1): left eye,
 * right eye, mouth-left corner, mouth-right corner, lower lip — x,y each. [skin] is the average
 * colour of that face (0–255 r,g,b), [alpha] how present it is (it fades in/out at the edges of
 * the frames where it was found).
 */
class FacePose(val points: FloatArray, val skin: FloatArray, val alpha: Float)

/** How the photo's face is laid onto the video's. */
data class SwapOptions(
    /** 1 = the photo's eyes and mouth land exactly on the video's. */
    val size: Float = 1f,
    /** 0 = the photo's own colours, 1 = fully recoloured to the video's skin and light. */
    val skinMatch: Float = 0.7f,
    /** Let the video person's real mouth show through, so the face still talks. */
    val talkingMouth: Boolean = false,
)

/** The face found in the video, sampled every so often; [poseAt] fills in between samples. */
class FaceTrack(private val times: LongArray, private val points: Array<FloatArray?>, private val skins: Array<FloatArray?>) {

    /** How much of the video the face was found in, 0–1. */
    val coverage: Float get() = if (points.isEmpty()) 0f else points.count { it != null } / points.size.toFloat()

    fun poseAt(tMs: Long): FacePose? {
        if (times.isEmpty()) return null
        var i = times.binarySearch(tMs)
        if (i < 0) i = (-i - 2).coerceAtLeast(0)
        val a = points[i]
        if (i == times.lastIndex) return a?.let { FacePose(it, skins[i]!!, 1f) }
        val b = points[i + 1]
        val f = ((tMs - times[i]).toFloat() / (times[i + 1] - times[i])).coerceIn(0f, 1f)
        return when {
            a != null && b != null -> FacePose(lerp(a, b, f), lerp(skins[i]!!, skins[i + 1]!!, f), 1f)
            // Found on one side only: hold it there and fade, rather than pop.
            a != null -> FacePose(a, skins[i]!!, 1f - f)
            b != null -> FacePose(b, skins[i + 1]!!, f)
            else -> null
        }
    }

    private fun lerp(a: FloatArray, b: FloatArray, f: Float) = FloatArray(a.size) { a[it] + (b[it] - a[it]) * f }
}

/**
 * FACE SWAP, done the classic (no-AI-server) way, entirely on the phone and free:
 *  1. ML Kit finds the face in the photo — its outline, eyes and mouth — and the outline becomes a
 *     soft-edged cut-out.
 *  2. ML Kit finds the face in the video every [stepFor] ms and the positions are smoothed.
 *  3. Every frame, the cut-out is stretched/turned so its eyes and mouth sit on the video face's
 *     (a 3-point affine warp), recoloured to the video's skin tone and light, and blended in.
 * It follows the head (move, turn, tilt, near/far); it can't invent expressions the photo
 * doesn't have — except the mouth, which can be let through from the video ([SwapOptions.talkingMouth]).
 */
object FaceSwap {

    /** Frames are scanned this big (longest side) — plenty for finding a face, and quick. */
    private const val SCAN_SIDE = 640
    /** The cut-out is kept at most this big. */
    private const val PATCH_MAX = 640

    /** The face in [photo], or null if there's no clear face. */
    suspend fun sourceFrom(photo: Bitmap): SourceFace? = withContext(Dispatchers.Default) {
        val src = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && photo.config == Bitmap.Config.HARDWARE) {
            photo.copy(Bitmap.Config.ARGB_8888, false) ?: return@withContext null
        } else photo
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build(),
        )
        try {
            val faces = runCatching { detector.process(InputImage.fromBitmap(src, 0)).await() }.getOrNull() ?: return@withContext null
            val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return@withContext null
            val p = pointsOf(face) ?: return@withContext null
            cutOut(src, face, p)
        } finally {
            detector.close()
        }
    }

    /** The face outline of [face] (or an oval from its eyes and mouth), feathered and cropped. */
    private fun cutOut(src: Bitmap, face: Face, p: FloatArray): SourceFace? {
        val eyeDist = hypot(p[2] - p[0], p[3] - p[1])
        if (eyeDist < 8f) return null
        val mouth = PointF((p[4] + p[6]) / 2f, (p[5] + p[7]) / 2f)
        val outline = face.getContour(FaceContour.FACE)?.points?.takeIf { it.size >= 10 }
            ?: ovalAround(p, eyeDist)
        // Pulled in a little towards its middle, so hair and background at the edge stay out.
        val cx = outline.sumOf { it.x.toDouble() }.toFloat() / outline.size
        val cy = outline.sumOf { it.y.toDouble() }.toFloat() / outline.size
        val shrunk = outline.map { PointF(cx + (it.x - cx) * 0.93f, cy + (it.y - cy) * 0.93f) }
        val feather = eyeDist * 0.22f
        val left = (shrunk.minOf { it.x } - feather).toInt().coerceAtLeast(0)
        val top = (shrunk.minOf { it.y } - feather).toInt().coerceAtLeast(0)
        val right = (shrunk.maxOf { it.x } + feather).roundToInt().coerceAtMost(src.width)
        val bottom = (shrunk.maxOf { it.y } + feather).roundToInt().coerceAtMost(src.height)
        if (right - left < 16 || bottom - top < 16) return null
        val crop = Rect(left, top, right, bottom)
        val scale = min(1f, PATCH_MAX.toFloat() / max(crop.width(), crop.height()))
        val pw = (crop.width() * scale).roundToInt().coerceAtLeast(1)
        val ph = (crop.height() * scale).roundToInt().coerceAtLeast(1)
        fun x(v: Float) = (v - left) * scale
        fun y(v: Float) = (v - top) * scale

        val patch = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
        val c = Canvas(patch)
        val path = Path().apply {
            moveTo(x(shrunk[0].x), y(shrunk[0].y))
            for (i in 1 until shrunk.size) lineTo(x(shrunk[i].x), y(shrunk[i].y))
            close()
        }
        // The soft mask first, then the photo kept only where the mask is.
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            maskFilter = BlurMaskFilter((feather * scale * 0.5f).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        })
        c.drawBitmap(src, crop, RectF(0f, 0f, pw.toFloat(), ph.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        })
        val anchors = floatArrayOf(x(p[0]), y(p[1]), x(p[2]), y(p[3]), x(mouth.x), y(mouth.y))
        return SourceFace(patch, anchors, skinOf(src, p))
    }

    /** An oval face outline guessed from the eyes and mouth, for when there's no contour. */
    private fun ovalAround(p: FloatArray, eyeDist: Float): List<PointF> {
        val cx = (p[0] + p[2] + p[4] + p[6]) / 4f
        val cy = (p[1] + p[3] + p[5] + p[7]) / 4f
        val angle = atan2(p[3] - p[1], p[2] - p[0])
        val rx = eyeDist * 1.05f
        val ry = eyeDist * 1.45f
        return (0 until 32).map { i ->
            val a = i / 32.0 * 2 * Math.PI
            val ox = (rx * kotlin.math.cos(a)).toFloat()
            val oy = (ry * kotlin.math.sin(a)).toFloat()
            PointF(cx + ox * kotlin.math.cos(angle) - oy * kotlin.math.sin(angle), cy + ox * kotlin.math.sin(angle) + oy * kotlin.math.cos(angle))
        }
    }

    /**
     * The 10 face points (pixels): left eye, right eye, mouth-left, mouth-right, lower lip. Missing
     * mouth points are guessed from the eyes — the eyes themselves are required.
     */
    private fun pointsOf(face: Face): FloatArray? {
        val le = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return null
        val re = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return null
        val eyeDist = hypot(re.x - le.x, re.y - le.y)
        if (eyeDist < 2f) return null
        // "Down the face": perpendicular to the eye line, on the side the face's box centre is.
        var dx = -(re.y - le.y) / eyeDist
        var dy = (re.x - le.x) / eyeDist
        val mid = PointF((le.x + re.x) / 2f, (le.y + re.y) / 2f)
        val box = face.boundingBox
        if ((box.exactCenterX() - mid.x) * dx + (box.exactCenterY() - mid.y) * dy < 0) { dx = -dx; dy = -dy }
        val guessMouth = PointF(mid.x + dx * eyeDist * 1.05f, mid.y + dy * eyeDist * 1.05f)
        val half = PointF((re.x - le.x) * 0.38f, (re.y - le.y) * 0.38f)
        val ml = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position ?: PointF(guessMouth.x - half.x, guessMouth.y - half.y)
        val mr = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: PointF(guessMouth.x + half.x, guessMouth.y + half.y)
        val mc = PointF((ml.x + mr.x) / 2f, (ml.y + mr.y) / 2f)
        val mb = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
            ?: PointF(mc.x + dx * eyeDist * 0.22f, mc.y + dy * eyeDist * 0.22f)
        // A mouth that isn't clearly below the eyes means a bad detection.
        if ((mc.x - mid.x) * dx + (mc.y - mid.y) * dy < eyeDist * 0.4f) return null
        return floatArrayOf(le.x, le.y, re.x, re.y, ml.x, ml.y, mr.x, mr.y, mb.x, mb.y)
    }

    /** Average colour (0–255) of the cheeks-and-nose area between the eyes and the mouth;
     * [p] are [pointsOf] in [bmp]'s pixels. */
    private fun skinOf(bmp: Bitmap, p: FloatArray): FloatArray {
        val eyeY = (p[1] + p[3]) / 2f
        val mouthY = (p[5] + p[7]) / 2f
        val x0 = min(p[0], p[2])
        val x1 = max(p[0], p[2])
        val top = eyeY + (mouthY - eyeY) * 0.25f
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (j in 0 until 8) for (i in 0 until 8) {
            val x = (x0 + (x1 - x0) * (i + 0.5f) / 8f).toInt()
            val y = (top + (mouthY - top) * (j + 0.5f) / 8f).toInt()
            if (x !in 0 until bmp.width || y !in 0 until bmp.height) continue
            val c = bmp.getPixel(x, y)
            r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++
        }
        if (n == 0) return floatArrayOf(160f, 120f, 100f)
        return floatArrayOf(r / n.toFloat(), g / n.toFloat(), b / n.toFloat())
    }

    /** Milliseconds between scanned frames: denser for short clips, so quick moves are followed. */
    private fun stepFor(durMs: Long): Long = when {
        durMs <= 20_000L -> 125L
        durMs <= 40_000L -> 200L
        else -> 250L
    }

    /**
     * Finds the (largest) face throughout [uri] — the first [SWAP_MAX_MS] of it. Heavy: decodes a
     * frame every [stepFor] ms. [onProgress] 0–1.
     */
    suspend fun track(context: Context, uri: Uri, onProgress: (Float) -> Unit): FaceTrack = withContext(Dispatchers.Default) {
        val dur = min(VideoToWebp.durationMs(context, uri), SWAP_MAX_MS)
        val step = stepFor(dur)
        val count = (dur / step).toInt() + 1
        val times = LongArray(count) { it * step }
        val points = arrayOfNulls<FloatArray>(count)
        val skins = arrayOfNulls<FloatArray>(count)
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setMinFaceSize(0.06f)
                .enableTracking()
                .build(),
        )
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            var followId: Int? = null
            for (i in 0 until count) {
                coroutineContext.ensureActive()
                val frame = VideoToWebp.frameAt(mmr, times[i] * 1000L, SCAN_SIDE)
                if (frame != null) {
                    val faces = runCatching { detector.process(InputImage.fromBitmap(frame, 0)).await() }.getOrDefault(emptyList())
                    // Stay on the same person once one is picked; otherwise the biggest face.
                    val face = faces.firstOrNull { followId != null && it.trackingId == followId }
                        ?: faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    val px = face?.let { pointsOf(it) }
                    if (face != null && px != null) {
                        face.trackingId?.let { followId = it }
                        val w = frame.width.toFloat()
                        val h = frame.height.toFloat()
                        points[i] = FloatArray(10) { k -> px[k] / (if (k % 2 == 0) w else h) }
                        skins[i] = skinOf(frame, px)
                    }
                    frame.recycle()
                }
                onProgress((i + 1f) / count)
            }
        } finally {
            runCatching { mmr.release() }
            detector.close()
        }
        smooth(points, skins)
        FaceTrack(times, points, skins)
    }

    /** Fills one-frame gaps and takes the jitter out of the detections. */
    private fun smooth(points: Array<FloatArray?>, skins: Array<FloatArray?>) {
        val n = points.size
        for (i in 1 until n - 1) {
            val a = points[i - 1]; val b = points[i + 1]
            if (points[i] == null && a != null && b != null) {
                points[i] = FloatArray(a.size) { (a[it] + b[it]) / 2f }
                skins[i] = FloatArray(3) { (skins[i - 1]!![it] + skins[i + 1]!![it]) / 2f }
            }
        }
        val p = points.map { it?.copyOf() }
        for (i in 0 until n) {
            val cur = p[i] ?: continue
            val a = p.getOrNull(i - 1)
            val b = p.getOrNull(i + 1)
            if (a != null && b != null) points[i] = FloatArray(cur.size) { 0.25f * a[it] + 0.5f * cur[it] + 0.25f * b[it] }
        }
        // Colour changes slowly in real life; a wide average keeps the tone from flickering.
        val s = skins.map { it?.copyOf() }
        for (i in 0 until n) {
            if (s[i] == null) continue
            val near = (i - 3..i + 3).mapNotNull { s.getOrNull(it) }
            skins[i] = FloatArray(3) { k -> near.sumOf { it[k].toDouble() }.toFloat() / near.size }
        }
    }
}

/**
 * Draws the photo's face onto a video face. One per user (the preview, the export) — it keeps
 * its scratch objects between frames, so it isn't thread-safe.
 */
class FaceSwapPainter(private val source: SourceFace) {
    private val matrix = Matrix()
    private val dst = FloatArray(6)
    private val colors = ColorMatrix()
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }

    /** Draws [pose] onto [canvas], which shows the whole [w]×[h] video frame. */
    fun draw(canvas: Canvas, w: Float, h: Float, pose: FacePose, options: SwapOptions) {
        val p = pose.points
        val lx = p[0] * w; val ly = p[1] * h
        val rx = p[2] * w; val ry = p[3] * h
        val mx = (p[4] + p[6]) / 2f * w; val my = (p[5] + p[7]) / 2f * h
        val eyeDist = hypot(rx - lx, ry - ly)
        if (eyeDist < 3f) return
        // A face turned almost side-on squeezes the eyes together; swapping it would just smear.
        val eyeToMouth = hypot(mx - (lx + rx) / 2f, my - (ly + ry) / 2f)
        val frontal = eyeDist / eyeToMouth.coerceAtLeast(1f) // about 1 face-on, falls as the head turns
        val sideFade = ((frontal - 0.35f) / 0.25f).coerceIn(0f, 1f)
        val alpha = pose.alpha * sideFade
        if (alpha <= 0.01f) return

        val cx = (lx + rx + mx) / 3f
        val cy = (ly + ry + my) / 3f
        val s = options.size
        dst[0] = cx + (lx - cx) * s; dst[1] = cy + (ly - cy) * s
        dst[2] = cx + (rx - cx) * s; dst[3] = cy + (ry - cy) * s
        dst[4] = cx + (mx - cx) * s; dst[5] = cy + (my - cy) * s
        if (!matrix.setPolyToPoly(source.anchors, 0, dst, 0, 3)) return

        val k = options.skinMatch
        fun gain(i: Int) = 1f + (((pose.skin[i] + 8f) / (source.skin[i] + 8f)).coerceIn(0.5f, 1.9f) - 1f) * k
        colors.setScale(gain(0), gain(1), gain(2), 1f)
        facePaint.colorFilter = ColorMatrixColorFilter(colors)
        facePaint.alpha = (alpha * 255).roundToInt()
        canvas.drawBitmap(source.patch, matrix, facePaint)

        if (options.talkingMouth) {
            val mlx = p[4] * w; val mly = p[5] * h
            val mrx = p[6] * w; val mry = p[7] * h
            val bx = p[8] * w; val by = p[9] * h
            val mouthW = hypot(mrx - mlx, mry - mly).coerceAtLeast(eyeDist * 0.5f)
            val open = hypot(bx - mx, by - my)
            val radiusX = mouthW * 0.75f
            val radiusY = max(mouthW * 0.34f, open * 1.3f)
            holePaint.shader = RadialGradient(
                0f, 0f, radiusX,
                intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.save()
            canvas.translate(mx + (bx - mx) * 0.4f, my + (by - my) * 0.4f)
            canvas.rotate(Math.toDegrees(atan2((mry - mly).toDouble(), (mrx - mlx).toDouble())).toFloat())
            canvas.scale(1f, radiusY / radiusX)
            canvas.drawCircle(0f, 0f, radiusX, holePaint)
            canvas.restore()
        }
    }
}
