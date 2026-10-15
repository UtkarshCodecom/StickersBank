package com.stickers.bank.admin.r2

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Uploads sticker bytes to Cloudflare R2 via the S3 PUT API + SigV4, and returns the
 * public read URL. Call off the main thread.
 */
object R2Uploader {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * @param objectKey e.g. "stickers/deepika/1699999999.webp"
     * @return public URL of the uploaded object
     */
    @Throws(IOException::class)
    fun uploadBytes(
        bytes: ByteArray,
        objectKey: String,
        contentType: String = "image/webp",
    ): String {
        if (!R2Config.isConfigured) throw IOException("R2 not configured (endpoint/bucket/public URL).")

        val payloadSha = AwsV4Signer.sha256Hex(bytes)
        val signed = AwsV4Signer.signPut(
            accessKeyId = R2Config.ACCESS_KEY_ID,
            secretAccessKey = R2Config.SECRET_ACCESS_KEY,
            region = R2Config.REGION,
            service = R2Config.SERVICE,
            endpoint = R2Config.ENDPOINT,
            bucket = R2Config.BUCKET,
            objectKey = objectKey,
            contentType = contentType,
            payloadSha256Hex = payloadSha,
        )

        val url = "${R2Config.ENDPOINT}/${R2Config.BUCKET}/$objectKey"
        val body = bytes.toRequestBody(contentType.toMediaType())
        val builder = Request.Builder().url(url).put(body)
        signed.headers.forEach { (k, v) ->
            // OkHttp sets Host/Content-Type itself; skip to avoid duplicates/mismatch.
            if (!k.equals("Host", true)) builder.header(k, v)
        }

        http.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                val msg = resp.body?.string() ?: "HTTP ${resp.code}"
                throw IOException("R2 upload failed: ${resp.code} $msg")
            }
        }
        return R2Config.publicUrl(objectKey)
    }
}
