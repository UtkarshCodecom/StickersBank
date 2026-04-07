package com.stickers.bank.utils;


import android.content.Context;
import android.util.Log;

import com.stickers.bank.data.model.StickerPackApi;

import java.io.File;
import java.util.ArrayList;

public class StickerBook {

    private static final String TAG = StickerBook.class.getName();
    static Context myContext;
    public static ArrayList<StickerPackApi> allStickerPacks = checkIfPacksAreNull();

    public static void init(Context context) {
        myContext = context;
        ArrayList<StickerPackApi> lsp = DataArchiver.readStickerPackJSON(context);
        if (lsp != null && lsp.size() != 0) {
            allStickerPacks = lsp;
        }
    }

    public static void clear() {
        allStickerPacks = new ArrayList<>();
    }

    private static ArrayList<StickerPackApi> checkIfPacksAreNull() {
        Log.e(TAG, "checkIfPacksAreNull: ");
        if (allStickerPacks == null) {
            Log.e(TAG + "  IS PACKS NULL?", "YES");
            return new ArrayList<>();
        }
        Log.e(TAG + "  IS PACKS NULL?", "NO");
        return allStickerPacks;
    }

    /*public static String addNewStickerPack(String name, String publisher, String trayImage){
        String newId = UUID.randomUUID().toString();
        StickerPackApi sp = new StickerPackApi(newId,
                name,
                publisher,
                trayImage,
                "",
                "",
                "",
                "");
        allStickerPacks.add(sp);
        return newId;
    }*/

    public static void addStickerPackExisting(StickerPackApi sp) {
        allStickerPacks.add(sp);
    }

    public static ArrayList<StickerPackApi> getAllStickerPacks() {
        return allStickerPacks;
    }

    public static StickerPackApi getStickerPackByName(String stickerPackName) {
        for (StickerPackApi sp : allStickerPacks) {
            if (sp.getName().equals(stickerPackName)) {
                return sp;
            }
        }
        return null;
    }

    public static StickerPackApi getStickerPackById(String stickerPackId) {
        if (allStickerPacks.isEmpty()) {
            init(myContext);
        }
        Log.w("THIS IS THE ALL STICKER", allStickerPacks.toString());
        for (StickerPackApi sp : allStickerPacks) {
            if (sp.getIdentifier().equals(stickerPackId)) {
                return sp;
            }
        }
        return null;
    }

    public static StickerPackApi getStickerPackByIdWithContext(String stickerPackId, Context context) {
        if (allStickerPacks.isEmpty()) {
            init(context);
        }
        for (StickerPackApi sp : allStickerPacks) {
            if (sp.getIdentifier().equals(stickerPackId)) {
                return sp;
            }
        }
        return null;
    }

    public static void deleteStickerPackById(String stickerPackId) {
        StickerPackApi myStickerPack = getStickerPackById(stickerPackId);
        new File(myStickerPack.getTrayImageUri().getPath()).getParentFile().delete();
        allStickerPacks.remove(myStickerPack);
    }

    public static StickerPackApi getStickerPackByIndex(int index) {
        return allStickerPacks.get(index);
    }
}
