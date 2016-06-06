package ru.jehy.rutracker_free;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

/**
 * Created by Bond on 2016-03-14.
 */

public class Utils {

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean is_adv(Uri url) {
        String[] adv_hosts = {"marketgid.com", "adriver.ru", "thisclick.network", "hghit.com",
                "onedmp.com", "acint.net", "yadro.ru", "tovarro.com", "marketgid.com", "rtb.com", "adx1.com",
                "directadvert.ru", "rambler.ru"};

        String[] adv_paths = {"brand", "iframe"};

        String host = url.getHost().toLowerCase();
        for (String item : adv_hosts) {
            //if (StringUtils.containsIgnoreCase(host, item))
            if (host.contains(item.toLowerCase())) {
                return true;
            }
        }
        //if (StringUtils.containsIgnoreCase(url.getHost(), "rutracker.org")) {
        if (host.contains("rutracker.org")) {
            String path = url.getPath().toLowerCase();
            for (String item : adv_paths) {
                {
                    if (path.contains(item.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String[] authHeader() {
        String[] result = new String[2];
        result[0] = "Chrome-Proxy";
        String authValue = "ac4500dd3b7579186c1b0620614fdb1f7d61f944";
        String timestamp = Long.toString(System.currentTimeMillis()).substring(0, 10);
        String[] chromeVersion = {"49", "0", "2623", "87"};

        String sid = (timestamp + authValue + timestamp);

        sid = Utils.md5(sid);
        result[1] = "ps=" + timestamp + "-" + Integer.toString((int) (Math.random() * 1000000000)) +
                "-" + Integer.toString((int) (Math.random() * 1000000000)) +
                "-" + Integer.toString((int) (Math.random() * 1000000000)) +
                ", sid=" + sid + ", b=" + chromeVersion[2] + ", p=" + chromeVersion[3] + ", c=win";
        return result;
    }


    static String convertStreamToString(java.io.InputStream is, String encoding) {
        //java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        //return s.hasNext() ? s.next() : "";
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(is, encoding));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder total = new StringBuilder();
        String line;
        try {
            if (r != null)
                while ((line = r.readLine()) != null) {
                    total.append("\n").append(line);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total.toString();
    }

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    @SuppressWarnings("UnusedAssignment")
    public static UrlEncodedFormEntity get2post(Uri url) {
        Set<String> params = url.getQueryParameterNames();
        if (params.isEmpty())
            return null;

        List<NameValuePair> paramsArray = new ArrayList<>();

        Log.d("Utils", "Getting URL parameters from URL " + url.toString());
        //String urlStr = null;

        Map<String, String> map = getQueryMap(url.toString());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            try {
                value = URLDecoder.decode(value, "windows-1251");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Log.d("Utils", "converting parameter " + name + " to post, value " + value);
            paramsArray.add(new BasicNameValuePair(name, value));
        }
        try {
            return new UrlEncodedFormEntity(paramsArray, "windows-1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean is_rutracker(Uri url) {
        String host = url.getHost().toLowerCase();
        return ((host.equals("login.rutracker.org")
                || host.equals("rutracker.org")
                || host.equals("post.rutracker.org")
                //|| host.equals("rutracker.wiki") //does not require auth and not 1251
        ));
    }

    public static boolean is_login_form(Uri url) {
        return (is_rutracker(url) && url.getPath().toLowerCase().contains("forum/login.php"));
    }
}
