package com.kaii.photos.file_management.editing

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.files.FileOperationAction
import com.kaii.photos.domain.files.FileOperationError
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.file_management.managers.gateways.MediaStoreGateway
import com.kaii.photos.file_management.managers.traits.PrepareFileForWrite
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoModification
import io.github.kaii_lb.lavender.immichintegration.clients.AlbumsClient
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import javax.inject.Inject

class HybridFileEditor(
    override val mediaDao: MediaDao,
    private val cloudFileEditor: CloudFileEditor,
    private val localFileEditor: LocalFileEditor
) : GenericFileEditor, PrepareFileForWrite {
    class Factory @Inject constructor(
        private val mediaDao: MediaDao,
        private val customDao: CustomEntityDao,
        private val gateway: MediaStoreGateway,
        private val assetsClient: AssetsClient,
        private val albumsClient: AlbumsClient
    ) {
        fun create(
            album: AlbumType
        ): HybridFileEditor =
            HybridFileEditor(
                mediaDao = mediaDao,
                cloudFileEditor = CloudFileEditor(
                    mediaDao = mediaDao,
                    assetsClient = assetsClient,
                    albumsClient = albumsClient,
                    albumImmichId = album.immichId
                ),
                localFileEditor =
                    if (album is AlbumType.Custom) {
                        CustomFileEditor(
                            customDao = customDao,
                            mediaDao = mediaDao,
                            albumId = album.id,
                            gateway = gateway
                        )
                    } else {
                        LocalFileEditor(
                            mediaDao = mediaDao,
                            gateway = gateway
                        )
                    }
            )
    }

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
    ): Long? {
        return if (uri.startsWith("/api")) cloudFileEditor.editVideo(
            context = context,
            modifications = modifications,
            videoEditingState = videoEditingState,
            basicVideoData = basicVideoData,
            uri = uri,
            info = info,
            overwrite = overwrite,
            containerDimens = containerDimens,
            canvasSize = canvasSize,
            textMeasurer = textMeasurer,
            isFromOpenWithView = isFromOpenWithView
        ) else localFileEditor.editVideo(
            context = context,
            modifications = modifications,
            videoEditingState = videoEditingState,
            basicVideoData = basicVideoData,
            uri = uri,
            info = info,
            overwrite = overwrite,
            containerDimens = containerDimens,
            canvasSize = canvasSize,
            textMeasurer = textMeasurer,
            isFromOpenWithView = isFromOpenWithView
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
    ): Long? {
        return if (uri.startsWith("/api")) cloudFileEditor.editImage(
            context = context,
            image = image,
            uri = uri,
            containerDimens = containerDimens,
            exportQuality = exportQuality,
            drawingPaintState = drawingPaintState,
            imageEditingState = imageEditingState,
            modifications = modifications,
            textMeasurer = textMeasurer,
            actualLeft = actualLeft,
            actualTop = actualTop,
            overwrite = overwrite,
            isFromOpenWithView = isFromOpenWithView
        ) else localFileEditor.editImage(
            context = context,
            image = image,
            uri = uri,
            containerDimens = containerDimens,
            exportQuality = exportQuality,
            drawingPaintState = drawingPaintState,
            imageEditingState = imageEditingState,
            modifications = modifications,
            textMeasurer = textMeasurer,
            actualLeft = actualLeft,
            actualTop = actualTop,
            overwrite = overwrite,
            isFromOpenWithView = isFromOpenWithView
        )
    }

    override fun prepareFileForWrite(
        files: List<FileOperationItemMetadata>,
        followUpAction: FileOperationAction
    ): Result<Unit, FileOperationError> =
        localFileEditor.prepareFileForWrite(
            files = files.filter { !it.isCloud },
            followUpAction = followUpAction
        )
}