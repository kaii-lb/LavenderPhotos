package com.kaii.photos.models.data_and_backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.domain.Result
import com.kaii.photos.presentation.settings.DataAndBackupAction
import com.kaii.photos.presentation.settings.DataAndBackupEffect
import com.kaii.photos.repositories.DataAndBackupRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DataAndBackupViewModel(
    private val repo: DataAndBackupRepository
) : ViewModel() {
    private val channel = Channel<DataAndBackupEffect>(Channel.CONFLATED)
    val effects = channel.receiveAsFlow()

    fun onAction(action: DataAndBackupAction) {
        when (action) {
            is DataAndBackupAction.Load -> {
                load(action.uri)
            }

            is DataAndBackupAction.Save -> {
                save(action.uri)
            }
        }
    }

    private fun save(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null || repo.save(uri) is Result.Error) {
                channel.send(
                    DataAndBackupEffect.ShowSnackbar(
                        message = R.string.export_settings_backup_save_error,
                        icon = R.drawable.error_2
                    )
                )
            } else {
                channel.send(
                    DataAndBackupEffect.ShowSnackbar(
                        message = R.string.export_settings_backup_save_success,
                        icon = R.drawable.storage
                    )
                )
            }
        }
    }

    private fun load(uri: Uri?) {
        viewModelScope.launch {
            if (uri == null || repo.load(uri) is Result.Error) {
                channel.send(
                    DataAndBackupEffect.ShowSnackbar(
                        message = R.string.export_settings_backup_load_error,
                        icon = R.drawable.error_2
                    )
                )
            } else {
                channel.send(
                    DataAndBackupEffect.ShowSnackbar(
                        message = R.string.export_settings_backup_save_success,
                        icon = R.drawable.storage
                    )
                )
            }
        }
    }
}