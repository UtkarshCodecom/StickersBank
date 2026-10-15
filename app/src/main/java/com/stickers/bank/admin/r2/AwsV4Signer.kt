package com.stickers.bank.admin.r2

import java.net.URI
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Minimal AWS Signature V4 signer for S3-compatible PUT object requests (Cloudflare R2).
 */
object AwsV4Signer {

    data class SignedHeaders(val headers: Map<String, String>)

    fun signPut(
        accessKeyId: String,
        secretAccessKey: String,
        region: String,
        service: String,
        endpoint: String,
        bucket: String,
        objectKey: String,
        contentType: String,
        payloadSha256Hex: String,
    ): SignedHeaders {
        val host = URI(endpoint).host

        val now = Date()
        val amzDate = amzDateFormat().format(now)      // yyyyMMdd'T'HHmmss'Z'
        val dateStamp = dateStampFormat().format(now)   // yyyyMMdd

        val canonicalUri = "/" + uriEncode(bucket, false) + "/" + uriEncode(objectKey, false)
        val canonicalQuery = ""

        // Signed headers (sorted, lowercase names)
        val headersToSign = sortedMapOf(
            "content-type" to contentType,
            "host" to host,
            "x-amz-content-sha256" to payloadSha256Hex,
            "x-amz-date" to amzDate,
        )
        val canonicalHeaders = headersToSign.entries.joinToString("") { "${it.key}:${it.value.trim()}\n" }
        val signedHeaders = headersToSign.keys.joinToString(";")

        val canonicalRequest = listOf(
            "PUT",
            canonicalUri,
            canonicalQuery,
            canonicalHeaders,
            signedHeaders,
            payloadSha256Hex,
        ).joinToString("\n")

        val algorithm = "AWS4-HMAC-SHA256"
        val credentialScope = "$dateStamp/$region/$service/aws4_request"
        val stringToSign = listOf(
            algorithm,
            amzDate,
            credentialScope,
            sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8)),
        ).joinToString("\n")

        val signingKey = signatureKey(secretAccessKey, dateStamp, region, service)
        val signature = toHex(hmacSha256(signingKey, stringToSign.toByteArray(Charsets.UTF_8)))

        val authorization = "$algorithm Credential=$accessKeyId/$credentialScope, " +
            "SignedHeaders=$signedHeaders, Signature=$signature"

        return SignedHeaders(
            mapOf(
                "Authorization" to authorization,
                "x-amz-date" to amzDate,
                "x-amz-content-sha256" to payloadSha256Hex,
                "Content-Type" to contentType,
                "Host" to host,
            ),
        )
    }

    // ---- helpers ----

    fun sha256Hex(bytes: ByteArray): String =
        toHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun signatureKey(secret: String, dateStamp: String, region: String, service: String): ByteArray {
        val kDate = hmacSha256("AWS4$secret".toByteArray(Charsets.UTF_8), dateStamp.toByteArray(Charsets.UTF_8))
        val kRegion = hmacSha256(kDate, region.toByteArray(Charsets.UTF_8))
        val kService = hmacSha256(kRegion, service.toByteArray(Charsets.UTF_8))
        return hmacSha256(kService, "aws4_request".toByteArray(Charsets.UTF_8))
    }

    private fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append(String.format(Locale.US, "%02x", b))
        return sb.toString()
    }

    /** RFC 3986 encoding; keeps '/' when [encodeSlash] is false (for object keys). */
    private fun uriEncode(input: String, encodeSlash: Boolean): String {
        val sb = StringBuilder()
        for (ch in input) {
            when {
                ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' ||
                    ch == '_' || ch == '-' || ch == '~' || ch == '.' -> sb.append(ch)
                ch == '/' -> sb.append(if (encodeSlash) "%2F" else "/")
                else -> for (b in ch.toString().toByteArray(Charsets.UTF_8)) {
                    sb.append(String.format(Locale.US, "%%%02X", b))
                }
            }
        }
        return sb.toString()
    }

    private fun amzDateFormat() = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun dateStampFormat() = SimpleDateFormat("yyyyMMdd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
}
