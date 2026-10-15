package com.stickers.bank.export

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves sticker packs to WhatsApp following the official WhatsApp sticker
 * ContentProvider contract. Data is read from [WaPackStore] on disk so it works
 * across process restarts.
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        const val METADATA = "metadata"
        const val STICKERS = "stickers"
        const val STICKERS_ASSET = "stickers_asset"

        // metadata columns
        const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link"
        const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        const val AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"

        // stickers columns
        const val STICKER_FILE_NAME_IN_QUERY = "sticker_file_name"
        const val STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji"
        const val STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY = "sticker_accessibility_text"

        private const val METADATA_CODE = 1
        private const val METADATA_CODE_FOR_SINGLE_PACK = 2
        private const val STICKERS_CODE = 3
        private const val STICKERS_ASSET_CODE = 4
    }

    private lateinit var matcher: UriMatcher

    override fun onCreate(): Boolean {
        matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(WHATSAPP_AUTHORITY, METADATA, METADATA_CODE)
            addURI(WHATSAPP_AUTHORITY, "$METADATA/*", METADATA_CODE_FOR_SINGLE_PACK)
            addURI(WHATSAPP_AUTHORITY, "$STICKERS/*", STICKERS_CODE)
            addURI(WHATSAPP_AUTHORITY, "$STICKERS_ASSET/*/*", STICKERS_ASSET_CODE)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val ctx = context ?: return null
        return when (matcher.match(uri)) {
            METADATA_CODE -> metadataCursor(WaPackStore.loadAll(ctx))
            METADATA_CODE_FOR_SINGLE_PACK -> {
                val id = uri.lastPathSegment
                metadataCursor(WaPackStore.loadAll(ctx).filter { it.identifier == id })
            }
            STICKERS_CODE -> {
                val id = uri.lastPathSegment
                val pack = WaPackStore.load(ctx, id ?: "") ?: return null
                stickersCursor(pack)
            }
            else -> null
        }
    }

    private fun metadataCursor(packs: List<WaStickerPack>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_PACK_IDENTIFIER_IN_QUERY,
                STICKER_PACK_NAME_IN_QUERY,
                STICKER_PACK_PUBLISHER_IN_QUERY,
                STICKER_PACK_ICON_IN_QUERY,
                ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                PUBLISHER_EMAIL,
                PUBLISHER_WEBSITE,
                PRIVACY_POLICY_WEBSITE,
                LICENSE_AGREEMENT_WEBSITE,
                IMAGE_DATA_VERSION,
                AVOID_CACHE,
                ANIMATED_STICKER_PACK,
            ),
        )
        for (p in packs) {
            cursor.newRow()
                .add(p.identifier)
                .add(p.name)
                .add(p.publisher)
                .add(p.trayImageFile)
                .add(p.playStoreLink)
                .add(p.iosAppLink)
                .add(p.publisherEmail)
                .add(p.publisherWebsite)
                .add(p.privacyPolicyWebsite)
                .add(p.licenseAgreementWebsite)
                .add(p.imageDataVersion)
                .add(if (p.avoidCache) 1 else 0)
                .add(if (p.animatedStickerPack) 1 else 0)
        }
        return cursor
    }

    private fun stickersCursor(pack: WaStickerPack): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_FILE_NAME_IN_QUERY,
                STICKER_FILE_EMOJI_IN_QUERY,
                STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY,
            ),
        )
        for (s in pack.stickers) {
            cursor.newRow()
                .add(s.imageFileName)
                .add(s.emojis.joinToString(","))
                .add("")
        }
        return cursor
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val ctx = context ?: return null
        if (matcher.match(uri) != STICKERS_ASSET_CODE) return null
        val segments = uri.pathSegments
        // .../stickers_asset/{identifier}/{fileName}
        if (segments.size != 3) throw FileNotFoundException("bad uri $uri")
        val identifier = segments[1]
        val fileName = segments[2]
        val file: File = WaPackStore.file(ctx, identifier, fileName)
        if (!file.exists()) throw FileNotFoundException("not found: $file")
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        // WhatsApp needs a real declared length; UNKNOWN_LENGTH makes it read an
        // empty stream and silently reject the pack.
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: return null
        if (matcher.match(uri) != STICKERS_ASSET_CODE) return null
        val segments = uri.pathSegments
        if (segments.size != 3) throw FileNotFoundException("bad uri $uri")
        val file = WaPackStore.file(ctx, segments[1], segments[2])
        if (!file.exists()) throw FileNotFoundException("not found: $file")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = when (matcher.match(uri)) {
        METADATA_CODE, METADATA_CODE_FOR_SINGLE_PACK ->
            "vnd.android.cursor.dir/vnd.$WHATSAPP_AUTHORITY.$METADATA"
        STICKERS_CODE ->
            "vnd.android.cursor.dir/vnd.$WHATSAPP_AUTHORITY.$STICKERS"
        STICKERS_ASSET_CODE ->
            if (uri.lastPathSegment?.endsWith(".png") == true) "image/png" else "image/webp"
        else -> "application/octet-stream"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}
