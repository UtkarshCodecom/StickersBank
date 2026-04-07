package com.stickers.bank;

import android.app.Application;
import android.content.Context;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.orhanobut.hawk.Hawk;
import com.stickers.bank.utils.TypefaceUtil;

public class StickersBankApplication extends Application {

    private static StickersBankApplication singleton;

    public StickersBankApplication() {
        singleton = this;
    }

    public static StickersBankApplication getInstance() {
        return singleton;
    }

    public static Context getAppContext() {
        if (singleton == null) {
            singleton = new StickersBankApplication();
        }
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(getApplicationContext());
        TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Quicksand-Medium.ttf");
        singleton = this;
        Fresco.initialize(this);
        Hawk.init(this).build();

        MobileAds.initialize(this, initializationStatus -> {
        });
    }

    public String getRandomString() {

        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        return ts.toString();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
