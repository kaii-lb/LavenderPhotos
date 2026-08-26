package com.kaii.photos.database

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.domain.files.FileOperationItemMetadata

suspend fun MediaDao.getMediaFromMetadata(
    files: List<FileOperationItemMetadata>
) = files.chunked(500).flatMap { chunk ->
    this.getMedia(ids = chunk.fastMap { it.id })
}

fun String.escapeLikeWildcards(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")