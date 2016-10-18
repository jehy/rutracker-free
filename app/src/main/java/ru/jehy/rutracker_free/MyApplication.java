package ru.jehy.rutracker_free;

import android.app.Application;

/**
 * Created by Jehy on 18.10.2016.
 */

public class MyApplication extends Application {
    public String currentUrl=Rutracker.mainUrl;
    @Override
    public void onCreate()
    {
        super.onCreate();
    }
}
