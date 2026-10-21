package com.stickers.bank.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Interstitial ads. One ad is kept preloaded; [show] plays it and runs the caller's continuation
 * once it is dismissed — or straight away when none is ready, so an ad never blocks the flow it
 * sits in front of. The next ad starts loading as soon as one is shown.
 *
 * Release builds use the real ad unit; debug builds use Google's public test unit, because
 * AdMob treats impressions/clicks on your own real ads while developing as invalid traffic and
 * can suspend the account. (The matching App ID is set per build type in app/build.gradle.)
 */
object Ads {
    private const val TAG = "Ads"
    private const val REAL_INTERSTITIAL_UNIT_ID = "ca-app-pub-1281325310603643/3137800153"
    private const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private val INTERSTITIAL_UNIT_ID =
        if (com.stickers.bank.BuildConfig.DEBUG) TEST_INTERSTITIAL_UNIT_ID else REAL_INTERSTITIAL_UNIT_ID

    private var appContext: Context? = null
    private var ad: InterstitialAd? = null
    private var loading = false

    // Shared by every full-screen format (this interstitial and [AppOpenAds]), so two are never
    // stacked or shown back to back.
    private const val COOLDOWN_MS = 2 * 60 * 1000L
    private var showingFullScreen = false
    private var lastFullScreenEndedAt = 0L

    fun canShowFullScreen(): Boolean =
        !showingFullScreen &&
            (lastFullScreenEndedAt == 0L || android.os.SystemClock.elapsedRealtime() - lastFullScreenEndedAt >= COOLDOWN_MS)

    fun fullScreenStarted() { showingFullScreen = true }

    fun fullScreenFinished() {
        showingFullScreen = false
        lastFullScreenEndedAt = android.os.SystemClock.elapsedRealtime()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        // Off the main thread, as AdMob recommends: initialize() does real work and starts a
        // WebView, and on the main thread at launch it's a known cause of "app not responding".
        // Ad loads must start on the main thread, so the completion hops back there.
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        Thread {
            runCatching {
                MobileAds.initialize(context.applicationContext) {
                    main.post {
                        load()
                        AppOpenAds.load()
                    }
                }
            }.onFailure { Log.w(TAG, "MobileAds init failed", it) }
        }.start()
    }

    private fun load() {
        val ctx = appContext ?: return
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            ctx, INTERSTITIAL_UNIT_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) { ad = loaded; loading = false }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "interstitial failed to load: ${error.message}")
                    ad = null; loading = false
                }
            },
        )
    }

    /** Shows an interstitial over [activity], then calls [onFinished] (once) when it's closed.
     * Calls it immediately if no ad is ready. */
    fun show(activity: Activity?, onFinished: () -> Unit) {
        val ready = ad
        // Not ready — or another full-screen ad (e.g. an app-open one) only just ended.
        if (activity == null || ready == null || !canShowFullScreen()) {
            load()
            onFinished()
            return
        }
        ad = null
        fullScreenStarted()
        var done = false
        val finish = { if (!done) { done = true; fullScreenFinished(); load(); onFinished() } }
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        ready.show(activity)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** `(then) -> Unit`: shows an interstitial, then runs [then] once it closes (or at once if none). */
@Composable
fun rememberInterstitial(): (then: () -> Unit) -> Unit {
    val context = LocalContext.current
    return remember(context) { { then -> Ads.show(context.findActivity(), then) } }
}

/** The host [Activity] for the current composition, for callers that need to pass it to [Ads.show]. */
@Composable
fun rememberActivity(): Activity? {
    val context = LocalContext.current
    return remember(context) { context.findActivity() }
}
