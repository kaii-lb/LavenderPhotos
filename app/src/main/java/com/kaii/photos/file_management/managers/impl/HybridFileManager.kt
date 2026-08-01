package com.kaii.photos.file_management.managers.impl

import android.content.Intent
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTask
import com.kaii.photos.database.entities.SyncTaskStatus
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.traits.Copy
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Favourite
import com.kaii.photos.file_management.managers.traits.Move
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Secure
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.managers.traits.Trash
import com.kaii.photos.helpers.exif.MediaData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.time.Clock

class HybridFileManager @AssistedInject constructor(
    private val syncTaskDao: SyncTaskDao,
    @Assisted private val other: LocalSourceFileManager,
    private val cloud: CloudFileManager,
) : Copy, Move, RenameFile, RenameAlbum, Trash, Delete, Secure, Share, Favourite, ExtractExif, CountAndSize {
    override suspend fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        val (localFiles, cloudFiles) = files.partition { it.isLocalOrLinked }

        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            task = SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Copy,
                destination = destination.id
            )
        ).toInt()

        coroutineScope {
            val localDeferred = async { collectCopy(other, localFiles, destination, sharedTaskId) }
            val cloudDeferred = async { collectCopy(cloud, cloudFiles, destination, sharedTaskId) }

            val localResult = localDeferred.await()
            val cloudResult = cloudDeferred.await()

            val combined = when {
                localResult is Result.Error -> localResult
                cloudResult is Result.Error -> cloudResult
                else -> Result.Success((localResult as Result.Success).data + (cloudResult as Result.Success).data)
            }

            send(FileOperationProgress.Finished(combined))
        }
    }

    override suspend fun moveFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?,
        origin: AlbumType?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        requireNotNull(origin) { "HybridFileManager cannot move without an origin" }

        send(
            element = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Move,
                fileCount = files.size
            )
        )

        val (cloudFiles, localFiles) = files.partition { it.isCloud }
        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Move,
                destination = destination.id,
                extraData = origin.immichId
            )
        ).toInt()

        var localResult: Result<List<FileOperationCopyResult>, FileOperationError> = Result.Success(emptyList())
        var cloudResult: Result<List<FileOperationCopyResult>, FileOperationError> = Result.Success(emptyList())

        if (localFiles.isNotEmpty()) {
            other.moveFiles(localFiles, destination, sharedTaskId, origin).collect { progress ->
                when (progress) {
                    is FileOperationProgress.Started -> Unit
                    is FileOperationProgress.ItemDone -> send(progress)
                    is FileOperationProgress.Finished -> localResult = progress.result
                }
            }
        }

        if (cloudFiles.isNotEmpty() && origin.immichId != null) {
            cloud.moveFiles(localFiles, destination, sharedTaskId, origin).collect { progress ->
                when (progress) {
                    is FileOperationProgress.Started -> Unit
                    is FileOperationProgress.ItemDone -> send(progress)
                    is FileOperationProgress.Finished -> cloudResult = progress.result
                }
            }
        }

        val combined = when {
            localResult is Result.Error -> localResult
            cloudResult is Result.Error -> cloudResult
            else -> Result.Success((localResult as Result.Success).data + (cloudResult as Result.Success).data)
        }

        send(element = FileOperationProgress.Finished(result = combined))
    }

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError> =
        if (file.isCloud) throw IllegalArgumentException("This operation is not supported: Cannot rename individual cloud items!")
        else other.renameFile(file, newName)

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> {
        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Move,
                destination = album.id,
                extraData = album.immichId
            )
        ).toInt()

        return coroutineScope {
            mergeResults(
                localCall = {
                    if (album is AlbumType.Cloud) Result.Success(Unit)
                    else other.renameAlbum(album, newName, sharedTaskId)
                },
                cloudCall = {
                    if (album.immichId == null) Result.Success(Unit)
                    else cloud.renameAlbum(album, newName, sharedTaskId)
                }
            )
        }
    }

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> {
        val localFiles = files.filter { it.isLocalOrLinked }
        val cloudFiles = files.filter { it.isCloudOrLinked }

        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Trash,
                destination = albumId
            )
        ).toInt()

        return coroutineScope {
            mergeResults(
                localCall = {
                    if (localFiles.isEmpty()) Result.Success(Unit)
                    else other.trashFiles(localFiles, isTrashed, albumId, null, sharedTaskId)
                },
                cloudCall = {
                    if (cloudFiles.isEmpty()) Result.Success(Unit)
                    else cloud.trashFiles(cloudFiles, isTrashed, albumId, immichId, sharedTaskId)
                }
            )
        }
    }

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> {
        val localFiles = files.filter { it.isLocalOrLinked }
        val cloudFiles = files.filter { it.isCloudOrLinked }

        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Delete,
                destination = albumId,
                extraData = immichId
            )
        ).toInt()

        return coroutineScope {
            mergeResults(
                localCall = {
                    if (localFiles.isEmpty()) Result.Success(Unit)
                    else other.deleteFiles(localFiles, albumId, immichId, sharedTaskId)
                },
                cloudCall = {
                    if (cloudFiles.isEmpty()) Result.Success(Unit)
                    else cloud.deleteFiles(cloudFiles, albumId, immichId, sharedTaskId)
                }
            )
        }
    }

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> =
        if (files.any { it.isCloud }) throw IllegalArgumentException("This operation is not supported: Cannot secure cloud items!")
        else other.encryptFiles(files)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError> {
        val (cloudFiles, localFiles) = files.partition { it.isCloud }

        val cachedCloudFiles = when (val result = cloud.getRawShareFiles(cloudFiles)) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> result.data
        }

        return other.shareFiles(localFiles + cachedCloudFiles)
    }

    override suspend fun favouriteFile(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        albumId: String?,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> {
        val localFiles = files.filter { it.isLocalOrLinked }
        val cloudFiles = files.filter { it.isCloudOrLinked }

        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Favourite,
                destination = albumId
            )
        ).toInt()

        return coroutineScope {
            mergeResults(
                localCall = {
                    if (localFiles.isEmpty()) Result.Success(Unit)
                    else other.favouriteFile(localFiles, isFavourite, albumId, immichId, sharedTaskId)
                },
                cloudCall = {
                    if (cloudFiles.isEmpty()) Result.Success(Unit)
                    else cloud.favouriteFile(cloudFiles, isFavourite, albumId, immichId, sharedTaskId)
                }
            )
        }
    }

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> =
        if (file.isCloud) cloud.getExifData(file)
        else other.getExifData(file)

    private suspend fun ProducerScope<FileOperationProgress<List<FileOperationCopyResult>>>.collectCopy(
        manager: Copy,
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        taskId: Int
    ): Result<List<FileOperationCopyResult>, FileOperationError> {
        if (files.isEmpty()) return Result.Success(emptyList())

        var result: Result<List<FileOperationCopyResult>, FileOperationError> = Result.Error(FileOperationError.Failed)

        manager.copyFiles(files, destination, taskId).collect { progress ->
            when (progress) {
                is FileOperationProgress.Started -> send(progress)
                is FileOperationProgress.ItemDone -> send(progress)
                is FileOperationProgress.Finished -> result = progress.result
            }
        }

        return result
    }

    private suspend inline fun mergeResults(
        crossinline localCall: suspend () -> Result<Unit, FileOperationError>,
        crossinline cloudCall: suspend () -> Result<Unit, FileOperationError>
    ): Result<Unit, FileOperationError> = coroutineScope {
        val local = async { localCall() }
        val cloud = async { cloudCall() }

        local.await().let { if (it is Result.Error) return@coroutineScope it }
        cloud.await().let { if (it is Result.Error) return@coroutineScope it }

        Result.Success(Unit)
    }

    override suspend fun getMediaCount(
        album: AlbumType
    ): Int = when (album) {
        AlbumType.PlaceHolder -> throw IllegalArgumentException("Cannot get media count for PlaceHolder album!")

        is AlbumType.Cloud -> cloud.getMediaCount(album)

        else -> other.getMediaCount(album)
    }

    override suspend fun getMediaSize(
        album: AlbumType
    ): Long = when (album) {
        AlbumType.PlaceHolder -> throw IllegalArgumentException("Cannot get media count for PlaceHolder album!")

        is AlbumType.Cloud -> cloud.getMediaSize(album)

        else -> other.getMediaSize(album)
    }
}