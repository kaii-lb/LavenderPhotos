package com.kaii.photos.datastore.preferences

import android.content.Context
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.datastore
import com.kaii.photos.helpers.EncryptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsImmichImpl(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val immichEncryptionIV = byteArrayPreferencesKey("immich_encryption_iv")
    private val immichEndpoint = stringPreferencesKey("immich_endpoint")
    private val immichToken = byteArrayPreferencesKey("immich_token")
    private val username = stringPreferencesKey("immich_username")
    private val userId = stringPreferencesKey("immich_user_id")
    private val updatedAt = stringPreferencesKey("immich_updated_at")

    fun getImmichBasicInfo() = context.datastore.data.map { data ->
        val endpoint = data[immichEndpoint] ?: return@map ImmichBasicInfo.Empty
        val token = data[immichToken] ?: return@map ImmichBasicInfo.Empty
        val iv = data[immichEncryptionIV] ?: return@map ImmichBasicInfo.Empty
        val username = data[username] ?: return@map ImmichBasicInfo.Empty
        val userId = data[userId] ?: return@map ImmichBasicInfo.Empty
        val updatedAt = data[updatedAt] ?: return@map ImmichBasicInfo.Empty

        val decToken = EncryptionManager.decryptBytes(
            bytes = token,
            iv = iv
        )

        ImmichBasicInfo(
            endpoint = endpoint,
            accessToken = decToken.decodeToString(),
            username = username,
            userId = userId,
            updatedAt = updatedAt
        )
    }

    fun setImmichBasicInfo(info: ImmichBasicInfo) = scope.launch {
        context.datastore.edit { data ->
            val (enc, iv) = EncryptionManager.encryptBytes(info.accessToken.toByteArray())

            data[username] = info.username
            data[immichEndpoint] = info.endpoint
            data[immichToken] = enc
            data[immichEncryptionIV] = iv
            data[userId] = info.userId
            data[updatedAt] = info.updatedAt
        }
    }

    fun setUsername(name: String) = scope.launch {
        context.datastore.edit { data ->
            data[username] = name
        }
    }

    fun setUpdatedAt(date: String) = scope.launch {
        context.datastore.edit { data ->
            data[updatedAt] = date
        }
    }
}