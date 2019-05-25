package ru.jehy.rutracker_free.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;

import java.io.File;

import ru.jehy.rutracker_free.R;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class TorrentOpener {
    public static void openTorrent(String name, Context context) {
        // грязный хак- без него не работает доступ к Kindle, та не умеет в новый метод с контентом
        //todo По возможности- разобраться и заменить на валидное решение
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        // ========================================================================================
        // получу путь к файлу
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
        if(file.exists()){
            // отправлю запрос на открытие файла
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(Uri.fromFile(file), "application/x-bittorrent");
            Intent starter = Intent.createChooser(openIntent, context.getString(R.string.open_with_message));
            starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(starter);
        }
    }
}
