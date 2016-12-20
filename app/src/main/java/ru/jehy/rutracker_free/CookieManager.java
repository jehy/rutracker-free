package ru.jehy.rutracker_free;

/**
 * Created by Bond on 2016-03-14.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Bond on 01-Dec-15.
 */
public class CookieManager {
    public static final String KEY = "cookie";
    private static final String TAG = "CookieManager";

    public static String get(Context mContext) {
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
    public static void clear(Context mContext) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(KEY);
        editor.commit();
        Log.d(TAG, "Cleared saved cookie");
    }

    @SuppressLint("CommitPrefEdits")
    public static void put(Context mContext, String token) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY, token);
        Log.d(TAG, "Saved token " + token);
        editor.commit();
    }
}

