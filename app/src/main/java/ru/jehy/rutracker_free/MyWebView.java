package ru.jehy.rutracker_free;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

/**
 * Created by Bond on 2016-03-12.
 */

public class MyWebView extends WebView {
    private boolean init = false;
    private Context activityContext;
    public MyWebView(Context context) {
        super(context);
        this.activityContext=context;
        setUpWebView();
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.activityContext=context;
        setUpWebView();
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.activityContext=context;
        setUpWebView();
    }

    public void setUpWebView() {
        if (this.isInEditMode())
            return;
        this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= 21) {
            MyWebViewClient webClient = new MyWebViewClient(this.getContext());
            this.setWebViewClient(webClient);
        } else {
            MyWebViewClientOld webClient = new MyWebViewClientOld(this.getContext());
            this.setWebViewClient(webClient);
        }
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        this.getSettings().setBuiltInZoomControls(true);
        this.getSettings().setDisplayZoomControls(false);
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
    }

    private void initProgressBar() {
        if (init) return;
        init = true;
        final ProgressBar p = (ProgressBar) ((MainActivity)activityContext).findViewById(R.id.progressBar);
        this.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress > 80) {
                    if (p.getVisibility() == View.VISIBLE)
                        p.setVisibility(View.GONE);
                } else {
                    if (p.getVisibility() == View.GONE)
                        p.setVisibility(View.VISIBLE);
                    p.setProgress(progress);
                }
            }
        });
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);
        MyApplication appState = ((MyApplication) this.getContext().getApplicationContext());
        appState.currentUrl = url;
        initProgressBar();
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        // fail, never works
        super.postUrl(url, postData);
    }
}