# capacitor-app-updater Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a Capacitor 7 plugin that enables Android APK self-update — downloading an APK from a URL and triggering the system installer.

**Architecture:** Standard Capacitor plugin with TypeScript definitions (3 methods: `getAppVersion`, `downloadApk`, `installApk`), a web fallback that throws "not supported", and Java Android native code split into a Plugin bridge class and an implementation class. The plugin uses Android's `DownloadManager` for APK downloads and `FileProvider` + `ACTION_VIEW` intent for installation.

**Tech Stack:** TypeScript, Java, Capacitor 7, Android DownloadManager, FileProvider

---

### Task 1: Scaffold Project — package.json, tsconfig, rollup, gitignore, git init

**Files:**
- Create: `package.json`
- Create: `tsconfig.json`
- Create: `rollup.config.mjs`
- Create: `.gitignore`

**Step 1: Create `package.json`**

```json
{
  "name": "capacitor-app-updater",
  "version": "0.0.1",
  "description": "Capacitor plugin for Android APK self-update",
  "main": "dist/plugin.cjs.js",
  "module": "dist/esm/index.js",
  "types": "dist/esm/index.d.ts",
  "unpkg": "dist/plugin.js",
  "files": [
    "android/src/main/",
    "android/build.gradle",
    "dist/"
  ],
  "author": "Rencsik Marian",
  "license": "MIT",
  "keywords": [
    "capacitor",
    "plugin",
    "native",
    "android",
    "updater",
    "apk"
  ],
  "scripts": {
    "verify": "npm run verify:android && npm run verify:web",
    "verify:android": "cd android && ./gradlew clean build test && cd ..",
    "verify:web": "npm run build",
    "lint": "npm run eslint && npm run prettier -- --check",
    "fmt": "npm run eslint -- --fix && npm run prettier -- --write",
    "eslint": "eslint . --ext ts",
    "prettier": "prettier \"**/*.{css,html,ts,js,java}\" --plugin=prettier-plugin-java",
    "build": "npm run clean && tsc && rollup -c rollup.config.mjs",
    "clean": "rimraf ./dist",
    "watch": "tsc --watch",
    "prepublishOnly": "npm run build"
  },
  "devDependencies": {
    "@capacitor/android": "^7.0.0",
    "@capacitor/core": "^7.0.0",
    "@ionic/eslint-config": "^0.4.0",
    "@ionic/prettier-config": "^4.0.0",
    "eslint": "^8.57.0",
    "prettier": "^3.4.2",
    "prettier-plugin-java": "^2.6.6",
    "rimraf": "^6.0.1",
    "rollup": "^4.30.1",
    "typescript": "~4.1.5"
  },
  "peerDependencies": {
    "@capacitor/core": ">=7.0.0"
  },
  "prettier": "@ionic/prettier-config",
  "eslintConfig": {
    "extends": "@ionic/eslint-config/recommended"
  },
  "capacitor": {
    "android": {
      "src": "android"
    }
  }
}
```

**Step 2: Create `tsconfig.json`**

```json
{
  "compilerOptions": {
    "allowUnreachableCode": false,
    "declaration": true,
    "esModuleInterop": true,
    "inlineSources": true,
    "lib": ["dom", "es2017"],
    "module": "esnext",
    "moduleResolution": "node",
    "noFallthroughCasesInSwitch": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "outDir": "dist/esm",
    "pretty": true,
    "sourceMap": true,
    "strict": true,
    "target": "es2017"
  },
  "files": ["src/index.ts"]
}
```

**Step 3: Create `rollup.config.mjs`**

```javascript
export default {
  input: 'dist/esm/index.js',
  output: [
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorAppUpdater',
      globals: {
        '@capacitor/core': 'capacitorExports',
      },
      sourcemap: true,
      inlineDynamicImports: true,
    },
    {
      file: 'dist/plugin.cjs.js',
      format: 'cjs',
      sourcemap: true,
      inlineDynamicImports: true,
    },
  ],
  external: ['@capacitor/core'],
};
```

**Step 4: Create `.gitignore`**

Use the same `.gitignore` from `capacitor-usercentrics` (node, iOS, Android build artifacts).

**Step 5: Initialize git repo**

```bash
cd capacitor-app-updater && git init && git add -A && git commit -m "chore: scaffold capacitor-app-updater project"
```

---

### Task 2: TypeScript Layer — definitions.ts, index.ts, web.ts

**Files:**
- Create: `src/definitions.ts`
- Create: `src/index.ts`
- Create: `src/web.ts`

**Step 1: Create `src/definitions.ts`**

```typescript
export interface AppUpdaterPlugin {
  getAppVersion(): Promise<{ versionCode: number; versionName: string }>;

  downloadApk(options: { url: string; title?: string }): Promise<{ filePath: string }>;

  installApk(options: { filePath: string }): Promise<void>;
}
```

**Step 2: Create `src/index.ts`**

```typescript
import { registerPlugin } from '@capacitor/core';

import type { AppUpdaterPlugin } from './definitions';

const AppUpdater = registerPlugin<AppUpdaterPlugin>('AppUpdater', {
  web: () => import('./web').then((m) => new m.AppUpdaterWeb()),
});

export * from './definitions';
export { AppUpdater };
```

**Step 3: Create `src/web.ts`**

```typescript
import { WebPlugin } from '@capacitor/core';

import type { AppUpdaterPlugin } from './definitions';

export class AppUpdaterWeb extends WebPlugin implements AppUpdaterPlugin {
  async getAppVersion(): Promise<{ versionCode: number; versionName: string }> {
    throw this.unavailable('getAppVersion is not supported on web');
  }

  async downloadApk(_options: { url: string; title?: string }): Promise<{ filePath: string }> {
    throw this.unavailable('downloadApk is not supported on web');
  }

  async installApk(_options: { filePath: string }): Promise<void> {
    throw this.unavailable('installApk is not supported on web');
  }
}
```

**Step 4: Build TypeScript and verify**

```bash
npm install && npm run build
```

Expected: Build succeeds, `dist/` contains compiled output.

**Step 5: Commit**

```bash
git add -A && git commit -m "feat: add TypeScript definitions, index, and web fallback"
```

---

### Task 3: Android Gradle Setup — build.gradle, settings.gradle

**Files:**
- Create: `android/build.gradle`
- Create: `android/settings.gradle`

**Step 1: Create `android/build.gradle`**

```groovy
ext {
    junitVersion = project.hasProperty('junitVersion') ? rootProject.ext.junitVersion : '4.13.2'
    androidxAppCompatVersion = project.hasProperty('androidxAppCompatVersion') ? rootProject.ext.androidxAppCompatVersion : '1.7.0'
    androidxJunitVersion = project.hasProperty('androidxJunitVersion') ? rootProject.ext.androidxJunitVersion : '1.2.1'
    androidxEspressoCoreVersion = project.hasProperty('androidxEspressoCoreVersion') ? rootProject.ext.androidxEspressoCoreVersion : '3.6.1'
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.7.2'
    }
}

apply plugin: 'com.android.library'

android {
    namespace "com.capacitor.appupdater"
    compileSdk project.hasProperty('compileSdkVersion') ? rootProject.ext.compileSdkVersion : 35
    defaultConfig {
        minSdkVersion project.hasProperty('minSdkVersion') ? rootProject.ext.minSdkVersion : 23
        targetSdkVersion project.hasProperty('targetSdkVersion') ? rootProject.ext.targetSdkVersion : 35
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    lintOptions {
        abortOnError false
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_21
        targetCompatibility JavaVersion.VERSION_21
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation project(':capacitor-android')
    implementation "androidx.appcompat:appcompat:$androidxAppCompatVersion"
    implementation "androidx.core:core:1.12.0"
    testImplementation "junit:junit:$junitVersion"
    androidTestImplementation "androidx.test.ext:junit:$androidxJunitVersion"
    androidTestImplementation "androidx.test.espresso:espresso-core:$androidxEspressoCoreVersion"
}
```

**Step 2: Create `android/settings.gradle`**

```groovy
include ':capacitor-android'
project(':capacitor-android').projectDir = new File('../node_modules/@capacitor/android/capacitor')
```

**Step 3: Commit**

```bash
git add android/ && git commit -m "chore: add Android Gradle build configuration"
```

---

### Task 4: Android Manifest and FileProvider

**Files:**
- Create: `android/src/main/AndroidManifest.xml`
- Create: `android/src/main/res/xml/file_paths.xml`

**Step 1: Create `android/src/main/AndroidManifest.xml`**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28"/>

    <application>
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.appupdater.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/app_updater_file_paths"/>
        </provider>
    </application>

</manifest>
```

> **Note:** Uses `${applicationId}.appupdater.fileprovider` as the authority to avoid conflicts with any existing FileProvider in the host app. The `meta-data` resource is named `app_updater_file_paths` to avoid collisions.

**Step 2: Create `android/src/main/res/xml/app_updater_file_paths.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path
        name="apk_downloads"
        path="downloads/"/>
</paths>
```

**Step 3: Commit**

```bash
git add android/ && git commit -m "feat: add AndroidManifest with permissions and FileProvider"
```

---

### Task 5: Android Implementation — AppUpdater.java

**Files:**
- Create: `android/src/main/java/com/capacitor/appupdater/AppUpdater.java`

**Step 1: Create the implementation class**

```java
package com.capacitor.appupdater;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;

public class AppUpdater {

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

            // Remove old APK if it exists
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
                        callback.onSuccess(apkFile.getAbsolutePath());
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
                    activity.getPackageName() + ".appupdater.fileprovider",
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
```

**Step 2: Commit**

```bash
git add android/ && git commit -m "feat: add AppUpdater.java with download and install logic"
```

---

### Task 6: Android Plugin Bridge — AppUpdaterPlugin.java

**Files:**
- Create: `android/src/main/java/com/capacitor/appupdater/AppUpdaterPlugin.java`

**Step 1: Create the Capacitor plugin bridge**

```java
package com.capacitor.appupdater;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AppUpdater")
public class AppUpdaterPlugin extends Plugin {

    private AppUpdater implementation = new AppUpdater();

    @Override
    public void load() {
        super.load();
        implementation.setActivity(getActivity());
    }

    @PluginMethod
    public void getAppVersion(PluginCall call) {
        implementation.getAppVersion(new AppUpdater.VersionCallback() {
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

        // Keep the call alive while download is in progress
        call.setKeepAlive(true);

        implementation.downloadApk(url, title, new AppUpdater.DownloadCallback() {
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

        implementation.installApk(filePath, new AppUpdater.InstallCallback() {
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
```

**Step 2: Commit**

```bash
git add android/ && git commit -m "feat: add AppUpdaterPlugin.java Capacitor bridge"
```

---

### Task 7: Build, Verify, Final Commit

**Step 1: Install dependencies and build TypeScript**

```bash
cd capacitor-app-updater && npm install && npm run build
```

Expected: Build succeeds, `dist/` directory contains `esm/`, `plugin.js`, `plugin.cjs.js`.

**Step 2: Verify dist output**

```bash
ls dist/esm/ && cat dist/esm/definitions.d.ts
```

Expected: Type declarations are generated correctly.

**Step 3: Final commit**

```bash
git add -A && git commit -m "chore: build dist output and verify"
```

---

## Consumer Usage

In the host Capacitor app:

**1. Install the plugin:**
```bash
npm install /path/to/capacitor-app-updater
npx cap sync android
```

**2. Register the plugin in `MainActivity.java`:**
```java
import com.capacitor.appupdater.AppUpdaterPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppUpdaterPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
```

**3. Call from TypeScript/Angular:**
```typescript
import { AppUpdater } from 'capacitor-app-updater';

// Get current version
const { versionCode, versionName } = await AppUpdater.getAppVersion();

// Download and install (after user confirms in your UI)
const { filePath } = await AppUpdater.downloadApk({
  url: 'https://your-server.com/app-release.apk',
  title: 'Downloading v1.0.1...'
});
await AppUpdater.installApk({ filePath });
```
