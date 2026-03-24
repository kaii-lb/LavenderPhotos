package com.kaii.photos.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.kaii.photos.helpers.grid_management.SelectionManager
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
    Trash,
    Upload,
    Favourite,
    RenameAlbum,
    Move,
    Copy
}

@Entity(tableName = "sync_tasks")
@TypeConverters(SyncTaskConverters::class)
data class SyncTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateModified: Long,
    val status: SyncTaskStatus,
    val type: SyncTaskType,
    val destination: String?,
    val items: List<SelectionManager.SelectedItem>
)

@Suppress("unused")
class SyncTaskConverters {
    @TypeConverter
    fun fromIds(ids: List<String>) = Json.encodeToString(ids)

    @TypeConverter
    fun toIds(ids: String): List<String> = Json.decodeFromString(ids)

    @TypeConverter
    fun fromItems(items: List<SelectionManager.SelectedItem>) = Json.encodeToString(items)

    @TypeConverter
    fun toItems(items: String): List<SelectionManager.SelectedItem> = Json.decodeFromString(items)
}
