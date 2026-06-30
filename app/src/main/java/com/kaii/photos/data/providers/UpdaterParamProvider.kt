package com.kaii.photos.data.providers

import com.kaii.photos.PhotosApplication

class UpdaterParamProvider {
    val showUpdateNotice = PhotosApplication.appModule.settings.versions.getShowUpdateNotice()
}