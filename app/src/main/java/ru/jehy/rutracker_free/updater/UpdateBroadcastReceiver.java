package ru.jehy.rutracker_free.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;

import ru.jehy.rutracker_free.MainActivity;

import static ru.jehy.rutracker_free.MainActivity.ACTION_SHOW_UPDATE_DIALOG;
import static ru.jehy.rutracker_free.MainActivity.isAppBeingUpdated;

/**
 * Created by Jehy on 18.10.2016.
 */

public class UpdateBroadcastReceiver extends BroadcastReceiver {
    public boolean isRegistered;
    private MainActivity activity;
    public void register(MainActivity context, IntentFilter filter) {
        isRegistered = true;
        activity=context;
        LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(ACTION_SHOW_UPDATE_DIALOG));//context.registerReceiver(this, filter);
    }

    public boolean unregister(Context context) {
        if (isRegistered) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            //context.unregisterReceiver(this);  // edited
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
            AlertDialog updateDialog = AppUpdateUtil.getAppUpdateDialog(activity, update);
            updateDialog.show();
        }
    }
}
