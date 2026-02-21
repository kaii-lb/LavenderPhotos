package com.kaii.photos.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class SyncTaskStatus {
    Waiting,
    Processing,
    Synced
}

@Serializable
enum class SyncTaskType {
    Delete,
    Upload,
    Update
}

@Entity(tableName = "sync_tasks")
@TypeConverters(SyncTaskConverters::class)
data class SyncTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateModified: Long,
    val status: SyncTaskStatus,
    val type: SyncTaskType,
    val itemIds: List<String>
)

class SyncTaskConverters {
    @TypeConverter
    fun fromIds(ids: List<String>) = Json.encodeToString(ids)

    @TypeConverter
    fun toIds(ids: String): List<String> = Json.decodeFromString(ids)
}
