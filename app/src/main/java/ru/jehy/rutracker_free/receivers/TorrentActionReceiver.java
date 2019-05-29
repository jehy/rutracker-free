package ru.jehy.rutracker_free.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ru.jehy.rutracker_free.utils.Notificator;
import ru.jehy.rutracker_free.utils.TorrentOpener;
import ru.jehy.rutracker_free.utils.TorrentSharer;

public class TorrentActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // закрою меню уведомлений
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeIntent);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notificator.TORRENT_LOADED_NOTIFICATION);

        String actionType = intent.getStringExtra(TorrentLoadedReceiver.EXTRA_ACTION_TYPE);
        Log.d("surprise", "BookActionReceiver onReceive: action type is " + actionType);
        String name = intent.getStringExtra(TorrentLoadedReceiver.EXTRA_TORRENT_NAME);
        if(actionType.equals(TorrentLoadedReceiver.ACTION_TYPE_SHARE))
            TorrentSharer.shareTorrent(name, context.getApplicationContext());
        else if(actionType.equals(TorrentLoadedReceiver.ACTION_TYPE_OPEN))
            TorrentOpener.openTorrent(name, context.getApplicationContext());
    }
}
