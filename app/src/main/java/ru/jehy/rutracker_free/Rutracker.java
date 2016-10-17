package ru.jehy.rutracker_free;

import android.net.Uri;

/**
 * Created by Jehy on 17.10.2016.
 */

public class Rutracker {
    public static final String mainUrl = "https://rutracker.org/forum/index.php";

    public static boolean isLoginForm(Uri url) {
        return (isRutracker(url) && url.getPath().toLowerCase().contains("forum/login.php"));
    }


    public static boolean isRutracker(Uri url) {
        String host = url.getHost().toLowerCase();
        return ((host.equals("rutracker.org")
                || host.endsWith(".rutracker.org")
        ));
    }

    public static boolean isWiki(Uri url) {
        String host = url.getHost().toLowerCase();
        return host.equals("rutracker.wiki") || isRutracker(url)&&url.getPath().toLowerCase().startsWith("/go/");
    }


    public static boolean isAdvertisment(Uri url) {
        String[] adv_hosts = {"marketgid.com", "adriver.ru", "thisclick.network", "hghit.com",
                "onedmp.com", "acint.net", "yadro.ru", "tovarro.com", "marketgid.com", "rtb.com", "adx1.com",
                "directadvert.ru", "rambler.ru", "advertserve.com", "bannersvideo.com", "mc.yandex.ru"};

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
}
