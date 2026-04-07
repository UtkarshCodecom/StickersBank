package com.stickers.bank.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageManipulation {

    public static Uri convertImageToWebP(Uri uri, String StickerBookId, String StickerId, Context context) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            dirChecker(context.getFilesDir() + "/" + StickerBookId);

            String path = context.getFilesDir() + "/" + StickerBookId + "/" + StickerBookId + "-" + StickerId + ".webp";

            Log.w("Conversion Data: ", "path: " + path);

            /*FileOutputStream out = new FileOutputStream(path);
            bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);

            Log.w("IMAGE SIZE before comperssion", ""+FilesUtils.getUriSize(Uri.fromFile(new File(path)), context));

            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out); //100-best quality

            Log.w("IMAGE SIZE first", ""+FilesUtils.getUriSize(Uri.fromFile(new File(path)), context));
            */
            makeSmallestBitmapCompatible(path, bitmap);

            return Uri.fromFile(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    public static Uri convertIconTrayToWebP(Uri uri, String StickerBookId, String StickerId, Context context) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

            dirChecker(context.getFilesDir() + "/" + StickerBookId);

            String path = context.getFilesDir() + "/" + StickerBookId + "/" + StickerBookId + "-" + StickerId + ".webp";

            Log.w("Conversion Data: ", "path: " + path);

            FileOutputStream out = new FileOutputStream(path);
            bitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out); //100-best quality
            out.close();

            return Uri.fromFile(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    /*public static void convertSaveImageToWebP(Bitmap bitmap, String identifier, String iconName, Context context) {
        try {
            dirChecker(context.getFilesDir() + "/" + identifier);
            String path = context.getFilesDir() + "/" + identifier + "/" + identifier + "-" + iconName + ".webp";
            Log.e("Download Data: ", "path: " + path);
            //makeSmallestBitmapCompatible(path, bitmap);
            saveBitmap(path, bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static void convertSaveImageToWebP(Bitmap bitmap, String identifier, String path, Context context) {
        try {
            dirChecker(context.getFilesDir() + "/" + identifier);
            //String path = context.getFilesDir() + "/" + identifier + "/" + identifier + "-" + iconName + ".webp";
            //makeSmallestBitmapCompatible(path, bitmap);
            saveBitmap(path, bitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dirChecker(String dir) {
        File f = new File(dir);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    private static byte[] getByteArray(Bitmap bitmap, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.WEBP,
                quality,
                bos);

        return bos.toByteArray();
    }

    public static String saveBitmap(String path, Bitmap bitmap) {

        File mediaFile = new File(path);
        if (mediaFile.exists()) {
            mediaFile.delete();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(mediaFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mediaFile.getAbsolutePath();
    }

    private static void makeSmallestBitmapCompatible(String path, Bitmap bitmap) {
        int quality = 100;
        FileOutputStream outs = null;
        try {
            outs = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);

        int byteArrayLength = 100000;
        ByteArrayOutputStream bos = null;

        while ((byteArrayLength / 1000) >= 100) {
            bos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.WEBP,
                    quality,
                    bos);

            byteArrayLength = bos.toByteArray().length;
            quality -= 10;

            Log.w("IMAGE SIZE IS NOW", byteArrayLength + "");
        }
        try {
            outs.write(bos.toByteArray());
            outs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
