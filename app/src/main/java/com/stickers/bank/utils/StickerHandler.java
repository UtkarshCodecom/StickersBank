package com.stickers.bank.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.BuildConfig;
import com.stickers.bank.R;
import com.stickers.bank.data.model.StickerApi;
import com.stickers.bank.data.model.StickerModel;
import com.stickers.bank.data.model.StickerPackApi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StickerHandler {

    private static final String TAG = StickerHandler.class.getSimpleName();
    StickerPackApi stickerPackApi;
    Activity mContext;
    public ArrayList<String> stringsImagesUri = new ArrayList<>();
    public ArrayList<String> stringsImagesFileURL = new ArrayList<>();
    private ProgressDialog pDialog;

    private int totalImages = 0;
    private int faildImages = 0;
    private int downloadeImages = 0;
    public static final int ADD_PACK = 201;
    Fragment fragment;

    /**
     * Do not change values of below 3 lines as this is also used by WhatsApp
     */
    public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
    public static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
    public static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

    public static final String EXTRA_STICKER_PACK_WEBSITE = "sticker_pack_website";
    public static final String EXTRA_STICKER_PACK_EMAIL = "sticker_pack_email";
    public static final String EXTRA_STICKER_PACK_PRIVACY_POLICY = "sticker_pack_privacy_policy";
    public static final String EXTRA_STICKER_PACK_TRAY_ICON = "sticker_pack_tray_icon";
    public static final String EXTRA_SHOW_UP_BUTTON = "show_up_button";
    public static final String EXTRA_STICKER_PACK_DATA = "sticker_pack";

    public void buildAndDownloadAdd(ArrayList<StickerModel> stickerModels, Activity mContext, Fragment fragment, String identifier, boolean isAnimated) {
        if (stickerModels.size() >= 3 && stickerModels.size() <= 30) {
            Log.e(TAG, "buildAndDownloadAdd: " + stickerModels.size());
            this.mContext = mContext;
            this.fragment = fragment;
            if (stickerModels == null || stickerModels.size() == 0) {
                showInfoMsgDlg("", "No sticker exist");
                return;
            }

            DataArchiver.removeStickerBookJSON(mContext);//todo deleted for temp check if any error
            StickerBook.clear();
            StickerBook.init(mContext);
            ArrayList<StickerPackApi> packApis = StickerBook.getAllStickerPacks();
            if (packApis != null && packApis.size() > 0) {
                stickerPackApi = packApis.get(0);
            }

            if (stickerPackApi == null)
                stickerPackApi = new StickerPackApi();

            stickerPackApi.setAndroidPlayStoreLink("https://play.google.com/store/apps/details?id=" + mContext.getPackageName());
            stickerPackApi.setIosAppStoreLink("");
            stickerPackApi.setName(identifier);
            stickerPackApi.setPrivacyPolicyWebsite("");
            stickerPackApi.setPublisher(mContext.getResources().getString(R.string.app_name));
            stickerPackApi.setPublisherEmail(""); //add it later
            stickerPackApi.setPublisherWebsite(""); //add it later
            stickerPackApi.setTotalSize(100); //add it later
            stickerPackApi.setIdentifier(identifier);
            stickerPackApi.setTray_image_file_name("icon");
            stickerPackApi.setImageDataVersion("" + Preferences.getInt(Preferences.KEY_VERSION));
            stickerPackApi.setTrayImageUri(getTrayIconUri(stickerPackApi.identifier, "icon"));
            stickerPackApi.setTray_image_file(stickerModels.get(0).getStickerUrl()); //set 1st sticker as a tray icon for now later have to change with app icon
            stickerPackApi.setAnimatedStickerPack(isAnimated); //either all sticker should be animated or all should be non animated, mix sticker will cause error

            List<StickerApi> stickerApiList = new ArrayList<>();
            if (stickerPackApi.getStickers() != null) {
                stickerApiList = stickerPackApi.getStickers();
            }

            for (int i = 0; i < stickerModels.size(); i++) {

                StickerApi stickerApi = new StickerApi();
                stickerApi.setImage_file(stickerModels.get(i).getStickerUrl());
                stickerApi.setUri(getStickerIconUri(stickerPackApi.identifier, String.valueOf(stickerModels.get(i).getId())/*String.valueOf(i + 1)*/));
                stickerApi.setImage_file_name(String.valueOf(stickerModels.get(i).getId()));

                List<String> emojis = new ArrayList<>();
                emojis.add("\uD83D\uDE42");
                stickerApi.setEmojis(emojis);

                int index = stickerApiList.indexOf(stickerApi);
                if (index != -1) {
                    stickerApiList.set(index, stickerApi);
                } else {
                    stickerApiList.add(stickerApi);
                }
            }
            stickerPackApi.setStickers(stickerApiList);

            if (stickerPackApi != null) {
                stickerPackApi.setIsWhitelisted(WhitelistCheck.isWhitelisted(mContext, stickerPackApi.getIdentifier())); //set sticker pack as white listed.
                ArrayList<StickerPackApi> stickerPackList = new ArrayList<>();
                stickerPackList.add(stickerPackApi);
                DataArchiver.writeStickerBookJSON(stickerPackList, mContext);
            }

            stringsImagesUri.add(stickerPackApi.getTrayImageUri().getPath());
            stringsImagesFileURL.add(stickerPackApi.getTray_image_file());

            for (int i = 0; i < stickerPackApi.getStickers().size(); i++) {
                stringsImagesUri.add(stickerPackApi.getStickers().get(i).getUri().getPath());
                stringsImagesFileURL.add(stickerPackApi.getStickers().get(i).getImage_file());
            }

            try {
                File file = new File(mContext.getFilesDir() + "/" + stickerPackApi.getIdentifier());
                deleteRecursive(file);
                saveTrayIcon();
            } catch (Exception e) {
                e.printStackTrace();
            }

            download();
        } else {
            new MaterialAlertDialogBuilder(mContext, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setTitle("Stickers Bank")
                    .setMessage("Selected stickers should be minimum 3 to maximum 30")
                    .setCancelable(false)
                    .setPositiveButton("Ok", null)
                    .show();
        }
    }

    public void showInfoMsgDlg(String title, String msg) {
        new MaterialAlertDialogBuilder(mContext, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(mContext.getString(R.string.ok), (dialogInterface, i) -> {

                })
                .show();
    }

    public Uri getTrayIconUri(String identifier, String trayIcon) {

        try {
            ImageManipulation.dirChecker(mContext.getFilesDir() + "/" + identifier);
            String path = mContext.getFilesDir() + "/" + identifier + "/" + identifier + "-" + trayIcon + ".png";
            return Uri.fromFile(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Uri.parse("");
    }

    public Uri getStickerIconUri(String identifier, String stickerIcon) {
        try {
            ImageManipulation.dirChecker(mContext.getFilesDir() + "/" + identifier);
            String path = mContext.getFilesDir() + "/" + identifier + "/" + identifier + "-" + stickerIcon + ".webp";
            return Uri.fromFile(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Uri.parse("");
    }

    public void download() {
        if (stringsImagesUri.size() > 0) {
            showStickerProgressDialog();
            totalImages = 0;
            faildImages = 0;
            downloadeImages = 0;
            downloadImages();
        } else {
            showToastMsg("Something went wrong");
        }
    }

    private void downloadImages() {
        if (totalImages < stringsImagesUri.size()) {

            if (!new File(stringsImagesUri.get(totalImages)).exists()) {
                //DownloadImagesToCacheTask(stickerPack.identifier, stringsImagesFileURL.get(totalImages));
                //DownloadImagesToCacheTaskUsingPicaso(stickerPack.identifier, stringsImagesFileURL.get(totalImages));
                //new DownloadImagesToCacheTask(stickerPackApi.identifier, stringsImagesFileURL.get(totalImages)).execute();
                if (!stickerPackApi.isAnimatedStickerPack()) {
                    new DownloadImagesToCacheTask(stickerPackApi.identifier, stringsImagesFileURL.get(totalImages)).execute();
                } else {
                    //to download animated webp as it is without compress
                    downloadImagesTask(stickerPackApi.identifier, stringsImagesFileURL.get(totalImages), stringsImagesUri.get(totalImages));
                }
            } else {
                totalImages++;
                pDialog.setProgress((totalImages * 100) / stringsImagesUri.size());
                downloadeImages++;
                downloadImages();
            }
        } else {
            //mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file:///data/user/0/" + mContext.getPackageName() + "/files/" + Constants.IDENTIFIER + "/")));
            hideStickerDialog();
            //addStickerPackToWhatsApp(stickerPackApi.identifier, stickerPackApi.name);
            addStickerPackToWhatsAppDirect(stickerPackApi.identifier, stickerPackApi.name);
        }
    }

    private void downloadImagesTask(String identifier, String path, String local_path) {
        ImageManipulation.dirChecker(mContext.getFilesDir() + "/" + identifier);
        String fileName = local_path.substring(local_path.lastIndexOf('/') + 1);

        PRDownloader.download(path, mContext.getFilesDir() + "/" + identifier, fileName)
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {

                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {

                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {

                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {

                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        totalImages++;
                        pDialog.setProgress((totalImages * 100) / stringsImagesUri.size());
                        downloadeImages++;
                        downloadImages();
                    }

                    @Override
                    public void onError(Error error) {

                    }
                });
    }

    private class DownloadImagesToCacheTask extends AsyncTask<String, Void, Bitmap> {

        String identifier, path;

        public DownloadImagesToCacheTask(String identifier, String path) {
            this.identifier = identifier;
            this.path = path;
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            /*FutureTarget<File> future = Glide.with(getApplicationContext())
                    .load(params[0])
                    .downloadOnly(300, 300);

            *//*.downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);*//*

            File file = null;
            try {
                file = future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            try {
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(true);
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (bitmap != null) {

                int index = path.lastIndexOf('/');
                String fileLocalName = path.substring(index).replace("/", "").replace(".png", "");

                ImageManipulation.convertSaveImageToWebP(bitmap, identifier, stringsImagesUri.get(totalImages), mContext);

                totalImages++;
                pDialog.setProgress((totalImages * 100) / stringsImagesUri.size());
                downloadeImages++;
            }
            /*mImagesDownloadMsg = "Downloding Images..." + arrayListImages.size() + "/" + totalImages + "\nSuccess " + downloadeImages + "\nFailed " + faildImages;
            mProgressDialogImage.setMessage(mImagesDownloadMsg);*/
            downloadImages();
        }
    }

    protected void showStickerProgressDialog() {
        try {
            if (pDialog == null) {
                pDialog = new ProgressDialog(mContext);
                pDialog.setMessage("Downloading Sticker Pack...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(false);
            }
            if (!pDialog.isShowing())
                pDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void hideStickerDialog() {
        try {
            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showToastMsg(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    protected void addStickerPackToWhatsApp(String identifier, String stickerPackName) {
        try {
            //if neither WhatsApp Consumer or WhatsApp Business is installed, then tell user to install the apps.
            if (!WhitelistCheckApi.isWhatsAppConsumerAppInstalled(mContext.getPackageManager()) && !WhitelistCheckApi.isWhatsAppSmbAppInstalled(mContext.getPackageManager())) {
                Toast.makeText(mContext, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG).show();
                return;
            }
            final boolean stickerPackWhitelistedInWhatsAppConsumer = WhitelistCheckApi.isStickerPackWhitelistedInWhatsAppConsumer(mContext, identifier);
            final boolean stickerPackWhitelistedInWhatsAppSmb = WhitelistCheckApi.isStickerPackWhitelistedInWhatsAppSmb(mContext, identifier);
            if (!stickerPackWhitelistedInWhatsAppConsumer && !stickerPackWhitelistedInWhatsAppSmb) {
                //ask users which app to add the pack to.
                launchIntentToAddPackToChooser(identifier, stickerPackName);
            } else if (!stickerPackWhitelistedInWhatsAppConsumer) {
                launchIntentToAddPackToSpecificPackage(identifier, stickerPackName, WhitelistCheckApi.CONSUMER_WHATSAPP_PACKAGE_NAME);
            } else if (!stickerPackWhitelistedInWhatsAppSmb) {
                launchIntentToAddPackToSpecificPackage(identifier, stickerPackName, WhitelistCheckApi.SMB_WHATSAPP_PACKAGE_NAME);
            } else {
                Toast.makeText(mContext, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "error adding sticker pack to WhatsApp", e);
            Toast.makeText(mContext, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG).show();
        }
    }

    protected void addStickerPackToWhatsAppDirect(String identifier, String stickerPackName) {
        Log.e(TAG, "addStickerPackToWhatsAppDirect:identifier " + identifier);
        Log.e(TAG, "addStickerPackToWhatsAppDirect:stickerPackName " + stickerPackName);
        /*addStickerPackToWhatsAppDirect:identifier feature_1711114924901
        addStickerPackToWhatsAppDirect:stickerPackName feature_1711114924901*/

        Intent intent = new Intent();
        intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra(EXTRA_STICKER_PACK_ID, identifier);
        intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, BuildConfig.CONTENT_PROVIDER_AUTHORITY_API);
        intent.putExtra(EXTRA_STICKER_PACK_NAME, stickerPackName);
        try {
            if (fragment != null) {
                fragment.startActivityForResult(intent, ADD_PACK);
            } else {
                mContext.startActivityForResult(intent, ADD_PACK);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.error_adding_sticker_pack, Toast.LENGTH_LONG).show();
        }
    }

    //Handle cases either of WhatsApp are set as default app to handle this intent. We still want users to see both options.
    private void launchIntentToAddPackToChooser(String identifier, String stickerPackName) {
        Intent intent = createIntentToAddStickerPack(identifier, stickerPackName);
        try {
            if (fragment != null) {
                fragment.startActivityForResult(Intent.createChooser(intent, mContext.getString(R.string.add_to_whatsapp)), ADD_PACK);
            } else {
                mContext.startActivityForResult(Intent.createChooser(intent, mContext.getString(R.string.add_to_whatsapp)), ADD_PACK);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private Intent createIntentToAddStickerPack(String identifier, String stickerPackName) {
        Intent intent = new Intent();
        intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra(EXTRA_STICKER_PACK_ID, identifier);
        intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, BuildConfig.CONTENT_PROVIDER_AUTHORITY_API);
        intent.putExtra(EXTRA_STICKER_PACK_NAME, stickerPackName);
        return intent;
    }

    private void launchIntentToAddPackToSpecificPackage(String identifier, String stickerPackName, String whatsappPackageName) {
        Intent intent = createIntentToAddStickerPack(identifier, stickerPackName);
        intent.setPackage(whatsappPackageName);
        try {
            if (fragment != null) {
                fragment.startActivityForResult(intent, ADD_PACK);
            } else {
                mContext.startActivityForResult(intent, ADD_PACK);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG).show();
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    private void saveTrayIcon() {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_tray_icon);
            Bitmap convertedImage = getResizedBitmap(largeIcon, 128);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            convertedImage.compress(Bitmap.CompressFormat.PNG, 40, bytes);

            // you can create a new file name "test.jpg" in sdcard folder.
            File f = Utils.getTrayIconUri(mContext, stickerPackApi.getIdentifier(), "icon");

            f.createNewFile();

            // write the bytes in file
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());

            // remember close de FileOutput
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

}

