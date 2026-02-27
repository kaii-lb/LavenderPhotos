package com.kaii.photos.mediastore

import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.database.entities.MediaStoreData
import kotlinx.parcelize.Parcelize

val MEDIA_STORE_FILE_URI: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

@Immutable
@Parcelize
data class ImmichInfo(
    val thumbnail: String,
    val original: String,
    val hash: String,
    val accessToken: String,
    val useThumbnail: Boolean
) : Parcelable

/** The type of data. */
enum class MediaType {
    Video,
    Image,
    Section
}

fun String.toMediaType() = when (this) {
    "Image" -> MediaType.Image
    "Video" -> MediaType.Video
    else -> MediaType.Section
}

fun ByteArray.getIv() = copyOfRange(0, 16)
fun ByteArray.getThumbnailIv() = copyOfRange(16, 32)
fun ByteArray.getOriginalPath() = decodeToString(32, size)

fun MediaStoreData.isRawImage(): Boolean {
    val rawTypes = listOf("image/dng", "image/tiff", "image/x-raw-adobe") // might not be complete for glide?

    return this.mimeType.lowercase() in rawTypes
}

fun MediaStoreData.signature() = ObjectKey(dateTaken + dateModified + absolutePath.hashCode() + id + mimeType.hashCode())