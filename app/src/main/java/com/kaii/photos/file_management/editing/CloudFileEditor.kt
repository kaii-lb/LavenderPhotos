package com.kaii.photos.file_management.editing

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.core.net.toFile
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ProgressHolder
import com.kaii.photos.R
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.AssetSource
import io.github.kaii_lb.lavender.immichintegration.BitmapAssetSource
import io.github.kaii_lb.lavender.immichintegration.FileAssetSource
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetMediaCreateDto
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.CopyAssetRequest
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CloudFileEditor(
    override val mediaDao: MediaDao,
    private val assetsClient: AssetsClient,
    private val albumsClient: AlbumsClient,
    private val albumImmichId: String?
) : GenericFileEditor {
    @androidx.annotation.OptIn(UnstableApi::class)
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
        val mediaItem = mediaDao.getMediaFromUri(uri) ?: return@withContext null

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
            context, modifications,
            videoEditingState, basicVideoData,
            mediaItem, info, overwrite,
            containerDimens, canvasSize,
            textMeasurer, percentage, body,
            totalPercentage, progressHolder
        ) ?: return@withContext null

        percentage.floatValue = 1f

        val isLoading = mutableStateOf(true)
        LavenderSnackbarController.pushEvent(
            LavenderSnackbarEvent.LoadingEvent(
                message = context.resources.getString(R.string.editing_export_video_uploading),
                icon = R.drawable.cloud_upload,
                isLoading = isLoading
            )
        )

        val id = upload(
            mediaItem = mediaItem,
            assetSource = FileAssetSource(
                file = result.newUri.toFile()
            ),
            overwrite = overwrite
        )

        isLoading.value = false

        if (id == null) return@withContext null

        // TODO: figure something out for this
        return@withContext 0L
    }

    @OptIn(ExperimentalUuidApi::class)
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
        val mediaItem = mediaDao.getMediaFromUri(uri) ?: return@withContext null

        val result = super.editImageImpl(
            context, image, containerDimens,
            drawingPaintState, imageEditingState,
            modifications, textMeasurer,
            actualLeft, actualTop
        ) ?: return@withContext null

        val id = upload(
            mediaItem = mediaItem,
            assetSource = BitmapAssetSource(bitmap = result),
            overwrite = overwrite
        ) ?: return@withContext null

        return@withContext Uuid.parse(id).toLongs { a, _ -> a }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun upload(
        mediaItem: MediaStoreData,
        assetSource: AssetSource,
        overwrite: Boolean
    ): String? {
        val response = assetsClient.upload(
            asset = AssetMediaCreateDto(
                assetSource = assetSource,
                fileCreatedAt = Instant.fromEpochSeconds(mediaItem.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                fileModifiedAt = Instant.fromEpochSeconds(mediaItem.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                metadata = emptyList(),
                filename = mediaItem.displayName
            )
        ) ?: return null

        if (albumImmichId != null) {
            albumsClient.addAssets(
                albumId = Uuid.parse(albumImmichId),
                assetIds = listOf(response.id)
            )
        }

        if (overwrite) {
            val success = assetsClient.delete(
                ids = listOf(
                    Uuid.parse(mediaItem.immichId!!)
                ),
                force = true
            )

            if (success) {
                mediaDao.delete(mediaItem.id)

                assetsClient.copyInfo(
                    request = CopyAssetRequest(
                        albums = true,
                        favorite = true,
                        sharedLinks = true,
                        sidecar = true,
                        sourceId = Uuid.parse(mediaItem.immichId!!),
                        stack = true,
                        targetId = response.id
                    )
                )
            }
        }

        assetsClient.get(
            id = response.id
        )?.let { newAsset ->
            mediaDao.insert(newAsset.toMediaStoreData())
        }

        return response.id.toString()
    }
}