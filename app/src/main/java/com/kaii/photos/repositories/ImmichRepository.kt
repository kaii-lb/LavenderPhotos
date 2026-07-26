package com.kaii.photos.repositories

import android.content.Intent
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.data.immich.RefreshCloudAlbumOperation
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.impl.CloudFileManager
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ImmichRepository(
    private val album: AlbumType.Cloud,
    private val scope: CoroutineScope,
    private val fileManager: CloudFileManager,
    private val refreshOperation: RefreshCloudAlbumOperation,
    private val customDao: CustomEntityDao,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>,
    info: Flow<ImmichBasicInfo>
) : BaseRepo, RenameAlbum, CountAndSize {
    private val params = combine(info, sortMode, format) { info, sortMode, format ->
        RoomQueryParams(
            sortMode = sortMode,
            format = format,
            info = info
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = RoomQueryParams(
            sortMode = MediaItemSortMode.DateTaken,
            format = DisplayDateFormat.Default,
            info = ImmichBasicInfo.Empty
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val mediaFlow = params.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 80,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 80
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


    suspend fun refresh() = refreshOperation.execute(
        albumId = album.id,
        immichId = album.immichId
    )

    init {
        scope.launch {
            info.distinctUntilChanged().collectLatest {
                refresh()
            }
        }
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
}