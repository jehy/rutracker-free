package ru.jehy.rutracker_free;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import ru.jehy.rutracker_free.updater.AppUpdate;
import ru.jehy.rutracker_free.updater.AppUpdateUtil;
import ru.jehy.rutracker_free.updater.DownloadUpdateService;
import ru.jehy.rutracker_free.updater.UpdateBroadcastReceiver;


@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    public static OnionProxyManager onionProxyManager = null;
    public ShareActionProvider mShareActionProvider;
    public static final String ACTION_SHOW_UPDATE_DIALOG = "ru.jehy.rutracker_free.SHOW_UPDATE_DIALOG";
    private final UpdateBroadcastReceiver showUpdateDialog = new UpdateBroadcastReceiver();

    public static Intent createUpdateDialogIntent(AppUpdate update) {
        Intent updateIntent = new Intent(MainActivity.ACTION_SHOW_UPDATE_DIALOG);
        updateIntent.putExtra("update", update);
        return updateIntent;
    }


    public static boolean isAppBeingUpdated(Context context) {

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterByStatus(DownloadManager.STATUS_RUNNING);
        Cursor c = downloadManager.query(q);
        if (c.moveToFirst()) {
            String fileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
            if (fileName.equals(DownloadUpdateService.DOWNLOAD_UPDATE_TITLE)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbar, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        //this.invalidateOptionsMenu();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Rutracker free", "OnCreate");
        if (onionProxyManager != null)
            return;
        //first init
        Thread updateThread = new Thread() {
            @Override
            public void run() {
                AppUpdateUtil.checkForUpdate(MainActivity.this);
            }
        };
        updateThread.start();
        String fileStorageLocation = "torfiles";
        onionProxyManager =
                new AndroidOnionProxyManager(this, fileStorageLocation);

    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d("Rutracker free", "onPause");
        showUpdateDialog.unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onStop();
        Log.d("Rutracker free", "onDestroy");
        /*if(onionProxyManager!=null)
            try {
                onionProxyManager.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("Rutracker free", "onResume");
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        MyWebView myWebView = (MyWebView) MainActivity.this.findViewById(R.id.myWebView);
        if (!myWebView.setUp)
            setUpWebView(myWebView);
        showUpdateDialog.register(this, new IntentFilter(ACTION_SHOW_UPDATE_DIALOG));
        try {
            if (!onionProxyManager.isRunning())
                new TorPogressTask(MainActivity.this).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //onionProxyManager.isRunning is a surprisingly heavy operation and should not be done on main thread...


        String loaded = myWebView.getOriginalUrl();
        MyApplication appState = ((MyApplication) getApplicationContext());
        try {
            if (loaded == null && onionProxyManager.isRunning())
                myWebView.loadUrl(appState.currentUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setUpWebView(MyWebView myWebView) {
        myWebView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= 21) {
            MyWebViewClient webClient = new MyWebViewClient(MainActivity.this);
            myWebView.setWebViewClient(webClient);
        } else {
            MyWebViewClientOld webClient = new MyWebViewClientOld(MainActivity.this);
            myWebView.setWebViewClient(webClient);
        }
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setDisplayZoomControls(false);
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
    }

    public void setShareIntent(final Intent shareIntent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mShareActionProvider != null) {
                    mShareActionProvider.setShareIntent(shareIntent);
                }

            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    WebView myWebView = (WebView) findViewById(R.id.myWebView);
                    assert myWebView != null;
                    if (myWebView.canGoBack()) {
                        myWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
}

