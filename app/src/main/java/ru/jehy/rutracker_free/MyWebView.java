package ru.jehy.rutracker_free;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Created by Bond on 2016-03-12.
 */

public class MyWebView extends WebView {
    Context ActivityContext;

    public MyWebView(Context context) {
        super(context);
        //this.ActivityContext=context;
        setUpWebView(this);
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUpWebView(this);
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpWebView(this);
    }

    public void setUpWebView(MyWebView myWebView) {
        myWebView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= 21) {
            MyWebViewClient webClient = new MyWebViewClient(this.getContext());
            myWebView.setWebViewClient(webClient);
        } else {
            MyWebViewClientOld webClient = new MyWebViewClientOld(this.getContext());
            myWebView.setWebViewClient(webClient);
        }
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.getSettings().setDisplayZoomControls(false);
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
    }

    @Override
    public void loadUrl(String url) {

        MyApplication appState = ((MyApplication) this.getContext().getApplicationContext());
        appState.currentUrl = url;
        super.loadUrl(url);

    }

    @Override
    public void postUrl(String url, byte[] postData) {
        // fail, never works
        super.postUrl(url, postData);
    }
}