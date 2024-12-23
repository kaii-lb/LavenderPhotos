package com.kaii.photos.helpers

import android.net.Uri
import kotlinx.serialization.Serializable

enum class MultiScreenViewType {
    MainScreen,
    SinglePhotoView,
    SingleAlbumView,
    SingleTrashedPhotoView,
    TrashedPhotoView,
    LockedFolderView,
    SingleHiddenPhotoVew,
    AboutAndUpdateView,
    FavouritesGridView,
    SettingsMainView,
    SettingsDebuggingView,
    SettingsGeneralView,
    SettingsMemoryAndStorageView
}

@Serializable
data class EditingScreen(
    val absolutePath: String,
    val uri: String,
    val dateTaken: Long
)
