package com.kaii.photos.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.kaii.photos.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsLogs(private val viewModelScope: CoroutineScope, private val context: Context) {
    private val recordLogsKey = booleanPreferencesKey("record_logs")

    val recordLogs = context.datastore.data.map {
        it[recordLogsKey] ?: false
    }

    fun setRecordLogs(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[recordLogsKey] = value
        }
    }
}

class SettingsPermissions(private val viewModelScope: CoroutineScope, private val context: Context) {
    private val isMediaManagerKey = booleanPreferencesKey("is_media_manager")

    val isMediaManager = context.datastore.data.map {
        it[isMediaManagerKey] ?: false
    }

    fun setIsMediaManager(value: Boolean) = viewModelScope.launch {
        context.datastore.edit {
            it[isMediaManagerKey] = value
        }
    }
}