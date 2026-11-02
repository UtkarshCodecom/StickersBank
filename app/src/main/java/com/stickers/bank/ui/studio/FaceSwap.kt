package com.stickers.bank.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.stickers.bank.util.VideoToWebp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Only this much of a video is face-swapped (and exported) — longer clips are cut here. */
const val SWAP_MAX_MS = 60_000L

/** Points in ML Kit's face mesh (the same 468 for every face, always in the same places). */
private const val MESH_POINTS = 468
/** The tip of the nose in the mesh — used to tell which way "towards the camera" is. */
private const val NOSE_TIP = 1

/**
 * The face taken from the photo: the photo cropped around the face, faded to transparent
 * outside the face outline ([patch]); where each mesh point is in that crop ([tex], x,y pairs);
 * the mesh triangles to draw (with the mouth opening left out, and optionally the eyes); and
 * the blurred colour under each point ([skin], r,g,b), which is the photo's lighting.
 */
class SourceFace(
    val patch: Bitmap,
    val tex: FloatArray,
    val triangles: ShortArray,
    val trianglesNoEyes: ShortArray,
    val skin: FloatArray,
)

/**
 * The face mesh in one video frame: [points] x,y as fractions of the frame and z (depth, in
 * frame widths) for each mesh point; [skin] the blurred colour under each point (the video's
 * light and skin tone); [alpha] how present it is (it fades where the face comes and goes).
 */
class FacePose(val points: FloatArray, val skin: FloatArray, val alpha: Float)

/** How the photo's face is laid onto the video's. */
data class SwapOptions(
    /** 0 = the photo's own colours and light, 1 = fully relit to the video's skin tone and light. */
    val skinMatch: Float = 0.85f,
    /** Let the video person's own eyes show through, so they blink and look around. */
    val realEyes: Boolean = false,
)

/** The face mesh through the video, sampled every so often; [poseAt] fills in between samples. */
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
 * FACE SWAP by morphing, entirely on the phone and free (no AI server, no per-use cost):
 *  1. ML Kit's face mesh puts the same 468 points on the photo's face and, every [stepFor] ms,
 *     on the video's face — eyes, brows, nose, lips, jaw, in 3D.
 *  2. Every frame the photo's face is cut into the mesh's ~900 triangles and each one is warped
 *     onto the same triangle of the video face, so the photo face takes on the video face's
 *     shape, head turn and expression — the mouth opens, smiles, the eyes close when it blinks.
 *     The mouth opening is left empty, so the video's real teeth and tongue show when it talks.
 *  3. The photo is relit point by point to the video's light and skin tone (the blurred colour
 *     under each point in the video ÷ the same in the photo), so it sits in the scene instead of
 *     looking pasted on, and the edge is feathered into the video's own skin.
 * What it can't do (what paid server AI does) is invent what the photo doesn't show — teeth the
 * photo's mouth doesn't have, the far side of a face turned away in the photo.
 */
object FaceSwap {

    /** Frames are scanned this big (longest side) — plenty for the mesh, and quick. */
    private const val SCAN_SIDE = 720
    /** The cut-out is kept at most this big — about the size faces appear in phone videos. */
    private const val PATCH_MAX = 512
    /** The photo is sampled for its lighting at most this big. */
    private const val LIGHT_SIDE = 256
    /** Lighting is the colour averaged over this fraction of the face's width around a point. */
    private const val LIGHT_RADIUS = 0.08f

    private fun detector() = FaceMeshDetection.getClient(
        FaceMeshDetectorOptions.Builder().setUseCase(FaceMeshDetectorOptions.FACE_MESH).build(),
    )

    /** The face in [photo], or null if there's no clear face. */
    suspend fun sourceFrom(photo: Bitmap): SourceFace? = withContext(Dispatchers.Default) {
        val src = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && photo.config == Bitmap.Config.HARDWARE) {
            photo.copy(Bitmap.Config.ARGB_8888, false) ?: return@withContext null
        } else photo
        val detector = detector()
        try {
            val meshes = runCatching { detector.process(InputImage.fromBitmap(src, 0)).await() }.getOrNull() ?: return@withContext null
            val mesh = meshes.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return@withContext null
            cutOut(src, mesh)
        } finally {
            detector.close()
        }
    }

    private fun cutOut(src: Bitmap, mesh: FaceMesh): SourceFace? {
        val xy = FloatArray(MESH_POINTS * 2)
        mesh.allPoints.forEach { p ->
            if (p.index in 0 until MESH_POINTS) { xy[p.index * 2] = p.position.x; xy[p.index * 2 + 1] = p.position.y }
        }
        val oval = mesh.getPoints(FaceMesh.FACE_OVAL).map { it.position.x to it.position.y }
        if (oval.size < 10) return null
        val faceW = oval.maxOf { it.first } - oval.minOf { it.first }
        if (faceW < 24f) return null

        // The crop: the face outline plus a little room for the soft edge.
        val pad = faceW * 0.06f
        val left = (oval.minOf { it.first } - pad).toInt().coerceAtLeast(0)
        val top = (oval.minOf { it.second } - pad).toInt().coerceAtLeast(0)
        val right = (oval.maxOf { it.first } + pad).roundToInt().coerceAtMost(src.width)
        val bottom = (oval.maxOf { it.second } + pad).roundToInt().coerceAtMost(src.height)
        if (right - left < 16 || bottom - top < 16) return null
        val crop = Rect(left, top, right, bottom)
        val scale = min(1f, PATCH_MAX.toFloat() / max(crop.width(), crop.height()))
        val pw = (crop.width() * scale).roundToInt().coerceAtLeast(1)
        val ph = (crop.height() * scale).roundToInt().coerceAtLeast(1)

        // Soft mask: the outline pulled in a touch (so hair and background stay out) and
        // blurred, so the face fades into the video's skin instead of ending in a hard line.
        val cx = oval.sumOf { it.first.toDouble() }.toFloat() / oval.size
        val cy = oval.sumOf { it.second.toDouble() }.toFloat() / oval.size
        val path = Path()
        oval.forEachIndexed { i, (x, y) ->
            val px = (cx + (x - cx) * 0.93f - left) * scale
            val py = (cy + (y - cy) * 0.93f - top) * scale
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val patch = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
        val c = Canvas(patch)
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            maskFilter = BlurMaskFilter((faceW * 0.035f * scale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
        })
        c.drawBitmap(src, crop, RectF(0f, 0f, pw.toFloat(), ph.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        })
        val tex = FloatArray(MESH_POINTS * 2) { i -> (xy[i] - if (i % 2 == 0) left else top) * scale }

        // The photo's lighting, read from an unmasked small copy of the same crop.
        val ls = min(1f, LIGHT_SIDE.toFloat() / max(crop.width(), crop.height()))
        val small = Bitmap.createBitmap(
            (crop.width() * ls).roundToInt().coerceAtLeast(1), (crop.height() * ls).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888,
        )
        Canvas(small).drawBitmap(src, crop, RectF(0f, 0f, small.width.toFloat(), small.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        val lightXy = FloatArray(MESH_POINTS * 2) { i -> (xy[i] - if (i % 2 == 0) left else top) * ls }
        val skin = BlurSampler(small, Rect(0, 0, small.width, small.height)).meansAt(lightXy, faceW * ls * LIGHT_RADIUS)
        small.recycle()

        // Triangles, minus the mouth opening (so the video's own mouth shows when it talks).
        val innerLips = (mesh.getPoints(FaceMesh.UPPER_LIP_BOTTOM) + mesh.getPoints(FaceMesh.LOWER_LIP_TOP)).map { it.index }.toSet()
        val eyes = (mesh.getPoints(FaceMesh.LEFT_EYE) + mesh.getPoints(FaceMesh.RIGHT_EYE)).map { it.index }.toSet()
        val all = mesh.allTriangles.map { t -> t.allPoints.map { it.index } }
            .filter { t -> t.size == 3 && t.all { it in 0 until MESH_POINTS } && !t.all { it in innerLips } }
        if (all.size < 100) return null
        fun pack(list: List<List<Int>>) = ShortArray(list.size * 3) { list[it / 3][it % 3].toShort() }
        return SourceFace(patch, tex, pack(all), pack(all.filterNot { t -> t.all { it in eyes } }), skin)
    }

    /** Milliseconds between scanned frames: denser for short clips, so lips and blinks are caught. */
    private fun stepFor(durMs: Long): Long = when {
        durMs <= 10_000L -> 83L
        durMs <= 20_000L -> 125L
        durMs <= 40_000L -> 200L
        else -> 250L
    }

    /**
     * Maps the face through [uri] — the first [SWAP_MAX_MS] of it. Heavy: decodes and meshes a
     * frame every [stepFor] ms. [onProgress] 0–1.
     */
    suspend fun track(context: Context, uri: Uri, onProgress: (Float) -> Unit): FaceTrack = withContext(Dispatchers.Default) {
        val dur = min(VideoToWebp.durationMs(context, uri), SWAP_MAX_MS)
        val step = stepFor(dur)
        val count = (dur / step).toInt() + 1
        val times = LongArray(count) { it * step }
        val points = arrayOfNulls<FloatArray>(count)
        val skins = arrayOfNulls<FloatArray>(count)
        val detector = detector()
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            var lastCenter: Pair<Float, Float>? = null
            for (i in 0 until count) {
                coroutineContext.ensureActive()
                val frame = VideoToWebp.frameAt(mmr, times[i] * 1000L, SCAN_SIDE)
                if (frame != null) {
                    val meshes = runCatching { detector.process(InputImage.fromBitmap(frame, 0)).await() }.getOrDefault(emptyList())
                    // Stay on the same person: the face nearest where it last was, else the biggest.
                    val mesh = lastCenter?.let { (lx, ly) ->
                        meshes.minByOrNull { m ->
                            val dx = m.boundingBox.exactCenterX() - lx; val dy = m.boundingBox.exactCenterY() - ly
                            dx * dx + dy * dy
                        }
                    } ?: meshes.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (mesh != null && mesh.allPoints.size >= MESH_POINTS) {
                        lastCenter = mesh.boundingBox.exactCenterX() to mesh.boundingBox.exactCenterY()
                        val w = frame.width.toFloat()
                        val h = frame.height.toFloat()
                        val p = FloatArray(MESH_POINTS * 3)
                        val xy = FloatArray(MESH_POINTS * 2)
                        mesh.allPoints.forEach { pt ->
                            val k = pt.index
                            if (k in 0 until MESH_POINTS) {
                                p[k * 3] = pt.position.x / w; p[k * 3 + 1] = pt.position.y / h; p[k * 3 + 2] = pt.position.z / w
                                xy[k * 2] = pt.position.x; xy[k * 2 + 1] = pt.position.y
                            }
                        }
                        val box = mesh.boundingBox
                        val radius = box.width() * LIGHT_RADIUS
                        val region = Rect(
                            (box.left - radius).toInt().coerceIn(0, frame.width - 1), (box.top - radius).toInt().coerceIn(0, frame.height - 1),
                            (box.right + radius).roundToInt().coerceIn(1, frame.width), (box.bottom + radius).roundToInt().coerceIn(1, frame.height),
                        )
                        if (region.width() > 2 && region.height() > 2) {
                            points[i] = p
                            skins[i] = BlurSampler(frame, region).meansAt(xy, radius)
                        }
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

    /** Fills one-sample gaps and takes the jitter out, lightly — so lips and blinks still move. */
    private fun smooth(points: Array<FloatArray?>, skins: Array<FloatArray?>) {
        val n = points.size
        for (i in 1 until n - 1) {
            val a = points[i - 1]; val b = points[i + 1]
            if (points[i] == null && a != null && b != null) {
                points[i] = FloatArray(a.size) { (a[it] + b[it]) / 2f }
                skins[i] = FloatArray(skins[i - 1]!!.size) { (skins[i - 1]!![it] + skins[i + 1]!![it]) / 2f }
            }
        }
        val p = points.map { it?.copyOf() }
        for (i in 0 until n) {
            val cur = p[i] ?: continue
            val a = p.getOrNull(i - 1)
            val b = p.getOrNull(i + 1)
            if (a != null && b != null) points[i] = FloatArray(cur.size) { 0.2f * a[it] + 0.6f * cur[it] + 0.2f * b[it] }
        }
        // Light changes slowly in real life; a wide average keeps the face from flickering.
        val s = skins.map { it?.copyOf() }
        for (i in 0 until n) {
            if (s[i] == null) continue
            val near = (i - 3..i + 3).mapNotNull { s.getOrNull(it) }
            skins[i] = FloatArray(s[i]!!.size) { k -> near.sumOf { it[k].toDouble() }.toFloat() / near.size }
        }
    }
}

/** Box-blurred colour anywhere inside [region] of a bitmap, from summed-area tables. */
private class BlurSampler(bmp: Bitmap, private val region: Rect) {
    private val w = region.width()
    private val h = region.height()
    private val sr = IntArray((w + 1) * (h + 1))
    private val sg = IntArray((w + 1) * (h + 1))
    private val sb = IntArray((w + 1) * (h + 1))

    init {
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, region.left, region.top, w, h)
        for (y in 0 until h) {
            var r = 0; var g = 0; var b = 0
            for (x in 0 until w) {
                val c = px[y * w + x]
                r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
                val at = (y + 1) * (w + 1) + x + 1
                val above = y * (w + 1) + x + 1
                sr[at] = sr[above] + r; sg[at] = sg[above] + g; sb[at] = sb[above] + b
            }
        }
    }

    /** The mean colour (r,g,b 0–255) within [radius] of each x,y pair in [xy] (bitmap pixels). */
    fun meansAt(xy: FloatArray, radius: Float): FloatArray {
        val out = FloatArray(xy.size / 2 * 3)
        val r = radius.coerceAtLeast(1f)
        for (i in 0 until xy.size / 2) {
            val x0 = (xy[i * 2] - r - region.left).toInt().coerceIn(0, w)
            val x1 = (xy[i * 2] + r - region.left).toInt().plus(1).coerceIn(0, w)
            val y0 = (xy[i * 2 + 1] - r - region.top).toInt().coerceIn(0, h)
            val y1 = (xy[i * 2 + 1] + r - region.top).toInt().plus(1).coerceIn(0, h)
            val area = (x1 - x0) * (y1 - y0)
            if (area <= 0) { out[i * 3] = 128f; out[i * 3 + 1] = 128f; out[i * 3 + 2] = 128f; continue }
            fun sum(s: IntArray) = s[y1 * (w + 1) + x1] - s[y0 * (w + 1) + x1] - s[y1 * (w + 1) + x0] + s[y0 * (w + 1) + x0]
            out[i * 3] = sum(sr) / area.toFloat()
            out[i * 3 + 1] = sum(sg) / area.toFloat()
            out[i * 3 + 2] = sum(sb) / area.toFloat()
        }
        return out
    }
}

/**
 * Draws the photo's face onto a video face, triangle by triangle. One per user (the preview, the
 * export) — it keeps its scratch arrays between frames, so it isn't thread-safe.
 */
class FaceSwapPainter(private val source: SourceFace) {
    private val verts = FloatArray(MESH_POINTS * 2)
    // Sized like verts, not one per point: older Androids check the colour array that way.
    private val colors = IntArray(MESH_POINTS * 2)
    private val gains = FloatArray(MESH_POINTS * 3)
    private val depth = FloatArray(source.triangles.size / 3)
    private val sorted = ShortArray(source.triangles.size)
    private val colorMatrix = ColorMatrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        shader = BitmapShader(source.patch, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    /** Draws [pose] onto [canvas], which shows the whole [w]×[h] video frame. */
    fun draw(canvas: Canvas, w: Float, h: Float, pose: FacePose, options: SwapOptions) {
        val p = pose.points
        for (i in 0 until MESH_POINTS) {
            verts[i * 2] = p[i * 3] * w
            verts[i * 2 + 1] = p[i * 3 + 1] * h
        }

        // Relighting: each point's colour is scaled by video light ÷ photo light there. Vertex
        // colours can only darken (they multiply the photo), so they carry each point's share of
        // the strongest gain and the colour filter supplies that strongest gain once, for all.
        val k = options.skinMatch
        var maxR = 0f; var maxG = 0f; var maxB = 0f
        for (i in 0 until MESH_POINTS * 3) {
            val ratio = ((pose.skin[i] + 6f) / (source.skin[i] + 6f)).coerceIn(0.35f, 2.6f)
            val g = 1f + (ratio - 1f) * k
            gains[i] = g
            when (i % 3) { 0 -> maxR = max(maxR, g); 1 -> maxG = max(maxG, g); else -> maxB = max(maxB, g) }
        }
        for (i in 0 until MESH_POINTS) {
            colors[i] = Color.argb(
                255,
                (gains[i * 3] / maxR * 255f).roundToInt().coerceIn(0, 255),
                (gains[i * 3 + 1] / maxG * 255f).roundToInt().coerceIn(0, 255),
                (gains[i * 3 + 2] / maxB * 255f).roundToInt().coerceIn(0, 255),
            )
        }
        colorMatrix.setScale(maxR, maxG, maxB, 1f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.alpha = (pose.alpha * 255).roundToInt().coerceIn(0, 255)

        // Far triangles first, so on a turned head the near cheek covers the far one, not the
        // other way round. Which way is "near" is read off the nose tip, which always sticks out.
        val tris = if (options.realEyes) source.trianglesNoEyes else source.triangles
        val count = tris.size / 3
        var meanZ = 0f
        for (i in 0 until MESH_POINTS) meanZ += p[i * 3 + 2]
        meanZ /= MESH_POINTS
        val nearIsSmaller = p[NOSE_TIP * 3 + 2] < meanZ
        for (t in 0 until count) {
            val z = p[tris[t * 3] * 3 + 2] + p[tris[t * 3 + 1] * 3 + 2] + p[tris[t * 3 + 2] * 3 + 2]
            depth[t] = if (nearIsSmaller) -z else z
        }
        val order = (0 until count).sortedBy { depth[it] }
        order.forEachIndexed { j, t ->
            sorted[j * 3] = tris[t * 3]; sorted[j * 3 + 1] = tris[t * 3 + 1]; sorted[j * 3 + 2] = tris[t * 3 + 2]
        }

        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES, MESH_POINTS * 2, verts, 0, source.tex, 0, colors, 0,
            sorted, 0, count * 3, paint,
        )
    }
}
