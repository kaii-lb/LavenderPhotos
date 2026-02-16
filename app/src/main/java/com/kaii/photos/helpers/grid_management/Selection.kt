package com.kaii.photos.helpers.grid_management

import android.content.Context
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.appRestoredFilesDir
import com.kaii.photos.helpers.appSecureFolderDir
import com.kaii.photos.helpers.getSecuredCacheImageForFile
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

suspend fun List<SelectionManager.SelectedItem>.toSecureMedia(
    context: Context
): List<PhotoLibraryUIModel.SecuredMedia> = withContext(Dispatchers.IO) {
    val context = context.applicationContext
    val dao = MediaDatabase.getInstance(context).securedItemEntityDao()
    val selectedPaths = fastMap { dao.getSecuredPathFromOriginalPath(originalPath = it.parentPath) }

    val secureFolderFiles = File(context.appSecureFolderDir).listFiles { file ->
        file.absolutePath in selectedPaths
    }

    return@withContext secureFolderFiles?.mapNotNull { file ->
        val mimeType = Files.probeContentType(Path(file.absolutePath))

        val type =
            if (mimeType.lowercase().contains("image")) MediaType.Image
            else if (mimeType.lowercase().contains("video")) MediaType.Video
            else return@mapNotNull null

        val decryptedBytes =
            run {
                val iv = dao.getIvFromSecuredPath(file.absolutePath)
                val thumbnailIv = dao.getIvFromSecuredPath(
                    securedPath = getSecuredCacheImageForFile(file = file, context = context).absolutePath
                )

                if (iv != null && thumbnailIv != null) iv + thumbnailIv else null
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
            immichThumbnail = null,
            hash = null,
            favourited = false
        )

        PhotoLibraryUIModel.SecuredMedia(
            item = item,
            accessToken = "",
            bytes = decryptedBytes?.plus(originalPath.encodeToByteArray())
        )
    } ?: emptyList()
}