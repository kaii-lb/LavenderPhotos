package com.kaii.photos.helpers

import com.kaii.photos.datastore.AlbumInfo
import kotlinx.serialization.Serializable

enum class MultiScreenViewType {
    MainScreen,
    TrashedPhotoView,
    SecureFolder,
    AboutAndUpdateView,
    FavouritesGridView,
    SettingsMainView,
    SettingsDebuggingView,
    SettingsGeneralView,
    SettingsMemoryAndStorageView,
    SettingsLookAndFeelView,
    OpenWithView,
    UpdatesPage,
    DataAndBackup,
    PrivacyAndSecurity,
    ImmichMainPage,
    LicensePage,
    ExtendedLicensePage
}

object Screens {
    @Serializable
    data class SinglePhotoView(
        val albumInfo: AlbumInfo,
        val mediaItemId: Long,
        val loadsFromMainViewModel: Boolean
    )

    @Serializable
    data class SingleAlbumView(
        val albumInfo: AlbumInfo
    )

    @Serializable
    data class SingleTrashedPhotoView(
        val mediaItemId: Long
    )

    @Serializable
    data class SingleHiddenPhotoView(
        val mediaItemId: Long
    )

    @Serializable
    data class ImageEditor(
        val absolutePath: String,
        val uri: String,
        val dateTaken: Long
    )

    @Serializable
    data class ImmichAlbumPage(
        val albumInfo: AlbumInfo
    )

    @Serializable
    data class VideoEditor(
        val uri: String,
        val absolutePath: String
    )
}
