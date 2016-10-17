package ru.jehy.rutracker_free.updater;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;
import ru.jehy.rutracker_free.BuildConfig;
import ru.jehy.rutracker_free.MainActivity;
import ru.jehy.rutracker_free.R;


public class AppUpdateUtil {

    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/jehy/rutracker-free/releases/latest";

    public static void checkForUpdate(final Context context) {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            final HttpGet httpget = new HttpGet(GITHUB_RELEASES_URL);

            System.out.println("Executing request " + httpget.getRequestLine());

            // Create a custom response handler
            final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        //return entity != null ? EntityUtils.toString(entity) : null;
                        String body = EntityUtils.toString(entity);
                        try {

                            JSONObject releaseInfo = new JSONObject(body);
                            JSONObject releaseAssets = releaseInfo.getJSONArray("assets").getJSONObject(0);

                            AppUpdate update = new AppUpdate(releaseAssets.getString("browser_download_url"),
                                    releaseInfo.getString("tag_name"), releaseInfo.getString("body"), AppUpdate.UP_TO_DATE);

                            SemVer currentVersion = SemVer.parse(BuildConfig.VERSION_NAME);
                            SemVer remoteVersion = SemVer.parse(update.getVersion());

                            //If current version is smaller than remote version
                            if (currentVersion.compareTo(remoteVersion) < 0) {
                                update.setStatus(AppUpdate.UPDATE_AVAILABLE);
                            } else {
                                Log.v("Rutracker-free Updater", "App version is up to date");
                            }

                            Intent updateIntent = MainActivity.createUpdateDialogIntent(update);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
                        } catch (JSONException je) {
                            Log.e("Updater", "Exception thrown while checking for update");
                            Log.e("Updater", je.toString());
                        }
                    } else {
                        //throw new ClientProtocolException("Unexpected response status: " + status);
                        AppUpdate update = new AppUpdate(null, null, null, AppUpdate.ERROR);
                        Intent updateIntent = MainActivity.createUpdateDialogIntent(update);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
                    }
                    return null;
                }
            };
            try {
                httpclient.execute(httpget, responseHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static AlertDialog getAppUpdateDialog(final Context context, final AppUpdate update) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(R.string.update_available).setMessage(
                context.getString(R.string.app_name) + " v" + update.getVersion() + " " + "is available"
                        + "\n\n" + "Changes:" + "\n\n" + update.getChangelog()).setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        Intent startDownloadIntent = new Intent(context, DownloadUpdateService.class);
                        startDownloadIntent.putExtra(DownloadUpdateService.KEY_DOWNLOAD_URL, update.getAssetUrl());
                        context.startService(startDownloadIntent);
                    }
                }).setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setCancelable(false);
        AlertDialog dialog = builder.create();
        return dialog;
    }
}
