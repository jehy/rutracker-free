package ru.jehy.rutracker_free;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Bond on 01-Dec-15.
 */
 class CookieManager {
     private static final String KEY = "cookie";
    private static final String TAG = "CookieManager";

     static String get(Context mContext) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        //if(!settings.contains(KEY))
        //    return null;
        String value = settings.getString(KEY, null);
        if (value == null) {
            Log.d(TAG, "No value stored! ");
        } else {
            Log.d(TAG, "Got value " + value);
        }
        return value;
    }

    @SuppressLint("CommitPrefEdits")
     static void clear(Context mContext) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(KEY);
        editor.apply();
        Log.d(TAG, "Cleared saved cookie");
    }

    @SuppressLint("CommitPrefEdits")
     static void put(Context mContext, String token) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY, token);
        Log.d(TAG, "Saved token " + token);
        editor.apply();
    }
}

