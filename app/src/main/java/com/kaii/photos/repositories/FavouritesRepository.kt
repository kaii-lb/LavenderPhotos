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
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.impl.HybridFileManager
import com.kaii.photos.file_management.managers.impl.LocalFileManager
import com.kaii.photos.file_management.managers.traits.ClearExif
import com.kaii.photos.file_management.managers.traits.PrepareFileForWrite
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Secure
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
import com.kaii.photos.presentation.ui.LocalizedDateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesRepository(
    private val mediaDao: MediaDao,
    private val fileManager: HybridFileManager,
    dateFormatter: LocalizedDateFormatter,
    scope: CoroutineScope,
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>
) : BaseRepo, RenameFile, Secure, PrepareFileForWrite, ClearExif {
    class Factory @Inject constructor(
        private val mediaDao: MediaDao,
        private val fileManagerFactory: HybridFileManagerFactory,
        private val localFileManager: LocalFileManager,
        private val dateFormatter: LocalizedDateFormatter,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl
    ) {
        fun create(
            scope: CoroutineScope
        ): FavouritesRepository =
            FavouritesRepository(
                mediaDao = mediaDao,
                fileManager = fileManagerFactory.create(localFileManager),
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
                if (params.sortMode.isDateModified) mediaDao.getPagedFavouritesDateModified()
                else mediaDao.getPagedFavouritesDateTaken()
            }
        ).flow
            .mapToMedia(
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

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ) = fileManager.encryptFiles(files)

    override fun prepareFileForWrite(
        files: List<FileOperationItemMetadata>,
        followUpAction: FileOperationAction
    ) = fileManager.prepareFileForWrite(files, followUpAction)

    override suspend fun clearExifData(
        absolutePath: String
    ) = fileManager.clearExifData(absolutePath)
}