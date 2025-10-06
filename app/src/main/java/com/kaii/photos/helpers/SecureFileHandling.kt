package com.kaii.photos.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.SecuredItemEntity
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import java.io.ByteArrayOutputStream
import java.io.File

fun getSecuredCacheImageForFile(
    file: File,
    context: Context
) : File = File(context.appSecureThumbnailCacheDir + "/" + file.name + ".png")

fun getSecuredCacheImageForFile(
    fileName: String,
    context: Context
) : File = File(context.appSecureThumbnailCacheDir + "/" + fileName + ".png")

fun getDecryptCacheForFile(
    file: File,
    context: Context
) : File = File(context.appSecureThumbnailCacheDir + "/" + "${file.nameWithoutExtension}-decrypt.${file.extension}")

fun getSecureDecryptedVideoFile(
	name: String,
	context: Context
) : File = File(context.appSecureVideoCacheDir, name)

fun addSecuredCachedMediaThumbnail(
    context: Context,
    mediaItem: MediaStoreData,
    metadataRetriever: MediaMetadataRetriever,
    applicationDatabase: MediaDatabase,
    file: File
) {
    val thumbnailFile = getSecuredCacheImageForFile(file = file, context = context)

    if (thumbnailFile.parentFile?.exists() == false) thumbnailFile.parentFile?.mkdirs()

    val thumbnail =
        if (mediaItem.type == MediaType.Video) {
            metadataRetriever.setDataSource(context, mediaItem.uri)

            metadataRetriever.getScaledFrameAtTime(
                -1L,
                MediaMetadataRetriever.OPTION_CLOSEST,
                1024,
                1024
            )
        } else {
            val image = BitmapFactory.decodeFile(mediaItem.absolutePath)
            val ratio = image.width.toFloat() / image.height.toFloat()

            image.scale((1024 * ratio).toInt(), 1024, false)
        }

    val actual =
        if (thumbnail != null) {
            val bytes = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, bytes)

            bytes.toByteArray()
        } else {
            val image = ResourcesCompat.getDrawable(context.resources, R.drawable.broken_image, null)?.toBitmap()
            val bytes = ByteArrayOutputStream()
            image?.compress(Bitmap.CompressFormat.PNG, 100, bytes)

            bytes.toByteArray()
        }

    val iv = EncryptionManager.encryptInputStream(
        actual.inputStream(),
        thumbnailFile.outputStream(),
    )

    applicationDatabase.securedItemEntityDao().insertEntity(
        SecuredItemEntity(
            originalPath = thumbnailFile.absolutePath,
            securedPath = thumbnailFile.absolutePath,
            iv = iv
        )
    )
}
