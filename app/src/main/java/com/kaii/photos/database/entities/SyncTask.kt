package com.kaii.photos.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class SyncTaskStatus {
    Waiting,
    Processing,
    Synced
}

@Serializable
enum class SyncTaskType {
    Delete,
    Trash,
    Upload,
    Favourite,
    RenameAlbum,
    Copy
}

@Entity(tableName = "sync_tasks")
data class SyncTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateModified: Long,
    val status: SyncTaskStatus,
    val type: SyncTaskType,
    val destination: String?,
    val extraData: String? = null
)
