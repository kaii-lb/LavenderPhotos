package com.kaii.photos.helpers

import android.content.Context
import android.os.Environment
import java.io.File

private enum class AppDirectories(val path: String) {
    MainDir("LavenderPhotos"),
    LockedFolder("locked_folder")
}

/** ends with a "/" */
val baseInternalStorageDirectory = run {
    val absolutePath = Environment.getExternalStorageDirectory().absolutePath

    absolutePath.removeSuffix("/") + "/"
}

/** ends with a "/" */
fun Context.getAppLockedFolderDirectory() : String {
    var dir = this.getDir(AppDirectories.LockedFolder.path, Context.MODE_PRIVATE)?.absolutePath ?: throw Exception("cannot get absolute path of null object")
    if (!dir.endsWith("/")) dir += "/"

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}

/** ends with a "/" */
fun getAppRestoredFromLockedFolderDirectory() : String {
    val dir = baseInternalStorageDirectory + AppDirectories.MainDir + "/Restored Files/"

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}

/** ends with a "/" */
fun getAppStorageDir() : String {
    val dir = baseInternalStorageDirectory + AppDirectories.MainDir + "/"

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}
