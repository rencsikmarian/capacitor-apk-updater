export interface ApkUpdaterPlugin {
  /**
   * Get the current app version code and name.
   *
   * @since 0.0.1
   */
  getAppVersion(): Promise<GetAppVersionResult>;

  /**
   * Download an APK file from a URL using the system DownloadManager.
   *
   * A notification will be shown during the download.
   *
   * @since 0.0.1
   */
  downloadApk(options: DownloadApkOptions): Promise<DownloadApkResult>;

  /**
   * Install a previously downloaded APK file.
   *
   * The user will be prompted by the system to confirm the installation.
   *
   * @since 0.0.1
   */
  installApk(options: InstallApkOptions): Promise<void>;
}

export interface GetAppVersionResult {
  /**
   * The numeric version code of the app (e.g. `10`).
   *
   * @since 0.0.1
   */
  versionCode: number;

  /**
   * The version name string of the app (e.g. `"1.2.0"`).
   *
   * @since 0.0.1
   */
  versionName: string;
}

export interface DownloadApkOptions {
  /**
   * The URL to download the APK from.
   *
   * @since 0.0.1
   */
  url: string;

  /**
   * The title shown in the download notification.
   *
   * @default "Downloading update..."
   * @since 0.0.1
   */
  title?: string;
}

export interface DownloadApkResult {
  /**
   * The local file path of the downloaded APK.
   *
   * @since 0.0.1
   */
  filePath: string;
}

export interface InstallApkOptions {
  /**
   * The local file path of the APK to install.
   *
   * @since 0.0.1
   */
  filePath: string;
}
