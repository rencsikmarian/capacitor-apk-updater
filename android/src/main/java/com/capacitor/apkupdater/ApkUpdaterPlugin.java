package com.capacitor.apkupdater;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "ApkUpdater")
public class ApkUpdaterPlugin extends Plugin {

    private ApkUpdater implementation = new ApkUpdater();

    @Override
    public void load() {
        super.load();
        implementation.setActivity(getActivity());
    }

    @PluginMethod
    public void getAppVersion(PluginCall call) {
        implementation.getAppVersion(new ApkUpdater.VersionCallback() {
            @Override
            public void onSuccess(int versionCode, String versionName) {
                JSObject result = new JSObject();
                result.put("versionCode", versionCode);
                result.put("versionName", versionName);
                call.resolve(result);
            }

            @Override
            public void onError(String error) {
                call.reject(error);
            }
        });
    }

    @PluginMethod
    public void downloadApk(PluginCall call) {
        String url = call.getString("url");
        if (url == null || url.isEmpty()) {
            call.reject("url parameter is required");
            return;
        }

        String title = call.getString("title", "Downloading update...");

        call.setKeepAlive(true);

        implementation.downloadApk(url, title, new ApkUpdater.DownloadCallback() {
            @Override
            public void onSuccess(String filePath) {
                JSObject result = new JSObject();
                result.put("filePath", filePath);
                call.resolve(result);
            }

            @Override
            public void onError(String error) {
                call.reject(error);
            }
        });
    }

    @PluginMethod
    public void installApk(PluginCall call) {
        String filePath = call.getString("filePath");
        if (filePath == null || filePath.isEmpty()) {
            call.reject("filePath parameter is required");
            return;
        }

        implementation.installApk(filePath, new ApkUpdater.InstallCallback() {
            @Override
            public void onSuccess() {
                call.resolve();
            }

            @Override
            public void onError(String error) {
                call.reject(error);
            }
        });
    }
}
