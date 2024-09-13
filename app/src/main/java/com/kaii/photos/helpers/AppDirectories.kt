package com.kaii.photos.helpers

import android.content.Context
import java.io.File

private enum class AppDirectories(val path: String) {
    TrashBin("trash_bin"),
    LockedFolder("locked_folder")
}

fun Context.getAppTrashBinDirectory() : String {
    val dir = "/storage/emulated/0/LavenderPhotos/" + AppDirectories.TrashBin.path + "/" // TODO: switch to Environment.getExternalStoragePublicDir

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}

fun Context.getAppLockedFolderDirectory() : String {
    var dir = this.getDir(AppDirectories.LockedFolder.path, Context.MODE_PRIVATE)?.absolutePath ?: throw Exception("cannot get absolute path of null object")
    if (!dir.endsWith("/")) dir += "/"

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}

fun Context.getAppRestoredFromLockedFolderDirectory() : String {
    val dir = "/storage/emulated/0/LavenderPhotos/Restored Files/"
    
    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}
