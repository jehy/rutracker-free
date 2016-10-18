package ru.jehy.rutracker_free;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by Bond on 2016-03-12.
 */

public class MyWebView extends WebView {
    Context ActivityContext;
    public boolean setUp=false;
    public MyWebView(Context context) {
        super(context);
        this.ActivityContext=context;
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void loadUrl(String url) {

        MyApplication appState = ((MyApplication) this.getContext().getApplicationContext());
        appState.currentUrl=url;
        super.loadUrl(url);

    }

    @Override
    public void postUrl(String url, byte[] postData) {
        // fail, never works
        super.postUrl(url, postData);
    }
}