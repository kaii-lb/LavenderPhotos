package com.kaii.photos.helpers

import android.util.Log
import com.kaii.photos.helpers.getBaseInternalStorageDirectory
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import kotlin.io.path.isRegularFile
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

private const val TAG = "DIRECTORY_FUNCTIONS"

/** returns null if the folder doesn't exist ,
 * otherwise returns true if it has files, false if not */
fun Path.checkHasFiles(flipDotFileMatch: Boolean = false): Boolean? {
    var hasFiles = false

    val folder = try {
        Files.walkFileTree(this, object: FileVisitor<Path> {
            override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                return if (dir?.startsWith(getBaseInternalStorageDirectory() + "Android/data") == true) {
                	if (this@checkHasFiles.startsWith(getBaseInternalStorageDirectory() + "Android/data")) {
                		throw IOException("Can't access file with path $this")
                	} else {
                		FileVisitResult.SKIP_SUBTREE
                	}
                } else {
                    FileVisitResult.CONTINUE
                }
            }

            override fun visitFile(path: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                if (path != null) {
                    val matchForDotFiles = Regex("\\.[A-z]")
                    val file = path.toFile()
                    val isAlr = file.absolutePath.contains(matchForDotFiles) && file.name.startsWith(".")

                    val matches = if (flipDotFileMatch) isAlr else !isAlr
                    if (matches) {
                        val isNormal = path.isRegularFile(LinkOption.NOFOLLOW_LINKS)
                        if (isNormal) {
                            hasFiles = true
                            return FileVisitResult.TERMINATE
                        }
                    }
                }

                hasFiles = false
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
				if (exc != null) {
					throw exc
				}
				return FileVisitResult.TERMINATE
            }

            override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
            	if (exc != null) {
            		throw exc
            		return FileVisitResult.TERMINATE
            	}

                return FileVisitResult.CONTINUE
            }
        })
    } catch (e: Throwable) {
        Log.e(TAG, "The needed folder for this PhotoGrid doesn't exist!")
        Log.e(TAG, e.toString())

        null
    }

    if (folder == null) return null

    return hasFiles
}
