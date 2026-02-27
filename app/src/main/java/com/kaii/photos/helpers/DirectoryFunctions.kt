package com.kaii.photos.helpers

import android.os.Environment
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.util.fastMinByOrNull
import java.io.File

const val EXTERNAL_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

fun String.checkPathIsDownloads(): Boolean = run {
    toRelativePath(true).startsWith(Environment.DIRECTORY_DOWNLOADS)
            && toRelativePath(true).endsWith(Environment.DIRECTORY_DOWNLOADS)
}

fun String.filename(): String = trim().split("/").takeLast(1).first()

fun String.parent(): String =
    trim().split("/").dropLast(1).joinToString("/") { it }

val File.relativePath: String
    get() = this.absolutePath.toRelativePath()

fun String.toRelativePath(removePrefix: Boolean = false) =
    (if (removePrefix) "" else "/") + trim().replace(toBasePath(), "")

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

/** finds the highest level shared parent between a given set of paths */
fun findMinParent(paths: List<String>) = run {
    val mut = paths.toMutableList()
    var currentMin = mut.fastMinByOrNull {
        it.toRelativePath(true).length
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
            it.toRelativePath(true).length
        }
    }

    unique
}