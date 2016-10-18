package ru.jehy.rutracker_free;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.IOException;

import static ru.jehy.rutracker_free.MainActivity.onionProxyManager;

public class TorPogressTask extends AsyncTask<String, String, Boolean> {
    private ProgressDialog dialog;
    private MainActivity activity;

    public TorPogressTask(MainActivity activity) {
        this.activity = activity;
        context = activity;
        dialog = new ProgressDialog(context);
    }

    private Context context;

    protected void onPreExecute() {
        Log.d("rutracker-free", "onPreExecute");
        dialog = new ProgressDialog(context);
        dialog.setMessage("Initializing Tor... Please be patient");
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
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
        assert myWebView != null;

        MyApplication appState = ((MyApplication) activity.getApplicationContext());
        myWebView.loadUrl(appState.currentUrl);
        //String url = "https://rutracker.org/forum/index.php";
        //String url = "http://myip.ru/";
        Log.d("Rutracker free", "Opening: " + appState.currentUrl);
        dialog.setMessage("Loading Page...");
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
    protected void onProgressUpdate(String... log) {
        super.onProgressUpdate(log);
        dialog.setMessage("Initializing Tor..." + log[0]);
    }

    @Override
    protected Boolean doInBackground(final String... args) {
        dialog.setProgress(10);
        Thread torThread = new Thread() {
            @Override
            public void run() {
                int totalSecondsPerTorStartup = 4 * 60;
                int totalTriesPerTorStartup = 5;
                try {
                    boolean ok = onionProxyManager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                    if (!ok)
                        Log.e("TorTest", "Couldn't start Tor!");

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        };
        torThread.start();

        Thread torChecker = new Thread() {
            @Override
            public void run() {
                try {
                    while (!onionProxyManager.isRunning()) {
                        Thread.sleep(90);

                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        torChecker.start();

// Start the Tor Onion Proxy
        dialog.setProgress(20);
        try {
            while (torChecker.isAlive()) {
                Thread.sleep(100);
                publishProgress(onionProxyManager.getLastLog());
                //Log.e("ru", "ololo");
            }

            Log.v("Rutracker Free", "Tor initialized on port " + onionProxyManager.getIPv4LocalHostSocksPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
        dialog.setProgress(90);

        return true;
    }

}
