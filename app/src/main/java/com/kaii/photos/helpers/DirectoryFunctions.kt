package com.kaii.photos.helpers

import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.util.fastMinByOrNull

const val EXTERNAL_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

fun String.checkPathIsDownloads(): Boolean = run {
    toRelativePath().startsWith(Environment.DIRECTORY_DOWNLOADS)
            && toRelativePath().endsWith(Environment.DIRECTORY_DOWNLOADS)
}

fun String.filename(): String = trim().removeSuffix("/").substringAfterLast('/')

fun String.parent(): String = trim().removeSuffix("/").let { path ->
    if (!path.contains('/')) ""
    else path.substringBeforeLast('/')
}

/** does not end with a "/" */
fun String.toRelativePath(
    baseStorageDir: String = baseInternalStorageDirectory
): String {
    val basePath = toBasePath(baseStorageDir)
    val trimmed = trim()

    val relative =
        if (trimmed.startsWith("/tree/")) trimmed.substring(trimmed.lastIndexOf(':') + 1).removeSuffix("/")
        else if (trimmed.startsWith(basePath)) trimmed.substring(basePath.length).removeSuffix("/")
        else trimmed.removeSuffix("/")

    return relative.removePrefix("/")
}

/** only use with strings that are absolute paths
 *
 * does not end with a "/" */
fun String.toBasePath(
    baseStorageDir: String = baseInternalStorageDirectory
): String {
    val trimmed = this.trim()

    // document uri
    if (trimmed.startsWith("/tree/")) {
        val content = trimmed.removePrefix("/tree/")

        val volumeToken = content.substringBefore(':').substringBefore('/')

        return if (volumeToken.equals("primary", ignoreCase = true)) "/storage/emulated/0"
        else "/storage/$volumeToken"
    }

    // /storage/emulated/0
    if (trimmed.startsWith(baseStorageDir)) return baseStorageDir

    return trimmed.substring(0, trimmed.indexOfOccurrence('/', 3))
}

private fun String.indexOfOccurrence(char: Char, occurrence: Int): Int {
    var count = 0
    var index = -1
    do {
        index = this.indexOf(char, index + 1)
        if (index != -1) count++
    } while (count < occurrence && index != -1)
    return index
}

/** finds the highest level shared parent between a given set of paths */
fun findMinParent(paths: List<String>) = run {
    val mut = paths.toMutableList()
    var currentMin = mut.fastMinByOrNull {
        it.toRelativePath().length
    }
    val unique = mutableListOf<String>()

    while (currentMin != null) {
        val children = mut.filter {
            it.startsWith(currentMin)
        }

        if (children.isNotEmpty()) {
            mut.removeAll(children)
            unique.add(currentMin)
        }
        currentMin = mut.fastMinByOrNull {
            it.toRelativePath().length
        }
    }

    unique
}

fun String.volumeName(
    currentVolumes: Set<String>,
    baseStorageDir: String = baseInternalStorageDirectory
) =
    if (toBasePath(baseStorageDir).startsWith(baseStorageDir)) MediaStore.VOLUME_EXTERNAL
    else currentVolumes.find {
        val possible = toBasePath(baseStorageDir).replace("/storage/", "").removeSuffix("/")
        it.equals(possible, ignoreCase = true)
    }