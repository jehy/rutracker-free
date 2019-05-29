package ru.jehy.rutracker_free.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import ru.jehy.rutracker_free.utils.Notificator;

public class TorrentLoadedReceiver extends BroadcastReceiver {
    public static final String EXTRA_TORRENT_NAME = "torrent name";
    public static final String EXTRA_ACTION_TYPE = "action type";

    public static final String ACTION_TYPE_OPEN = "open";
    public static final String ACTION_TYPE_SHARE = "share";


    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra(EXTRA_TORRENT_NAME);
        new Notificator(context).sendLoadedTorrentNotification(name);
        Toast.makeText(context, name + " - загружено!", Toast.LENGTH_LONG).show();
    }
}
