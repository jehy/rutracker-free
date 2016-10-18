package ru.jehy.rutracker_free.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;

import ru.jehy.rutracker_free.MainActivity;

import static ru.jehy.rutracker_free.MainActivity.isAppBeingUpdated;

/**
 * Created by Jehy on 18.10.2016.
 */

public class UpdateBroadcastReceiver extends BroadcastReceiver {
    public boolean isRegistered;
    public Intent register(Context context, IntentFilter filter) {
        isRegistered = true;
        return context.registerReceiver(this, filter);
    }

    public boolean unregister(Context context) {
        if (isRegistered) {
            context.unregisterReceiver(this);  // edited
            isRegistered = false;
            return true;
        }
        return false;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        AppUpdate update = intent.getParcelableExtra("update");
        if (update.getStatus() == AppUpdate.UPDATE_AVAILABLE && !isAppBeingUpdated(
                context)) {
            AlertDialog updateDialog = AppUpdateUtil.getAppUpdateDialog(context, update);
            updateDialog.show();
        }
    }
}
