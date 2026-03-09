package com.capacitor.apkupdater;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;

public class ApkUpdater {

    public interface VersionCallback {
        void onSuccess(int versionCode, String versionName);
        void onError(String error);
    }

    public interface DownloadCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }

    public interface InstallCallback {
        void onSuccess();
        void onError(String error);
    }

    private Activity activity;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void getAppVersion(VersionCallback callback) {
        try {
            PackageInfo packageInfo = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);

            int versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = (int) packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }

            callback.onSuccess(versionCode, packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            callback.onError("Failed to get app version: " + e.getMessage());
        }
    }

    public void downloadApk(String url, String title, DownloadCallback callback) {
        try {
            String fileName = "app-update.apk";
            File downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            File apkFile = new File(downloadsDir, fileName);

            if (apkFile.exists()) {
                apkFile.delete();
            }

            String downloadTitle = (title != null && !title.isEmpty())
                    ? title
                    : "Downloading update...";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(downloadTitle)
                    .setDescription("Please wait...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(apkFile))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false);

            DownloadManager downloadManager = (DownloadManager) activity
                    .getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        activity.unregisterReceiver(this);

                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        Cursor cursor = downloadManager.query(query);

                        if (cursor != null && cursor.moveToFirst()) {
                            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            int status = cursor.getInt(statusIndex);
                            cursor.close();

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                callback.onSuccess(apkFile.getAbsolutePath());
                            } else {
                                callback.onError("Download failed with status: " + status);
                            }
                        } else {
                            if (cursor != null) cursor.close();
                            callback.onError("Download failed: could not query download status");
                        }
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(
                        receiver,
                        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                        Context.RECEIVER_EXPORTED
                );
            } else {
                activity.registerReceiver(
                        receiver,
                        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                );
            }
        } catch (Exception e) {
            callback.onError("Download failed: " + e.getMessage());
        }
    }

    public void installApk(String filePath, InstallCallback callback) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                callback.onError("APK file not found: " + filePath);
                return;
            }

            Uri uri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".apkupdater.fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            activity.startActivity(intent);
            callback.onSuccess();
        } catch (Exception e) {
            callback.onError("Install failed: " + e.getMessage());
        }
    }
}
