package ru.jehy.rutracker_free;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.IOException;

import static ru.jehy.rutracker_free.MyApplication.onionProxyManager;

public class TorPogressTask extends AsyncTask<String, String, Boolean> {
    ProgressDialog torStartProgress;
    private MainActivity activity;

    public TorPogressTask(MainActivity activity) {
        this.activity = activity;
    }


    protected void onPreExecute() {
        Log.d("rutracker-free", "onPreExecute");
        torStartProgress = new ProgressDialog(activity);
        torStartProgress.setMessage("Starting Tor... Please be patient");
        torStartProgress.setIndeterminate(false);
        torStartProgress.setCancelable(false);
        torStartProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        torStartProgress.show();
    }

    @Override
    protected void onPostExecute(final Boolean success) {

        if (!success) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
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
        Log.d("Rutracker free", "Opening: " + appState.currentUrl);
        torStartProgress.dismiss();
        final ProgressDialog pageLoadProgress = new ProgressDialog(activity);
        pageLoadProgress.setMessage("Loading Page...");
        pageLoadProgress.setIndeterminate(false);
        pageLoadProgress.setCancelable(true);
        pageLoadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pageLoadProgress.show();

        myWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress > 80) {
                    if (pageLoadProgress.isShowing())
                        pageLoadProgress.dismiss();
                } else
                    pageLoadProgress.setProgress(progress);

            }
        });
    }

    @Override
    protected void onProgressUpdate(String... log) {
        super.onProgressUpdate(log);
        Log.e("TorProgressTask", "logging");
        torStartProgress.setMessage("Initializing Tor..." + log[0]);
    }

    @Override
    protected Boolean doInBackground(final String... args) {
        Thread torThread = new Thread() {
            @Override
            public void run() {
                try {
                    //boolean ok = onionProxyManager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                    boolean ok = onionProxyManager.installAndStartTorOp();
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

        try {
            int timePassed = 0;
            String log = null;
            while (torChecker.isAlive()) {
                Thread.sleep(100);
                timePassed += 100;
                String logNew = onionProxyManager.getLastLog();
                if (logNew.length() > 1 && !logNew.equals(log)) {
                    publishProgress(logNew);
                    log = logNew;
                }
                if (timePassed > 1000 * 60 * 2)
                    return false;
            }

            Log.v("Rutracker Free", "Tor initialized on port " + onionProxyManager.getIPv4LocalHostSocksPort());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

}
