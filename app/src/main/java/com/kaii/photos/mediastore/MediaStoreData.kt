package com.kaii.photos.mediastore

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.immichDurationToSecondsOrNull
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetResponse
import io.github.kaii_lb.lavender.immichintegration.serialization.assets.AssetType
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val MEDIA_STORE_FILE_URI: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

@Immutable
data class ImmichInfo(
    val thumbnail: String,
    val original: String,
    val hash: String,
    val accessToken: String,
    val endpoint: String,
    val useThumbnail: Boolean
)

@Immutable
data class SecureInfo(
    val iv: ByteArray,
    val absolutePath: String,
    val key: ObjectKey
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecureInfo

        if (!iv.contentEquals(other.iv)) return false
        if (absolutePath != other.absolutePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + absolutePath.hashCode()
        return result
    }
}

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
    val rawTypes = listOf(
        "image/dng",
        "image/x-adobe-dng",
        "image/x-raw-adobe",
        "image/tiff",
        "image/x-canon-cr2"
    ) // might not be complete for glide?

    return this.mimeType.lowercase() in rawTypes
}

fun MediaStoreData.isGIF(): Boolean = this.mimeType.lowercase() == "image/gif"

fun MediaStoreData.signature() = ObjectKey("$dateTaken$dateModified$absolutePath$id$mimeType$size".hashCode())

@OptIn(ExperimentalUuidApi::class)
fun AssetResponse.toMediaStoreData() =
    MediaStoreData(
        id = Uuid.parse(id).toLongs { a, _ -> a },
        uri = "/api/assets/${id}/original",
        dateTaken = Instant.parse(fileCreatedAt).epochSeconds,
        dateModified = Instant.parse(fileModifiedAt).epochSeconds,
        type = if (type == AssetType.Image) MediaType.Image else MediaType.Video,
        absolutePath = "",
        parentPath = "",
        displayName = originalFileName,
        mimeType = originalMimeType,
        immichUrl = "/api/assets/${id}/original",
        hash = checksum,
        size = exifInfo?.fileSizeInByte ?: 0L,
        favourited = isFavorite,
        duration = duration.immichDurationToSecondsOrNull()
    )