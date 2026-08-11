package com.kaii.photos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.di.HybridFileManagerFactory
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.impl.CustomFileManager
import com.kaii.photos.file_management.managers.impl.HybridFileManager
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Secure
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CustomRepository(
    private val album: AlbumType,
    private val fileManager: HybridFileManager,
    private val customDao: CustomEntityDao,
    dateFormatter: LocalizedDateFormatter,
    scope: CoroutineScope,
    sortMode: Flow<MediaItemSortMode>,
    info: Flow<ImmichBasicInfo>
) : BaseRepo, RenameFile, RenameAlbum, CountAndSize, Secure {
    class Factory @Inject constructor(
        private val customDao: CustomEntityDao,
        private val fileManagerFactory: HybridFileManagerFactory,
        private val customFileManager: CustomFileManager,
        private val dateFormatter: LocalizedDateFormatter,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl
    ) {
        fun create(
            scope: CoroutineScope,
            album: AlbumType.Custom
        ): CustomRepository =
            CustomRepository(
                album = album,
                fileManager = fileManagerFactory.create(customFileManager),
                customDao = customDao,
                scope = scope,
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
            dateFormatter = dateFormatter
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

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ) = fileManager.renameFile(file, newName)

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.encryptFiles(files)

    override fun prepareEncryptFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.prepareEncryptFiles(files)

    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ) = fileManager.getExifData(file)

    override suspend fun getMediaCount(album: AlbumType) = fileManager.getMediaCount(album)

    override suspend fun getMediaSize(album: AlbumType) = fileManager.getMediaSize(album)

    suspend fun removeRelatedRows(albumId: String) = withContext(Dispatchers.Default) {
        customDao.deleteAlbum(album = albumId)
    }
}