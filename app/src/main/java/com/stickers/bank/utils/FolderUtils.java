package com.stickers.bank.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stickers.bank.R;
import com.stickers.bank.data.database.DatabaseClient;
import com.stickers.bank.data.database.entity.StickerFolder;
import com.stickers.bank.data.database.entity.StickerModelDB;
import com.stickers.bank.data.listeners.InterstitialAdvListener;
import com.stickers.bank.data.model.StickerModel;
import com.stickers.bank.ui.fragments.NewArrivalFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FolderUtils {
    public static class AddFolder extends AsyncTask<Void, Void, Void> {
        String folder_name;
        ArrayList<StickerModel> stickerModels;
        boolean is_checked;
        Activity activity;

        public AddFolder(Activity act, String folder_name, boolean is_checked, ArrayList<StickerModel> stickerModels) {
            this.activity = act;
            this.folder_name = folder_name;
            this.stickerModels = stickerModels;
            this.is_checked = is_checked;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //adding to database
            StickerFolder stickerFolder = new StickerFolder();
            stickerFolder.setFolderName(folder_name);
            stickerFolder.setIs_animated(is_checked);
            DatabaseClient.getInstance(activity).getAppDatabase().getStickerDao().insertFolder(stickerFolder);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            /*if (stickerModels.size() > 3)
            new FolderUtils.GetAllFolders(activity, stickerModels).execute();*/
        }
    }

    public static class GetAllFolders extends AsyncTask<Void, Void, List<StickerFolder>> {
        ArrayList<StickerModel> stickerModels;
        Activity activity;

        public GetAllFolders(Activity act, ArrayList<StickerModel> newArrivalModels) {
            this.stickerModels = newArrivalModels;
            this.activity = act;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<StickerFolder> doInBackground(Void... voids) {
            //adding to database
            List<StickerFolder> arrayList = DatabaseClient.getInstance(activity).getAppDatabase().getStickerDao().getAllFolders();
            return arrayList;
        }

        @Override
        protected void onPostExecute(List<StickerFolder> stickerFolderList) {
            super.onPostExecute(stickerFolderList);
            //new FolderUtils.AddAllStickers(activity, stickerModels, stickerFolderList.get(stickerFolderList.size() - 1).getId(), stickerFolderList.get(stickerFolderList.size() - 1).getFolderName()).execute();

        }
    }

    private static class AddAllStickers extends AsyncTask<Void, Void, Void> {
        Activity activity;
        ArrayList<StickerModel> stkrModels;
        int folderId;
        String name;

        public AddAllStickers(Activity act, ArrayList<StickerModel> stickerModels, int folderId, String name) {
            this.activity = act;
            this.stkrModels = stickerModels;
            this.folderId = folderId;
            this.name = name;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //adding to database
            List<StickerModelDB> stickerModelDBList = new ArrayList<>();

            for (int i = 0; i < stkrModels.size(); i++) {
                StickerModelDB db = new StickerModelDB();
                db.setId(stkrModels.get(i).getId());
                db.setFolderId(folderId);
                db.setMainCategoryId(stkrModels.get(i).getMainCategoryId());
                db.setSubCategoryId(stkrModels.get(i).getSubCategoryId());
                db.setSubSubCategoryId(stkrModels.get(i).getSubSubCategoryId());
                db.setStickerImage(stkrModels.get(i).getStickerImage());
                db.setIsActive(stkrModels.get(i).getIsActive());
                db.setCreatedAt(stkrModels.get(i).getCreatedAt());
                db.setStickerUrl(stkrModels.get(i).getStickerUrl());
                stickerModelDBList.add(db);
            }

            DatabaseClient.getInstance(activity).getAppDatabase().getStickerDao().insert(stickerModelDBList);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            /*new MaterialAlertDialogBuilder(activity, R.style.MyThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setCancelable(false)
                    .setMessage("Sticker added to " + name)
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();*/

            Log.e("TAG", "onPostExecute: " + "Stickers aaded in folder successfully");


        }
    }


    public static String getRandomString(final int sizeOfRandomString) {
        /*final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();*/

        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        return ts.toString();
    }

}