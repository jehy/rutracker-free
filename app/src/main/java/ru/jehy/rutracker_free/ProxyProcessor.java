package ru.jehy.rutracker_free;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import cz.msebera.android.httpclient.ssl.SSLContexts;

import static ru.jehy.rutracker_free.RutrackerApplication.onionProxyManager;

//import org.apache.custom.http.conn.scheme.PlainSocketFactory;
//import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
//import cz.msebera.android.httpclient.impl.conn.tsccm.ThreadSafeClientConnManager;

/**
 * Created by jehy on 2016-03-31.
 */
public class ProxyProcessor {

    private static final String VIEW_TAG = "RutrackerWebView";
    private static final String MIME_TEXT_HTML = "text/html";
    private static final String MIME_TEXT_CSS = "text/css";
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String ENCODING_WINDOWS_1251 = "WINDOWS-1251";
    private final Context context;

    public ProxyProcessor(Context context) {
        this.context = context;
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

    public WebResourceResponse getWebResourceResponse(Uri url, String method, Map<String, String> headers) {

        Log.d(VIEW_TAG, "Request for url: " + url + " intercepted");

        if (url.getHost() == null) {
            Log.d(VIEW_TAG, "No url or host provided, better let webView deal with it");
            return null;
        }

        if (!Rutracker.isRutracker(url)) {
            Log.d(VIEW_TAG, "Not trying to proxy data from other domains");
            return null;
        }

        if (Rutracker.isAdvertisment(url)) {
            Log.d(VIEW_TAG, "Not fetching advertisment");
            return new WebResourceResponse("text/javascript", ENCODING_UTF_8, null);
        }

        if (url.getPath().equals("/custom.css")) {
            Log.d(VIEW_TAG, "Adding custom css file...");
            try {
                return new WebResourceResponse(MIME_TEXT_CSS, ENCODING_UTF_8, (context).getAssets().open("rutracker.css"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        if (url.getPath().contains("logout.php")) {
            CookieManager.clear(context);
            url = Uri.parse(Rutracker.MAIN_URL);
        }

        try {

            HttpResponse response;
            UrlEncodedFormEntity params = null;
            String requestUrl = url.toString();

            if (url.toString().contains("convert_post=1") || method.equals("post")) {
                //we need to emulate POST request
                Log.d(VIEW_TAG, "It is a post request!");
                int queryPart = requestUrl.indexOf("?");
                if (queryPart != -1) {
                    requestUrl = requestUrl.substring(0, queryPart);
                }
                params = Utils.get2post(url);
            }

            try {
                response = executeRequest(requestUrl,headers,params);
            } catch (Exception e) {
                return createExceptionError(e, url);
            }

            if (Rutracker.isLoginForm(url)) {
                Header[] all = response.getAllHeaders();
                for (Header header1 : all) {
                    Log.d(VIEW_TAG, "LOGIN HEADER: " + header1.getName() + " : " + header1.getValue());
                }
                Header[] cookies = response.getHeaders("set-cookie");
                if (cookies.length > 0) {
                    String value = cookies[0].getValue();
                    value = value.substring(0, value.indexOf(";"));
                    String authCookie = value.trim();
                    CookieManager.put(this.context, authCookie);
                    Log.d(VIEW_TAG, "=== Auth cookie: ==='" + value + "'");
                    Log.d(VIEW_TAG, "redirecting to main page...");
                    String msgText = "<script>window.location = \"http://rutracker.org/forum/index.php\"</script>";
                    return createFromString(msgText);
                } else {
                    Log.d(VIEW_TAG, "No cookie received!!!");
                }
            }

            int responseCode = response.getStatusLine().getStatusCode();
            String responseMessage = response.getStatusLine().getReasonPhrase();

            if (responseCode == 200) {
                InputStream input = response.getEntity().getContent();
                String encoding = null;
                if (response.getEntity().getContentEncoding() != null) {
                    encoding = response.getEntity().getContentEncoding().getValue();
                }
                Log.d(VIEW_TAG, "data ok");
                InputStream inputStream;

                if ("gzip".equals(encoding)) {
                    inputStream = (new GZIPInputStream(input));
                } else {
                    inputStream = input;
                }

                Log.d(VIEW_TAG, "connection encoding : " + encoding);
                String mime = response.getEntity().getContentType().getValue();
                Log.d(VIEW_TAG, "mime full: " + mime);
                if (mime.contains(";")) {
                    String[] arr = mime.split(";");
                    mime = arr[0];
                    arr = arr[1].split("=");
                    encoding = arr[1];
                    Log.d(VIEW_TAG, "encoding from mime: " + encoding);
                }
                if (encoding == null || encoding.equals("gzip")) {
                    encoding = ENCODING_UTF_8;
                }

                Log.d(VIEW_TAG, "clean mime: " + mime);
                if (Rutracker.isRutracker(url) || url.toString().contains("static.t-ru.org")) {
                    encoding = ENCODING_WINDOWS_1251;//for rutracker only, for mimes other then html
                }
                if (Rutracker.isWiki(url)) {
                    encoding = ENCODING_UTF_8;
                }

                Log.d(VIEW_TAG, "encoding final: " + encoding);

                if (mime.equals(MIME_TEXT_HTML) && Rutracker.isRutracker(url)) {
                    //conversions for rutacker

                    encoding = ENCODING_WINDOWS_1251;//for rutracker only
                    String data = Utils.convertStreamToString(inputStream, encoding);

                    //convert POST data to GET data to be able ro intercept it
                    String replace = "<form(.*?)method=\"post\"(.*?)>";
                    String replacement = "<form$1method=\"get\"$2><input type=\"hidden\" name=\"convert_post\" value=1>";
                    data = data.replaceAll(replace, replacement);

                    //inject custom CSS
                    data = data.replace("</head>", "<link rel=\"stylesheet\" href=\"/custom.css\" type=\"text/css\"></head>");

                    //replace with custom logout
                    data = data.replace("<a href=\"#\" onclick=\"return post2url('login.php', {logout: 1});\">Выход</a>",
                            "<a href=\"logout.php\">Выход</a>");

                    inputStream = new ByteArrayInputStream(data.getBytes(encoding));
                    //Log.d(VIEW_TAG, "data " + data);
                    String shareUrl = url.toString();
                    int pos = shareUrl.indexOf("&login_username");
                    if (pos != -1) {
                        shareUrl = shareUrl.substring(0, pos);
                    }
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
                    ((MainActivity) this.context).setShareIntent(mShareIntent);
                    //((MainActivity) context).invalidateOptionsMenu();

                }
                return createFromString(mime, encoding, inputStream);
            } else {
                return createResponseError(responseMessage, url.toString(), String.valueOf(responseCode));
            }
        } catch (Exception e) {
            Log.d(VIEW_TAG, "Error fetching URL " + url + ":");
            e.printStackTrace();
        }
        return null;
    }

    public String makeError(Exception e, String url) {
        return makeError(e.getMessage(), url, String.valueOf(e.hashCode()));
    }

    public String makeError(String errorMessage, String url, String errorCode) {
        Log.d(VIEW_TAG, "Url: " + url);
        Log.d(VIEW_TAG, "Response code: " + errorCode);
        Log.d(VIEW_TAG, "Response message: " + errorMessage);

        return "Что-то пошло не так:<br>" + "Адрес: " + url + "<br><br>" +
                "Сообщение: " + errorMessage + "<br><br>" +
                "Код: " + errorCode + "<br><br>" +
                "Вы можете <a href=\"javascript:location.reload(true)\">Обновить страницу</a>" +
                "или <a href=\"" + Rutracker.MAIN_URL + "\">вернуться на главную</a>";
    }

    private HttpResponse executeRequest(Uri url, Map<String, String> headers, UrlEncodedFormEntity params) throws IOException {
        return executeRequest(url.toString(),headers, params);
    }
    private HttpResponse executeRequest(String url, Map<String, String> headers, UrlEncodedFormEntity params) throws IOException {
        HttpClient httpClient = getNewHttpClient();
        int port = onionProxyManager.getIPv4LocalHostSocksPort();
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);
        HttpClientContext clientContext = HttpClientContext.create();
        clientContext.setAttribute("socks.address", socketAddress);

        HttpPost request = new HttpPost(url);

        if (params != null) {
            request.setEntity(params);
        }
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }

        request.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, sdch");
        request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, "ru,en-US;q=0.8,en;q=0.6");
        String authCookie = CookieManager.get(this.context);
        if (authCookie != null && Rutracker.isRutracker(url)) {
            request.setHeader("Cookie", authCookie);
            Log.d(VIEW_TAG, "cookie sent:" + authCookie);
        }
        request.setHeader(HttpHeaders.REFERER, "http://rutracker.org/forum/index.php");
        request.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");


        return httpClient.execute(request, clientContext);

    }

    private WebResourceResponse createExceptionError(Exception e, Uri url) throws UnsupportedEncodingException {
        return createExceptionError(e, url.toString());
    }

    private WebResourceResponse createExceptionError(Exception e, String url) throws UnsupportedEncodingException {
        String msgText = makeError(e, url);
        return createFromString(msgText);
    }

    private WebResourceResponse createResponseError(String errorMessage, String url, String errorCode) throws UnsupportedEncodingException {
        String msgText = makeError(errorMessage, url, errorCode);
        return createFromString(msgText);
    }

    private WebResourceResponse createFromString(String buf) throws UnsupportedEncodingException {
        return createFromString(buf,MIME_TEXT_HTML,ENCODING_UTF_8);
    }

    private WebResourceResponse createFromString(String buf, String mime) throws UnsupportedEncodingException {
        return createFromString(buf,mime,ENCODING_UTF_8);
    }

    private WebResourceResponse createFromString(String buf, String mime, String encoding) throws UnsupportedEncodingException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf.getBytes(encoding));
        return createFromString(mime,encoding,inputStream);
    }

    private WebResourceResponse createFromString(String mime, String encoding, InputStream inputStream) throws UnsupportedEncodingException {
        return new WebResourceResponse(mime, encoding, inputStream);
    }
}
