package com.kaii.photos.database.sync

import android.content.Context
import androidx.core.content.edit

class SyncManager(
    context: Context
) {
    private val prefs = context.getSharedPreferences("media_sync_prefs", Context.MODE_PRIVATE)

    fun getGeneration() = prefs.getLong("last_sync_generation", 0L)

    fun setGeneration(gen: Long) {
        prefs.edit {
            putLong("last_sync_generation", gen)
        }
    }
}