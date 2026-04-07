package com.stickers.bank.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.stickers.bank.BuildConfig;
import com.stickers.bank.data.listeners.InterstitialAdvListener;

import java.util.Locale;
import java.util.Random;

public class AdvUtils {

    private static final String TAG = AdvUtils.class.getSimpleName();
    private static AdvUtils _instance = null;
    private Context mContext;
    private NativeAd nativeAd;
    private InterstitialAd interstitialAd;
    private boolean interstitialLoadInProgress = false;
    private InterstitialAdvListener interstitialAdListener;
    Random random;
    int intCount = 2;

    public static synchronized AdvUtils getInstance(Context context) {
        AdvUtils advUtils;
        synchronized (AdvUtils.class) {
            if (_instance == null) {
                _instance = new AdvUtils(context);
            }
            advUtils = _instance;
        }
        return advUtils;
    }

    private AdvUtils(Context context) {
        mContext = context;
        initSDK();
    }

    private void initSDK() {
        MobileAds.initialize(mContext, initializationStatus -> {
            //to check mediation
            /*Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            for (String adapterClass : statusMap.keySet()) {
                AdapterStatus status = statusMap.get(adapterClass);
                Log.e("MyApp", String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status.getDescription(), status.getLatency()));
            }*/
        });
    }

    public void loadShowBMed(FrameLayout flBContainer, AdSize adSize) {
        if (isNetworkAvailable()) {
            AdView adView = new AdView(mContext);
            adView.setAdSize(adSize);
            adView.setAdUnitId(BuildConfig.ADV_BIDS);

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    flBContainer.removeAllViews();
                    flBContainer.addView(adView);
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    // Code to be executed when an ad request fails.
                    //Log.e(TAG, "onAdFailedToLoad: " + adError.toString());
                }

                @Override
                public void onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                    //Log.e(TAG, "onAdOpened: ");
                }

                @Override
                public void onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                    //Log.e(TAG, "onAdClicked: ");
                }

                @Override
                public void onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                    //Log.e(TAG, "onAdClosed: ");
                }
            });
            adView.loadAd(adRequest);
        }
    }

    public boolean isNetworkAvailable() {
        boolean isNetAvailable = false;
        if (mContext != null) {
            final ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (mConnectivityManager != null) {
                boolean mobileNetwork = false;
                boolean wifiNetwork = false;
                boolean mobileNetworkConnecetd = false;
                boolean wifiNetworkConnecetd = false;

                final NetworkInfo mobileInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                final NetworkInfo wifiInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (mobileInfo != null) {
                    mobileNetwork = mobileInfo.isAvailable();
                }

                if (wifiInfo != null) {
                    wifiNetwork = wifiInfo.isAvailable();
                }

                if (wifiNetwork || mobileNetwork) {
                    if (mobileInfo != null)
                        mobileNetworkConnecetd = mobileInfo
                                .isConnectedOrConnecting();
                    wifiNetworkConnecetd = wifiInfo.isConnectedOrConnecting();
                }

                isNetAvailable = (mobileNetworkConnecetd || wifiNetworkConnecetd);
            }
        }
        return isNetAvailable;
    }

    /*
     *  Anchored adaptive banners
     */
    public static AdSize getAdSize(Activity activity) {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        if (activity == null) return AdSize.SMART_BANNER;

        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    public boolean isInterstitialLoaded() {
        try {
            if (interstitialAd != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void showInterstitial(boolean isRandom, int ranNum, InterstitialAdvListener interstitialAdListener) {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        this.interstitialAdListener = interstitialAdListener;
        if (interstitialAd != null) {
            if (isRandom) {
                if (getFullscreenRandomNumber(ranNum) == 0) {
                    interstitialAd.show((Activity) mContext);
                } else {
                    if (interstitialAdListener != null) {
                        interstitialAdListener.onContinue();
                    }
                }
            } else {
                interstitialAd.show((Activity) mContext);
            }
        } else {
            if (interstitialAdListener != null) {
                interstitialAdListener.onContinue();
            }
            //load only if is not in progress
            loadIAd();
        }
    }

    public void showInterstitialAlternate(InterstitialAdvListener interstitialAdListener) {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        this.interstitialAdListener = interstitialAdListener;
        if (interstitialAd != null) {
            if (intCount == 2) {
                interstitialAd.show((Activity) mContext);
                intCount = 0;
            } else {
                intCount++;
                if (interstitialAdListener != null) {
                    interstitialAdListener.onContinue();
                }
            }
        } else {
            if (interstitialAdListener != null) {
                interstitialAdListener.onContinue();
            }
            //load only if is not in progress
            loadIAd();
        }
    }

    public void loadIAd() {
        if (!isNetworkAvailable()) return;
        if (interstitialLoadInProgress) return;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(mContext, BuildConfig.ADV_IIDS, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                AdvUtils.this.interstitialAd = interstitialAd;
                //Log.e(TAG, "InterstitialAd onAdLoaded");
                interstitialAd.setFullScreenContentCallback(
                        new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                AdvUtils.this.interstitialAd = null;
                                interstitialLoadInProgress = false;
                                if (interstitialAdListener != null) {
                                    interstitialAdListener.onInterstitialAdClosed();
                                }
                                loadIAd();
                                //Log.e("TAG", "InterstitialAd The ad was dismissed.");
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when fullscreen content failed to show.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                AdvUtils.this.interstitialAd = null;
                                interstitialLoadInProgress = false;
                                //Log.e("TAG", "InterstitialAd The ad failed to show.");
                                if (interstitialAdListener != null) {
                                    interstitialAdListener.onContinue();
                                }
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                //Log.e("TAG", "InterstitialAd The ad was shown.");
                            }
                        });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                interstitialLoadInProgress = false;
                Log.e(TAG, loadAdError.getMessage());
                interstitialAd = null;
                String error =
                        String.format(Locale.ENGLISH,
                                "domain: %s, code: %d, message: %s",
                                loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
                //Log.e(TAG, "InterstitialAd onAdFailedToLoad: " + error);
            }
        });
        interstitialLoadInProgress = true;
    }

    public int getFullscreenRandomNumber(int n) {
        if (random == null) {
            random = new Random();
        }
        int ran = random.nextInt(n);
        //Log.e(TAG, "getFullscreenRandomNumber: " + ran);
        return ran;
    }
}
