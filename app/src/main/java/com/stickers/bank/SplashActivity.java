package com.stickers.bank;

import static com.stickers.bank.data.common.Constants.token;
import static com.stickers.bank.utils.PermissionUtils.PERMISSIONS_READ_WRITE_STORAGE_NEW;
import static com.stickers.bank.utils.PermissionUtils.PERMISSIONS_READ_WRITE_STORAGE_OLD;
import static com.stickers.bank.utils.PermissionUtils.REQUEST_PERMISSION_READ_WRITE_STORAGE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.stickers.bank.databinding.ActivitySplashBinding;
import com.stickers.bank.ui.base.BaseActivity;

public class SplashActivity extends BaseActivity<ActivitySplashBinding> {
    private static final int SPLASH_TIME_OUT = 2000;
    Handler handler = new Handler();

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        //handler.postDelayed(runnable, SPLASH_TIME_OUT);

        FirebaseApp.initializeApp(SplashActivity.this);
        Glide.with(this).
                load(R.drawable.ic_sticker_splash)
                .into(binding.ivSplash);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkReadWriteStoragePermission_New()) {
                requestReadWriteStoragePermission(PERMISSIONS_READ_WRITE_STORAGE_NEW, REQUEST_PERMISSION_READ_WRITE_STORAGE);
            } else {
                handler.postDelayed(runnable, SPLASH_TIME_OUT);
            }
        } else {
            if (!checkReadWriteStoragePermission_OLD()) {
                requestReadWriteStoragePermission(PERMISSIONS_READ_WRITE_STORAGE_OLD, REQUEST_PERMISSION_READ_WRITE_STORAGE);
            } else {
                handler.postDelayed(runnable, SPLASH_TIME_OUT);
            }
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        token= task.getResult();

                        // Log and toast
                        Log.e("--------Token-----", token);
                        //Toast.makeText(SplashActivity.this, token, Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    protected void setListeners() {

    }

    Runnable runnable = () -> {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler.removeCallbacks(runnable);
    }

    public boolean checkReadWriteStoragePermission_OLD() {
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    public void requestReadWriteStoragePermission(String[] permissionsReadWriteStorageCamera, int myPermissionReadWriteStorage) {
        ActivityCompat.requestPermissions(this, permissionsReadWriteStorageCamera, myPermissionReadWriteStorage);
    }

    public boolean checkReadWriteStoragePermission_New() {
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO);
        int permission3 = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO);

        if (permission1 != PackageManager.PERMISSION_GRANTED
                || permission2 != PackageManager.PERMISSION_GRANTED
                || permission3 != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_WRITE_STORAGE:
                boolean isGrantedAll = false;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        isGrantedAll = true;
                    } else {
                        isGrantedAll = false;
                        break;
                    }
                }
                if (isGrantedAll) {
                    handler.postDelayed(runnable, SPLASH_TIME_OUT);
                } else {
                    showToastMsg(getString(R.string.plz_grant_storage_permission));
                }
                break;
        }
    }
}