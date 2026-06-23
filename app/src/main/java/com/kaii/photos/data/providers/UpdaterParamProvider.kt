package com.kaii.photos.data.providers

import android.content.Context
import com.kaii.photos.di.appModule

class UpdaterParamProvider(
    context: Context
) {
    val showUpdateNotice = context.appModule.settings.versions.getShowUpdateNotice()
}