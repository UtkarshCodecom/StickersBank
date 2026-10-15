package com.stickers.bank.admin.r2

/**
 * Cloudflare R2 (S3-compatible) configuration for sticker image storage.
 *
 * SECURITY: these keys grant write access to the bucket. They live in local.properties
 * (r2.accessKeyId / r2.secretAccessKey — never committed) and reach the code through
 * BuildConfig, so they stay out of the repository. They still ship inside the (admin,
 * PIN-gated) APK; the production-safe path is a backend that hands out presigned URLs.
 *
 * TODO(user): confirm ENDPOINT / BUCKET / PUBLIC_BASE_URL for the STICKERS bucket. The
 * values below are guessed from the wallpaper-app reference and almost certainly need
 * changing before uploads work.
 */
object R2Config {
    val ACCESS_KEY_ID: String = com.stickers.bank.BuildConfig.R2_ACCESS_KEY_ID
    val SECRET_ACCESS_KEY: String = com.stickers.bank.BuildConfig.R2_SECRET_ACCESS_KEY

    // <accountid>.r2.cloudflarestorage.com  (S3 API endpoint)
    const val ENDPOINT = "https://3abdc6da80177d38e57f385014487886.r2.cloudflarestorage.com"
    const val BUCKET = "stickersbank"

    // Public read base (r2.dev public URL or a custom domain), no trailing slash.
    const val PUBLIC_BASE_URL = "https://pub-b5cb467d283d41b998d86adaea21e57a.r2.dev"

    const val REGION = "auto"
    const val SERVICE = "s3"

    val isConfigured: Boolean
        get() = ACCESS_KEY_ID.isNotBlank() &&
            SECRET_ACCESS_KEY.isNotBlank() &&
            PUBLIC_BASE_URL.startsWith("http")

    fun publicUrl(objectKey: String): String = "$PUBLIC_BASE_URL/$objectKey"
}
