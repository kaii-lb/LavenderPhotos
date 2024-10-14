package com.kaii.photos.helpers

import android.util.Log
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

private const val TAG = "DIRECTORY_FUNCTIONS"

/** returns null if the folder doesn't exist ,
 * otherwise returns true if it has files, false if not */
fun Path.checkHasFiles(): Boolean? {
    var hasFiles = false

    val folder = try {
        Files.walk(this).iterator()
    } catch(e: Throwable) {
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
	        val file = folder.next()

	        // TODO: match for ".letters" in file path
	        if (!file.toString().contains(".thumbnails")) {
	            val isNormal = file.isRegularFile(LinkOption.NOFOLLOW_LINKS)
	            if (isNormal) {
	                hasFiles = true
	                break
	            }
	        }
	        hasFiles = false
    	}
   	} catch(e: Throwable) {
   		println(e.toString())
   	}

    return hasFiles
}
