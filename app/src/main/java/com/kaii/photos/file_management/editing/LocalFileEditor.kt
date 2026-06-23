package com.kaii.photos.file_management.editing

import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import com.kaii.photos.R
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.appCloudFolderDir
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.helpers.parent
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.mediastore.copyUriToUri
import com.kaii.photos.mediastore.getAbsolutePathFromUri
import com.kaii.photos.mediastore.getMediaStoreDataFromUri
import com.kaii.photos.mediastore.insertMedia
import com.kaii.photos.mediastore.setDateForMedia
import com.kaii.photos.mediastore.toContentId
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock

open class LocalFileEditor(
    override val mediaDao: MediaDao
) : GenericFileEditor {
    companion object {
        private val TAG = LocalFileEditor::class.qualifiedName
    }

    @OptIn(UnstableApi::class)
    override suspend fun editVideo(
        context: Context,
        modifications: List<VideoModification>,
        videoEditingState: VideoEditingState,
        basicVideoData: BasicVideoData,
        uri: String,
        info: ImmichBasicInfo,
        overwrite: Boolean,
        containerDimens: Size,
        canvasSize: Size,
        textMeasurer: TextMeasurer,
        isFromOpenWithView: Boolean
    ): Long? = withContext(Dispatchers.IO) {
        val media = mediaDao.getMediaFromUri(uri)
            ?: context.contentResolver.getMediaStoreDataFromUri(uri.toUri())
            ?: context.contentResolver.getAbsolutePathFromUri(uri.toUri()).let { absolutePath ->
                MediaStoreData.dummyItem.copy(
                    uri = uri,
                    absolutePath = absolutePath ?: "",
                    parentPath = absolutePath?.parent() ?: "",
                    type = MediaType.Video
                )
            }

        // 100 * 2 for each of the transformer.start's, and 40 for the copying
        val totalPercentage = 120f * 2
        val percentage = mutableFloatStateOf(0f)
        val progressHolder = ProgressHolder()
        val body = mutableStateOf(context.resources.getString(R.string.editing_export_video_loading_body, 0, 3))

        LavenderSnackbarController.pushEvent(
            LavenderSnackbarEvent.ProgressEvent(
                message = context.resources.getString(R.string.editing_export_video_loading),
                body = body,
                icon = R.drawable.videocam_filled,
                percentage = percentage
            )
        )

        val result = super.editVideoImpl(
            context,
            modifications,
            videoEditingState,
            basicVideoData,
            media,
            info,
            overwrite,
            containerDimens,
            canvasSize,
            textMeasurer,
            percentage,
            body,
            totalPercentage,
            progressHolder
        )

        if (result == null) {
            LavenderSnackbarController.pushEvent(
                event = LavenderSnackbarEvent.MessageEvent(
                    message = context.resources.getString(R.string.editing_export_video_failed),
                    icon = R.drawable.error_2,
                    duration = SnackbarDuration.Short
                )
            )

            return@withContext null
        }

        if (overwrite) {
            File(media.absolutePath).delete()
        }

        val newUri = context.contentResolver.insertMedia(
            context = context,
            media = media,
            destination = media.parentPath,
            currentVolumes = MediaStore.getExternalVolumeNames(context),
            preserveDate = true,
            overrideDisplayName = media.displayName.replaceAfterLast(".", "mp4"),
            onInsert = { _, new ->
                context.contentResolver.copyUriToUri(
                    from = result.newUri,
                    to = new
                )
            }
        )

        if (newUri == null) {
            Log.d(TAG, "Video export failed, could not insert new media")

            LavenderSnackbarController.pushEvent(
                event = LavenderSnackbarEvent.MessageEvent(
                    message = context.resources.getString(R.string.editing_export_video_failed),
                    icon = R.drawable.error_2,
                    duration = SnackbarDuration.Short
                )
            )

            return@withContext null
        }

        val tempFile = File(result.tempPath!!)
        if (tempFile.exists()) tempFile.delete()

        body.value = context.resources.getString(R.string.editing_export_video_loading_body, 3, 3)
        percentage.floatValue = 1f

        return@withContext newUri.toContentId(
            contentResolver = context.contentResolver,
            type = MediaType.Video
        )
    }

    override suspend fun editImage(
        context: Context,
        image: ImageBitmap,
        uri: String,
        containerDimens: Size,
        exportQuality: Int,
        drawingPaintState: DrawingPaintState,
        imageEditingState: ImageEditingState,
        modifications: List<ImageModification>,
        textMeasurer: TextMeasurer,
        actualLeft: Float,
        actualTop: Float,
        overwrite: Boolean,
        isFromOpenWithView: Boolean
    ): Long? = withContext(Dispatchers.IO) {
        val media = mediaDao.getMediaFromUri(uri)
            ?: context.contentResolver.getMediaStoreDataFromUri(uri.toUri())
            ?: context.contentResolver.getAbsolutePathFromUri(uri.toUri()).let { absolutePath ->
                MediaStoreData.dummyItem.copy(
                    uri = uri,
                    absolutePath = absolutePath ?: "",
                    parentPath = absolutePath?.parent() ?: ""
                )
            }

        val bitmap = super.editImageImpl(
            context,
            image,
            containerDimens,
            drawingPaintState,
            imageEditingState,
            modifications,
            textMeasurer,
            actualLeft,
            actualTop
        )

        if (bitmap == null) {
            Log.d(TAG, "Image export failed, bitmap was null")

            return@withContext null
        }

        val newUri =
            if (!overwrite || isFromOpenWithView) {
                context.contentResolver.insertMedia(
                    context = context,
                    media = media,
                    destination = media.parentPath.ifBlank {
                        appCloudFolderDir.absolutePath
                    },
                    currentVolumes = MediaStore.getExternalVolumeNames(context),
                    overrideDisplayName = media.displayName.replaceAfterLast(".", "jpeg"),
                    onInsert = { _, _ -> }
                )
            } else {
                media.uri.toUri()
            }

        if (newUri == null) {
            Log.d(TAG, "Image export failed, could not insert new media")

            return@withContext null
        }

        val wroteData = context.contentResolver.openOutputStream(newUri)?.use { outputStream ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                exportQuality * 10, // exportQuality is from 2 to 8
                outputStream
            )
        } != null

        context.contentResolver.setDateForMedia(
            uri = newUri,
            type = media.type,
            dateTaken = if (overwrite) media.dateTaken else Clock.System.now().epochSeconds,
            overwriteLastModified = true
        )

        if (!wroteData) {
            Log.d(TAG, "Image export failed, did not write data")
            context.contentResolver.delete(newUri, null)
            return@withContext null
        }

        return@withContext newUri.toContentId(
            contentResolver = context.contentResolver,
            type = MediaType.Video
        )
    }
}