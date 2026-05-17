package com.kaii.photos.file_management.editing

import android.content.Context
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import com.kaii.photos.database.daos.CustomEntityDao
import com.kaii.photos.database.daos.MediaDao
import com.kaii.photos.database.entities.CustomItem
import com.kaii.photos.helpers.editing.BasicVideoData
import com.kaii.photos.helpers.editing.DrawingPaintState
import com.kaii.photos.helpers.editing.ImageEditingState
import com.kaii.photos.helpers.editing.ImageModification
import com.kaii.photos.helpers.editing.VideoEditingState
import com.kaii.photos.helpers.editing.VideoModification
import kotlinx.coroutines.delay

class CustomFileEditor(
    private val customDao: CustomEntityDao,
    private val albumId: String,
    override val mediaDao: MediaDao
) : LocalFileEditor(mediaDao) {
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
        val id = super.editImage(
            context,
            image,
            uri,
            containerDimens,
            exportQuality,
            drawingPaintState,
            imageEditingState,
            modifications,
            textMeasurer,
            actualLeft,
            actualTop,
            overwrite,
            isFromOpenWithView
        ) ?: return null

        if (!overwrite) {
            var tries = 0
            while (tries < 60 && !mediaDao.exists(id)) {
                tries += 1
                delay(500)
            }

            customDao.upsertAll(
                items = listOf(
                    CustomItem(
                        id = id,
                        album = albumId
                    )
                )
            )
        }

        return id
    }

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
        val id = super.editVideo(
            context,
            modifications,
            videoEditingState,
            basicVideoData,
            uri,
            overwrite,
            containerDimens,
            canvasSize,
            textMeasurer,
            isFromOpenWithView
        ) ?: return null

        if (!overwrite) {
            var tries = 0
            while (tries < 60 && !mediaDao.exists(id)) {
                tries += 1
                delay(500)
            }

            customDao.upsertAll(
                items = listOf(
                    CustomItem(
                        id = id,
                        album = albumId
                    )
                )
            )
        }

        return id
    }
}