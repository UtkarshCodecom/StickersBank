package com.stickers.bank.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stickers.bank.StickersBankApplication;
import com.stickers.bank.data.common.Constants;
import com.stickers.bank.data.model.User;

import java.lang.reflect.Type;

public class Preferences {

    public static final String KEY_USER_DATA = "key_user_data";
    public static final String KEY_IS_LOGGED_IN = "key_is_logged_in";
    public static final String KEY_VERSION = "key_version";

    private static SharedPreferences get() {
        return StickersBankApplication.getAppContext().getSharedPreferences(Constants.MY_PREFS, Context.MODE_PRIVATE);
    }

    public static boolean removePref() {
        return get().edit().clear().commit();
    }

    public static boolean setString(String _key, String value) {
        return get().edit().putString(_key, value).commit();
    }

    public static boolean setDouble(String _key, Float value) {
        return get().edit().putFloat(_key, value).commit();
    }

    public static float getDouble(String _key) {
        return get().getFloat(_key, 0.0f);
    }

    public static String getString(String _key) {
        return get().getString(_key, null);
    }

    public static int getInt(String _key) {
        return get().getInt(_key, 0);
    }

    public static boolean setInt(String _key, int value) {
        return get().edit().putInt(_key, value).commit();
    }

    public static boolean getBoolean(String _key) {
        return get().getBoolean(_key, false);
    }

    public static boolean setBoolean(String _key, boolean value) {
        return get().edit().putBoolean(_key, value).commit();
    }

    public static void remove(String _key) {
        get().edit().remove(_key).commit();
    }

    public static boolean isContainKey(String _key) {
        return get().contains(_key);
    }

    public static User getUserData() {
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<User>() {
            }.getType();
            return gson.fromJson(getString(KEY_USER_DATA), type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setUserData(User profile) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(profile);
            setString(KEY_USER_DATA, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}