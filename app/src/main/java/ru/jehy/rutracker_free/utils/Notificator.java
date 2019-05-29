package ru.jehy.rutracker_free.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import ru.jehy.rutracker_free.MainActivity;
import ru.jehy.rutracker_free.R;
import ru.jehy.rutracker_free.receivers.TorrentActionReceiver;
import ru.jehy.rutracker_free.receivers.TorrentLoadedReceiver;

public class Notificator {
    private final Context mContext;
    private final NotificationManager mNotificationManager;

    public static final int TORRENT_LOADED_NOTIFICATION = 1;
    private static final int START_SHARING_REQUEST_CODE = 2;
    private static final int START_OPEN_REQUEST_CODE = 3;
    private static final int START_APP_CODE = 4;

    private static final String TORRENTS_CHANNEL_ID = "torrents";

    public Notificator(Context context){
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // создам канал уведомлений о скачанных книгах
            NotificationChannel nc = new NotificationChannel(TORRENTS_CHANNEL_ID, mContext.getString(R.string.torrent_loaded_channel), NotificationManager.IMPORTANCE_DEFAULT);
            nc.setDescription(mContext.getString(R.string.torrent_loaded_reminder));
            nc.enableLights(true);
            nc.setLightColor(Color.BLUE);
            nc.enableVibration(true);
            mNotificationManager.createNotificationChannel(nc);
        }
    }

    public void sendLoadedTorrentNotification(String name){

        // создам интент для функции отправки файла
        Intent shareIntent = new Intent(mContext, TorrentActionReceiver.class);
        shareIntent.putExtra(TorrentLoadedReceiver.EXTRA_ACTION_TYPE, TorrentLoadedReceiver.ACTION_TYPE_SHARE);
        shareIntent.putExtra(TorrentLoadedReceiver.EXTRA_TORRENT_NAME, name);
        PendingIntent sharePendingIntent = PendingIntent.getBroadcast(mContext, START_SHARING_REQUEST_CODE, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // создам интент для функции открытия файла

        Intent openIntent = new Intent(mContext, TorrentActionReceiver.class);
        openIntent.putExtra(TorrentLoadedReceiver.EXTRA_ACTION_TYPE, TorrentLoadedReceiver.ACTION_TYPE_OPEN);
        openIntent.putExtra(TorrentLoadedReceiver.EXTRA_TORRENT_NAME, name);
        PendingIntent openPendingIntent = PendingIntent.getBroadcast(mContext, START_OPEN_REQUEST_CODE, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openMainIntent = new Intent(mContext, MainActivity.class);
        PendingIntent startMainPending = PendingIntent.getActivity(mContext, START_APP_CODE, openMainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext, TORRENTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_file_download_white_24dp)
                .setContentTitle("Загружен торрент-файл")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(name + " :успешно загружено"))
                .setContentIntent(startMainPending)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_share_white_24dp, "Отправить", sharePendingIntent)
                .addAction(R.drawable.ic_book_white_24dp, "Открыть", openPendingIntent);
        Notification notification = notificationBuilder.build();
        mNotificationManager.notify(TORRENT_LOADED_NOTIFICATION, notification);
    }
}
