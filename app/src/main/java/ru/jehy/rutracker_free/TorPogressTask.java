package ru.jehy.rutracker_free;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import java.io.IOException;

import static ru.jehy.rutracker_free.MainActivity.onionProxyManager;

public class TorPogressTask extends AsyncTask<String, Void, Boolean> {
    private ProgressDialog dialog;
    private MainActivity activity;

    public TorPogressTask(MainActivity activity) {
        this.activity = activity;
        context = activity;
        dialog = new ProgressDialog(context);
    }

    private Context context;

    protected void onPreExecute() {
        Log.d("rutracker-free","onPreExecute");
        dialog = new ProgressDialog(context);
        dialog.setMessage("Initializing Tor... Please be patient");
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();
    }

    @Override
    protected void onPostExecute(final Boolean success) {

        if (!success) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
            builder1.setMessage("Failed to load Tor. Retry?");
            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new TorPogressTask(activity).execute();
                            dialog.cancel();
                        }
                    });

            builder1.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
        MyWebView myWebView = (MyWebView) activity.findViewById(R.id.myWebView);
        assert myWebView!=null;

        MyApplication appState = ((MyApplication) activity.getApplicationContext());
        myWebView.loadUrl(appState.currentUrl);
        //String url = "https://rutracker.org/forum/index.php";
        //String url = "http://myip.ru/";
        Log.d("Rutracker free", "Opening: " + appState.currentUrl);
        dialog.setMessage("Loading...");
        myWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress > 80) {
                    if (dialog.isShowing())
                        dialog.dismiss();
                } else
                    dialog.setProgress(progress);

            }
        });
    }

    @Override
    protected Boolean doInBackground(final String... args) {
        int totalSecondsPerTorStartup = 4 * 60;
        int totalTriesPerTorStartup = 5;
        dialog.setProgress(10);

// Start the Tor Onion Proxy
        try {
            if (!onionProxyManager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup)) {
                Log.e("TorTest", "Couldn't start Tor!");
                return false;
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        dialog.setProgress(50);
        try {
            Log.v("Rutracker Free", "Tor initialized on port " + onionProxyManager.getIPv4LocalHostSocksPort());
            while (onionProxyManager == null || !onionProxyManager.isRunning() || !onionProxyManager.isNetworkEnabled())
                Thread.sleep(100);
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        dialog.setProgress(90);

        return true;
    }

}
