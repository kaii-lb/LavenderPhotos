package com.kaii.photos.file_management.managers.gateways

import android.app.RecoverableSecurityException
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.getExifDataForMedia
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.mediastore.toContentId
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kaii_lb.lavender.immichintegration.AssetSource
import io.github.kaii_lb.lavender.immichintegration.UriAssetSource
import io.github.kaii_lb.lavender.immichintegration.UriWriteChannel
import java.io.File
import javax.inject.Inject

interface AndroidMediaStoreGateway : MediaStoreGateway, CloudCacheGateway, SyncWorkerGateway

class AndroidMediaStoreGatewayImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AndroidMediaStoreGateway {
    private val contentResolver = context.contentResolver

    override fun insertMedia(
        media: MediaStoreData,
        destination: String
    ): Result<Uri, FileOperationError> {
        val newUri = contentResolver.insertMedia(
            context = context,
            media = media,
            destination = destination
        ) ?: return Result.Error(FileOperationError.Failed)

        return Result.Success(newUri)
    }

    override fun setDateForMedia(
        uri: Uri,
        dateTaken: Long,
        type: MediaType
    ): Result<Unit, FileOperationError> =
        contentResolver.setDateForMedia(
            uri = uri,
            type = type,
            dateTaken = dateTaken
        ).let {
            if (it) Result.Success(Unit)
            else Result.Error(FileOperationError.Failed)
        }

    override fun getContentId(
        uri: Uri,
        type: MediaType
    ): Result<Long, FileOperationError> =
        uri.toContentId(contentResolver = contentResolver, type = type).let {
            if (it == null) Result.Error(FileOperationError.Failed)
            else Result.Success(it)
        }

    override fun copy(
        media: MediaStoreData,
        destination: String
    ): Result<FileOperationCopyResult, FileOperationError> {
        val newUri = when (val result = insertMedia(media, destination)) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> result.data
        }

        contentResolver.copyUriToUri(media.uri.toUri(), newUri)

        val newId = newUri.toContentId(contentResolver = contentResolver, type = media.type)

        return if (newId != null) {
            Result.Success(
                data = FileOperationCopyResult(
                    id = newId,
                    immichId = media.immichId
                )
            )
        } else {
            Result.Error(FileOperationError.Failed)
        }
    }

    override fun trash(
        files: List<FileOperationItemMetadata>,
        isTrashed: Boolean
    ): Result<Unit, FileOperationError> =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                MediaStore.canManageMedia(context)
            ) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_TRASHED, isTrashed)
                }

                files.chunked(500) { chunk ->
                    val operations = ArrayList<ContentProviderOperation>()
                    chunk.forEach { item ->
                        val operation = ContentProviderOperation.newUpdate(item.uri.toUri())
                            .withValues(contentValues)
                            .build()

                        operations.add(operation)
                    }

                    contentResolver.applyBatch(MediaStore.AUTHORITY, operations)
                }

                Result.Success(Unit)
            } else {
                val mediaUris = files.fastMap { it.uri.toUri() }
                val pendingIntent = MediaStore.createTrashRequest(contentResolver, mediaUris, isTrashed)

                Result.Error(
                    FileOperationError.MediaStoreRequest(
                        intentSender = pendingIntent.intentSender
                    )
                )
            }
        } catch (securityException: RecoverableSecurityException) {
            Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Setting trashed $isTrashed on photo list failed. ${securityException.message}")

            Result.Error(
                FileOperationError.RecoverableException(
                    intentSender = securityException.userAction.actionIntent.intentSender,
                    action = FileOperationAction.Trash(
                        files = files,
                        isTrashed = isTrashed,
                        album = AlbumType.PlaceHolder
                    )
                )
            )
        } catch (e: Throwable) {
            Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Setting trashed $isTrashed on photo list failed. ${e.message}")
            e.printStackTrace()

            Result.Error(FileOperationError.Failed)
        }

    override fun renameFile(
        file: FileOperationItemMetadata,
        newName: String
    ): Result<Unit, FileOperationError> = try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }

        contentResolver.update(file.uri.toUri(), contentValues, null)
        contentResolver.notifyChange(file.uri.toUri(), null)
        Result.Success(Unit)
    } catch (securityException: RecoverableSecurityException) {
        Result.Error(
            error = FileOperationError.RecoverableException(
                intentSender = securityException.userAction.actionIntent.intentSender,
                action = FileOperationAction.RenameFile(
                    file = file,
                    newName = newName
                )
            )
        )
    } catch (e: Throwable) {
        Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Failed to rename file! ${e.message}")
        e.printStackTrace()
        Result.Error(FileOperationError.Failed)
    }

    override fun favourite(
        files: List<FileOperationItemMetadata>,
        isFavourite: Boolean
    ): Result<Unit, FileOperationError> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            MediaStore.canManageMedia(context)
        ) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_FAVORITE, isFavourite)
            }

            files.chunked(500) { chunk ->
                val operations = ArrayList<ContentProviderOperation>()
                chunk.forEach { item ->
                    val operation = ContentProviderOperation.newUpdate(item.uri.toUri())
                        .withValues(contentValues)
                        .build()

                    operations.add(operation)
                }
            }

            Result.Success(Unit)
        } else {
            val mediaUris = files.fastMap { it.uri.toUri() }
            val pendingIntent = MediaStore.createFavoriteRequest(contentResolver, mediaUris, isFavourite)

            Result.Error(
                FileOperationError.MediaStoreRequest(
                    intentSender = pendingIntent.intentSender
                )
            )
        }
    } catch (securityException: RecoverableSecurityException) {
        Result.Error(
            FileOperationError.RecoverableException(
                intentSender = securityException.userAction.actionIntent.intentSender,
                action = FileOperationAction.Favourite(
                    files = files,
                    isFavourite = isFavourite,
                    album = AlbumType.PlaceHolder
                )
            )
        )
    } catch (e: Throwable) {
        Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Setting favourite $isFavourite on photo list failed. ${e.message}")
        e.printStackTrace()

        Result.Error(FileOperationError.Failed)
    }

    override fun share(
        files: List<FileOperationItemMetadata>
    ): Result<Intent, FileOperationError> {
        if (files.size == 1) {
            val item = files.first()

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = if (item.isImage) "image/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, item.uri.toUri())
            }

            return Result.Success(shareIntent)
        } else {
            val hasVideos = files.any { !it.isImage }
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = if (hasVideos) "video/*" else "image/*"
            }

            val fileUris = ArrayList<Uri>()
            files.forEach { file ->
                fileUris.add(file.uri.toUri())
            }

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

            return Result.Success(shareIntent)
        }
    }

    override fun getExifData(
        media: MediaStoreData
    ): Result<Map<MediaData, String>, FileOperationError> {
        val map = getExifDataForMedia(
            inputStream =
                context.contentResolver.openInputStream(media.uri.toUri())
                    ?: File(media.absolutePath).inputStream(),
            absolutePath = media.absolutePath,
            is24Hr = is24HrFormat(),
            fallback = media.dateTaken
        )

        return if (map.isEmpty()) Result.Error(FileOperationError.Failed)
        else Result.Success(data = map)
    }

    override fun delete(
        files: List<FileOperationItemMetadata>
    ): Result<Unit, FileOperationError> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            MediaStore.canManageMedia(context)
        ) {
            files.chunked(500) { chunk ->
                val operations = ArrayList<ContentProviderOperation>()
                chunk.forEach { item ->
                    operations.add(
                        ContentProviderOperation.newDelete(item.uri.toUri()).build()
                    )
                }

                contentResolver.applyBatch(MediaStore.AUTHORITY, operations)
            }

            Result.Success(Unit)
        } else {
            val mediaUris = files.fastMap { it.uri.toUri() }
            val pendingIntent = MediaStore.createDeleteRequest(contentResolver, mediaUris)

            Result.Error(
                FileOperationError.MediaStoreRequest(
                    intentSender = pendingIntent.intentSender
                )
            )
        }
    } catch (securityException: RecoverableSecurityException) {
        Result.Error(
            FileOperationError.RecoverableException(
                intentSender = securityException.userAction.actionIntent.intentSender,
                action = FileOperationAction.Delete(
                    files = files,
                    album = AlbumType.PlaceHolder
                )
            )
        )
    } catch (e: Throwable) {
        Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Deleting photo list failed. ${e.message}")
        e.printStackTrace()

        Result.Error(FileOperationError.Failed)
    }

    override fun cacheFile(fileName: String): File = File(context.cacheDir, fileName)

    override fun shareableUri(file: File): Uri = FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, file)

    override fun enqueueSyncWorker(albumId: String) {
        CloudSyncWorker.immediateEnqueue(context, albumId)
    }

    override fun getWriteChannel(uri: Uri): UriWriteChannel = UriWriteChannel(uri, context)
    override fun getAssetSource(uri: Uri): AssetSource = UriAssetSource(context, uri)

    override fun is24HrFormat(): Boolean = DateFormat.is24HourFormat(context)
}