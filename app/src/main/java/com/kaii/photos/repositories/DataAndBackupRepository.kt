package com.kaii.photos.repositories

import android.net.Uri
import com.kaii.photos.data.datasources.DataAndBackupDatasource
import com.kaii.photos.domain.Result
import com.kaii.photos.domain.settings.SettingsError

class DataAndBackupRepository(
    private val datasource: DataAndBackupDatasource
) {
    suspend fun save(uri: Uri): Result<Unit, SettingsError> =
        if (datasource.save(uri)) {
            Result.Success(Unit)
        } else {
            Result.Error(SettingsError.IOError)
        }

    suspend fun load(uri: Uri): Result<Unit, SettingsError> =
        if (datasource.load(uri)) {
            Result.Success(Unit)
        } else {
            Result.Error(SettingsError.LoadError)
        }
}