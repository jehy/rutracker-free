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

public class RutrackerWebView extends WebView {
    private boolean init = false;

    public RutrackerWebView(Context context) {
        super(context);
        setUpWebView();
    }

    public RutrackerWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUpWebView();
    }

    public RutrackerWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpWebView();
    }

    public void setUpWebView() {
        if (this.isInEditMode()) {
            return;
        }

        RutrackerWebViewClient webClient;
        webClient = new RutrackerWebViewClient(this.getContext());
        this.setWebViewClient(webClient);

        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
    }

    private void initProgressBar() {
        if (init) {
            return;
        }
        init = true;
        final ProgressBar progressBar = (ProgressBar) ((MainActivity) getContext()).findViewById(R.id.progressBar);
        this.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress > 80) {
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    if (progressBar.getVisibility() == View.GONE) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    progressBar.setProgress(progress);
                }
            }
        });
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);
        RutrackerApplication appState = ((RutrackerApplication) getContext().getApplicationContext());
        appState.currentUrl = url;
        initProgressBar();
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        // fail, never works
        super.postUrl(url, postData);
    }
}