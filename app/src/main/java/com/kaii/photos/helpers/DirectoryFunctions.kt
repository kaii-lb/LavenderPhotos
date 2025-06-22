package com.kaii.photos.helpers

import android.content.Context
import android.os.CancellationSignal
import android.os.Environment
import android.util.Log
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMap
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.SQLiteQuery
import com.kaii.photos.mediastore.MultiAlbumDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile

private const val TAG = "DIRECTORY_FUNCTIONS"
const val EXTERNAL_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

// TODO: rework to use mediastore for faster lookup
/** returns null if the folder doesn't exist ,
 * otherwise returns true if it has files, false if not */
fun Path.checkHasFiles(
    basePath: String,
    flipDotFileMatch: Boolean = false,
    matchSubDirs: Boolean = false
): Boolean? {
    var hasFiles = false

    Log.d(TAG, "Trying to traverse path $this")

    val folder = try {
        Files.walkFileTree(this, object : FileVisitor<Path> {
            override fun preVisitDirectory(
                dir: Path?,
                attrs: BasicFileAttributes?
            ): FileVisitResult {
                if (dir?.startsWith(this@checkHasFiles) == true && !dir.endsWith(this@checkHasFiles) && matchSubDirs) {
                    throw IOException("won't search path that's a subdir of ${this@checkHasFiles}")
                }

                val dataPath = basePath + "Android/data"
                val obbPath = basePath + "Android/obb"
                return if (dir?.startsWith(dataPath) == true || dir?.startsWith(obbPath) == true) {
                    if (this@checkHasFiles.startsWith(dataPath) || this@checkHasFiles.startsWith(
                            obbPath
                        )
                    ) {
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
                    val isDotFile =
                        file.absolutePath.contains(matchForDotFiles) && file.name.startsWith(".")

                    Log.d(TAG, "Trying to scan file ${file.absolutePath}.")

                    val matches = if (flipDotFileMatch) isDotFile else !isDotFile

                    if (matches) {
                        val isNormal = path.isRegularFile(LinkOption.NOFOLLOW_LINKS)
                        val mimeType = Files.probeContentType(path)

                        if (mimeType != null) {
                            val isMedia = mimeType.contains("image") || mimeType.contains("video")

                            if (isNormal && isMedia) {
                                Log.d(
                                    TAG,
                                    "Scanned file ${file.absolutePath} matches all prerequisites, exiting...."
                                )

                                hasFiles = true
                                return FileVisitResult.TERMINATE
                            }
                        }
                    }
                }

                hasFiles = false
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }
        })
    } catch (e: Throwable) {
        Log.e(TAG, "The needed folder for this PhotoGrid doesn't exist!")
        Log.e(TAG, "Path for folder $this")
        Log.e(TAG, e.toString())

        null
    }

    if (folder == null) return null

    return hasFiles
}

fun String.checkPathIsDownloads(): Boolean = run {
    Log.d(TAG, "Relative path to downloads " + toRelativePath())
    toRelativePath().removePrefix("/").startsWith(Environment.DIRECTORY_DOWNLOADS)
            && toRelativePath().removeSuffix("/").endsWith(Environment.DIRECTORY_DOWNLOADS)
}

fun String.getFileNameFromPath(): String = trim().removeSuffix("/").split("/").last()

fun String.getParentFromPath(): String =
    trim().replace(this.getFileNameFromPath(), "").removeSuffix("/")

val File.relativePath: String
    get() = this.absolutePath.toRelativePath()

fun String.toRelativePath() = "/" + trim().replace(toBasePath(), "")

/** only use with strings that are absolute paths*/
fun String.toBasePath() = run {
    val possible = trim().split("/").fastJoinToString(
        separator = "/",
        limit = 4,
        truncated = ""
    )

    // Log.d(TAG, "Possible is $possible")

    when {
        possible.startsWith(baseInternalStorageDirectory) -> possible

        possible.startsWith("/tree/") -> possible.replace("tree", "storage") + "/"

        else -> possible.removeSuffix("/").substringBeforeLast("/") + "/"
    }
}

/** returns the absolute paths to all the found albums */
fun tryGetAllAlbums(context: Context): Flow<List<AlbumInfo>> = channelFlow {
    val cancellationSignal = CancellationSignal()
    val mediaStoreDataSource =
        MultiAlbumDataSource(
            context = context,
            queryString = SQLiteQuery(query = "", paths = null, includedBasePaths = null),
            sortBy = MediaItemSortMode.DateTaken,
            cancellationSignal = cancellationSignal
        )

    suspend fun emitNew(list: List<AlbumInfo>) = send(list)

    mediaStoreDataSource.loadMediaStoreData().collectLatest { list ->
        val new = list.fastDistinctBy { media ->
            media.absolutePath.getParentFromPath()
        }.fastMap { media ->
            val album = media.absolutePath.getParentFromPath()

            AlbumInfo(
                name = album.split("/").last(),
                paths = listOf(album),
                id = media.absolutePath.hashCode()
            )
        }.fastFilter {
            it.name != "" && it.paths.isNotEmpty()
        }

        Log.d(TAG, "new albums are being added")
        emitNew(new)
    }
}