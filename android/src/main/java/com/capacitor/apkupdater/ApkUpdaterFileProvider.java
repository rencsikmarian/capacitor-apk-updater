package com.capacitor.apkupdater;

import androidx.core.content.FileProvider;

/**
 * Empty subclass to avoid manifest merge conflicts when the host app
 * or another library also declares androidx.core.content.FileProvider.
 */
public class ApkUpdaterFileProvider extends FileProvider {
}
