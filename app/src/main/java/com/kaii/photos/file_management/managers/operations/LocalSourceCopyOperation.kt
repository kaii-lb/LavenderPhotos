package com.kaii.photos.file_management.managers.operations

import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationItemMetadata
import javax.inject.Inject

class LocalSourceCopyOperation @Inject constructor(
    private val toLocal: LocalToLocalOperation,
    private val toCustom: LocalToCustomOperation,
    private val toCloud: LocalToCloudOperation
) {
    fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType
    ) = when (destination) {
        is AlbumType.Folder -> toLocal.execute(files = files, destination = destination)

        is AlbumType.Custom -> toCustom.execute(mediaIds = files.map { it.id }, destination = destination)

        is AlbumType.Cloud -> toCloud.execute(files = files, destination = destination)

        else -> throw IllegalArgumentException("Cannot copy files to a placeholder album!")
    }
}