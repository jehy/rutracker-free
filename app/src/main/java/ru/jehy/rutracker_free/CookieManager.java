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

    static String get(Context mContext) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        //if(!settings.contains(KEY))
        //    return null;
        String access_token = settings.getString(KEY, null);
        if (access_token == null)
            Log.d("TokenManager", "No token stored! ");
        else
            Log.d("TokenManager", "Got token " + access_token);
        return access_token;
    }
    @SuppressLint("CommitPrefEdits")
    static void clear(Context mContext)
    {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(KEY);
        editor.commit();
        Log.d("TokenManager", "Cleared saved cookie");
    }

    @SuppressLint("CommitPrefEdits")
    static void put(Context mContext, String token) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY, token);
        Log.d("TokenManager", "Saved token " + token);
        editor.commit();
    }
}

