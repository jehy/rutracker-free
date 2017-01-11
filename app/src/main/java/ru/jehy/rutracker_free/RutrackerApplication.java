package ru.jehy.rutracker_free;

import android.app.Application;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

/**
 * Created by Jehy on 18.10.2016.
 */

public class RutrackerApplication extends Application {
    public static OnionProxyManager onionProxyManager = null;
    public String currentUrl = Rutracker.MAIN_URL;

    @Override
    public void onCreate() {
        super.onCreate();
        final String fileStorageLocation = "torfiles";

        Thread initThread = new Thread() {
            @Override
            public void run() {
                onionProxyManager =
                        new AndroidOnionProxyManager(RutrackerApplication.this, fileStorageLocation);
            }
        };
        initThread.start();
    }
}
