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
import com.kaii.photos.domain.mapTo
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
        send(
            element = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Copy,
                fileCount = files.size
            )
        )

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

            val combined = localResult.mapTo { l ->
                cloudResult.mapTo { c ->
                    Result.Success(l.data + c.data)
                }
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
            cloud.moveFiles(cloudFiles, destination, sharedTaskId, origin).collect { progress ->
                when (progress) {
                    is FileOperationProgress.Started -> Unit
                    is FileOperationProgress.ItemDone -> send(progress)
                    is FileOperationProgress.Finished -> cloudResult = progress.result
                }
            }
        }

        val combined = localResult.mapTo { l ->
            cloudResult.mapTo { c ->
                Result.Success(l.data + c.data)
            }
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
    ): Flow<FileOperationProgress<Unit>> = channelFlow {
        val localFiles = files.filter { it.isLocalOrLinked }
        val cloudFiles = files.filter { it.isCloudOrLinked }

        send(
            element = FileOperationProgress.Started(
                action =
                    if (isTrashed) FileOperationAction.LongOperationType.TrashDelete
                    else FileOperationAction.LongOperationType.TrashRestore,
                fileCount = files.size
            )
        )

        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Trash,
                destination = albumId
            )
        ).toInt()

        val result = runSplitUnitOperation(
            localFiles = localFiles,
            cloudFiles = cloudFiles,
            localCall = { other.trashFiles(it, isTrashed, albumId, null, sharedTaskId) },
            cloudCall = { cloud.trashFiles(it, isTrashed, albumId, immichId, sharedTaskId) }
        )

        send(FileOperationProgress.Finished(result = result))
    }

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<Unit>> = channelFlow {
        val localFiles = files.filter { it.isLocalOrLinked }
        val cloudFiles = files.filter { it.isCloudOrLinked }

        send(
            element = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Delete,
                fileCount = files.size
            )
        )

        val sharedTaskId = existingTaskId ?: syncTaskDao.insert(
            SyncTask(
                dateModified = Clock.System.now().epochSeconds,
                status = SyncTaskStatus.Processing,
                type = SyncTaskType.Delete,
                destination = albumId,
                extraData = immichId
            )
        ).toInt()

        val result = runSplitUnitOperation(
            localFiles = localFiles,
            cloudFiles = cloudFiles,
            localCall = { other.deleteFiles(it, albumId, immichId, sharedTaskId) },
            cloudCall = { cloud.deleteFiles(it, albumId, immichId, sharedTaskId) }
        )

        send(FileOperationProgress.Finished(result = result))
    }

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> =
        if (files.any { it.isCloud }) throw IllegalArgumentException("This operation is not supported: Cannot secure cloud items!")
        else other.encryptFiles(files)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Intent>> = channelFlow {
        val (cloudFiles, localFiles) = files.partition { it.isCloud }

        val cachedCloudFiles = mutableListOf<FileOperationItemMetadata>()

        if (cloudFiles.isNotEmpty()) {
            cloud.getRawShareFiles(cloudFiles).collect { progress ->
                when (progress) {
                    is FileOperationProgress.Started -> send(
                        element = FileOperationProgress.Started(
                            action = progress.action,
                            fileCount = progress.fileCount
                        )
                    )

                    is FileOperationProgress.ItemDone -> {
                        send(
                            element = FileOperationProgress.ItemDone(
                                uri = progress.uri
                            )
                        )
                    }

                    is FileOperationProgress.Finished -> {
                        when (val result = progress.result) {
                            is Result.Error -> {
                                send(
                                    element = FileOperationProgress.Finished(
                                        result = Result.Error(
                                            error = result.error
                                        )
                                    )
                                )
                            }

                            is Result.Success -> {
                                cachedCloudFiles.addAll(elements = result.data)
                            }
                        }
                    }
                }
            }
        }

        other.shareFiles(localFiles + cachedCloudFiles).collect { progress ->
            if (progress is FileOperationProgress.Finished) {
                send(element = progress)
            }
        }
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
                is FileOperationProgress.Started -> Unit
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

    private suspend fun ProducerScope<FileOperationProgress<Unit>>.runSplitUnitOperation(
        localFiles: List<FileOperationItemMetadata>,
        cloudFiles: List<FileOperationItemMetadata>,
        localCall: suspend (List<FileOperationItemMetadata>) -> Flow<FileOperationProgress<Unit>>,
        cloudCall: suspend (List<FileOperationItemMetadata>) -> Flow<FileOperationProgress<Unit>>,
    ): Result<Unit, FileOperationError> {
        val doneMap = (localFiles + cloudFiles).associate {
            it.uri to false
        }.toMutableMap()

        var localResult: Result<Unit, FileOperationError>? = null
        if (localFiles.isEmpty()) {
            localResult = Result.Success(Unit)
        } else {
            localCall(localFiles).collect { progress ->
                when (progress) {
                    is FileOperationProgress.ItemDone -> {
                        if (doneMap[progress.uri] != true) {
                            send(progress)

                            doneMap[progress.uri] = true
                        }
                    }

                    is FileOperationProgress.Finished -> localResult = progress.result
                    else -> Unit
                }
            }
        }

        var cloudResult: Result<Unit, FileOperationError>? = null
        if (cloudFiles.isEmpty()) {
            cloudResult = Result.Success(Unit)
        } else {
            cloudCall(cloudFiles).collect { progress ->
                when (progress) {
                    is FileOperationProgress.ItemDone -> {
                        if (progress.uri.startsWith("/api")) {
                            if (doneMap[progress.uri] != true) {
                                send(progress)

                                doneMap[progress.uri] = true
                            }
                        }
                    }

                    is FileOperationProgress.Finished -> cloudResult = progress.result
                    else -> Unit
                }
            }
        }

        return if (localResult != null && cloudResult != null) {
            localResult.mapTo(cloudResult)
        } else {
            Result.Error(FileOperationError.Failed)
        }
    }
}