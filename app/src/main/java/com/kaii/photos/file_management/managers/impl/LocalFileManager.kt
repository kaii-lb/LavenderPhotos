package com.kaii.photos.file_management.managers.impl

import android.content.Intent
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.managers.operations.LocalEncryptOperation
import com.kaii.photos.file_management.managers.operations.LocalGetExifOperation
import com.kaii.photos.file_management.managers.operations.LocalSourceCopyOperation
import com.kaii.photos.file_management.managers.operations.RenameAlbumOperation
import com.kaii.photos.helpers.exif.MediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalFileManager @Inject constructor(
    private val mediaDao: MediaDao,
    private val gateway: MediaStoreGateway,
    private val renameAlbum: RenameAlbumOperation,
    private val copyOperation: LocalSourceCopyOperation,
    private val encrypt: LocalEncryptOperation,
    private val getExif: LocalGetExifOperation
) : LocalSourceFileManager {
    override suspend fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = copyOperation.copyFiles(
        files = files,
        destination = destination,
        existingTaskId = existingTaskId
    )

    override suspend fun moveFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?,
        origin: AlbumType?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        var copyResult: Result<List<FileOperationCopyResult>, FileOperationError>? = null

        copyOperation.copyFiles(
            files = files,
            destination = destination,
            existingTaskId = existingTaskId
        ).collect { progress ->
            when (progress) {
                is FileOperationProgress.ItemDone -> send(progress)
                is FileOperationProgress.Finished -> copyResult = progress.result
            }
        }

        when (val result = copyResult) {
            is Result.Success -> {
                val newResult = deleteFiles(files, destination.id, existingTaskId)

                send(
                    element = FileOperationProgress.Finished(
                        result =
                            if (newResult is Result.Success) result
                            else Result.Error((newResult as Result.Error).error)
                    )
                )
            }

            is Result.Error -> send(element = FileOperationProgress.Finished(result))

            null -> send(element = FileOperationProgress.Finished(Result.Error(FileOperationError.Failed)))
        }
    }

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError> = gateway.renameFile(file, newName)

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = Result.Success(
        data = renameAlbum.execute(album, newName)
    )

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, Any>, FileOperationError> = withContext(Dispatchers.IO) {
        getExif.execute(file)
    }

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> = encrypt.execute(files)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError> = gateway.share(files)

    override suspend fun trashFile(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = gateway.trash(
        files = files,
        isTrashed = isTrashed
    )

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = gateway.delete(files)

    override suspend fun favouriteFile(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        albumId: String?,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = gateway.favourite(files, isFavourite)

    override suspend fun getMediaCount(
        album: AlbumType
    ): Int = mediaDao.countMediaInPaths(
        paths = (album as AlbumType.Folder).paths
    )

    override suspend fun getMediaSize(
        album: AlbumType
    ): Long = mediaDao.mediaSize(
        paths = (album as AlbumType.Folder).paths
    )
}