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
import android.widget.Toast;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

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
        dialog = new ProgressDialog(context);
        dialog.setMessage("Initializing Tor...");
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();
    }

    @Override
    protected void onPostExecute(final Boolean success) {

        if(!success)
        {
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
        MyWebView myWebView = new MyWebView(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            {
                activity.ViewId = activity.generateViewId();
                myWebView.setId(activity.ViewId);
            }

        } else {
            activity.ViewId = View.generateViewId();
            myWebView.setId(activity.ViewId);

        }
        myWebView.getSettings().setJavaScriptEnabled(true);
        RelativeLayout layout = (RelativeLayout) activity.findViewById(R.id.contentLayout);
        assert layout != null;
        layout.addView(myWebView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= 21) {
            MyWebViewClient webClient = new MyWebViewClient(activity);
            myWebView.setWebViewClient(webClient);
        } else {
            MyWebViewClientOld webClient = new MyWebViewClientOld(activity);
            myWebView.setWebViewClient(webClient);
        }
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setDisplayZoomControls(false);
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
        //String url = "https://rutracker.org/forum/index.php";
        //String url = "http://myip.ru/";
        Log.d("Rutracker free", "Opening: " + Rutracker.mainUrl);
        myWebView.loadUrl(Rutracker.mainUrl);
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

        String fileStorageLocation = "torfiles";
        onionProxyManager =
                new AndroidOnionProxyManager(context, fileStorageLocation);
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
