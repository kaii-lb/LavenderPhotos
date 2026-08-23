package com.kaii.photos.file_management.managers.gateways

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.mediastore.MediaType
import io.github.kaii_lb.lavender.immichintegration.AssetSource
import io.github.kaii_lb.lavender.immichintegration.WriteChannel
import java.io.OutputStream

interface MediaStoreGateway {
    fun insertMedia(
        media: MediaStoreData,
        destination: String
    ): Result<Uri, FileOperationError>

    fun setDateForMedia(
        uri: Uri,
        dateTaken: Long,
        type: MediaType
    ): Result<Unit, FileOperationError>

    fun getContentId(
        uri: Uri,
        type: MediaType
    ): Result<Long, FileOperationError>

    fun copy(
        media: MediaStoreData,
        destination: String
    ): Result<FileOperationCopyResult, FileOperationError>

    fun trash(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean
    ): Result<Unit, FileOperationError>

    fun delete(
        files: List<FileOperationItemMetadata>
    ): Result<Unit, FileOperationError>

    fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError>

    fun favourite(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean
    ): Result<Unit, FileOperationError>

    fun share(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError>

    fun getExifData(
        media: MediaStoreData
    ): Result<Map<MediaData, String>, FileOperationError>

    fun eraseExifData(
        absolutePath: String
    ): Result<Unit, FileOperationError>

    fun getTrashMediaById(
        id: Long
    ): Result<MediaStoreData, FileOperationError>

    fun getWriteChannel(uri: Uri): WriteChannel
    fun getAssetSource(uri: Uri): AssetSource

    fun is24HrFormat(): Boolean

    fun createWriteRequest(files: List<FileOperationItemMetadata>): PendingIntent

    fun openOutputStream(uri: Uri): OutputStream?
}