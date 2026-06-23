package com.kaii.photos.data.providers

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

class AppVersionProvider(
    private val context: Context
) {
    private val packageInfo by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }

    fun getCurrentVersionCode(): Long {
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }
}