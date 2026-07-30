package com.kaii.photos.file_management.managers.impl

import android.content.Intent
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.daos.SyncTaskDao
import com.kaii.photos.database.entities.SyncTaskType
import com.kaii.photos.database.getMediaFromMetadata
import com.kaii.photos.database.track
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
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
    private val syncTaskDao: SyncTaskDao,
    private val albumsClient: AlbumsClient,
    private val gateway: MediaStoreGateway,
    private val copyOperation: CloudSourceCopyOperation,
    private val renameAlbum: RenameAlbumOperation,
    private val cloudResolveShareable: CloudResolveShareableItemOperation,
    private val trash: CloudTrashOperation,
    private val delete: CloudDeleteOperation,
    private val favourite: CloudFavouriteOperation
) : Copy, Move, Trash, Delete, Favourite, Share, RenameAlbum, ExtractExif, CountAndSize {
    override suspend fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = copyOperation.copyItems(files, destination, existingTaskId)

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = trash.execute(files, isTrashed, albumId, immichId, existingTaskId)

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = delete.execute(files, immichId ?: albumId, existingTaskId)

    override suspend fun favouriteFile(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        albumId: String?,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = favourite.execute(files, isFavourite, existingTaskId)

    suspend fun getRawShareFiles(
        files: List<FileOperationItemMetadata>
    ): Result<List<FileOperationItemMetadata>, FileOperationError> = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext Result.Success(files)

        val names = mediaDao.getMediaFromMetadata(files).associate { it.id to it.displayName }

        val semaphore = Semaphore(permits = 5)

        val cached = files.map { item ->
            async {
                semaphore.withPermit {
                    cloudResolveShareable.execute(
                        item = item,
                        fileName = names[item.id]!!
                    )
                }
            }
        }.awaitAll()

        Result.Success(data = cached.filterNotNull())
    }

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError> = when (val result = getRawShareFiles(files)) {
        is Result.Error -> Result.Error(result.error)
        is Result.Success -> gateway.share(files = result.data)
    }

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = withContext(Dispatchers.IO) {
        syncTaskDao.track(
            existingTaskId = existingTaskId,
            type = SyncTaskType.RenameAlbum,
            destination = album.id,
            ids = emptyList()
        ) {
            val success = albumsClient.update(
                id = Uuid.parse(album.id),
                info = AlbumUpdateDto(
                    albumName = newName,
                    albumThumbnailAssetId = null,
                    description = null,
                    isActivityEnabled = null,
                    order = null
                )
            )

            if (success) {
                renameAlbum.execute(album, newName)

                Result.Success(Unit)
            } else {
                Result.Error(FileOperationError.Failed)
            }
        }
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

    override suspend fun moveFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?,
        origin: AlbumType?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = channelFlow {
        requireNotNull(origin) { "${CloudFileManager::class.simpleName} cannot move without an origin" }

        var copyResult: Result<List<FileOperationCopyResult>, FileOperationError>? = null
        copyFiles(files, destination, existingTaskId).collect { progress ->
            when (progress) {
                is FileOperationProgress.ItemDone -> send(progress)
                is FileOperationProgress.Finished -> copyResult = progress.result
            }
        }

        val finalResult = when (val copied = copyResult) {
            is Result.Error, null -> copied ?: Result.Error(FileOperationError.Failed)
            is Result.Success -> {
                trashFiles(
                    files = files,
                    isTrashed = true,
                    albumId = origin.id,
                    immichId = origin.immichId,
                    existingTaskId = existingTaskId
                ).mapTo(copied)
            }
        }

        send(
            element = FileOperationProgress.Finished(
                result = finalResult
            )
        )
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