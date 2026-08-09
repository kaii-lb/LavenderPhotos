package com.kaii.photos.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.di.HybridFileManagerFactory
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.impl.HybridFileManager
import com.kaii.photos.file_management.managers.impl.LocalSourceFileManager
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Secure
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class HybridRepository(
    private val mediaDao: MediaDao,
    private val fileManager: HybridFileManager,
    scope: CoroutineScope,
    initialAlbum: AlbumType.Folder,
    dateFormatter: LocalizedDateFormatter,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>
) : BaseRepo, RenameFile, RenameAlbum, Secure, CountAndSize {
    class Factory @Inject constructor(
        private val mediaDao: MediaDao,
        private val fileManagerFactory: HybridFileManagerFactory,
        private val dateFormatter: LocalizedDateFormatter,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl
    ) {
        fun create(
            scope: CoroutineScope,
            album: AlbumType.Folder,
            other: LocalSourceFileManager
        ): HybridRepository =
            HybridRepository(
                mediaDao = mediaDao,
                fileManager = fileManagerFactory.create(other),
                scope = scope,
                initialAlbum = album,
                dateFormatter = dateFormatter,
                info = immich.getImmichBasicInfo(),
                sortMode = photoGrid.getSortMode()
            )
    }

    private data class Params(
        val paths: Set<String>,
        override val sortMode: MediaItemSortMode,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, info)

    private val album = MutableStateFlow(initialAlbum)
    private val params = combine(info, sortMode, album) { info, sortMode, album ->
        Params(
            paths = album.paths,
            sortMode = sortMode,
            info = info
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val mediaFlow = params.flatMapLatest { details ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 50,
                enablePlaceholders = true,
                initialLoadSize = 100
            ),
            pagingSourceFactory = {
                if (details.sortMode.isDateModified) mediaDao.getPagedMediaDateModified(paths = details.paths)
                else mediaDao.getPagedMediaDateTaken(paths = details.paths)
            }
        ).flow.mapToMedia(
            auth = details.info.auth,
            endpoint = details.info.endpoint
        )
    }.cachedIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val gridMediaFlow = params.flatMapLatest { details ->
        mediaFlow.mapToSeparatedMedia(
            sortMode = details.sortMode,
            dateFormatter = dateFormatter
        )
    }.cachedIn(scope)

    fun changeAlbum(album: AlbumType.Folder) {
        this.album.value = album
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

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ) = fileManager.renameFile(file, newName)

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String
    ) = fileManager.renameAlbum(album, newName)

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.encryptFiles(files)

    override fun prepareEncryptFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.prepareEncryptFiles(files)

    override suspend fun getMediaCount(album: AlbumType) = fileManager.getMediaCount(album)

    override suspend fun getMediaSize(album: AlbumType) = fileManager.getMediaSize(album)
}