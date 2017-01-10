package ru.jehy.rutracker_free;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;


class RutrackerWebViewClient extends WebViewClient {

    private ProxyProcessor proxy = null;

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
        WebResourceResponse response = proxy.getWebResourceResponse(request.getUrl(), request.getMethod(), request.getRequestHeaders());
        if (response == null) {
            return super.shouldInterceptRequest(view, request);
        } else {
            return response;
        }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        WebResourceResponse response = proxy.getWebResourceResponse(Uri.parse(url), "GET", null);
        if (response == null) {
            return super.shouldInterceptRequest(view, url);
        } else {
            return response;
        }
    }

}