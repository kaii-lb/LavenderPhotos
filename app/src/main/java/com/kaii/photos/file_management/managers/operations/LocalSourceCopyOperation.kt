package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationItemMetadata

class LocalSourceCopyOperation(
    private val toLocal: LocalToLocalOperation,
    private val toCustom: LocalToCustomOperation,
    private val toCloud: LocalToCloudOperation
) {
    fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?
    ) = when (destination) {
        is AlbumType.Folder -> toLocal.execute(files = files, destination = destination)

        is AlbumType.Custom -> toCustom.execute(mediaIds = files.map { it.id }, destination = destination)

        is AlbumType.Cloud -> toCloud.execute(files = files, destination = destination, existingTaskId = existingTaskId)

        else -> throw IllegalArgumentException("Cannot copy files to a placeholder album!")
    }
}