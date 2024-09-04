package com.kaii.photos.helpers

import android.content.Context
import java.io.File

private enum class AppDirectories(val path: String) {
    TrashBin("trash_bin"),
    LockedFolder("locked_folder")
}

fun getAppTrashBinDirectory(context: Context) : String {
//    return context.getDir(AppDirectories.TrashBin.path, Context.MODE_PRIVATE)


    val folder = File("/storage/emulated/0/Download/RANDOMASSSHIT/")
    if (!folder.exists()) {
        folder.mkdirs()
    }
    return "/storage/emulated/0/Download/RANDOMASSSHIT/"
}

fun getAppLockedFolderDirectory(context: Context) : String {
    return context.getDir(AppDirectories.LockedFolder.path, Context.MODE_PRIVATE).absolutePath
}
