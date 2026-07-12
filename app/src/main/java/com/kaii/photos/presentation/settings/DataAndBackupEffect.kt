package com.kaii.photos.presentation.settings

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed interface DataAndBackupEffect {
    data class ShowSnackbar(
        @param:StringRes val message: Int,
        @param:DrawableRes val icon: Int
    ) : DataAndBackupEffect
}

sealed interface DataAndBackupAction {
    data class Save(
        val uri: Uri?
    ) : DataAndBackupAction

    data class Load(
        val uri: Uri?
    ) : DataAndBackupAction
}