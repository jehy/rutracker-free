package ru.jehy.rutracker_free;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;
import ru.jehy.rutracker_free.updater.AppUpdate;
import ru.jehy.rutracker_free.updater.AppUpdateUtil;
import ru.jehy.rutracker_free.updater.DownloadUpdateService;
import ru.jehy.rutracker_free.updater.UpdateBroadcastReceiver;

import static ru.jehy.rutracker_free.RutrackerApplication.onionProxyManager;


@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String ACTION_SHOW_UPDATE_DIALOG = "ru.jehy.rutracker_free.SHOW_UPDATE_DIALOG";
    public final static int PERMISSION_WRITE = 1;
    private final UpdateBroadcastReceiver showUpdateDialog = new UpdateBroadcastReceiver();
    //public ShareActionProvider mShareActionProvider;
    private boolean updateChecked = false;
    private Menu optionsMenu;

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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "PERMISSION_WRITE granted for updater");
                    AppUpdateUtil.startUpdate(this);

                } else {
                    Log.w(TAG, "PERMISSION_WRITE NOT granted for updater");
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbar, menu);
        optionsMenu = menu;
        MenuItem item = menu.findItem(R.id.menu_item_share);
        item.setVisible(false);
        item = menu.findItem(R.id.menu_item_magnet);
        item.setVisible(false);
        //mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        //this.invalidateOptionsMenu();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OnCreate");
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build();

        Crashlytics crashlytics = new Crashlytics.Builder()
                .core(crashlyticsCore)
                .build();

        Answers answers = new Answers();

        Fabric fabric = new Fabric.Builder(this)
                .kits(crashlytics, answers)
                .build();

        Fabric.with(fabric);
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
        showUpdateDialog.register(this, new IntentFilter(ACTION_SHOW_UPDATE_DIALOG));
        new TorProgressTask(MainActivity.this).execute();
        initWebView();
        checkUpdates();
    }

    public void initWebView() {
        /*
        * That function looks damn bad. But we need to call onionProxyManager.isRunning from non UI thread
        * and then we need to call myWebView.loadUrl from UI thread...
        * */
        final RutrackerWebView myWebView = (RutrackerWebView) MainActivity.this.findViewById(R.id.myWebView);
        final String loaded = myWebView.getOriginalUrl();
        final RutrackerApplication appState = ((RutrackerApplication) getApplicationContext());

        Thread checkTorThread = new Thread() {
            @Override
            public void run() {
                try {
                    if (loaded == null && onionProxyManager.isRunning())
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                myWebView.loadUrl(appState.currentUrl);
                            }
                        });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        checkTorThread.start();
    }

    public void checkUpdates() {

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


    public void setShareIntent(final Intent shareIntent) {
        MenuItem item = optionsMenu.findItem(R.id.menu_item_share);
        String msg = getResources().getString(R.string.action_share);
        setIntent(item, shareIntent, msg);
    }

    public void setShareLinkIntent(final Intent shareIntent) {
        MenuItem item = optionsMenu.findItem(R.id.menu_item_magnet);
        String msg = getResources().getString(R.string.action_share_magnet);
        setIntent(item, shareIntent, msg);
    }

    public void setIntent(final MenuItem item, final Intent shareIntent, final String title) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (shareIntent == null) {
                    item.setVisible(false);
                    return;
                }
                item.setVisible(true);
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        startActivity(Intent.createChooser(shareIntent, title));
                        return false;
                    }
                });
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

