import { WebPlugin } from '@capacitor/core';

import type { ApkUpdaterPlugin, DownloadApkOptions, DownloadApkResult, GetAppVersionResult, InstallApkOptions } from './definitions';

export class ApkUpdaterWeb extends WebPlugin implements ApkUpdaterPlugin {
  async getAppVersion(): Promise<GetAppVersionResult> {
    throw this.unavailable('getAppVersion is not supported on web');
  }

  async downloadApk(_options: DownloadApkOptions): Promise<DownloadApkResult> {
    throw this.unavailable('downloadApk is not supported on web');
  }

  async installApk(_options: InstallApkOptions): Promise<void> {
    throw this.unavailable('installApk is not supported on web');
  }
}
