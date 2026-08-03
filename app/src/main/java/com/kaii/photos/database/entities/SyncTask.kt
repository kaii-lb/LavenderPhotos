package com.kaii.photos.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "sync_tasks")
@TypeConverters(SyncOperationConverters::class)
data class SyncTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val createdAt: Long,
    val status: SyncTaskStatus,
    val operation: SyncOperation,
    val isRemoval: Boolean,
    val attempts: Int = 0,
    val lastError: String? = null
) {
    companion object {
        const val MAX_ATTEMPTS = 6
    }
}

