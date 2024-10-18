package com.kaii.photos.helpers

import android.util.Log
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.isRegularFile

private const val TAG = "DIRECTORY_FUNCTIONS"

/** returns null if the folder doesn't exist ,
 * otherwise returns true if it has files, false if not */
fun Path.checkHasFiles(flipDotFileMatch: Boolean = false): Boolean? {
    var hasFiles = false

    val folder = try {
        Files.walk(this).iterator()
    } catch (e: Throwable) {
        Log.e(TAG, "The needed folder for this PhotoGrid doesn't exist!")
        Log.e(TAG, e.toString())

        // TODO: maybe wait a bit before exiting
        // Toast.makeText(context, "This folder doesn't exist", Toast.LENGTH_LONG).show()
        // navController.navigate(MultiScreenViewType.MainScreen.name)
        null
    }

    if (folder == null) return null

    try {
        while (folder.hasNext()) {
            val path = folder.next()
			
			val matchForDotFiles = Regex("\\.[A-z]")
			val file = path.toFile()
			val isAlr = file.absolutePath.contains(matchForDotFiles) && file.name.startsWith(".")

			val matches = if (flipDotFileMatch) isAlr else !isAlr
            if (matches) {
                val isNormal = path.isRegularFile(LinkOption.NOFOLLOW_LINKS)
                if (isNormal) {
                    hasFiles = true
                    break
                }
            }
            hasFiles = false
        }
    } catch (e: Throwable) {
        println(e.toString())
    }

    return hasFiles
}
