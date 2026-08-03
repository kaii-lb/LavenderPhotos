package com.kaii.photos.file_management.managers.impl

import android.content.Intent
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
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
import com.kaii.photos.domain.mapTo
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.managers.operations.CloudDeleteOperation
import com.kaii.photos.file_management.managers.operations.CloudFavouriteOperation
import com.kaii.photos.file_management.managers.operations.CloudResolveShareableItemOperation
import com.kaii.photos.file_management.managers.operations.CloudSourceCopyOperation
import com.kaii.photos.file_management.managers.operations.CloudTrashOperation
import com.kaii.photos.file_management.managers.operations.RenameAlbumOperation
import com.kaii.photos.file_management.managers.traits.Copy
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Favourite
import com.kaii.photos.file_management.managers.traits.Move
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.managers.traits.Trash
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.exifDataToMediaData
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.albums.AlbumUpdateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Instant
import kotlin.uuid.Uuid

class CloudFileManager @Inject constructor(
    private val mediaDao: MediaDao,
    private val customDao: CustomEntityDao,
    private val albumsClient: AlbumsClient,
    private val gateway: MediaStoreGateway,
    private val copyOperation: CloudSourceCopyOperation,
    private val renameAlbumOperation: RenameAlbumOperation,
    private val cloudResolveShareable: CloudResolveShareableItemOperation,
    private val trash: CloudTrashOperation,
    private val delete: CloudDeleteOperation,
    private val favourite: CloudFavouriteOperation,
    private val recorder: SyncTaskRecorder
) : Copy, Move, Trash, Delete, Favourite, Share, RenameAlbum, ExtractExif, CountAndSize {
    override suspend fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = copyOperation.copyItems(files, destination)

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?
    ): Flow<FileOperationProgress<Unit>> = trash.execute(files, isTrashed, albumId, immichId)

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?
    ): Flow<FileOperationProgress<Unit>> = delete.execute(files, immichId ?: albumId)

    override suspend fun favouriteFile(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        albumId: String?,
        immichId: String?
    ): Result<Unit, FileOperationError> = favourite.execute(files, isFavourite)

    override suspend fun moveFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        origin: AlbumType?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        requireNotNull(origin) {
            "${CloudFileManager::class.simpleName} cannot move without an origin"
        }

        var copyResult: Result<List<FileOperationCopyResult>, FileOperationError>? = null

        copyFiles(files = files, destination = destination).collect { progress ->
            when (progress) {
                is FileOperationProgress.Started -> send(
                    element = FileOperationProgress.Started(
                        action = FileOperationAction.LongOperationType.Move,
                        fileCount = files.size
                    )
                )

                is FileOperationProgress.ItemDone -> send(progress)

                is FileOperationProgress.Finished -> copyResult = progress.result
            }
        }

        val copied = copyResult
        if (copied !is Result.Success) {
            send(
                element = FileOperationProgress.Finished(
                    result = copied ?: Result.Error(
                        FileOperationError.Failed
                    )
                )
            )

            return@channelFlow
        }

        var finalResult: Result<List<FileOperationCopyResult>, FileOperationError>? = null

        trashFiles(
            files = files,
            isTrashed = true,
            albumId = origin.id,
            immichId = origin.immichId
        ).collect { progress ->
            if (progress is FileOperationProgress.Finished) {
                finalResult = progress.result.mapTo(to = copied)
            }
        }

        send(
            element = FileOperationProgress.Finished(
                result = finalResult ?: Result.Error(FileOperationError.Failed)
            )
        )
    }

    fun getRawShareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<List<FileOperationItemMetadata>>> = channelFlow {
        send(
            element = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Share,
                fileCount = files.size
            )
        )

        if (files.isEmpty()) {
            send(
                element = FileOperationProgress.Finished(
                    result = Result.Success(files)
                )
            )

            return@channelFlow
        }

        val names = mediaDao.getMediaFromMetadata(files).associate { it.id to it.displayName }

        val semaphore = Semaphore(permits = 5)

        val cached = files.map { item ->
            async {
                semaphore.withPermit {
                    val result = cloudResolveShareable.execute(
                        item = item,
                        fileName = names[item.id]!!
                    )

                    if (result != null) {
                        send(
                            element = FileOperationProgress.ItemDone(
                                uri = result.uri
                            )
                        )
                    }

                    result
                }
            }
        }.awaitAll()

        send(
            element = FileOperationProgress.Finished(
                result = Result.Success(
                    data = cached.filterNotNull()
                )
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Intent>> = getRawShareFiles(files).map { progress ->
        when (progress) {
            is FileOperationProgress.Started -> FileOperationProgress.Started(progress.action, progress.fileCount)
            is FileOperationProgress.ItemDone -> FileOperationProgress.ItemDone(progress.uri)
            is FileOperationProgress.Finished -> FileOperationProgress.Finished(
                result = progress.result.mapTo {
                    gateway.share(files = it.data)
                }
            )
        }
    }

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String
    ): Result<Unit, FileOperationError> = withContext(Dispatchers.IO) {
        val immichId = album.immichId

        recorder.record(
            operation = SyncOperation.RenameAlbum(
                albumLocalId = album.id,
                newName = newName
            ),
            mediaIds = emptyList(),
            applyLocally = {
                renameAlbumOperation.execute(album, newName)
                Result.Success(Unit)
            },
            attemptRemote = {
                if (immichId == null) {
                    Result.Success(Unit)
                } else {
                    val success = albumsClient.update(
                        id = Uuid.parse(immichId),
                        info = AlbumUpdateDto(
                            albumName = newName,
                            albumThumbnailAssetId = null,
                            description = null,
                            isActivityEnabled = null,
                            order = null
                        )
                    )

                    if (success) Result.Success(Unit)
                    else Result.Error(FileOperationError.Failed)
                }
            }
        )
    }

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> = withContext(Dispatchers.IO) {
        val is24Hr = gateway.is24HrFormat()

        val media =
            mediaDao.getMedia(ids = listOf(file.id)).firstOrNull() ?: return@withContext Result.Error(FileOperationError.Failed)

        val exifData = customDao.getExifData(id = file.id)?.let { exifData ->
            exifDataToMediaData(
                name = media.displayName,
                path = file.uri,
                info = exifData,
                is24Hr = is24Hr,
                fallback = media.dateTaken
            )
        } ?: run {
            val formattedDateTime =
                Instant.fromEpochSeconds(media.dateTaken)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .toJavaLocalDateTime()
                    .format(
                        DateTimeFormatter.ofPattern(
                            if (is24Hr) "EEE dd MMM yyyy - HH:mm:ss"
                            else "EEE dd MMM yyyy - h:mm:ss a"
                        )
                    )

            mapOf(
                MediaData.Name to media.displayName,
                MediaData.Path to media.uri,
                MediaData.Size to media.size.toString(),
                MediaData.Date to formattedDateTime
            )
        }

        Result.Success(data = exifData)
    }

    override suspend fun getMediaCount(
        album: AlbumType
    ): Int = withContext(Dispatchers.IO) {
        customDao.countMediaInAlbum(
            album = (album as AlbumType.Cloud).id
        )
    }

    override suspend fun getMediaSize(
        album: AlbumType
    ): Long = withContext(Dispatchers.IO) {
        customDao.mediaSize(
            album = (album as AlbumType.Cloud).id
        )
    }
}