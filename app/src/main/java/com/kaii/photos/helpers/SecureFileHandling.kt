package com.kaii.photos.helpers

import android.content.Context
import java.io.File

fun getDecryptCacheForFile(
    file: File,
    context: Context
): File = File(context.appSecureThumbnailCacheDir, "${file.nameWithoutExtension}-decrypt.${file.extension}")

fun getSecureDecryptedVideoFile(
    name: String,
    context: Context
): File = File(context.appSecureVideoCacheDir, name)

fun File.secureThumbnailImage(context: Context) = File(context.appSecureThumbnailCacheDir, "$nameWithoutExtension.png")
fun File.secureVideoThumbnailImage(context: Context) = File(context.appSecureFolderVideoThumbnailDir, "$nameWithoutExtension.png")