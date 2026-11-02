package com.stickers.bank.ui.studio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

/** A language captions can be made in — each is one free offline Vosk model, downloaded once. */
enum class CaptionLanguage(val label: String, val modelName: String, val sizeMb: Int) {
    ENGLISH("English", "vosk-model-small-en-in-0.4", 38),
    HINDI("Hindi", "vosk-model-small-hi-0.22", 45);

    val url: String get() = "https://alphacephei.com/vosk/models/$modelName.zip"
}

class NoAudioException : Exception("This video has no sound to caption.")

/**
 * Speech → timed words, entirely on the phone (Vosk, Apache-2.0). The only network use is the
 * one-time model download; after that it works offline, costs nothing and sends nothing anywhere.
 */
object SpeechToText {

    private fun root(context: Context) = File(context.filesDir, "vosk")

    fun modelDir(context: Context, lang: CaptionLanguage) = File(root(context), lang.modelName)

    fun isModelReady(context: Context, lang: CaptionLanguage): Boolean {
        val dir = modelDir(context, lang)
        return File(dir, "am/final.mdl").exists() || File(dir, "conf/model.conf").exists()
    }

    /** Downloads and unpacks [lang]'s model (once). [onProgress] 0–1. False on failure. */
    suspend fun downloadModel(context: Context, lang: CaptionLanguage, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val root = root(context).apply { mkdirs() }
        val zip = File(context.cacheDir, "${lang.modelName}.zip")
        runCatching {
            val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
            client.newCall(Request.Builder().url(lang.url).header("User-Agent", "StickersBank-Android").build()).execute().use { resp ->
                if (!resp.isSuccessful) error("Download failed (${resp.code})")
                val body = resp.body ?: error("Empty download")
                val total = body.contentLength().takeIf { it > 0 } ?: (lang.sizeMb * 1_000_000L)
                body.byteStream().use { input ->
                    zip.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            onProgress((done.toFloat() / total * 0.85f).coerceAtMost(0.85f))
                        }
                    }
                }
            }
            unzip(zip, root) { onProgress(0.85f + it * 0.15f) }
            zip.delete()
            isModelReady(context, lang)
        }.getOrElse { zip.delete(); false }
    }

    private fun unzip(zip: File, into: File, onProgress: (Float) -> Unit) {
        val rootPath = into.canonicalPath + File.separator
        val total = zip.length().coerceAtLeast(1L)
        var read = 0L
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            while (true) {
                val entry = zin.nextEntry ?: break
                val out = File(into, entry.name)
                // Never write outside the model folder, whatever the archive says.
                if (!out.canonicalPath.startsWith(rootPath)) continue
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zin.copyTo(it) }
                    read += entry.compressedSize.coerceAtLeast(0L)
                    onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
    }

    /**
     * Every word spoken in [uri]'s sound, with when it's said. Throws [NoAudioException] if there
     * is no sound. [onProgress] 0–1. Heavy: decodes the whole audio track.
     */
    suspend fun transcribe(context: Context, uri: Uri, lang: CaptionLanguage, onProgress: (Float) -> Unit): List<CaptionWord> =
        withContext(Dispatchers.Default) {
            val model = Model(modelDir(context, lang).absolutePath)
            try {
                val rec = Recognizer(model, 16000f)
                try {
                    rec.setWords(true)
                    val words = ArrayList<CaptionWord>()
                    decodeToMono16k(context, uri, onProgress) { samples, n ->
                        if (rec.acceptWaveForm(samples, n)) words += parseWords(rec.result)
                    }
                    words += parseWords(rec.finalResult)
                    words
                } finally {
                    rec.close()
                }
            } finally {
                model.close()
            }
        }

    private fun parseWords(json: String): List<CaptionWord> = runCatching {
        val arr = JSONObject(json).optJSONArray("result") ?: return emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val word = o.optString("word")
            if (word.isBlank()) null
            else CaptionWord(word, (o.optDouble("start") * 1000).toLong(), (o.optDouble("end") * 1000).toLong())
        }
    }.getOrDefault(emptyList())

    /** Decodes [uri]'s first audio track to 16 kHz mono 16-bit, handed to [sink] in chunks. */
    private suspend fun decodeToMono16k(context: Context, uri: Uri, onProgress: (Float) -> Unit, sink: (ShortArray, Int) -> Unit) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        if (track == null) { extractor.release(); throw NoAudioException() }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        var resampler = Resampler(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
        var floatPcm = false
        val out = ShortArray(8192)
        var outN = 0
        val emit: (Short) -> Unit = { v ->
            out[outN++] = v
            if (outN == out.size) { sink(out, outN); outN = 0 }
        }
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val n = extractor.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val f = codec.outputFormat
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                    resampler = Resampler(f.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                    floatPcm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                } else if (outIdx >= 0) {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    buf.order(ByteOrder.nativeOrder())
                    val frames = if (floatPcm) info.size / 4 / channels else info.size / 2 / channels
                    if (frames > 0) {
                        val mono = FloatArray(frames)
                        if (floatPcm) {
                            val fb = buf.asFloatBuffer()
                            for (i in 0 until frames) { var s = 0f; for (c in 0 until channels) s += fb.get(i * channels + c); mono[i] = s / channels }
                        } else {
                            val sb = buf.asShortBuffer()
                            for (i in 0 until frames) { var s = 0f; for (c in 0 until channels) s += sb.get(i * channels + c) / 32768f; mono[i] = s / channels }
                        }
                        resampler.process(mono, frames, emit)
                    }
                    if (durationUs > 0) onProgress((info.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f))
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
            if (outN > 0) sink(out, outN)
        } finally {
            runCatching { codec.stop() }
            codec.release()
            extractor.release()
        }
    }

    /** Streaming linear resampler to 16 kHz (what the speech models expect). */
    private class Resampler(inRate: Int) {
        private val step = inRate.toDouble() / 16000.0
        private var pos = 0.0

        fun process(input: FloatArray, n: Int, emit: (Short) -> Unit) {
            while (pos < n) {
                val i = pos.toInt()
                val frac = (pos - i).toFloat()
                val a = input[i]
                val b = if (i + 1 < n) input[i + 1] else a
                val v = (a + (b - a) * frac).coerceIn(-1f, 1f)
                emit((v * 32767f).toInt().toShort())
                pos += step
            }
            pos -= n
        }
    }
}
