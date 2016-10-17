package ru.jehy.rutracker_free;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.ssl.SSLContexts;

import static ru.jehy.rutracker_free.MainActivity.onionProxyManager;

//import org.apache.custom.http.conn.scheme.PlainSocketFactory;
//import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
//import cz.msebera.android.httpclient.impl.conn.tsccm.ThreadSafeClientConnManager;

/**
 * Created by jehy on 2016-03-31.
 */
public class ProxyProcessor {

    private final Context MainContext;

    public ProxyProcessor(Context c) {
        MainContext = c;
    }


    public HttpClient getNewHttpClient() {

        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
                .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);
        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri url = request.getUrl();
        return this.process(url, request.getMethod(), view, request.getRequestHeaders());
    }

    public WebResourceResponse shouldInterceptRequest(WebView view, String urlString) {
        Uri url = Uri.parse(urlString);
        return this.process(url, "GET", view, null);
    }

    public String makeError(Exception e, String url) {
        return "Что-то пошло не так при запросе страницы<br>" + url + ":<br>Сообщение: " + e.getMessage() +
                "<br>Код: " +
                "Вы можете <a href=\"javascript:location.reload(true)\">Обновить страницу</a>" +
                "или <a href=\"" + Rutracker.mainUrl + "\">вернуться на главную</a>";
    }

    public WebResourceResponse process(Uri url, String method, WebView view, Map<String, String> headers) {

        Log.d("WebView", "Request for url: " + url + " intercepted");

        if (url.getHost() == null) {
            Log.d("WebView", "No url or host provided, better let webview deal with it");
            return null;
        }
        if (url.getPath().contains("logout.php")) {
            CookieManager.clear(MainContext);
            url = Uri.parse(Rutracker.mainUrl);
        }

        if (Rutracker.isAdvertisment(url)) {
            Log.d("WebView", "Not fetching advertisment");
            return new WebResourceResponse("text/javascript", "UTF-8", null);
        }

        if (!Rutracker.isRutracker(url)) {
            Log.d("WebView", "Not trying to proxy data from other domains");
            return null;
        }
        if (url.getPath().equals("/custom.css")) {
            Log.d("WebView", "Adding custom css file...");
            try {
                return new WebResourceResponse("text/css", "UTF-8", (MainContext).getAssets().open("rutracker.css"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            HttpClient cli = getNewHttpClient();
            int port = onionProxyManager.getIPv4LocalHostSocksPort();
            InetSocketAddress socksaddr = new InetSocketAddress("127.0.0.1", port);
            HttpClientContext context = HttpClientContext.create();
            context.setAttribute("socks.address", socksaddr);
            HttpResponse response;


            if (url.toString().contains("convert_post=1") || method.equals("post")) {
                //we need to emulate POST request
                Log.d("WebView", "WebviewClient: it is a post request!");
                String urlWithStrippedGet = url.toString();
                int queryPart = urlWithStrippedGet.indexOf("?");
                if (queryPart != -1)
                    urlWithStrippedGet = urlWithStrippedGet.substring(0, queryPart);

                HttpPost request1 = new HttpPost(urlWithStrippedGet);
                UrlEncodedFormEntity params = Utils.get2post(url);
                if (params != null)
                    request1.setEntity(params);
                if (headers != null)
                    for (Map.Entry<String, String> entry : headers.entrySet())
                        request1.setHeader(entry.getKey(), entry.getValue());
                //request1.setHeader(header[0], header[1]);
                request1.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                request1.setHeader("Accept-Encoding", "gzip, deflate, sdch");
                request1.setHeader("Accept-Language", "ru,en-US;q=0.8,en;q=0.6");
                String authCookie = CookieManager.get(MainContext);
                if (authCookie != null && Rutracker.isRutracker(url)) {
                    request1.setHeader("Cookie", authCookie);
                    Log.d("WebView", "cookie sent:" + authCookie);
                }
                request1.setHeader("Referer", "http://rutracker.org/forum/index.php");
                request1.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");


                try {
                    response = cli.execute(request1, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    String msgText = makeError(e, url.toString());
                    ByteArrayInputStream msgStream = new ByteArrayInputStream(msgText.getBytes("UTF-8"));
                    return new WebResourceResponse("text/html", "UTF-8", msgStream);
                }
            } else {
                HttpGet request1 = new HttpGet(url.toString());

                for (Map.Entry<String, String> entry : headers.entrySet())
                    request1.setHeader(entry.getKey(), entry.getValue());

                request1.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                request1.setHeader("Accept-Encoding", "gzip, deflate, sdch");
                request1.setHeader("Accept-Language", "ru,en-US;q=0.8,en;q=0.6");
                String authCookie = CookieManager.get(MainContext);
                if (authCookie != null && Rutracker.isRutracker(url)) {
                    request1.setHeader("Cookie", authCookie);
                    Log.d("WebView", "cookie sent:" + authCookie);
                }
                request1.setHeader("Referer", "http://rutracker.org/forum/index.php");
                request1.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");

                try {
                    response = cli.execute(request1, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    String msgText = makeError(e, url.toString());
                    ByteArrayInputStream msgStream = new ByteArrayInputStream(msgText.getBytes("UTF-8"));
                    return new WebResourceResponse("text/html", "UTF-8", msgStream);
                }

            }

            if (Rutracker.isLoginForm(url)) {

                Header[] all = response.getAllHeaders();
                for (Header header1 : all) {
                    Log.d("WebView", "LOGIN HEADER: " + header1.getName() + " : " + header1.getValue());

                }
                Header[] cookies = response.getHeaders("set-cookie");
                if (cookies.length > 0) {
                    String val = cookies[0].getValue();
                    val = val.substring(0, val.indexOf(";"));
                    String authCookie = val.trim();
                    CookieManager.put(MainContext, authCookie);
                    Log.d("WebView", "=== Auth cookie: ==='" + val + "'");
                    Log.d("WebView", "redirecting to main page...");
                    //redirect does not work o_O
                    String msgText = "<script>window.location = \"http://rutracker.org/forum/index.php\"</script>";
                    ByteArrayInputStream msgStream = new ByteArrayInputStream(msgText.getBytes("UTF-8"));
                    return new WebResourceResponse("text/html", "UTF-8", msgStream);
                } else
                    Log.d("WebView", "No cookie received!!!");
            }
            int responseCode = response.getStatusLine().getStatusCode();
            String responseMessage = response.getStatusLine().getReasonPhrase();
            /*if (responseCode == 302) {
                Log.d("WebView", "It is redirect!");
                Header[] locHeaders = response.getHeaders("location");
                String newLocation = locHeaders[0].getValue();
                Log.d("WebView", "going to " + newLocation);
                return this.shouldInterceptRequest(view, newLocation);
            }*/

            if (responseCode == 200) {
                InputStream input = response.getEntity().getContent();
                String encoding = null;
                if (response.getEntity().getContentEncoding() != null)
                    encoding = response.getEntity().getContentEncoding().getValue();
                Log.d("WebView", "data ok");
                InputStream inputStr;

                if (response.getEntity().getContentEncoding() != null && "gzip".equals(response.getEntity().getContentEncoding().getValue()))
                    inputStr = (new GZIPInputStream(input));
                else
                    inputStr = input;

                Log.d("WebView", "connection encoding : " + encoding);
                String mime = response.getEntity().getContentType().getValue();
                Log.d("WebView", "mime full: " + mime);
                if (mime.contains(";")) {
                    String[] arr = mime.split(";");
                    mime = arr[0];
                    arr = arr[1].split("=");
                    encoding = arr[1];
                    Log.d("WebView", "encoding from mime: " + encoding);
                }
                if (encoding == null || encoding.equals("gzip"))
                    encoding = "UTF-8";

                Log.d("WebView", "clean mime: " + mime);
                if (Rutracker.isRutracker(url) || url.toString().contains("static.t-ru.org"))
                    encoding = "windows-1251";//for rutracker only, for mimes other then html
                if(Rutracker.isWiki(url))
                    encoding = "utf-8";

                Log.d("WebView", "encoding final: " + encoding);

                if (mime.equals("text/html") && Rutracker.isRutracker(url)) {
                    //conversions for rutacker

                    encoding = "windows-1251";//for rutracker only
                    String data = Utils.convertStreamToString(inputStr, encoding);

                    //convert POST data to GET data to be able ro intercept it
                    String replace = "<form(.*?)method=\"post\"(.*?)>";
                    String replacement = "<form$1method=\"get\"$2><input type=\"hidden\" name=\"convert_post\" value=1>";
                    data = data.replaceAll(replace, replacement);

                    //inject custom CSS
                    data = data.replace("</head>", "<link rel=\"stylesheet\" href=\"/custom.css\" type=\"text/css\"></head>");

                    //replace with custom logout
                    data = data.replace("<a href=\"#\" onclick=\"return post2url('login.php', {logout: 1});\">Выход</a>",
                            "<a href=\"logout.php\">Выход</a>");

                    inputStr = new ByteArrayInputStream(data.getBytes(encoding));
                    //Log.d("WebView", "data " + data);
                    String shareUrl = url.toString();
                    int pos = shareUrl.indexOf("&login_username");
                    if (pos != -1)
                        shareUrl = shareUrl.substring(0, pos);
                    String shareMsg = "Посмотри, что я нашёл на рутрекере при помощи приложения rutracker free: \n" + shareUrl;
                    int start = data.indexOf("href=\"magnet:");
                    if (start != -1) {
                        start += 13;
                        int end = data.indexOf("\"", start);
                        String link = data.substring(start, end);
                        shareMsg += "\n\nMagnet ссылка на скачивание:\nmagnet:" + link;
                    }
                    Intent mShareIntent = new Intent();
                    mShareIntent.setAction(Intent.ACTION_SEND);
                    mShareIntent.setType("text/plain");
                    mShareIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
                    ((MainActivity) MainContext).setShareIntent(mShareIntent);
                    //((MainActivity) MainContext).invalidateOptionsMenu();

                }
                return new WebResourceResponse(mime, encoding, inputStr);


            } else {
                Log.d("WebView", "Response code: " + responseCode);
                Log.d("WebView", "Response message: " + responseMessage);
                String msgText = "Что-то пошло не так:<br>Сообщение: " + responseMessage +
                        "<br>Адрес: " + url.toString() + "<br>" +
                        "<br>Код: " + responseCode + "<br>" +
                        "Вы можете <a href=\"javascript:location.reload(true)\">Обновить страницу</a>" +
                        "или <a href=\"" + Rutracker.mainUrl + "\">вернуться на главную</a>";
                ByteArrayInputStream msgStream = new ByteArrayInputStream(msgText.getBytes("UTF-8"));
                return new WebResourceResponse("text/html", "UTF-8", msgStream);


            }
        } catch (Exception e) {
            Log.d("WebView", "Error fetching URL " + url + ":");
            e.printStackTrace();

        }
        return null;
    }
}
