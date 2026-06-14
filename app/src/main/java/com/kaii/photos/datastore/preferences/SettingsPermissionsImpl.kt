package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.kaii.photos.datastore.datastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsPermissionsImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val isMediaManagerKey = booleanPreferencesKey("is_media_manager")
    private val confirmToDelete = booleanPreferencesKey("confirm_to_delete")
    private val preserveDateOnMoveKey = booleanPreferencesKey("permissions_preserve_date_on_move_key")
    private val doNotTrashKey = booleanPreferencesKey("permissions_do_not_trash")
    private val allowSecureFolderScreenCaptureKey = booleanPreferencesKey("permissions_allow_secure_folder_screen_capture")

    fun getIsMediaManager(): Flow<Boolean> =
        context.datastore.data.map {
            it[isMediaManagerKey] == true
        }

    fun setIsMediaManager(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[isMediaManagerKey] = value
        }
    }

    fun getConfirmToDelete(): Flow<Boolean> =
        context.datastore.data.map {
            it[confirmToDelete] != false
        }

    fun setConfirmToDelete(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[confirmToDelete] = value
        }
    }

    fun getPreserveDateOnMove(): Flow<Boolean> =
        context.datastore.data.map {
            it[preserveDateOnMoveKey] != false
        }

    fun setPreserveDateOnMove(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[preserveDateOnMoveKey] = value
        }
    }

    fun getDoNotTrash(): Flow<Boolean> =
        context.datastore.data.map {
            it[doNotTrashKey] == true
        }

    fun setDoNotTrash(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[doNotTrashKey] = value
        }
    }

    /** When true, [android.view.WindowManager.LayoutParams.FLAG_SECURE] is not applied
     * while the secure folder is open, allowing screenshots, screen recording and screen
     * sharing of secure content. Defaults to false so secure content stays protected. */
    fun getAllowSecureFolderScreenCapture(): Flow<Boolean> =
        context.datastore.data.map {
            it[allowSecureFolderScreenCaptureKey] == true
        }

    fun setAllowSecureFolderScreenCapture(value: Boolean) = scope.launch {
        context.datastore.edit {
            it[allowSecureFolderScreenCaptureKey] = value
        }
    }
}