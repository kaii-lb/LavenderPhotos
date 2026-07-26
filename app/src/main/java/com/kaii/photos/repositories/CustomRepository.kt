package com.kaii.photos.repositories

import android.content.Intent
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.impl.CustomFileManager
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class CustomRepository(
    private val album: AlbumType,
    private val fileManager: CustomFileManager,
    private val customDao: CustomEntityDao,
    scope: CoroutineScope,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>
) : BaseRepo, RenameFile, RenameAlbum, CountAndSize {
    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        RoomQueryParams(
            sortMode = sortMode,
            format = format,
            info = info
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                if (params.sortMode.isDateModified) customDao.getPagedMediaDateModified(album = album.id)
                else customDao.getPagedMediaDateTaken(album = album.id)
            }
        ).flow.mapToMedia(
            auth = params.info.auth,
            endpoint = params.info.endpoint
        )
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val gridMediaFlow = params.flatMapLatest { params ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = params.sortMode,
            format = params.format
        )
    }.cachedIn(scope)

    suspend fun remove(
        items: Set<MediaStoreData>,
        albumId: String
    ) {
        customDao.deleteAll(ids = items.map { it.id }.toSet(), album = albumId)
    }

    override suspend fun copyFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = fileManager.copyFiles(files, destination, existingTaskId)

    override suspend fun moveFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        existingTaskId: Int?,
        origin: AlbumType?
    ): Flow<FileOperationProgress<List<FileOperationCopyResult>>> = fileManager.moveFiles(files, destination, existingTaskId, origin)

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.renameAlbum(album, newName, existingTaskId)

    override suspend fun trashFile(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.trashFile(files, isTrashed, albumId, immichId, existingTaskId)

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.deleteFiles(files, albumId, existingTaskId)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError> = fileManager.shareFiles(files)

    override suspend fun favouriteFile(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        albumId: String?,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.favouriteFile(files, isFavourite, albumId, immichId, existingTaskId)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, Any>, FileOperationError> = fileManager.getExifData(file)

    override suspend fun getMediaCount(album: AlbumType): Int = fileManager.getMediaCount(album)

    override suspend fun getMediaSize(album: AlbumType): Long = fileManager.getMediaSize(album)

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError> = fileManager.renameFile(file, newName)
}