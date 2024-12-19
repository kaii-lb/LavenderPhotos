package com.kaii.photos.helpers

import android.content.Context
import android.os.Environment
import java.io.File

private enum class AppDirectories(val path: String) {
    TrashBin("trash_bin"),
    LockedFolder("locked_folder")
}

/** ends with a "/" */
fun getBaseInternalStorageDirectory() : String {
    val absolutePath = Environment.getExternalStorageDirectory().absolutePath

    return absolutePath.removeSuffix("/") + "/"
}

/** ends with a "/" */
fun getAppTrashBinDirectory() : String {
    val dir = "${getBaseInternalStorageDirectory()}LavenderPhotos/" + AppDirectories.TrashBin.path + "/" // TODO: switch to Environment.getExternalStoragePublicDir

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
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
fun getAppRestoredFromLockedFolderDirectory(): String {
    val dir = "${getBaseInternalStorageDirectory()}LavenderPhotos/Restored Files/"

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}
