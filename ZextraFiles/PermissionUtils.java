package com.stickers.bank.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

public class PermissionUtils {
    public static String[] PERMISSIONS_READ_WRITE_STORAGE_OLD = {android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static String[] PERMISSIONS_READ_WRITE_STORAGE_NEW = {android.Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO};

    public static final int REQUEST_PERMISSION_READ_WRITE_STORAGE = 201;
    public static final int REQUEST_PERMISSION_READ_STORAGE_GALLERY = 202;
    public static final int REQUEST_PERMISSION_READ_STORAGE_CUSTOM = 500;


    public static boolean checkReadWriteStoragePermission(Context context) {
        boolean isPermitted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int permission1 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES);
            int permission2 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO);
            int permission3 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO);

            if (permission1 != PackageManager.PERMISSION_GRANTED
                    || permission2 != PackageManager.PERMISSION_GRANTED
                    || permission3 != PackageManager.PERMISSION_GRANTED) {
                isPermitted = false;
            } else {
                isPermitted = true;
            }
        } else {
            int permission1 = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int permission2 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
                isPermitted = false;
            } else {
                isPermitted = true;
            }
        }

        return isPermitted;
    }

    public static void checkReadWriteStoragePermissionWithRequest(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int permission1 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES);
            int permission2 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO);
            int permission3 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO);

            if (permission1 != PackageManager.PERMISSION_GRANTED
                    || permission2 != PackageManager.PERMISSION_GRANTED
                    || permission3 != PackageManager.PERMISSION_GRANTED) {
                requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_NEW, REQUEST_PERMISSION_READ_WRITE_STORAGE);
            }
        } else {
            int permission1 = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int permission2 = ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
                requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_OLD, REQUEST_PERMISSION_READ_WRITE_STORAGE);
            }
        }
    }

    public static void requestPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_NEW, REQUEST_PERMISSION_READ_WRITE_STORAGE);
        } else {
            requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_OLD, REQUEST_PERMISSION_READ_WRITE_STORAGE);
        }
    }

    public static void requestPermissionVideo(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_NEW, REQUEST_PERMISSION_READ_STORAGE_GALLERY);
        } else {
            requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_OLD, REQUEST_PERMISSION_READ_STORAGE_GALLERY);
        }
    }

    public static void requestPermissionCustom(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_NEW, REQUEST_PERMISSION_READ_STORAGE_CUSTOM);
        } else {
            requestReadWriteStoragePermission(context, PERMISSIONS_READ_WRITE_STORAGE_OLD, REQUEST_PERMISSION_READ_STORAGE_CUSTOM);
        }
    }

    public static void requestReadWriteStoragePermission(Context context, String[] permissionsReadWriteStorageCamera, int myPermissionReadWriteStorage) {
        ActivityCompat.requestPermissions((Activity) context, permissionsReadWriteStorageCamera, myPermissionReadWriteStorage);
    }
}
