package com.kaii.photos.file_management.managers.impl

import android.content.Intent
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.domain.mapTo
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.managers.operations.LocalEncryptOperation
import com.kaii.photos.file_management.managers.operations.LocalGetExifOperation
import com.kaii.photos.file_management.managers.operations.LocalSourceCopyOperation
import com.kaii.photos.file_management.managers.operations.RenameAlbumOperation
import com.kaii.photos.helpers.exif.MediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CustomFileManager @Inject constructor(
    private val customDao: CustomEntityDao,
    private val gateway: MediaStoreGateway,
    private val copyOperation: LocalSourceCopyOperation,
    private val renameAlbum: RenameAlbumOperation,
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
                is FileOperationProgress.Started -> send(element = FileOperationProgress.Started(
                    action = FileOperationAction.LongOperationType.Move,
                    fileCount = files.size
                ))

                is FileOperationProgress.ItemDone -> send(progress)
                is FileOperationProgress.Finished -> copyResult = progress.result
            }
        }

        if (copyResult == null || copyResult is Result.Error) {
            send(
                element = FileOperationProgress.Finished(
                    result = Result.Error((copyResult as Result.Error).error)
                )
            )

            return@channelFlow
        }

        var finalResult: Result<List<FileOperationCopyResult>, FileOperationError>? = null

        deleteFiles(
            files = files,
            albumId = destination.id,
            immichId = destination.immichId,
            existingTaskId = existingTaskId
        ).collect { progress ->
            if (progress is FileOperationProgress.Finished) {
                finalResult = progress.result.mapTo(
                    to = copyResult as Result.Success
                )
            }
        }

        send(
            element = FileOperationProgress.Finished(
                result = finalResult ?: Result.Error(FileOperationError.Failed)
            )
        )
    }

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError> = gateway.renameFile(file, newName)

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<Unit>> = flow {
        check(isTrashed) {
            "Physically impossible to restore files into a custom album"
        }

        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.TrashDelete,
                fileCount = files.size
            )
        )

        customDao.deleteAll(
            ids = files.map { it.id }.toSet(),
            album = albumId
        )

        emit(
            value = FileOperationProgress.Finished(
                result = Result.Success(Unit)
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<Unit>> = flow {
        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Delete,
                fileCount = files.size
            )
        )

        customDao.deleteAll(
            ids = files.map { it.id }.toSet(),
            album = albumId
        )

        emit(
            value = FileOperationProgress.Finished(
                result = Result.Success(Unit)
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> = encrypt.execute(files)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Intent>> = flow {
        // no point in showing a snackbar for an instant operation
        val result = gateway.share(files)

        emit(
            value = FileOperationProgress.Finished(
                result = result
            )
        )
    }

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = Result.Success(
        data = renameAlbum.execute(album, newName)
    )

    override suspend fun favouriteFile(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        albumId: String?,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = gateway.favourite(files, isFavourite)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> = getExif.execute(file)

    override suspend fun getMediaCount(
        album: AlbumType
    ): Int = withContext(Dispatchers.IO) {
        customDao.countMediaInAlbum(
            album = (album as AlbumType.Folder).id
        )
    }

    override suspend fun getMediaSize(
        album: AlbumType
    ): Long = withContext(Dispatchers.IO) {
        customDao.mediaSize(
            album = (album as AlbumType.Folder).id
        )
    }
}