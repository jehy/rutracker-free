package ru.jehy.rutracker_free;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

import static ru.jehy.rutracker_free.RutrackerApplication.onionProxyManager;

public class TorProgressTask extends AsyncTask<String, String, Boolean> {
    private static final String TAG = "TorProgressTask";
    private ProgressDialog torStartProgress;
    private MainActivity activity;

    public TorProgressTask(MainActivity activity) {
        this.activity = activity;
    }


    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute");
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
                    android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            new TorProgressTask(activity).execute();
                            dialog.cancel();
                        }
                    });

            builder1.setNegativeButton(
                    android.R.string.no,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
        RutrackerWebView myWebView = (RutrackerWebView) activity.findViewById(R.id.myWebView);
        RutrackerApplication appState = ((RutrackerApplication) activity.getApplicationContext());
        myWebView.loadUrl(appState.currentUrl);
        Log.d(TAG, "Opening: " + appState.currentUrl);

        //for crash http://stackoverflow.com/questions/22924825/view-not-attached-to-window-manager-crash
        // issue #29
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (activity.isDestroyed()) {
                return;
            }
        } else if (activity.isFinishing()) {
            return;
        }
        if (torStartProgress != null && torStartProgress.isShowing())
            torStartProgress.dismiss();
    }

    @Override
    protected void onProgressUpdate(String... log) {
        super.onProgressUpdate(log);
        Log.e(TAG, "logging");
        torStartProgress.setMessage("Initializing Tor..." + log[0]);
    }

    @Override
    protected Boolean doInBackground(final String... args) {
        try {
            if (onionProxyManager.isRunning()) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread torThread = new Thread() {
            @Override
            public void run() {
                try {
                    //boolean ok = onionProxyManager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup);
                    boolean ok = onionProxyManager.installAndStartTorOp();
                    if (!ok) {
                        Log.e(TAG, "Couldn't start Tor!");
                    }

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
                if (timePassed > 1000 * 60 * 2) {
                    return false;
                }
            }

            Log.v(TAG, "Tor initialized on port " + onionProxyManager.getIPv4LocalHostSocksPort());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

}
