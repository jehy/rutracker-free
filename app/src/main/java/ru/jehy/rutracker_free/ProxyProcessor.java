package ru.jehy.rutracker_free;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebResourceResponse;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import ru.jehy.rutracker_free.receivers.TorrentLoadedReceiver;

import static ru.jehy.rutracker_free.MainActivity.FINISH_TORRENT_LOADING;
import static ru.jehy.rutracker_free.RutrackerApplication.onionProxyManager;


/**
 * Created by jehy on 2016-03-31.
 */
class ProxyProcessor {

    private static final String LOGGED_IN_MARKER = "<a id=\"logged-in-username\"";
    static boolean isTorrent = false;
    private static final String VIEW_TAG = "RutrackerWebView";
    private static final String MIME_TEXT_HTML = "text/html";
    private static final String MIME_TEXT_CSS = "text/css";
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String ENCODING_WINDOWS_1251 = "WINDOWS-1251";
    private static final String DL_LINK = "dl.php?t=";
    private final Context context;

    ProxyProcessor(Context context) {
        this.context = context;
    }


    private HttpClient getNewHttpClient() {

        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
                .register("https", new MySSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);
        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    WebResourceResponse getWebResourceResponse(Uri url, String method, Map<String, String> headers) {

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

        if (url.getPath() != null && url.getPath().equals("/custom.css")) {
            Log.d(VIEW_TAG, "Adding custom css file...");
            try {
                return new WebResourceResponse(MIME_TEXT_CSS, ENCODING_UTF_8, (context).getAssets().open("rutracker.css"));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        if (url.getPath() != null && url.getPath().contains("logout.php")) {
            CookieManager.clear(context);
            url = Uri.parse(Rutracker.MAIN_URL);
        }

        try {

            HttpResponse response;
            UrlEncodedFormEntity params = null;
            String requestUrl = url.toString();

            if (url.toString().contains("convert_post=1") || method.equals("post")) {
                Log.d("surprise", "ProxyProcessor getWebResourceResponse: emulate post");
                //we need to emulate POST request
                Log.d(VIEW_TAG, "It is a post request!");
                int queryPart = requestUrl.indexOf("?");
                if (queryPart != -1) {
                    requestUrl = requestUrl.substring(0, queryPart);
                }
                params = Utils.get2post(url);
            }

            try {
                response = executeRequest(requestUrl, headers, params);
            } catch (Exception e) {
                return createExceptionError(e, url);
            }

            if (url.toString().contains(DL_LINK)) {
                // начинаю загружать торрент, пошлю оповещение о начале загрузки
                Intent startLoadingIntent = new Intent(MainActivity.TORRENT_LOAD_ACTION);
                startLoadingIntent.putExtra(MainActivity.TORRENT_LOAD_EVENT, MainActivity.START_TORRENT_LOADING);
                context.sendBroadcast(startLoadingIntent);
                String fileName = ((MainActivity) this.context).mTorrentName;
                // сохраняю файл в памяти устройства
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName + ".torrent");
                InputStream is = response.getEntity().getContent();
                FileOutputStream outputStream = new FileOutputStream(file);
                int read;
                byte[] buffer = new byte[1024];
                while ((read = is.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                is.close();
                // отправлю оповещение об окончании загрузки страницы
                Intent finishLoadingIntent = new Intent(MainActivity.TORRENT_LOAD_ACTION);
                finishLoadingIntent.putExtra(MainActivity.TORRENT_LOAD_EVENT, FINISH_TORRENT_LOADING);
                context.sendBroadcast(finishLoadingIntent);
                // отправлю сообщение о скачанном файле через broadcastReceiver
                Intent intent = new Intent(context, TorrentLoadedReceiver.class);
                intent.putExtra(TorrentLoadedReceiver.EXTRA_TORRENT_NAME, fileName + ".torrent");
                context.sendBroadcast(intent);
                String message = "<H1 style='text-align:center;'>Торрент скачан. Возвращаюсь на предыдущую страницу</H1>";
                ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes(ENCODING_UTF_8));
                isTorrent = true;
                return new WebResourceResponse("text/html", ENCODING_UTF_8, inputStream);
            }
            isTorrent = false;

            if (Rutracker.isLoginForm(url)) {
                Log.d("surprise", "ProxyProcessor getWebResourceResponse: have login form");
                Header[] all = response.getAllHeaders();
                for (Header header1 : all) {
                    Log.d(VIEW_TAG, "LOGIN HEADER: " + header1.getName() + " : " + header1.getValue());
                }
                Header[] cookies = response.getHeaders("set-cookie");
                if (cookies.length > 0) {
                    Log.d("surprise", "ProxyProcessor getWebResourceResponse: receive authorization cookie");
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
                if (mime.endsWith(".torrent\"")) {
                    Log.d(VIEW_TAG, "We`ve got a torrent file");
                    String[] arr = mime.split(";");
                    arr = arr[1].split("=");
                    String fileName = arr[1].replace("\"", "");
                    Log.d(VIEW_TAG, "file name from mime: " + fileName);
                    File file = new File(context.getFilesDir(), fileName);

                    FileOutputStream stream = new FileOutputStream(file);
                    //stream.write(Utils.convertStreamToString(inputStream, null).getBytes());

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        stream.write(buffer, 0, bytesRead);
                    }
                    stream.close();

                    ((MainActivity) this.context).shareFile(fileName);
                    return null;
                }
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

                    String title;
                    int start = data.indexOf("<title>");
                    int end = data.indexOf("</title>", start);
                    title = data.substring(start + 7, end);
                    if (title.length() != 0) {
                        title = title.replace(" :: RuTracker.org", "");
                        Answers.getInstance().logContentView(new ContentViewEvent()//just for lulz
                                .putContentName(title)
                                .putContentType("page"));
                    }

                    //convert POST data to GET data to be able ro intercept it
                    String replace = "<form(.*?)method=\"post\"(.*?)>";
                    String replacement = "<form$1method=\"get\"$2><input type=\"hidden\" name=\"convert_post\" value=1>";
                    data = data.replaceAll(replace, replacement);

                    //inject custom CSS
                    data = data.replace("</head>", "<link rel=\"stylesheet\" href=\"/custom.css\" type=\"text/css\"></head>");

                    //replace with custom logout
                    data = data.replace("<a href=\"#\" onclick=\"return post2url('login.php', {logout: 1});\">Выход</a>",
                            "<a href=\"logout.php\">Выход</a>");

                    if (data.contains(LOGGED_IN_MARKER)) {
                        ((MainActivity) this.context).hideLoginIcon();
                    } else {
                        ((MainActivity) this.context).showLoginIcon();
                    }
                    checkPageForTorrent(data);

                    inputStream = new ByteArrayInputStream(data.getBytes(encoding));
                    //Log.d(VIEW_TAG, "data " + data);
                    String shareUrl = url.toString();
                    start = shareUrl.indexOf("&login_username");
                    if (start != -1) {
                        shareUrl = shareUrl.substring(0, start);
                    }
                    start = data.indexOf("href=\"magnet:");
                    String link = "";
                    if (start != -1) {
                        Log.d("surprise", "ProxyProcessor getWebResourceResponse: magnet found");
                        start += 13;
                        end = data.indexOf("\"", start);
                        link = data.substring(start, end);
                    }
                    if (link.length() > 0) {
                        ((MainActivity) this.context).mMangetLink = "magnet:" + link;
                        ((MainActivity) this.context).showMagnetIcon();
                    } else {
                        ((MainActivity) this.context).mMangetLink = null;
                        ((MainActivity) this.context).hideMagnetIcon();
                    }
                    //((MainActivity) context).invalidateOptionsMenu();

                }
                return createFromString(mime, encoding, inputStream);
            } else if (responseCode == 302) {
                // перенаправлю на нужный адрес
                Log.d("surprise", "ProxyProcessor getWebResourceResponse: " + headers.get("Location"));
            } else {
                return createResponseError(responseMessage, url.toString(), String.valueOf(responseCode));
            }
        } catch (Exception e) {
            Log.d(VIEW_TAG, "Error fetching URL " + url + ":");
            e.printStackTrace();
        }
        return null;
    }

    private String makeError(Exception e, String url) {
        return makeError(e.getMessage(), url, String.valueOf(e.hashCode()));
    }

    private String makeError(String errorMessage, String url, String errorCode) {
        Log.d(VIEW_TAG, "Url: " + url);
        Log.d(VIEW_TAG, "Response code: " + errorCode);
        Log.d(VIEW_TAG, "Response message: " + errorMessage);

        return "Что-то пошло не так:<br>" + "Адрес: " + url + "<br><br>" +
                "Сообщение: " + errorMessage + "<br><br>" +
                "Код: " + errorCode + "<br><br>" +
                "Вы можете <a href=\"javascript:location.reload(true)\">Обновить страницу</a>" +
                "или <a href=\"" + Rutracker.MAIN_URL + "\">вернуться на главную</a>";
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
        return createFromString(buf, ENCODING_UTF_8);
    }

    private WebResourceResponse createFromString(String buf, String encoding) throws UnsupportedEncodingException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf.getBytes(encoding));
        return createFromString(ProxyProcessor.MIME_TEXT_HTML, encoding, inputStream);
    }

    private WebResourceResponse createFromString(String mime, String encoding, InputStream inputStream) {
        return new WebResourceResponse(mime, encoding, inputStream);
    }

    private void checkPageForTorrent(String data) {
        int count = (data.length() - data.replace(DL_LINK, "").length()) / DL_LINK.length();
        if (count == 2) {
            // найдена ссылка на торрент, найду её
            // определю, есть ли ссылка на скачивание торрент-файла
            int start = data.indexOf(DL_LINK);
            // получу ссылку полностью
            int end = data.indexOf("\"", start);
            String torrentUrl = data.substring(start, end);
            // получу название страницы
            start = data.indexOf("<title>");
            end = data.indexOf("</title>", start);
            String torrentName = data.substring(start + 7, end);
            torrentName = torrentName.split("/")[0];
            if (torrentName.length() > 100) {
                torrentName = torrentName.substring(0, 100);
            }
            ((MainActivity) this.context).setDownloadTorrentActive(torrentUrl, torrentName.trim());
        } else {
            // скрываю значок
            ((MainActivity) this.context).hideDownloadIcon();
        }
    }
}
