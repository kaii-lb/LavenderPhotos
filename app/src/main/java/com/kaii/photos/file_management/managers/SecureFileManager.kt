package com.kaii.photos.file_management.managers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.kaii.photos.database.daos.SecuredMediaItemEntityDao
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.domain.files.FileOperationProgress
import com.kaii.photos.file_management.managers.gateways.AndroidMediaStoreGateway
import com.kaii.photos.file_management.managers.traits.Delete
import com.kaii.photos.file_management.managers.traits.ExtractExif
import com.kaii.photos.file_management.managers.traits.Restore
import com.kaii.photos.file_management.managers.traits.Share
import com.kaii.photos.file_management.secure.LocalSecureManager
import com.kaii.photos.helpers.EncryptionManager
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.exif.MediaData
import com.kaii.photos.helpers.exif.getExifDataForMedia
import com.kaii.photos.helpers.grid_management.toSecureMedia
import com.kaii.photos.helpers.secureDecryptCacheFile
import com.kaii.photos.helpers.secureDecryptVideoFile
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.getIv
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class SecureFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val secureDao: SecuredMediaItemEntityDao,
    private val gateway: AndroidMediaStoreGateway,
    private val secureManager: LocalSecureManager
) : Delete, Restore, Share, ExtractExif {
    companion object {
        private val TAG = SecureFileManager::class.qualifiedName
    }

    override suspend fun decryptFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Unit>> = channelFlow {
        val media = files.toSecureMedia(context = context)

        send(
            element = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Restore,
                fileCount = files.size
            )
        )

        var operationError: FileOperationError? = null

        media.forEach {
            when (val result = secureManager.restore(media = it)) {
                is Result.Error -> {
                    if (operationError == null || operationError == FileOperationError.Failed) {
                        operationError = result.error
                    }

                    Log.e(TAG, "Restoring ${it.item.uri} failed: ${result.error}")
                }

                is Result.Success -> Unit
            }

            send(
                element = FileOperationProgress.ItemDone(
                    uri = it.item.uri
                )
            )
        }

        send(
            element = FileOperationProgress.Finished(
                result =
                    if (operationError == null) Result.Success(Unit)
                    else Result.Error(operationError)
            )
        )
    }

    override suspend fun shareFiles(
        files: List<FileOperationItemMetadata>
    ): Flow<FileOperationProgress<Intent>> = flow {
        emit(
            value = FileOperationProgress.Started(
                action = FileOperationAction.LongOperationType.Share,
                fileCount = files.size
            )
        )

        val cachedFiles = emptyList<FileOperationItemMetadata>().toMutableList()
        val items = files.toSecureMedia(context = context)

        items.forEach { item ->
            val iv = item.bytes?.getIv() ?: run {
                emit(
                    value = FileOperationProgress.Finished(
                        result = Result.Error(FileOperationError.Failed)
                    )
                )

                return@flow
            }

            val originalFile = File(item.item.absolutePath)
            val cachedFile =
                if (item.item.type == MediaType.Video) {
                    originalFile.secureDecryptVideoFile(context)
                } else {
                    originalFile.secureDecryptCacheFile(context)
                }

            if (!cachedFile.exists()) {
                if (item.item.type == MediaType.Video) {
                    EncryptionManager.decryptVideo(
                        absolutePath = originalFile.absolutePath,
                        context = context,
                        iv = iv,
                        progress = {}
                    )
                } else {
                    EncryptionManager.decryptInputStream(
                        inputStream = originalFile.inputStream(),
                        outputStream = cachedFile.outputStream(),
                        fileSize = originalFile.length(),
                        iv = iv
                    )
                }
            }

            val cachedUri = FileProvider.getUriForFile(
                context,
                LAVENDER_FILE_PROVIDER_AUTHORITY,
                cachedFile
            ).toString()

            cachedFile.deleteOnExit()
            cachedFiles.add(
                FileOperationItemMetadata(
                    id = item.item.id,
                    uri = cachedUri,
                    absolutePath = cachedFile.absolutePath,
                    isImage = item.item.type == MediaType.Image,
                    immichUrl = null,
                    parentPath = item.item.parentPath
                )
            )

            emit(
                value = FileOperationProgress.ItemDone(
                    uri = cachedUri
                )
            )
        }

        emit(
            value = FileOperationProgress.Finished(
                result = gateway.share(files = cachedFiles)
            )
        )
    }

    override suspend fun deleteFiles(
        files: List<FileOperationItemMetadata>,
        albumId: String,
        immichId: String?
    ): Flow<FileOperationProgress<Unit>> = flow {
        try {
            emit(
                value = FileOperationProgress.Started(
                    action = FileOperationAction.LongOperationType.Delete,
                    fileCount = files.size
                )
            )

            files.forEach { item ->
                val file = File(
                    context.appSecureFolderDir,
                    item.uri.substringAfterLast("/") // filename
                )

                file.delete()
                val thumbnail = file.secureThumbnailImage(context)
                thumbnail.delete()

                secureDao.deleteEntityBySecuredPath(securedPath = file.absolutePath)
                secureDao.deleteEntityBySecuredPath(securedPath = thumbnail.absolutePath)

                emit(
                    value = FileOperationProgress.ItemDone(
                        uri = item.uri
                    )
                )
            }

            emit(
                value = FileOperationProgress.Finished(
                    result = Result.Success(Unit)
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            emit(
                value = FileOperationProgress.Finished(
                    result = Result.Error(FileOperationError.Failed)
                )
            )
        }
    }

    // TODO: clean this up
    override suspend fun getExifData(
        file: FileOperationItemMetadata
    ): Result<Map<MediaData, String>, FileOperationError> = withContext(Dispatchers.IO) {
        val threshold = 500

        val media = listOf(file).toSecureMedia(context = context).firstOrNull() ?: return@withContext Result.Error(FileOperationError.Failed)
        val originalFile = File(file.absolutePath)

        val decryptedFile = if (!file.isImage) {
            val cachedFile = originalFile.secureDecryptVideoFile(context)

            if (cachedFile.length() < originalFile.length()) {
                while (cachedFile.length() + threshold < originalFile.length()) {
                    delay(100.milliseconds)
                }

                cachedFile
            } else {
                cachedFile
            }
        } else {
            val cachedFile = originalFile.secureDecryptCacheFile(context)

            if (!cachedFile.exists()) {
                val iv = media.bytes?.getIv()

                if (iv == null) {
                    Log.e(TAG, "IV for ${media.item.displayName} was null, aborting")
                    return@withContext Result.Error(FileOperationError.Failed)
                }

                EncryptionManager.decryptInputStream(
                    inputStream = originalFile.inputStream(),
                    outputStream = cachedFile.outputStream(),
                    fileSize = originalFile.length(),
                    iv = iv
                )

                cachedFile
            } else if (cachedFile.length() < originalFile.length()) {
                while (cachedFile.length() + threshold < originalFile.length()) {
                    delay(100.milliseconds)
                }

                cachedFile
            } else {
                cachedFile
            }
        }

        val dateTaken = secureDao.getDateTakenFor(securedPath = file.absolutePath)

        val mediaData = getExifDataForMedia(
            inputStream = decryptedFile.inputStream(),
            absolutePath = decryptedFile.absolutePath,
            is24Hr = gateway.is24HrFormat(),
            fallback = dateTaken ?: media.item.dateModified
        ).toMutableMap().apply {
            set(MediaData.Path, media.item.parentPath)
            set(MediaData.Name, media.item.displayName)
        }

        return@withContext Result.Success(data = mediaData)
    }

    override suspend fun clearCaches() = secureManager.clearCaches()
}