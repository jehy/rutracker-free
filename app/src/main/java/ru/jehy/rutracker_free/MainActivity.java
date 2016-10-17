package ru.jehy.rutracker_free;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends AppCompatActivity {
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    public static OnionProxyManager onionProxyManager = null;
    public ShareActionProvider mShareActionProvider;
    private int ViewId;

    public void Update(final Integer lastAppVersion) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Доступно обновление приложения rutracker free до версии " +
                        lastAppVersion + " - желаете обновиться? " +
                        "Если вы согласны - вы будете перенаправлены к скачиванию APK файла,"
                        + " который затем нужно будет открыть.")
                        .setCancelable(true)
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                String apkUrl = "https://github.com/jehy/rutracker-free/releases/download/" +
                                        lastAppVersion + "/app-release.apk";
                                //intent.setDataAndType(Uri.parse(apkUrl), "application/vnd.android.package-archive");
                                intent.setData(Uri.parse(apkUrl));

                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SettingsManager.put(MainActivity.this, "LastIgnoredUpdateVersion", lastAppVersion.toString());
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    /**
     * Generate a value suitable for use in setId(int).
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actionbar, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Updater().execute(this);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);


        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String fileStorageLocation = "torfiles";
                    onionProxyManager =
                            new AndroidOnionProxyManager(MainActivity.this.getApplicationContext(), fileStorageLocation);
                    int totalSecondsPerTorStartup = 4 * 60;
                    int totalTriesPerTorStartup = 5;

// Start the Tor Onion Proxy
                    try {
                        if (!onionProxyManager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup)) {
                            Log.e("TorTest", "Couldn't start Tor!");
                            return;
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }

// Start a hidden service listener
                    //int hiddenServicePort = 80;
                    //int localPort = 9343;
                    //String onionAddress = null;
                    try {
                        //onionAddress = onionProxyManager.publishHiddenService(hiddenServicePort, localPort);
                        Log.v("Rutracker Free", "Port: " + onionProxyManager.getIPv4LocalHostSocksPort());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.v("Rutracker free", "Tor initialized");

// It can taken anywhere from 30 seconds to a few minutes for Tor to start properly routing
// requests to to a hidden service. So you generally want to try to test connect to it a
// few times. But after the previous call the Tor Onion Proxy will route any requests
// to the returned onionAddress and hiddenServicePort to 127.0.0.1:localPort. So, for example,
// you could just pass localPort into the NanoHTTPD constructor and have a HTTP server listening
// to that port.

// Connect via the TOR network
// In this case we are trying to connect to the hidden service but any IP/DNS address and port can be
// used here.
                    //Socket clientSocket =
                    //Utilities.socks4aSocketConnection(onionAddress, hiddenServicePort, "127.0.0.1", localPort);

// Now the socket is open but note that it can take some time before the Tor network has everything
// connected and connection requests can fail for spurious reasons (especially when connecting to
// hidden services) so have lots of retry logic.

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            while (onionProxyManager == null || !onionProxyManager.isRunning() || !onionProxyManager.isNetworkEnabled()) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        RunWebView();
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

    public void RunWebView() {
        MyWebView myWebView = new MyWebView(MainActivity.this.getApplicationContext());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            {
                ViewId = MainActivity.this.generateViewId();
                myWebView.setId(ViewId);
            }

        } else {
            ViewId = View.generateViewId();
            myWebView.setId(ViewId);

        }
        myWebView.getSettings().setJavaScriptEnabled(true);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.contentLayout);
        assert layout != null;
        layout.addView(myWebView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

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
        CookieManager.getInstance().setAcceptCookie(true);
        //String url = "https://rutracker.org/forum/index.php";
        //String url = "http://myip.ru/";
        Log.d("Rutracker free", "Opening: " + Rutracker.mainUrl);
        myWebView.loadUrl(Rutracker.mainUrl);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    WebView myWebView = (WebView) findViewById(ViewId);
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

