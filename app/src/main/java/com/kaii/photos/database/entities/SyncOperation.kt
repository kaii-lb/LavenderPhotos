package com.kaii.photos.database.entities

import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface SyncOperation {
    @Serializable
    data class Upload(val destinationAlbumId: String) : SyncOperation

    @Serializable
    data class AddToAlbum(val destinationAlbumId: String) : SyncOperation

    @Serializable
    data class RemoveFromAlbum(val sourceAlbumId: String, val immichAlbumId: String) : SyncOperation

    @Serializable
    data class Delete(val sourceAlbumId: String) : SyncOperation

    @Serializable
    data class SetFavourite(val isFavourite: Boolean) : SyncOperation

    @Serializable
    data class RenameAlbum(val albumLocalId: String, val newName: String) : SyncOperation

    val isRemoval: Boolean
        get() = this is Delete || this is RemoveFromAlbum
}

object SyncOperationConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    @JvmStatic
    fun fromSyncOperation(operation: SyncOperation): String =
        json.encodeToString(SyncOperation.serializer(), operation)

    @TypeConverter
    @JvmStatic
    fun toSyncOperation(raw: String): SyncOperation =
        json.decodeFromString(SyncOperation.serializer(), raw)
}