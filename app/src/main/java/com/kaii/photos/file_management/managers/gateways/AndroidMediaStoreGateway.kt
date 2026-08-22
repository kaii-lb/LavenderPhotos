package com.kaii.photos.file_management.managers.gateways

import android.app.PendingIntent
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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.sync.CloudSyncWorker
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.BatchModificationResult
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationCopyResult
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.getExifDataForMedia
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getMediaFromTrashId
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.mediastore.toContentId
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kaii_lb.lavender.immichintegration.AssetSource
import io.github.kaii_lb.lavender.immichintegration.UriAssetSource
import io.github.kaii_lb.lavender.immichintegration.UriWriteChannel
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import kotlin.reflect.KClass

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

        setDateForMedia(
            uri = newUri,
            dateTaken = media.dateTaken,
            type = media.type
        )

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
    ): Result<Unit, FileOperationError> = try {
        if (files.isEmpty()) return Result.Success(Unit)

        val result =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(context)) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_TRASHED, isTrashed)
                }

                applyBatchedModification(
                    files = files,
                    operation = { uri ->
                        ContentProviderOperation.newUpdate(uri).withValues(contentValues).build()
                    },
                    applyIndividual = { uri ->
                        contentResolver.update(uri, contentValues, null)
                    }
                )
            } else {
                BatchModificationResult(
                    needsPermission = files,
                    failed = emptyList()
                )
            }

        resolveModificationResult(
            result = result,
            operationName = FileOperationAction.Trash::class,
            buildRequest = { uris ->
                MediaStore.createTrashRequest(contentResolver, uris, isTrashed)
            },
            buildAction = { files ->
                FileOperationAction.Trash(
                    files = files,
                    isTrashed = isTrashed,
                    album = AlbumType.PlaceHolder
                )
            }
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
            error = FileOperationError.RecoverableException.RequiresConsentOnly(
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
        if (files.isEmpty()) return Result.Success(Unit)

        val result =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(context)) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_FAVORITE, isFavourite)
                }

                applyBatchedModification(
                    files = files,
                    operation = { uri ->
                        ContentProviderOperation.newUpdate(uri).withValues(contentValues).build()
                    },
                    applyIndividual = { uri ->
                        contentResolver.update(uri, contentValues, null)
                    }
                )
            } else {
                BatchModificationResult(
                    needsPermission = files,
                    failed = emptyList()
                )
            }

        resolveModificationResult(
            result = result,
            operationName = FileOperationAction.Favourite::class,
            buildRequest = { uris ->
                MediaStore.createFavoriteRequest(contentResolver, uris, isFavourite)
            },
            buildAction = { files ->
                FileOperationAction.Favourite(
                    files = files,
                    isFavourite = isFavourite,
                    album = AlbumType.PlaceHolder
                )
            }
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

    override fun getTrashMediaById(
        id: Long
    ): Result<MediaStoreData, FileOperationError> {
        val result = getMediaFromTrashId(
            id = id,
            contentResolver = context.contentResolver
        )

        return if (result == null) Result.Error(FileOperationError.Failed)
        else Result.Success(data = result)
    }

    override fun delete(
        files: List<FileOperationItemMetadata>
    ): Result<Unit, FileOperationError> = try {
        if (files.isEmpty()) return Result.Success(Unit)

        val result =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(context)) {
                applyBatchedModification(
                    files = files,
                    operation = { uri -> ContentProviderOperation.newDelete(uri).build() },
                    applyIndividual = { uri ->
                        contentResolver.delete(uri, null, null)
                    }
                )
            } else {
                BatchModificationResult(
                    needsPermission = files,
                    failed = emptyList()
                )
            }

        resolveModificationResult(
            result = result,
            operationName = FileOperationAction.Delete::class,
            buildRequest = { uris ->
                MediaStore.createDeleteRequest(contentResolver, uris)
            },
            buildAction = { files ->
                FileOperationAction.Delete(
                    files = files,
                    album = AlbumType.PlaceHolder
                )
            }
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

    override fun createWriteRequest(files: List<FileOperationItemMetadata>): PendingIntent =
        MediaStore.createWriteRequest(
            contentResolver,
            files.map { it.uri.toUri() }
        )

    override fun openOutputStream(uri: Uri): OutputStream? = contentResolver.openOutputStream(uri)

    /** tries applying [operation] to a batch of [files] (chunked by 500 for the mediastore limit)
     * and if that fails applies per-item using [applyIndividual] */
    private fun applyBatchedModification(
        files: List<FileOperationItemMetadata>,
        operation: (Uri) -> ContentProviderOperation,
        applyIndividual: (Uri) -> Unit
    ): BatchModificationResult {
        val needsPermission = mutableListOf<FileOperationItemMetadata>()
        val failed = mutableListOf<FileOperationItemMetadata>()

        files.chunked(500).forEach { chunk ->
            val chunkApplied = try {
                val operations = ArrayList<ContentProviderOperation>(chunk.size)
                chunk.forEach { item ->
                    operations.add(operation(item.uri.toUri()))
                }

                contentResolver.applyBatch(MediaStore.AUTHORITY, operations)

                true
            } catch (e: Throwable) {
                Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Batched update of ${chunk.size} item(s) failed, retrying individually. ${e.message}")

                false
            }

            if (!chunkApplied) {
                chunk.forEach { item ->
                    try {
                        applyIndividual(item.uri.toUri())
                    } catch (_: RecoverableSecurityException) {
                        needsPermission.add(item)
                    } catch (e: Throwable) {
                        Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Updating ${item.uri} individually failed. ${e.message}")
                        e.printStackTrace()

                        failed.add(item)
                    }
                }
            }
        }

        return BatchModificationResult(
            needsPermission = needsPermission,
            failed = failed
        )
    }

    /** creates a `MediaStore.create[Write/Favourite/Delete]Request` from [result]
     * and transforms said [BatchModificationResult] to a [Result]
     * by the emptiness of `result.needsPermission` and `result.failed` */
    private fun resolveModificationResult(
        result: BatchModificationResult,
        operationName: KClass<out FileOperationAction>,
        buildRequest: (List<Uri>) -> PendingIntent,
        buildAction: (List<FileOperationItemMetadata>) -> FileOperationAction
    ): Result<Unit, FileOperationError> = when {
        result.needsPermission.isNotEmpty() -> try {
            val pendingIntent = buildRequest(result.needsPermission.map { it.uri.toUri() })

            Result.Error(
                FileOperationError.RecoverableException.RequiresConsentOnly(
                    intentSender = pendingIntent.intentSender,
                    action = buildAction(result.needsPermission)
                )
            )
        } catch (e: Throwable) {
            Log.e(AndroidMediaStoreGatewayImpl::class.qualifiedName, "Could not build a consent request for ${operationName.simpleName}. ${e.message}")
            e.printStackTrace()

            Result.Error(FileOperationError.Failed)
        }

        result.failed.isNotEmpty() -> Result.Error(FileOperationError.Failed)

        else -> Result.Success(Unit)
    }
}