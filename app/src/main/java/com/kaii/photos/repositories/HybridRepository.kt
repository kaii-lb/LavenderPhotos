package com.kaii.photos.repositories

import android.content.Intent
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.preferences.SettingsImmichImpl
import com.kaii.photos.datastore.preferences.SettingsLookAndFeelImpl
import com.kaii.photos.datastore.preferences.SettingsPhotoGridImpl
import com.kaii.photos.di.HybridFileManagerFactory
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.impl.HybridFileManager
import com.kaii.photos.file_management.managers.impl.LocalSourceFileManager
import com.kaii.photos.file_management.managers.traits.CountAndSize
import com.kaii.photos.file_management.managers.traits.RenameAlbum
import com.kaii.photos.file_management.managers.traits.RenameFile
import com.kaii.photos.file_management.managers.traits.Secure
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.paging.mapToMedia
import com.kaii.photos.helpers.paging.mapToSeparatedMedia
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
    info: Flow<ImmichBasicInfo>,
    sortMode: Flow<MediaItemSortMode>,
    format: Flow<DisplayDateFormat>
) : BaseRepo, RenameFile, RenameAlbum, Secure, CountAndSize {
    class Factory @Inject constructor(
        private val mediaDao: MediaDao,
        private val fileManagerFactory: HybridFileManagerFactory,
        private val immich: SettingsImmichImpl,
        private val photoGrid: SettingsPhotoGridImpl,
        private val lookAndFeel: SettingsLookAndFeelImpl
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
                info = immich.getImmichBasicInfo(),
                sortMode = photoGrid.getSortMode(),
                format = lookAndFeel.getDisplayDateFormat()
            )
    }

    private data class Params(
        val paths: Set<String>,
        override val sortMode: MediaItemSortMode,
        override val format: DisplayDateFormat,
        override val info: ImmichBasicInfo
    ) : RoomQueryParams(sortMode, format, info)

    private val album = MutableStateFlow(initialAlbum)
    private val params = combine(info, sortMode, format, album) { info, sortMode, format, album ->
        Params(
            paths = album.paths,
            sortMode = sortMode,
            format = format,
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
            format = details.format
        )
    }.cachedIn(scope)

    fun changeAlbum(album: AlbumType.Folder) {
        this.album.value = album
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

    override suspend fun trashFiles(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.trashFiles(files, isTrashed, albumId, immichId, existingTaskId)

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.deleteFiles(files, albumId, immichId, existingTaskId)

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
    ): Result<Map<MediaData, String>, FileOperationError> = fileManager.getExifData(file)

    override suspend fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError> = fileManager.renameFile(file, newName)

    override suspend fun renameAlbum(
        album: AlbumType,
        newName: String,
        existingTaskId: Int?
    ): Result<Unit, FileOperationError> = fileManager.renameAlbum(album, newName, existingTaskId)

    override suspend fun encryptFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> = fileManager.encryptFiles(files)

    override suspend fun getMediaCount(album: AlbumType): Int = fileManager.getMediaCount(album)

    override suspend fun getMediaSize(album: AlbumType): Long = fileManager.getMediaSize(album)
}