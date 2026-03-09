# capacitor-apk-updater

Capacitor plugin for Android APK self-update. Download and install APK files without the Google Play Store.

## Install

```bash
npm install capacitor-apk-updater
npx cap sync
```

## Usage

```typescript
import { ApkUpdater } from 'capacitor-apk-updater';

// Get the current app version
const { versionCode, versionName } = await ApkUpdater.getAppVersion();

// Download an APK
const { filePath } = await ApkUpdater.downloadApk({
  url: 'https://example.com/app-update.apk',
  title: 'Downloading update...',
});

// Install the downloaded APK
await ApkUpdater.installApk({ filePath });
```

## Android Configuration

The host app must add the following to its `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## API

<docgen-index>

* [`getAppVersion()`](#getappversion)
* [`downloadApk(...)`](#downloadapk)
* [`installApk(...)`](#installapk)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### getAppVersion()

```typescript
getAppVersion() => any
```

Get the current app version code and name.

**Returns:** <code>any</code>

**Since:** 0.0.1

--------------------


### downloadApk(...)

```typescript
downloadApk(options: DownloadApkOptions) => any
```

Download an APK file from a URL using the system DownloadManager.

A notification will be shown during the download.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#downloadapkoptions">DownloadApkOptions</a></code> |

**Returns:** <code>any</code>

**Since:** 0.0.1

--------------------


### installApk(...)

```typescript
installApk(options: InstallApkOptions) => any
```

Install a previously downloaded APK file.

The user will be prompted by the system to confirm the installation.

| Param         | Type                                                            |
| ------------- | --------------------------------------------------------------- |
| **`options`** | <code><a href="#installapkoptions">InstallApkOptions</a></code> |

**Returns:** <code>any</code>

**Since:** 0.0.1

--------------------


### Interfaces


#### GetAppVersionResult

| Prop              | Type                | Description                                          | Since |
| ----------------- | ------------------- | ---------------------------------------------------- | ----- |
| **`versionCode`** | <code>number</code> | The numeric version code of the app (e.g. `10`).     | 0.0.1 |
| **`versionName`** | <code>string</code> | The version name string of the app (e.g. `"1.2.0"`). | 0.0.1 |


#### DownloadApkOptions

| Prop        | Type                | Description                                   | Default                              | Since |
| ----------- | ------------------- | --------------------------------------------- | ------------------------------------ | ----- |
| **`url`**   | <code>string</code> | The URL to download the APK from.             |                                      | 0.0.1 |
| **`title`** | <code>string</code> | The title shown in the download notification. | <code>"Downloading update..."</code> | 0.0.1 |


#### DownloadApkResult

| Prop           | Type                | Description                                | Since |
| -------------- | ------------------- | ------------------------------------------ | ----- |
| **`filePath`** | <code>string</code> | The local file path of the downloaded APK. | 0.0.1 |


#### InstallApkOptions

| Prop           | Type                | Description                                | Since |
| -------------- | ------------------- | ------------------------------------------ | ----- |
| **`filePath`** | <code>string</code> | The local file path of the APK to install. | 0.0.1 |

</docgen-api>
