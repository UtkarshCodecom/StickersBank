package com.stickers.bank

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.stickers.bank.data.ServiceLocator

class StickersBankApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        com.stickers.bank.ads.AppOpenAds.init(this) // watches for the app coming back to the foreground
        com.stickers.bank.ads.Ads.init(this)
    }

    /**
     * Decoders:
     * - [ImageDecoderDecoder] (API 28+) plays **animated WebP** (and GIF) inline — this is
     *   how animated stickers move in the grid and detail sheet. On older devices we fall
     *   back to [GifDecoder] (GIF only; animated WebP shows its first frame).
     * - [VideoFrameDecoder] keeps thumbnails working for any legacy .mp4 clips.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
