package com.kaii.photos.helpers.grid_management

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.SecureIvRecovery
import com.kaii.photos.helpers.secureThumbnailImage
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import io.github.kaii_lb.lavender.immichintegration.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

suspend fun List<SelectionManager.SelectedItem>.toSecureMedia(
    context: Context
): List<PhotoLibraryUIModel.SecuredMedia> = withContext(Dispatchers.IO) {
    val context = context.applicationContext
    val metadataRetriever = MediaMetadataRetriever()
    val dao = MediaDatabase.getInstance(context).securedItemEntityDao()
    val selectedPaths = fastMap { dao.getSecuredPathFromOriginalPath(originalPath = it.parentPath) }

    val secureFolderFiles = File(context.appSecureFolderDir).listFiles { file ->
        file.absolutePath in selectedPaths
    }

    val list = secureFolderFiles?.mapNotNull { file ->
        val mimeType = Files.probeContentType(Path(file.absolutePath))

        val type =
            if (mimeType.lowercase().contains("image")) MediaType.Image
            else if (mimeType.lowercase().contains("video")) MediaType.Video
            else return@mapNotNull null

        val decryptedBytes =
            run {
                val iv = dao.getIvFromSecuredPath(file.absolutePath)
                val thumbnailIv = dao.getIvFromSecuredPath(
                    securedPath = file.secureThumbnailImage(context).absolutePath
                )

                // recover a corrupted file iv (ByteArray(0) from a failed-secure catch block) so a
                // restore/share of this item decrypts real bytes instead of being dropped as null.
                // the thumbnail iv is display-only here, so pad it to keep the [fileIv][thumbIv][path]
                // layout intact; a null fileIv means "no db row" (legacy unencrypted) -> leave null
                val fileIv = when {
                    iv == null -> null
                    iv.size == 16 -> iv
                    else -> SecureIvRecovery.recoverAndPersist(context, file, mimeType, dao)
                }

                fileIv?.plus(thumbnailIv?.takeIf { it.size == 16 } ?: ByteArray(16))
            }

        val originalPath =
            dao.getOriginalPathFromSecuredPath(file.absolutePath) ?: context.appRestoredFilesDir

        val item = MediaStoreData(
            type = type,
            id = file.hashCode() * file.length() * file.lastModified(),
            uri = FileProvider.getUriForFile(
                context,
                LAVENDER_FILE_PROVIDER_AUTHORITY,
                file
            ).toString(),
            mimeType = mimeType,
            dateModified = file.lastModified() / 1000,
            dateTaken = file.lastModified() / 1000,
            displayName = file.name,
            absolutePath = file.absolutePath,
            parentPath = originalPath,
            size = 0L,
            immichUrl = null, // TODO
            hash = null,
            favourited = false,
            duration = null
        )

        PhotoLibraryUIModel.SecuredMedia(
            item = item,
            auth = Auth.None,
            endpoint = "",
            bytes = decryptedBytes?.plus(originalPath.encodeToByteArray())
        )
    } ?: emptyList()

    metadataRetriever.close()

    return@withContext list
}