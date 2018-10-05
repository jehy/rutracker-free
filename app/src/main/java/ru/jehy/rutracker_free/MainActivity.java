package ru.jehy.rutracker_free;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import java.io.File;
import java.io.IOException;

import io.fabric.sdk.android.Fabric;

import static ru.jehy.rutracker_free.RutrackerApplication.onionProxyManager;


@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public final static int PERMISSION_SAVE_FILE = 2;
    //public ShareActionProvider mShareActionProvider;
    private Menu optionsMenu;
    private Intent shareIntent;
    private Intent shareLinkIntent;
    private String sharingFileName;


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_SAVE_FILE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "PERMISSION_SAVE_FILE granted");
                    this.shareFile(this.sharingFileName);

                } else {
                    Log.w(TAG, "PERMISSION_SAVE_FILE NOT granted");
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
        Log.d(TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
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
        new TorProgressTask(MainActivity.this).execute();
        initWebView();
        AppUpdater appUpdater = new AppUpdater(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("jehy", "rutracker-free");
        appUpdater.start();
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
                    while (onionProxyManager == null || !onionProxyManager.isRunning()) {
                        Thread.sleep(90);
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
                if (loaded == null) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myWebView.loadUrl(appState.currentUrl);
                        }
                    });
                }

            }
        };
        checkTorThread.start();
    }

    public void setShareIntent(final Intent shareIntent) {
        this.shareIntent = shareIntent;
        MenuItem item = optionsMenu.findItem(R.id.menu_item_share);
        String msg = getResources().getString(R.string.action_share);
        setIntent(item, shareIntent, msg);
    }

    public void setShareLinkIntent(final Intent shareIntent) {
        this.shareLinkIntent = shareIntent;
        MenuItem item = optionsMenu.findItem(R.id.menu_item_magnet);
        String msg = getResources().getString(R.string.action_share_magnet);
        setIntent(item, shareIntent, msg);
    }

    public void shareMagnet() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (shareIntent == null) {
                    return;
                }
                String msg = getResources().getString(R.string.action_share_magnet);
                startActivity(Intent.createChooser(shareIntent, msg));
            }
        });
    }

    public void shareFile(final String fileName) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (fileName == null) {
                    return;
                }
                MainActivity.this.sharingFileName = fileName;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Log.w(TAG, "no permission to write external storage, requesting");
                        String[] require = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                        MainActivity.this.requestPermissions(require, PERMISSION_SAVE_FILE);
                        return;
                    }
                }
                File fileFrom = new File(MainActivity.this.getFilesDir(), fileName);
                String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                String fromPath = MainActivity.this.getFilesDir().getPath();
                Utils.copyFile(fromPath + "/", fileName, downloadsPath + "/");
                fileFrom.delete();
                File fileDownloaded = new File(downloadsPath, fileName);
                DownloadManager downloadManager = (DownloadManager) MainActivity.this.getSystemService(MainActivity.DOWNLOAD_SERVICE);
                downloadManager.addCompletedDownload(fileDownloaded.getName(), fileDownloaded.getName(), true, "application/x-bittorrent", fileDownloaded.getAbsolutePath(), fileDownloaded.length(), true);

                Log.d(TAG, "Sharing file");
                String msg = getResources().getString(R.string.action_share_file);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                //File file = new File(MainActivity.this.getFilesDir(), fileName);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(fileDownloaded));
                shareIntent.setType("application/x-bittorrent");
                startActivity(Intent.createChooser(shareIntent, msg));
            }
        });
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

