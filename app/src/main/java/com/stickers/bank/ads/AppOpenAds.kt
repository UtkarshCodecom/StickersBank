package com.stickers.bank.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.stickers.bank.BuildConfig
import com.stickers.bank.MainActivity

/**
 * App-open ads: one shown when the user comes BACK to the app after being away. Never on the cold
 * start (the splash plays then), and only after a real absence ([MIN_AWAY_MS]) — so popping out
 * to the photo picker or WhatsApp's add-pack screen and straight back doesn't trigger one. Shares
 * [Ads]' cooldown so it never lands right after an interstitial.
 */
object AppOpenAds : Application.ActivityLifecycleCallbacks {
    private const val TAG = "AppOpenAds"
    private const val TEST_UNIT_ID = "ca-app-pub-1281325310603643/6984448348"

    // The real "App open" ad unit — its own unit, since the interstitial one can't serve this format.
    private const val REAL_UNIT_ID = "ca-app-pub-1281325310603643/6984448348"

    private val unitId: String get() = if (BuildConfig.DEBUG) TEST_UNIT_ID else REAL_UNIT_ID

    private const val MIN_AWAY_MS = 60_000L
    // AdMob's guidance: an app-open ad more than 4 hours old may no longer be valid.
    private const val MAX_AD_AGE_MS = 4 * 60 * 60 * 1000L

    private var app: Application? = null
    private var ad: AppOpenAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var startedActivities = 0
    private var retries = 0
    private var wentToBackgroundAt = 0L // 0 = hasn't been to the background since launch

    fun init(application: Application) {
        app = application
        application.registerActivityLifecycleCallbacks(this)
    }

    fun load() {
        val ctx = app ?: return
        if (unitId.isBlank() || ad != null || loading) return
        loading = true
        AppOpenAd.load(
            ctx, unitId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    Log.i(TAG, "app-open ad loaded")
                    ad = loaded; loadedAt = SystemClock.elapsedRealtime(); loading = false; retries = 0
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "app-open ad failed to load: code=${error.code} ${error.message} | ${error.responseInfo}")
                    ad = null; loading = false
                    Ads.retryLater(retries++) { load() }
                }
            },
        )
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        if (startedActivities == 1 && wentToBackgroundAt > 0L &&
            SystemClock.elapsedRealtime() - wentToBackgroundAt >= MIN_AWAY_MS
        ) {
            showIfReady(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0 && !activity.isChangingConfigurations) {
            wentToBackgroundAt = SystemClock.elapsedRealtime()
        }
    }

    private fun showIfReady(activity: Activity) {
        // Only over the app itself — not the splash, the admin panel, or an ad's own activity.
        if (activity !is MainActivity) return
        if (!Ads.canShowFullScreen()) return
        val ready = ad
        if (ready == null || SystemClock.elapsedRealtime() - loadedAt > MAX_AD_AGE_MS) {
            ad = null
            load()
            return
        }
        ad = null
        Ads.fullScreenStarted()
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finished()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finished()
        }
        ready.show(activity)
    }

    private fun finished() {
        Ads.fullScreenFinished()
        load()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
