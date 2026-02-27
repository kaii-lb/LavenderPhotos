package com.kaii.photos.helpers.paging

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.mediastore.signature

@Stable
@Immutable
sealed interface PhotoLibraryUIModel {
    @Stable
    @Immutable
    interface MediaImpl : PhotoLibraryUIModel {
        val item: MediaStoreData
        val accessToken: String?
    }

    @Stable
    @Immutable
    data class Media(
        override val item: MediaStoreData,
        override val accessToken: String? = null
    ) : MediaImpl

    @Stable
    @Immutable
    data class Section(
        val title: String,
        val timestamp: Long
    ) : PhotoLibraryUIModel

    @Stable
    @Immutable
    data class SecuredMedia(
        override val item: MediaStoreData,
        override val accessToken: String? = null,
        val bytes: ByteArray?
    ) : MediaImpl {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SecuredMedia

            if (item != other.item) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = item.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    fun signature() =
        if (this is MediaImpl) {
            item.signature()
        } else {
            throw IllegalStateException("Cannot get signature of a ${Section::class.simpleName}!")
        }

    fun itemKey() =
        if (this is MediaImpl) item.absolutePath + item.displayName + item.id
        else (this as Section).timestamp.toString()
}
