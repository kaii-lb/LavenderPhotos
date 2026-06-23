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

/**
 * A non-colliding destination in the secure folder for [originalName]. Two source files can share a
 * basename (e.g. DCIM/a/IMG_001.jpg and Pictures/b/IMG_001.jpg); without this the second would
 * overwrite the first's ciphertext and leave its db row pointing at the wrong bytes. Suffixes the
 * stem (`IMG_001_1.jpg`) until the path is free; thumbnail names derive from this so they stay in sync.
 */
fun uniqueSecureDestination(context: Context, originalName: String): File {
    val dir = File(context.appSecureFolderDir)
    val candidate = File(dir, originalName)
    if (!candidate.exists()) return candidate

    val stem = candidate.nameWithoutExtension
    val ext = candidate.extension.let { if (it.isEmpty()) "" else ".$it" }
    var i = 1
    while (true) {
        val next = File(dir, "${stem}_$i$ext")
        if (!next.exists()) return next
        i++
    }
}