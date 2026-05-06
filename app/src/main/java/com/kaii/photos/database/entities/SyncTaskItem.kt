package com.kaii.photos.database.entities

import androidx.room.Entity

@Entity(
    tableName = "sync_task_item",
    primaryKeys = ["mediaId", "taskId"]
)
data class SyncTaskItem(
    val mediaId: Long,
    val taskId: Int
)