package com.kaii.photos.helpers

import android.content.Context
import android.os.Environment
import java.io.File

enum class AppDirectories(val path: String) {
    MainDir("LavenderPhotos"),
    SecureFolder("secure_folder"),
    RestoredFolder("Restored Files"),
    SecureVideoCacheDir("secure_video_cache_dir"),
    SecureThumbnailCacheDir("secure_thumbnail_cache_dir"),
    OldSecureFolder("locked_folder")
}

/** ends with a "/" */
val baseInternalStorageDirectory = run {
    val absolutePath = Environment.getExternalStorageDirectory().absolutePath

    absolutePath.removeSuffix("/") + "/"
}

/** doesn't end with a "/" */
val Context.appSecureFolderDir: String
    get() {
        val path = filesDir.absolutePath.removeSuffix("/") + "/" + AppDirectories.SecureFolder.path // TODO: switch to external files dir for extra storage space

        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()

        return dir.absolutePath.removeSuffix("/")
    }

/** doesn't end with a "/" */
val Context.appRestoredFilesDir: String
    get() {
        val dataPath = getExternalFilesDir(AppDirectories.MainDir.path + "/" + AppDirectories.RestoredFolder.path)?.absolutePath ?: throw Exception("Cannot get path of null object: Restored Files doesn't exist.")

        val path = dataPath.replace("data", "media").replace("files", "")
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()

        return dir.absolutePath.removeSuffix("/")
    }

/** doesn't end with a "/" */
val Context.appStorageDir: String
    get() {
        val dataPath = getExternalFilesDir(AppDirectories.MainDir.path)?.absolutePath ?: throw Exception("Cannot get path of null object: Main Dir doesn't exist.")

        val path = dataPath.replace("data", "media").replace("files", "")
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()

        return dir.absolutePath.removeSuffix("/")
    }

/** doesn't end with a "/" */
val Context.appSecureVideoCacheDir: String
    get() {
        val path = cacheDir.absolutePath.removeSuffix("/") + "/" + AppDirectories.SecureVideoCacheDir.path

        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()

        return dir.absolutePath.removeSuffix("/")
    }

/** doesn't end with a "/" */
val Context.appSecureThumbnailCacheDir: String
    get() {
        val path = cacheDir.absolutePath.removeSuffix("/") + "/" + AppDirectories.SecureThumbnailCacheDir.path

        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()

        return dir.absolutePath.removeSuffix("/")
    }

val File.relativePath: String
    get() = this.absolutePath.replace(baseInternalStorageDirectory, "")

fun String.toRelativePath() = replace(baseInternalStorageDirectory, "")