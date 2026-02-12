package com.kaii.photos.helpers

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getAbsolutePathFromUri
import com.kaii.photos.mediastore.getIv
import com.kaii.photos.mediastore.getOriginalPath
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import java.io.File

private const val TAG = "com.kaii.photos.helpers.ImageFunctions"

fun permanentlyDeletePhotoList(context: Context, list: List<Uri>) {
    if (list.isNotEmpty()) {
        val deleteRequest = MediaStore.createDeleteRequest(
            context.contentResolver,
            list
        )

        (context as Activity).startIntentSenderForResult(
            deleteRequest.intentSender,
            9997,
            null,
            0,
            0,
            0
        )
    }
}

fun setFavouriteOnMedia(
    context: Context,
    favourite: Boolean,
    list: List<Uri>
) {
    if (list.isNotEmpty()) {
        val favRequest = MediaStore.createFavoriteRequest(
            context.contentResolver,
            list,
            favourite
        )

        (context as Activity).startIntentSenderForResult(
            favRequest.intentSender,
            9998,
            null,
            0,
            0,
            0
        )
    }
}

/** @param list is a list of pairs of item uri and its absolute path */
suspend fun setTrashedOnPhotoList(
    context: Context,
    list: List<Uri>,
    trashed: Boolean
) {
    val contentResolver = context.contentResolver

    val currentTimeMillis = System.currentTimeMillis()
    val trashedValues = ContentValues().apply {
        put(MediaColumns.IS_TRASHED, trashed)
        put(MediaColumns.DATE_MODIFIED, currentTimeMillis)
    }

    val body = mutableStateOf(context.resources.getString(R.string.media_operate_snackbar_body, 0, list.size))
    val percentage = mutableFloatStateOf(0f)

    LavenderSnackbarController.pushEvent(
        LavenderSnackbarEvents.ProgressEvent(
            message =
                if (trashed) context.resources.getString(R.string.media_delete_snackbar_title)
                else context.resources.getString(R.string.media_restore_snackbar_title),
            body = body,
            icon = R.drawable.content_paste,
            percentage = percentage
        )
    )

    setFavouriteOnMedia(
        context = context,
        favourite = false,
        list = list
    )

    try {
        list.forEachIndexed { index, uri ->
            // order is very important!
            // this WILL crash if you try to set last modified on a file that got moved from ex image.png to .trashed-{timestamp}-image.png
            // TODO: do this oneshot instead of for every uri
            contentResolver.getAbsolutePathFromUri(uri)?.let {
                File(it).setLastModified(currentTimeMillis)
            }
            contentResolver.update(uri, trashedValues, null)

            body.value = context.resources.getString(R.string.media_operate_snackbar_body, index + 1, list.size)
            percentage.floatValue = (index + 1f) / list.size
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Setting trashed $trashed on photo list failed.")
        e.printStackTrace()
    }
}

fun shareImage(uri: Uri, context: Context, mimeType: String? = null) {
    val contentResolver = context.contentResolver

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = mimeType ?: contentResolver.getType(uri)
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    val chooserIntent = Intent.createChooser(shareIntent, null)
    context.startActivity(chooserIntent)
}

fun shareMultipleImages(
    uris: List<Uri>,
    context: Context,
    hasVideos: Boolean
) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = if (hasVideos) "video/*" else "image/*"
    }

    val fileUris = ArrayList<Uri>()
    uris.forEach {
        fileUris.add(it)
    }

    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

    context.startActivity(Intent.createChooser(intent, null))
}

fun shareSecuredImage(absolutePath: String, context: Context) {
    val uri =
        FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, File(absolutePath))

    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = context.contentResolver.getType(uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    context.startActivity(Intent.createChooser(intent, "Share secured image"))
}

/** @param paths is a list of absolute paths and [MediaType]s of items */
fun shareMultipleSecuredImages(
    paths: List<Pair<String, MediaType>>,
    context: Context
) {
    val hasVideos = paths.any {
        it.second == MediaType.Video
    }

    val intent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        type = if (hasVideos) "video/*" else "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val fileUris = ArrayList(
        paths.map {
            FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, File(it.first))
        }
    )

    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

    context.startActivity(Intent.createChooser(intent, null))
}

fun moveImageToLockedFolder(
    list: List<MediaStoreData>,
    context: Context,
    applicationDatabase: MediaDatabase,
    onDone: () -> Unit
) {
    val lastModified = System.currentTimeMillis()
    val metadataRetriever = MediaMetadataRetriever()

    list.forEach { mediaItem ->
        val fileToBeHidden = File(mediaItem.absolutePath)
        val copyToPath = context.appSecureFolderDir + "/" + fileToBeHidden.name
        try {
            // set last modified so item shows up in correct place in locked folder
            fileToBeHidden.setLastModified(lastModified)

            val destinationFile = File(copyToPath)

            context.contentResolver.setDateForMedia(
                uri = mediaItem.uri.toUri(),
                type = mediaItem.type,
                dateTaken = mediaItem.dateTaken,
                overwriteLastModified = false
            )

            addSecuredCachedMediaThumbnail(
                context = context,
                mediaItem = mediaItem,
                file = fileToBeHidden,
                metadataRetriever = metadataRetriever,
                applicationDatabase = applicationDatabase
            )

            // encrypt file data and write to secure folder path
            val iv =
                EncryptionManager.encryptInputStream(
                    fileToBeHidden.inputStream(),
                    destinationFile.outputStream()
                )

            applicationDatabase.securedItemEntityDao().insertEntity(
                SecuredItemEntity(
                    originalPath = mediaItem.absolutePath,
                    securedPath = copyToPath,
                    iv = iv
                )
            )

            // cleanup
            permanentlyDeletePhotoList(
                context = context,
                list = listOf(mediaItem.uri.toUri())
            )
            applicationDatabase.mediaDao().deleteById(mediaItem.id)
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()

            applicationDatabase.securedItemEntityDao().insertEntity(
                SecuredItemEntity(
                    originalPath = mediaItem.absolutePath,
                    securedPath = copyToPath,
                    iv = ByteArray(0)
                )
            )
        }
    }

    onDone()
}

suspend fun moveImageOutOfLockedFolder(
    list: List<PhotoLibraryUIModel.SecuredMedia>,
    context: Context,
    applicationDatabase: MediaDatabase,
    onDone: () -> Unit
) {
    val contentResolver = context.contentResolver
    val restoredFilesDir = context.appRestoredFilesDir

    list.forEach { media ->
        val fileToBeRestored = File(media.item.absolutePath)
        val originalPath = media.bytes?.getOriginalPath() ?: restoredFilesDir

        Log.d(TAG, "ORIGINAL PATH $originalPath")

        val tempFile = File(context.cacheDir, fileToBeRestored.name)

        val iv = media.bytes?.getIv()
        try {
            if (iv != null) {
                EncryptionManager.decryptInputStream(
                    fileToBeRestored.inputStream(),
                    tempFile.outputStream(),
                    iv
                )
            } else {
                fileToBeRestored.inputStream().copyTo(tempFile.outputStream())
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()
        }

        Log.d(TAG, "Base path ${originalPath.toBasePath()}")

        contentResolver.insertMedia(
            context = context,
            media = media.item.copy(
                uri = FileProvider.getUriForFile(
                    context,
                    LAVENDER_FILE_PROVIDER_AUTHORITY,
                    tempFile
                ).toString()
            ),
            destination = originalPath.parent(),
            basePath = originalPath.toBasePath(),
            currentVolumes = MediaStore.getExternalVolumeNames(context),
            preserveDate = true,
            onInsert = { original, new ->
                contentResolver.copyUriToUri(original, new)
            }
        )?.let {
            try {
                fileToBeRestored.delete()
                tempFile.delete()
                applicationDatabase.securedItemEntityDao().deleteEntityBySecuredPath(media.item.absolutePath)

                val thumbnailFile =
                    getSecuredCacheImageForFile(file = fileToBeRestored, context = context)
                thumbnailFile.delete()
                applicationDatabase.securedItemEntityDao()
                    .deleteEntityBySecuredPath(thumbnailFile.absolutePath)
            } catch (e: Throwable) {
                Log.e(TAG, e.toString())
                e.printStackTrace()
            }
        }
    }

    onDone()
}

/** @param list is a list of the absolute path of every image to be deleted */
fun permanentlyDeleteSecureFolderImageList(list: List<String>, context: Context) {
    try {
        list.forEach { path ->
            File(path).let { file ->
                file.delete()
                getSecuredCacheImageForFile(file = file, context = context).delete()
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, e.toString())
    }
}

/** returns null if the operation succeeded, otherwise lets the caller handle the [RecoverableSecurityException] */
fun renameImage(context: Context, uri: Uri, newName: String): IntentSender? {
    val contentResolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaColumns.DISPLAY_NAME, newName)
    }

    try {
        contentResolver.update(uri, contentValues, null)
        contentResolver.notifyChange(uri, null)

        return null
    } catch (securityException: SecurityException) {
        Log.e(TAG, securityException.toString())
        securityException.printStackTrace()

        val recoverableSecurityException =
            securityException as? RecoverableSecurityException ?: throw RuntimeException(securityException.message, securityException)

        val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
        return intentSender
    }
}

fun renameDirectory(
    context: Context,
    absolutePath: String,
    base: String,
    newName: String,
) {
    try {
        val dir =
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_DOCUMENTS_AUTHORITY,
                "$base:${absolutePath.toRelativePath(true)}"
            )

        Log.d(TAG, "Dir is $dir")

        val newDirectory = DocumentFile.fromTreeUri(context, dir)
        newDirectory?.renameTo(newName)
    } catch (e: Throwable) {
        Log.e(TAG, "Couldn't rename directory $absolutePath to $newName")
        e.printStackTrace()
    }
}

/** @param destination where to move said files to, should be relative*/
suspend fun moveImageListToPath(
    context: Context,
    list: List<MediaStoreData>,
    destination: String,
    basePath: String,
    preserveDate: Boolean
) {
    val contentResolver = context.contentResolver

    val body = mutableStateOf(context.resources.getString(R.string.media_operate_snackbar_body, 0, list.size))
    val percentage = mutableFloatStateOf(0f)

    LavenderSnackbarController.pushEvent(
        LavenderSnackbarEvents.ProgressEvent(
            message = context.resources.getString(R.string.media_move_snackbar_title),
            body = body,
            icon = R.drawable.cut,
            percentage = percentage
        )
    )

    list.forEachIndexed { index, media ->
        contentResolver.insertMedia(
            context = context,
            media = media,
            destination = destination,
            basePath = basePath,
            currentVolumes = MediaStore.getExternalVolumeNames(context),
            preserveDate = preserveDate,
            onInsert = { original, new ->
                contentResolver.copyUriToUri(original, new)
            }
        )?.let {
            body.value = context.resources.getString(R.string.media_operate_snackbar_body, index + 1, list.size)
            percentage.floatValue = (index + 1f) / list.size

            contentResolver.delete(media.uri.toUri(), null)
        }
    }

    percentage.floatValue = 1f
}

/** @param destination where to copy said files to, should be relative
@param overrideDisplayName should not contain file extension */
suspend fun copyImageListToPath(
    context: Context,
    list: List<MediaStoreData>,
    destination: String,
    basePath: String,
    overwriteDate: Boolean,
    showProgressSnackbar: Boolean = true,
    overrideDisplayName: ((displayName: String) -> String)? = null,
    onSingleItemDone: (media: MediaStoreData) -> Unit
): MutableList<Uri> {
    val contentResolver = context.contentResolver

    val body = mutableStateOf(context.resources.getString(R.string.media_operate_snackbar_body, 0, list.size))
    val percentage = mutableFloatStateOf(0f)

    if (showProgressSnackbar) {
        LavenderSnackbarController.pushEvent(
            LavenderSnackbarEvents.ProgressEvent(
                message = context.resources.getString(R.string.media_copy_snackbar_title),
                body = body,
                icon = R.drawable.trash,
                percentage = percentage
            )
        )
    }

    val newUris = mutableListOf<Uri>()

    list.forEachIndexed { index, media ->
        contentResolver.insertMedia(
            context = context,
            media = media,
            destination = destination,
            basePath = basePath,
            overrideDisplayName = if (overrideDisplayName != null) overrideDisplayName(media.displayName) else null,
            currentVolumes = MediaStore.getExternalVolumeNames(context),
            preserveDate = overwriteDate,
            onInsert = { original, new ->
                contentResolver.copyUriToUri(original, new)
                newUris.add(new)
            }
        )?.let {
            body.value = context.resources.getString(R.string.media_operate_snackbar_body, index + 1, list.size)
            percentage.floatValue = (index + 1f) / list.size

            onSingleItemDone(media)
        }
    }

    percentage.floatValue = 1f

    return newUris
}