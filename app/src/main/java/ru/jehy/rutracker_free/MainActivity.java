package ru.jehy.rutracker_free;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import java.io.IOException;

import ru.jehy.rutracker_free.updater.AppUpdate;
import ru.jehy.rutracker_free.updater.AppUpdateUtil;
import ru.jehy.rutracker_free.updater.DownloadUpdateService;
import ru.jehy.rutracker_free.updater.UpdateBroadcastReceiver;

import static ru.jehy.rutracker_free.RutrackerApplication.onionProxyManager;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;


@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String ACTION_SHOW_UPDATE_DIALOG = "ru.jehy.rutracker_free.SHOW_UPDATE_DIALOG";
    private final UpdateBroadcastReceiver showUpdateDialog = new UpdateBroadcastReceiver();
    public ShareActionProvider mShareActionProvider;
    private boolean updateChecked = false;

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
        Fabric.with(this, new Crashlytics());
        Log.d(TAG, "OnCreate");
        if (updateChecked) {
            return;
        }
        //first init
        Thread updateThread = new Thread() {
            @Override
            public void run() {
                AppUpdateUtil.checkForUpdate(MainActivity.this);
                MainActivity.this.updateChecked = true;
            }
        };
        updateThread.start();
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
        Log.d(TAG, "onResume");
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        RutrackerWebView myWebView = (RutrackerWebView) MainActivity.this.findViewById(R.id.myWebView);
        showUpdateDialog.register(this, new IntentFilter(ACTION_SHOW_UPDATE_DIALOG));

        new TorProgressTask(MainActivity.this).execute();

        String loaded = myWebView.getOriginalUrl();
        RutrackerApplication appState = ((RutrackerApplication) getApplicationContext());
        try {
            if (loaded == null && onionProxyManager.isRunning())
                myWebView.loadUrl(appState.currentUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

