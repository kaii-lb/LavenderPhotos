package com.kaii.photos.file_management.managers.operations

import androidx.compose.ui.util.fastMap
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.database.entities.SyncOperation
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.database.sync.SyncTaskRecorder
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.AndroidMediaStoreGateway
import com.kaii.photos.helpers.appCloudFolderDir
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

class CloudSourceCopyOperation @Inject constructor(
    private val mediaDao: MediaDao,
    private val customDao: CustomEntityDao,
    private val gateway: AndroidMediaStoreGateway,
    private val assetsClient: AssetsClient,
    private val albumsClient: AlbumsClient,
    private val toCustom: LocalToCustomOperation,
    private val recorder: SyncTaskRecorder
) {
    fun copyItems(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> =
        when (destination) {
            is AlbumType.Folder -> copyToLocal(files, destination)

            is AlbumType.Custom -> copyToCustom(files, destination)

            is AlbumType.Cloud -> copyToCloud(files, destination)

            else -> throw IllegalArgumentException("Cannot copy files to a placeholder album!")
        }

    // TODO: add a task for this to download to local later but show result immediately
    private fun copyToLocal(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Folder
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        send(FileOperationProgress.Started(action = FileOperationAction.LongOperationType.Copy, fileCount = files.size))

        val mediaItems = mediaDao.getMediaFromMetadata(files).associateBy { it.id }
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

                        when (val contentId = gateway.getContentId(newUri, media.type)) {
                            is Result.Error -> return@async null
                            is Result.Success -> newId = contentId.data
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

                    send(FileOperationProgress.ItemDone(uri = item.uri))
                    FileOperationCopyResult(id = newId, immichId = item.immichId)
                }
            }
        }.awaitAll().filterNotNull()

        gateway.enqueueSyncWorker(albumId = destination.id)

        send(
            element = FileOperationProgress.Finished(
                result =
                    if (result.isNotEmpty() || files.isEmpty()) Result.Success(result)
                    else Result.Error(FileOperationError.Failed
                    )
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun copyToCloud(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Cloud
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        send(FileOperationProgress.Started(action = FileOperationAction.LongOperationType.Copy, fileCount = files.size))

        if (files.isEmpty()) {
            send(FileOperationProgress.Finished(Result.Success(emptyList())))
            return@channelFlow
        }

        val media = mediaDao.getMediaFromMetadata(files)

        val results = recorder.record(
            operation = SyncOperation.AddToAlbum(
                destinationAlbumId = destination.immichId
            ),
            mediaIds = media.map { it.id },
            immichIds = files.associate { it.id to it.immichId },
            applyLocally = {
                customDao.upsertAll(items = media.map { CustomItem(id = it.id, album = destination.id) })
                files.map { FileOperationCopyResult(id = it.id, immichId = it.immichId) }
            },
            attemptRemote = {
                val targets = files.mapNotNull { it.immichId }

                if (targets.isEmpty()) {
                    Result.Success(Unit)
                } else {
                    val success = albumsClient.addAssets(
                        albumId = Uuid.parse(destination.immichId),
                        assetIds = targets.map { Uuid.parse(it) }
                    )

                    if (success) Result.Success(Unit)
                    else Result.Error(FileOperationError.Failed)
                }
            }
        )

        files.forEach { send(FileOperationProgress.ItemDone(uri = it.uri)) }
        send(FileOperationProgress.Finished(Result.Success(results)))
    }.flowOn(Dispatchers.IO)

    private fun copyToCustom(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType.Custom
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        if (files.isEmpty()) return@channelFlow

        var copyResult: Result<List<FileOperationCopyResult>, FileOperationError>? = null

        val tempFolder = AlbumType.Folder(
            id = "",
            name = appCloudFolderDir.name,
            pinned = false,
            immichId = null,
            paths = setOf(appCloudFolderDir.absolutePath)
        )

        copyToLocal(files, tempFolder).collect { progress ->
            when (progress) {
                is FileOperationProgress.Started -> send(element = progress)

                is FileOperationProgress.ItemDone -> send(element = progress)

                is FileOperationProgress.Finished -> copyResult = progress.result
            }
        }

        when (val result = copyResult) {
            is Result.Success -> {
                if (result.data.isEmpty()) {
                    send(element = FileOperationProgress.Finished(Result.Error(FileOperationError.Failed)))
                    return@channelFlow
                }

                while (mediaDao.getMediaFromId(id = result.data.first().id) == null) {
                    delay(100.milliseconds)
                }

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
    }.flowOn(Dispatchers.IO)
}