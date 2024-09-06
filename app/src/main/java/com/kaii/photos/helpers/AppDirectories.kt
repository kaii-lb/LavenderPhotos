package com.kaii.photos.helpers

import android.content.Context
import java.io.File

private enum class AppDirectories(val path: String) {
    TrashBin("trash_bin"),
    LockedFolder("locked_folder")
}

fun getAppTrashBinDirectory(context: Context) : String {
    val dir = "/storage/emulated/0/LavenderPhotos/" + AppDirectories.TrashBin.path + "/" // TODO: switch to Environment.getExternalStoragePublicDir

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}

fun getAppLockedFolderDirectory(context: Context) : String {
    val dir = context.getDir(AppDirectories.LockedFolder.path, Context.MODE_PRIVATE).absolutePath

    val folder = File(dir)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    return dir
}
