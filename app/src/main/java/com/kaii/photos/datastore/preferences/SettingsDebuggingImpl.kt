package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.kaii.photos.datastore.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsDebuggingImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val recordLogsKey = booleanPreferencesKey("debugging_record_logs")

    fun getRecordLogs(): Flow<Boolean> =
        context.datastore.data.map {
            it[recordLogsKey] != false
        }

    fun setRecordLogs(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[recordLogsKey] = value
        }
    }
}