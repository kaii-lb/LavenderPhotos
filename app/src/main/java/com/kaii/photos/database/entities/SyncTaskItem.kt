package com.kaii.photos.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "sync_task_item",
    primaryKeys = ["mediaId", "taskId"],
    foreignKeys = [
        ForeignKey(
            entity = SyncTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("mediaId")]
)
data class SyncTaskItem(
    val mediaId: Long,
    val taskId: Int,
    val immichId: String? = null
)