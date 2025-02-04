package com.kaii.photos.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.kaii.photos.MainActivity.Companion.applicationDatabase
import com.kaii.photos.R
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

fun getSecureDecryptedVideoFile(
	name: String,
	context: Context
) : File = File(context.appSecureVideoCacheDir, name)

fun addSecuredCachedImage(
    context: Context,
    mediaItem: MediaStoreData,
    metadataRetriever: MediaMetadataRetriever,
    file: File,
    encryptionManager: EncryptionManager
) {
    val thumbnailFile = getSecuredCacheImageForFile(file = file, context = context)

    val thumbnail =
        if (mediaItem.type == MediaType.Video) {
            metadataRetriever.setDataSource(context, mediaItem.uri)

            metadataRetriever.getScaledFrameAtTime(
                1000000L,
                MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
                1024,
                1024
            )
        } else {
            val image = BitmapFactory.decodeFile(mediaItem.absolutePath)
            val ratio = image.width.toFloat() / image.height.toFloat()

            Bitmap.createScaledBitmap(
                image,
                (1024 * ratio).toInt(),
                1024,
                false
            )
        }

    val actual =
        if (thumbnail != null) {
            val bytes = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, bytes)

            bytes.toByteArray()
        } else {
            val image = BitmapFactory.decodeResource(context.resources, R.drawable.broken_image)
            val bytes = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, bytes)

            bytes.toByteArray()
        }

    val iv = encryptionManager.encryptInputStream(
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
