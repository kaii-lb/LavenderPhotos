package com.kaii.photos.mediastore

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.paging.ItemSnapshotList
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.database.entities.MediaStoreData
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class ImmichInfo(
    val thumbnail: String,
    val original: String,
    val hash: String,
    val accessToken: String
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

sealed class PhotoLibraryUIModel {
    data class Media(val item: MediaStoreData) : PhotoLibraryUIModel()

    data class Section(val title: String) : PhotoLibraryUIModel()

    fun signature() =
        if (this is Media) {
            item.signature()
        } else {
            throw IllegalStateException("Cannot get signature of a ${Section::class.simpleName}!")
        }

    fun itemKey() =
        if (this is Media) item.absolutePath + item.displayName + item.id
        else (this as Section).title
}

fun List<PhotoLibraryUIModel>.mapToMediaItems() = mapNotNull { if (it is PhotoLibraryUIModel.Media) it.item else null }
fun ItemSnapshotList<PhotoLibraryUIModel>.mapToMediaItems() = mapNotNull { if (it is PhotoLibraryUIModel.Media) it.item else null }