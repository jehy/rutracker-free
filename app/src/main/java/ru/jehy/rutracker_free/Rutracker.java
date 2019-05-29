package ru.jehy.rutracker_free;

import android.net.Uri;

/**
 * Created by Jehy on 17.10.2016.
 */

 class Rutracker {
     static final String MAIN_URL = "https://rutracker.org/forum/index.php";
     static final String BASE_URL = "https://rutracker.org/forum/";

     static boolean isLoginForm(Uri url) {
        return (isRutracker(url) && url.getPath() != null && url.getPath().toLowerCase().contains("forum/login.php"));
    }


     static boolean isRutracker(Uri url) {
        String host = url.getHost().toLowerCase();
        return ((host.equals("rutracker.org")
                || host.endsWith(".rutracker.org")
        ));
    }

     static boolean isRutracker(String url) {
        return isRutracker(Uri.parse(url));
    }

     static boolean isWiki(Uri url) {
        String host = url.getHost().toLowerCase();
        return host.equals("rutracker.wiki") || isRutracker(url) && url.getPath().toLowerCase().startsWith("/go/");
    }


     static boolean isAdvertisment(Uri url) {
        String[] advHosts = {"marketgid.com", "adriver.ru", "thisclick.network", "hghit.com",
                "onedmp.com", "acint.net", "yadro.ru", "tovarro.com", "marketgid.com", "rtb.com", "adx1.com",
                "directadvert.ru", "rambler.ru", "advertserve.com", "bannersvideo.com", "mc.yandex.ru"};

        String[] advPaths = {"brand", "iframe"};

        String host = url.getHost().toLowerCase();
        for (String item : advHosts) {
            //if (StringUtils.containsIgnoreCase(host, item))
            if (host.contains(item.toLowerCase())) {
                return true;
            }
        }
        //if (StringUtils.containsIgnoreCase(url.getHost(), "rutracker.org")) {
        if (host.contains("rutracker.org")) {
            String path = url.getPath().toLowerCase();
            for (String item : advPaths) {
                {
                    if (path.contains(item.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
