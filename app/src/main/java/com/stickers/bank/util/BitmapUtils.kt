package com.stickers.bank.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.stickers.bank.data.model.Sticker
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

object BitmapUtils {

    // Load a remote sticker through the app's shared Coil ImageLoader — the exact pipeline that
    // already displays these stickers everywhere (disk/memory cache, animated-WebP decoders,
    // robust networking). This is what makes tapping a sticker actually add it to the canvas;
    // the old raw-stream decode returned null on some networks. allowHardware(false) yields a
    // software bitmap we can draw/copy.
    private fun loadHttpBitmap(context: Context, url: String): Bitmap? = runCatching {
        runBlocking {
            val req = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            (context.imageLoader.execute(req) as? SuccessResult)?.drawable?.toBitmap()
        }
    }.getOrNull()

    /** Fetch a plain image URL (e.g. a canvas template's R2 art) as a software bitmap. */
    fun loadUrlBitmap(context: Context, url: String): Bitmap? = loadHttpBitmap(context, url)

    /** Raw bytes of any URL (e.g. an admin-uploaded custom font) — no image decoding. */
    fun downloadUrlBytes(url: String): ByteArray? = httpBytes(url)

    // Raw java.net.URL.openStream() chokes on R2's redirects/headers on some networks, which
    // silently returned null (sticker never appeared on the canvas). OkHttp follows redirects,
    // sends a User-Agent, and gives clear success/failure.
    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun httpBytes(url: String): ByteArray? {
        val req = Request.Builder().url(url).header("User-Agent", "StickersBank-Android").build()
        return runCatching {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.bytes()
            }
        }.getOrNull()
    }

    /**
     * Download the raw bytes of a sticker's source, preserving the original encoding.
     * Used for animated WebP, which must be copied into a WhatsApp pack *without*
     * re-encoding (re-encoding through a Bitmap would flatten it to a single frame).
     */
    fun downloadBytes(context: Context, sticker: Sticker): ByteArray? = runCatching {
        when {
            sticker.image.startsWith("http") -> httpBytes(sticker.image)
            sticker.custom ->
                File(sticker.image).readBytes()
            else ->
                context.assets.open(sticker.image).use { it.readBytes() }
        }
    }.getOrNull()

    /** Decode a sticker's source image: R2 URL (download), custom file, or bundled asset. */
    fun loadBitmap(context: Context, sticker: Sticker): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        when {
            sticker.image.startsWith("http") ->
                loadHttpBitmap(context, sticker.image)
                    ?: httpBytes(sticker.image)?.let { BitmapFactory.decodeByteArray(it, 0, it.size, opts) }
            sticker.custom ->
                BitmapFactory.decodeFile(sticker.image, opts)
            else ->
                context.assets.open(sticker.image).use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }.getOrNull()

    /** Render [src] centered and fit into a transparent [size]x[size] canvas. */
    fun fitSquare(src: Bitmap, size: Int = 512): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val scale = minOf(size.toFloat() / src.width, size.toFloat() / src.height)
        val w = src.width * scale
        val h = src.height * scale
        val left = (size - w) / 2f
        val top = (size - h) / 2f
        canvas.drawBitmap(
            src,
            Rect(0, 0, src.width, src.height),
            RectF(left, top, left + w, top + h),
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG),
        )
        return out
    }

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else Bitmap.CompressFormat.WEBP

    /** WhatsApp-compliant static sticker: 512x512 webp under [maxBytes]. */
    fun toWhatsAppWebp(src: Bitmap, maxBytes: Int = 99_000): ByteArray {
        val square = fitSquare(src, 512)
        var quality = 100
        var bytes: ByteArray
        do {
            val bos = ByteArrayOutputStream()
            square.compress(webpFormat(), quality, bos)
            bytes = bos.toByteArray()
            quality -= 8
        } while (bytes.size > maxBytes && quality > 8)
        return bytes
    }

    /**
     * Downscale-if-needed + webp-encode WITHOUT forcing a square crop — unlike [toWhatsAppWebp],
     * which is for the WhatsApp sticker format specifically. Used for admin-authored canvas
     * template art (base photo, layer images, thumbnail), which can be any shape.
     */
    fun toAspectWebp(src: Bitmap, maxDim: Int = 1024, maxBytes: Int = 400_000): ByteArray {
        val longest = maxOf(src.width, src.height)
        val scaled = if (longest > maxDim) {
            val f = maxDim.toFloat() / longest
            Bitmap.createScaledBitmap(
                src, (src.width * f).toInt().coerceAtLeast(1), (src.height * f).toInt().coerceAtLeast(1), true,
            )
        } else src
        var quality = 95
        var bytes: ByteArray
        do {
            val bos = ByteArrayOutputStream()
            scaled.compress(webpFormat(), quality, bos)
            bytes = bos.toByteArray()
            quality -= 8
        } while (bytes.size > maxBytes && quality > 8)
        return bytes
    }

    /** WhatsApp tray icon: 96x96 png under 50KB. */
    fun toTrayPng(src: Bitmap): ByteArray {
        val square = fitSquare(src, 96)
        val bos = ByteArrayOutputStream()
        square.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return bos.toByteArray()
    }

    fun writeWebp(src: Bitmap, dest: File, maxBytes: Int = 99_000) {
        dest.writeBytes(toWhatsAppWebp(src, maxBytes))
    }

    /** Decode a picked image Uri into a right-sized ARGB bitmap. */
    fun loadUriBitmap(context: Context, uri: android.net.Uri, maxDim: Int = 1600): Bitmap? =
        runCatching {
            val cr = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sample > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }.getOrNull()

    /**
     * Write [bmp] into the device's Pictures/Stickers Bank folder as a PNG so it shows up in
     * the gallery. Used by the editor's Photo mode, where the result is a picture to keep
     * rather than a sticker to send.
     *
     * On API 29+ this goes through MediaStore and needs no permission. Below that MediaStore
     * has no RELATIVE_PATH, so we write the file ourselves and tell the scanner about it —
     * which is why WRITE_EXTERNAL_STORAGE is declared with maxSdkVersion="28".
     */
    fun saveToGallery(context: Context, bmp: Bitmap, displayName: String): Boolean = runCatching {
        val cr = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Stickers Bank")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = cr.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false
            cr.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: return@runCatching false
            cr.update(uri, android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
            true
        } else {
            val dir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                "Stickers Bank",
            ).apply { mkdirs() }
            val file = File(dir, "$displayName.png")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            true
        }
    }.getOrDefault(false)

    /** Encode a bitmap to a shareable webp file (for single-sticker share intents). */
    fun exportShareableWebp(context: Context, sticker: Sticker): File? {
        val bmp = loadBitmap(context, sticker) ?: return null
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "${sticker.id}.webp")
        writeWebp(bmp, file)
        return file
    }
}
