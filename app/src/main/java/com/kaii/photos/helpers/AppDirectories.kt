package com.kaii.photos.helpers

import android.content.Context
import android.os.Environment
import java.io.File

private enum class AppDirectories(val path: String) {
    MainDir("LavenderPhotos"),
    LockedFolder("secure_folder"),
    RestoredFolder("Restored Files")
}

/** ends with a "/" */
val baseInternalStorageDirectory = run {
    val absolutePath = Environment.getExternalStorageDirectory().absolutePath

    absolutePath.removeSuffix("/") + "/"
}

/** doesn't end with a "/" */
val Context.appSecureFolderDir: String
    get() {
        val path = filesDir.absolutePath.removeSuffix("/") + "/" + AppDirectories.LockedFolder.path // TODO: switch to external files dir for extra storage space

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
