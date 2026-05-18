package com.kaii.photos.file_management.editing

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoModification
import com.kaii.photos.mediastore.toMediaStoreData
import io.github.kaii_lb.lavender.immichintegration.BitmapAssetSource
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetUploadRequest
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.CopyAssetRequest
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
    override suspend fun editVideo(
        context: Context,
        modifications: List<VideoModification>,
        videoEditingState: VideoEditingState,
        basicVideoData: BasicVideoData,
        uri: String,
        overwrite: Boolean,
        containerDimens: Size,
        canvasSize: Size,
        textMeasurer: TextMeasurer,
        isFromOpenWithView: Boolean
    ): Long? {
        TODO("Not yet implemented")
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
    ): Long? {
        val mediaItem = mediaDao.getMediaFromUri(uri) ?: return null

        val result = super.editImageImpl(
            context, image, containerDimens,
            drawingPaintState, imageEditingState,
            modifications, textMeasurer,
            actualLeft, actualTop
        ) ?: return null

        // this is okay because it is not being used to tracking purposes, only for identification to the immich server.
        @SuppressLint("HardwareIds")
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

        val response = assetsClient.upload(
            asset = AssetUploadRequest(
                assetSource = BitmapAssetSource(bitmap = result),
                deviceAssetId = "${mediaItem.displayName}-${mediaItem.size}",
                deviceId = deviceId,
                fileCreatedAt = Instant.fromEpochSeconds(mediaItem.dateTaken).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                fileModifiedAt = Instant.fromEpochSeconds(mediaItem.dateModified).format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
                metadata = emptyList(),
                filename = mediaItem.displayName
            )
        ) ?: return null

        if (albumImmichId != null) {
            albumsClient.addAssets(
                albumId = Uuid.parse(albumImmichId),
                assetIds = listOf(
                    Uuid.parse(response.id)
                )
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
                        targetId = Uuid.parse(response.id)
                    )
                )
            }
        }

        assetsClient.get(
            id = Uuid.parse(response.id)
        )?.let { newAsset ->
            mediaDao.insert(newAsset.toMediaStoreData())
        }

        return Uuid.parse(response.id).toLongs { a, _ -> a }
    }
}