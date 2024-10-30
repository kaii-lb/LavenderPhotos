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
    EditingView
}

@Serializable
data class EditingScreen(
    val absolutePath: String,
    val uri: String
)
