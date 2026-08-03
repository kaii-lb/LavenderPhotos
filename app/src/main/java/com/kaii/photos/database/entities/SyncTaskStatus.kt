package com.kaii.photos.database.entities

import kotlinx.serialization.Serializable

@Serializable
enum class SyncTaskStatus {
    Waiting,
    Synced,
    Failed
}