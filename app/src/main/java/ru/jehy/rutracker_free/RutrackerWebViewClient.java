package ru.jehy.rutracker_free;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;


class RutrackerWebViewClient extends WebViewClient {

    ProxyProcessor proxy = null;

    public RutrackerWebViewClient(Context c) {
        proxy = new ProxyProcessor(c);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse w = proxy.shouldInterceptRequest(view, request);
        if (w == null)
            return super.shouldInterceptRequest(view, request);
        else
            return w;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String urlString) {
        WebResourceResponse w = proxy.shouldInterceptRequest(view, urlString);
        if (w == null)
            return super.shouldInterceptRequest(view, urlString);
        else
            return w;
    }


}