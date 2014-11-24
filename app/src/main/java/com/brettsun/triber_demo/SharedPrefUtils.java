package com.brettsun.triber_demo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Convenience class to obtain this application's default shared preferences
 */
public class SharedPrefUtils {
    private static final String TRIBER_DEMO_PREFS_NAME = "com.brettsun.triber_demo.DEFAULT_PREFS";

    private static Context mContext;

    public static void init(Context context) {
        // getApplicationContext() is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        mContext = context.getApplicationContext();
    }

    public static String getString(String key, String defValue) {
        return getmPrefs().getString(key, defValue);
    }

    public static int getInt(String key, int defVal) {
        return getmPrefs().getInt(key, defVal);
    }

    public static long getLong(String key, long defVal) {
        return getmPrefs().getLong(key, defVal);
    }

    public static boolean getBoolean(String key, boolean defVal) {
        return getmPrefs().getBoolean(key, defVal);
    }

    public static void putInt(String key, int value) {
        getmPrefs().edit().putInt(key, value).apply();
    }

    public static void putLong(String key, long value) {
        getmPrefs().edit().putLong(key, value).apply();
    }

    public static void putString(String key, String value) {
        getmPrefs().edit().putString(key, value).apply();
    }

    public static void putBoolean(String key, boolean value) {
        getmPrefs().edit().putBoolean(key, value).apply();
    }

    public static void remove(String key) {
        getmPrefs().edit().remove(key).apply();
    }

    public static void clear() {
        getmPrefs().edit().clear().apply();
    }

    public static boolean contains(String key) {
        return getmPrefs().contains(key);
    }

    public static SharedPreferences getmPrefs() {
        return mContext.getSharedPreferences(TRIBER_DEMO_PREFS_NAME, Context.MODE_PRIVATE);
    }

}
