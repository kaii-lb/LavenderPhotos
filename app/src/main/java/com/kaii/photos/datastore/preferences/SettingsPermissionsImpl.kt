package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
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
    private val passwordKey = byteArrayPreferencesKey("permissions_password_key")
    private val saltKey = byteArrayPreferencesKey("permissions_password_salt_key")

    suspend fun migrate() {
        context.datastore.edit { data ->
            val oldPasswordKey = byteArrayPreferencesKey("permissions_password")
            val oldSaltKey = byteArrayPreferencesKey("permissions_key")

            val oldPassword = data[oldPasswordKey]?.takeIf { it.isNotEmpty() }
            val oldSalt = data[oldSaltKey]?.takeIf { it.isNotEmpty() }

            if (oldPassword != null && oldSalt != null) {
                data[passwordKey] = oldPassword
                data[saltKey] = oldSalt

                data.remove(oldPasswordKey)
                data.remove(oldSaltKey)
            }
        }
    }

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

    fun getPassword() = context.datastore.data.map { data ->
        data[passwordKey]?.takeIf {
            it.isNotEmpty()
        }
    }

    fun setPassword(password: ByteArray?) = scope.launch {
        context.datastore.edit {
            it[passwordKey] = password ?: ByteArray(0)
        }
    }

    fun getSalt() = context.datastore.data.map { data ->
        data[saltKey]?.takeIf {
            it.isNotEmpty()
        }
    }

    fun setSalt(salt: ByteArray?) = scope.launch {
        context.datastore.edit {
            it[saltKey] = salt ?: ByteArray(0)
        }
    }
}