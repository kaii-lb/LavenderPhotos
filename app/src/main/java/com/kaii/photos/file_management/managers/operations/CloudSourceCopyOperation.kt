package com.kaii.photos.file_management.managers.operations

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.getMediaByIds
import com.kaii.photos.database.track
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.AndroidMediaStoreGateway
import com.kaii.photos.helpers.appCloudFolderDir
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.uuid.Uuid

class CloudSourceCopyOperation(
    private val mediaDao: MediaDao,
    private val syncTaskDao: SyncTaskDao,
    private val gateway: AndroidMediaStoreGateway,
    private val assetsClient: AssetsClient,
    private val albumsClient: AlbumsClient,
    private val toCustom: LocalToCustomOperation
) {
    fun copyItems(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> =
        when (destination) {
            is AlbumType.Folder -> download(files, destination, existingTaskId)

            is AlbumType.Custom -> copyToCustom(files, destination, existingTaskId)

            is AlbumType.Cloud -> copyToCloud(files, destination, existingTaskId)

            else -> throw IllegalArgumentException("Cannot copy files to a placeholder album!")
        }

    private fun download(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Folder,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        val result = syncTaskDao.track(
            existingTaskId = existingTaskId,
            type = SyncTaskType.Copy,
            destination = destination.immichId,
            ids = files.fastMap { it.id }
        ) {
            val mediaItems = mediaDao.getMediaByIds(files).associateBy { it.id }

            val semaphore = Semaphore(permits = 5)

            val result = files.map { item ->
                async {
                    semaphore.withPermit {
                        val media = mediaItems[item.id]!!

                        var newId = 0L
                        destination.paths.forEach { path ->
                            val newUri = when (val inserted = gateway.insertMedia(media, path)) {
                                is Result.Success -> inserted.data
                                is Result.Error -> return@async null
                            }

                            when (val result = gateway.getContentId(newUri, media.type)) {
                                is Result.Error -> return@async null
                                is Result.Success -> newId = result.data
                            }

                            val downloaded = assetsClient.download(
                                id = Uuid.parse(item.immichId!!),
                                channel = gateway.getWriteChannel(newUri)
                            )

                            if (!downloaded) return@async null

                            gateway.setDateForMedia(
                                uri = newUri,
                                type = media.type,
                                dateTaken = media.dateTaken
                            )
                        }

                        send(element = FileOperationProgress.ItemDone(uri = item.uri))

                        FileOperationCopyResult(
                            id = newId,
                            immichId = item.immichId
                        )
                    }
                }
            }.awaitAll().filterNotNull()

            gateway.enqueueSyncWorker(albumId = destination.id)

            Result.Success(result)
        }

        send(element = FileOperationProgress.Finished(result))
    }

    private fun copyToCloud(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Cloud,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        val result = syncTaskDao.track(
            existingTaskId = existingTaskId,
            type = SyncTaskType.Copy,
            destination = destination.immichId,
            ids = files.fastMap { it.id }
        ) {
            val success = albumsClient.addAssets(
                albumId = Uuid.parse(destination.immichId),
                assetIds = files.fastMap { Uuid.parse(it.immichId!!) }
            )

            if (success) {
                Result.Success(
                    data = files.map { file ->
                        FileOperationCopyResult(
                            id = file.id,
                            immichId = file.immichId
                        )
                    }
                )
            } else {
                Result.Error(FileOperationError.Failed)
            }
        }

        send(element = FileOperationProgress.Finished(result))
    }

    private fun copyToCustom(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Custom,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        var copyResult: Result<List<FileOperationCopyResult>, FileOperationError>? = null

        val tempFolder = AlbumType.Folder(
            id = "",
            name = appCloudFolderDir.name,
            pinned = false,
            immichId = null,
            paths = setOf(appCloudFolderDir.absolutePath)
        )

        download(files, tempFolder, existingTaskId).collect { progress ->
            when (progress) {
                is FileOperationProgress.ItemDone -> send(element = progress)
                is FileOperationProgress.Finished -> copyResult = progress.result
            }
        }

        when (val result = copyResult) {
            is Result.Success -> {
                toCustom.execute(
                    mediaIds = result.data.fastMap { it.id },
                    destination = destination
                ).collect { progress ->
                    // we don't care about ItemDone's because they're instant
                    if (progress is FileOperationProgress.Finished) {
                        send(element = progress)
                    }
                }
            }

            is Result.Error -> send(element = FileOperationProgress.Finished(result))

            null -> send(element = FileOperationProgress.Finished(Result.Error(FileOperationError.Failed)))
        }
    }
}