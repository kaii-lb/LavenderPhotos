package com.kaii.photos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.data.immich.RefreshCloudAlbumOperation
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.impl.CloudFileManager
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ImmichRepository(
    private val album: AlbumType.Cloud,
    private val scope: CoroutineScope,
    private val fileManager: CloudFileManager,
    private val refreshOperation: RefreshCloudAlbumOperation,
    private val customDao: CustomEntityDao,
    dateFormatter: LocalizedDateFormatter,
    sortMode: Flow<MediaItemSortMode>,
    info: Flow<ImmichBasicInfo>
) : BaseRepo, RenameAlbum, CountAndSize {
    class Factory @Inject constructor(
        private val customDao: CustomEntityDao,
        private val refreshOperation: RefreshCloudAlbumOperation,
        private val fileManager: CloudFileManager,
        private val dateFormatter: LocalizedDateFormatter,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl
    ) {
        fun create(
            scope: CoroutineScope,
            album: AlbumType.Cloud
        ): ImmichRepository =
            ImmichRepository(
                album = album,
                scope = scope,
                fileManager = fileManager,
                refreshOperation = refreshOperation,
                customDao = customDao,
                dateFormatter = dateFormatter,
                info = immich.getImmichBasicInfo(),
                sortMode = photoGrid.getSortMode()
            )
    }

    private val params = combine(info, sortMode) { info, sortMode ->
        RoomQueryParams(
            sortMode = sortMode,
            info = info
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = RoomQueryParams(
            sortMode = MediaItemSortMode.DateTaken,
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
            dateFormatter = dateFormatter
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
        destination: AlbumType
    ) = fileManager.copyFiles(files, destination)

    override suspend fun moveFiles(
        files: List<FileOperationItemMetadata>,
        destination: AlbumType,
        origin: AlbumType?
    ) = fileManager.moveFiles(files, destination, origin)

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String
    ) = fileManager.renameAlbum(album, newName)

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?
    ) = fileManager.trashFiles(files, isTrashed, albumId, immichId)

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?
    ) = fileManager.deleteFiles(files, albumId, immichId)

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.shareFiles(files)

    override suspend fun favouriteFile(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean,
        albumId: String?,
        immichId: String?
    ) = fileManager.favouriteFile(files, isFavourite, albumId, immichId)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ) = fileManager.getExifData(file)

    override suspend fun getMediaCount(album: AlbumType) = fileManager.getMediaCount(album)

    override suspend fun getMediaSize(album: AlbumType) = fileManager.getMediaSize(album)
}