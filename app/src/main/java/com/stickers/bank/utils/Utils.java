package com.stickers.bank.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.R;

import java.io.File;

public class Utils {

    public static boolean isNetworkAvailable(final Context context, boolean canShowErrorDialogOnFail, final boolean isFinish) {
        boolean isNetAvailable = false;

        if (context != null) {
            final ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

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
            context.setTheme(R.style.Theme_StickersBank);
            if (!isNetAvailable && canShowErrorDialogOnFail) {
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showAlertWithFinish((Activity) context, context.getString(R.string.no_internet), context.getString(R.string.network_alert), isFinish);
                        }
                    });
                }
            }
        }

        return isNetAvailable;
    }

    public static void showAlertWithFinish(final Activity activity, String title, String message, final boolean isFinish) {
        new MaterialAlertDialogBuilder(activity,R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(activity.getString(R.string.ok), (dialog, which) -> {
                    if (isFinish) {
                        dialog.dismiss();
                        activity.finish();
                    } else {
                        dialog.dismiss();
                    }
                }).show();
    }

    public static File getTrayIconUri(Context context, String identifier, String trayIcon) {

        try {
            ImageManipulation.dirChecker(context.getFilesDir() + "/" + identifier);
            String path = context.getFilesDir() + "/" + identifier + "/" + identifier + "-" + trayIcon + ".png";
            return new File(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File("");
    }
}
