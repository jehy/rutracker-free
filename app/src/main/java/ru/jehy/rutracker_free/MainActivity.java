package ru.jehy.rutracker_free;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.fabric.sdk.android.Fabric;
import ru.jehy.rutracker_free.updater.AppUpdate;
import ru.jehy.rutracker_free.updater.AppUpdateUtil;
import ru.jehy.rutracker_free.updater.DownloadUpdateService;
import ru.jehy.rutracker_free.updater.UpdateBroadcastReceiver;

import static ru.jehy.rutracker_free.RutrackerApplication.onionProxyManager;


@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "MainActivity";
    public static final String ACTION_SHOW_UPDATE_DIALOG = "ru.jehy.rutracker_free.SHOW_UPDATE_DIALOG";
    static final String TORRENT_LOAD_ACTION = "ru.jehy.rutracker_free..action.BOOK_LOAD_EVENT";
    public final static int PERMISSION_UPDATE_WRITE = 1;
    public final static int PERMISSION_SAVE_FILE = 2;
     static final int START_TORRENT_LOADING = 1;
     static final int FINISH_TORRENT_LOADING = 2;
    static final String TORRENT_LOAD_EVENT = "book load event";
    private final UpdateBroadcastReceiver showUpdateDialog = new UpdateBroadcastReceiver();
    public String mMangetLink = null;
    //public ShareActionProvider mShareActionProvider;
    private boolean updateChecked = false;
    private Intent shareIntent;
    private String sharingFileName;
    private SwipeRefreshLayout mRefresher;
    private WebView mWebView;
    private String mTorrentUrl;
    public String mTorrentName = "noname";
    private boolean mShowDownloadIcon = false;
    private boolean mShowMagnetIcon = false;
    private AlertDialog mTorrentLoadingDialog;
    private TorrentLoadingReceiver mTorrentLoadReceiver;
    private boolean mShowLoginIcon;
    private SearchView mSearchView;
    private boolean mIsTorrentDownloadRequired;
    private Menu mMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_main);

        super.onCreate(savedInstanceState);
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

        // добавлю модный перезагрузчик страницы
        mRefresher = findViewById(R.id.refreshView);
        mRefresher.setOnRefreshListener(this);

        // проверю, не запущено ли приложение с помощью интента, если да- перейду на присланную страницу
        if(getIntent().getData()!=null){
            Uri data = getIntent().getData();
            RutrackerApplication.getInstance().currentUrl = data.toString();
        }

        // зарегистрирую получатель команды возвращения на предыдущую страницу
        IntentFilter filter = new IntentFilter();
        filter.addAction(TORRENT_LOAD_ACTION);
        mTorrentLoadReceiver = new TorrentLoadingReceiver();
        registerReceiver(mTorrentLoadReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        showUpdateDialog.register(this, new IntentFilter(ACTION_SHOW_UPDATE_DIALOG));
        new TorProgressTask(MainActivity.this).execute();
        initWebView();
        checkUpdates();
    }


    @Override
    public void onPause() {
        super.onPause();
        showUpdateDialog.unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTorrentLoadReceiver);
        /*if(onionProxyManager!=null)
            try {
                onionProxyManager.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
    }


    @Override
    public void onRefresh() {
        //mWebView.reload();
        mRefresher.setRefreshing(false);
    }

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
            return fileName.equals(DownloadUpdateService.DOWNLOAD_UPDATE_TITLE);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_UPDATE_WRITE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "PERMISSION_UPDATE_WRITE granted for updater");
                    AppUpdateUtil.startUpdate(this);

                } else {
                    Log.w(TAG, "PERMISSION_UPDATE_WRITE NOT granted for updater");
                }
            }
            break;
            case PERMISSION_SAVE_FILE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "PERMISSION_SAVE_FILE granted");
                    if(mIsTorrentDownloadRequired){
                        downloadTorrent();
                    }
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
        mMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbar, menu);
        menu.findItem(R.id.menu_item_download).setVisible(mShowDownloadIcon);
        menu.findItem(R.id.menu_item_magnet).setVisible(mShowMagnetIcon);
        menu.findItem(R.id.menu_item_login).setVisible(mShowLoginIcon);

        // добавлю обработку поиска
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchMenuItem.getActionView();
        if(mSearchView != null){
            mSearchView.setInputType(InputType.TYPE_CLASS_TEXT);
            mSearchView.setOnQueryTextListener(this);
        }
        else{
            Log.d("surprise", "MainActivity onCreateOptionsMenu: search view null");
        }
        //mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        //this.invalidateOptionsMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_download:
                downloadTorrent();
                return true;
            case R.id.menu_item_share:
                // отправлю ссылку на данную страницу
                shareCurrentPage();
                return true;
            case R.id.menu_item_magnet:
                shareMagnetLink();
                return true;
            case R.id.menu_item_login:
                loginToRutracker();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loginToRutracker() {
        android.app.AlertDialog.Builder ad = new android.app.AlertDialog.Builder(this);
        final View view = getLayoutInflater().inflate(R.layout.login_dialog_layout, null);
        final TextInputEditText login = view.findViewById(R.id.loginInput);
        final TextInputEditText password = view.findViewById(R.id.passInput);

        ad.setTitle(MainActivity.this.getString(R.string.login_dialog_title));
        ad.setView(view);
        ad.setNegativeButton(android.R.string.cancel, null);
        ad.setPositiveButton("Войти", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // попробую отправить форму входа
                Editable loginData = login.getText();
                String loginText = "";
                String passText = "";
                if(loginData != null){
                    loginText = loginData.toString();
                }
                Editable passData = password.getText();
                if(passData != null){
                    passText = passData.toString();
                }
                try {
                    String url = "https://rutracker.org/forum/login.php?convert_post=1" + "&login_username=" + URLEncoder.encode(loginText, "windows-1251") + "&login_password=" + URLEncoder.encode(passText, "windows-1251") + "&login=" + URLEncoder.encode("вход", "windows-1251");
                    mWebView.loadUrl(url);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        ad.create().show();
    }

    private void shareMagnetLink() {
        String shareMsg = mMangetLink;
        Log.d("surprise", "MainActivity shareMagnetLink: link is " + mMangetLink);
        Intent mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");
        mShareIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
        startActivity(Intent.createChooser(mShareIntent, getString(R.string.action_share_magnet)));
    }

    private void shareCurrentPage() {
        String shareMsg = "Посмотри, что я нашёл на рутрекере при помощи приложения rutracker free: \n" + RutrackerApplication.getInstance().currentUrl;
        if(mMangetLink != null){
            shareMsg += " \n\n Magnet ссылка на скачивание:\n " + mMangetLink;
        }
        Intent mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");
        mShareIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
        startActivity(Intent.createChooser(mShareIntent, getString(R.string.action_share)));
    }


    public void initWebView() {
        /*
         * That function looks damn bad. But we need to call onionProxyManager.isRunning from non UI thread
         * and then we need to call myWebView.loadUrl from UI thread...
         * */
        mWebView = MainActivity.this.findViewById(R.id.myWebView);
        final String loaded = mWebView.getOriginalUrl();
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
                            mWebView.loadUrl(appState.currentUrl);
                        }
                    });
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
                boolean result = fileFrom.delete();
                if (!result) {
                    Log.d("surprise", "MainActivity run: delete error");
                }
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                WebView myWebView = findViewById(R.id.myWebView);
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

    private void downloadTorrent() {
        // получу разрешения на доступ к файловой системе
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "no permission to write external storage, requesting");
                String[] require = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                MainActivity.this.requestPermissions(require, PERMISSION_SAVE_FILE);
                // установлю флаг, который покажет, что нужно повторно скачать торрент после получения прав
                mIsTorrentDownloadRequired = true;
                return;
            }
        }
        // попробую просто заставить webview подгрузить торрент по ссылке
        mWebView.loadUrl(Rutracker.BASE_URL + mTorrentUrl);
    }

    public void setDownloadTorrentActive(String torrentUrl, String torrentName) {
        mTorrentUrl = torrentUrl;
        mTorrentName = torrentName;
        mShowDownloadIcon = true;
        supportInvalidateOptionsMenu();
    }
    public void showMagnetIcon() {
        mShowMagnetIcon = true;
        supportInvalidateOptionsMenu();
    }

    public void hideDownloadIcon() {
        mShowDownloadIcon = false;
        supportInvalidateOptionsMenu();
    }

    public void hideMagnetIcon() {
        mShowMagnetIcon = false;
        supportInvalidateOptionsMenu();
    }


    public void showLoginIcon() {
        mShowLoginIcon = true;
        supportInvalidateOptionsMenu();

    }

    public void hideLoginIcon() {

        mShowLoginIcon = false;
        supportInvalidateOptionsMenu();
    }

    private void showTorrentLoadingDialog() {
        if (mTorrentLoadingDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.torrent_loading_dialog_title)
                    .setView(R.layout.torrent_loading_dialog_layout)
                    .setCancelable(false);
            mTorrentLoadingDialog = dialogBuilder.create();
        }
        mTorrentLoadingDialog.show();
    }

    private void hideTorrentLoadingDialog() {
        if (mTorrentLoadingDialog != null) {
            mTorrentLoadingDialog.hide();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        invalidateOptionsMenu();
        Log.d("surprise", "MainActivity onQueryTextSubmit: make search");
        try {
            String url = "https://rutracker.org/forum/tracker.php?convert_post=1" + "&nm=" + URLEncoder.encode(s, "windows-1251");
            mWebView.loadUrl(url);
            mSearchView.setIconified(true);
            mSearchView.clearFocus();
            // call your request, do some stuff..
            // collapse the action view
            if (mMenu != null) {
                Log.d("surprise", "MainActivity onQueryTextSubmit: hiding search");
                (mMenu.findItem(R.id.action_search)).collapseActionView();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }


    public class TorrentLoadingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra(TORRENT_LOAD_EVENT, 0);
            switch (action) {
                case START_TORRENT_LOADING:
                    showTorrentLoadingDialog();
                    break;
                case FINISH_TORRENT_LOADING:
                    mWebView.loadUrl(RutrackerApplication.getInstance().currentUrl);
                default:
                    hideTorrentLoadingDialog();
            }
        }
    }

}

