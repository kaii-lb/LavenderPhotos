package com.kaii.photos.file_management.managers.gateways

import android.net.Uri
import java.io.File

interface CloudCacheGateway {
    /** gets or creates a file in [android.content.Context.cacheDir] for the specified filename */
    fun cacheFile(fileName: String): File

    /** gets a shareable URI from the apps internal files */
    fun shareableUri(file: File): Uri

    fun enqueueSyncWorker(albumId: String)
}