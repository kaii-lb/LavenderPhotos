package com.kaii.photos.helpers

import com.kaii.photos.datastore.AlbumInfo
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

enum class MultiScreenViewType {
    MainScreen,
    SecureFolder,
    AboutAndUpdateView,
    FavouritesGridView,
    SettingsMainView,
    SettingsDebuggingView,
    SettingsGeneralView,
    SettingsMemoryAndStorageView,
    SettingsLookAndFeelView,
    SettingsBehaviourView,
    OpenWithView,
    UpdatesPage,
    DataAndBackup,
    PrivacyAndSecurity,
    ImmichMainPage,
    LicensePage,
    ExtendedLicensePage,
    FavouritesMigrationPage
}

enum class ScreenType {
    Immich,
    Search,
    Favourites,
    Normal
}

object Screens {
    @Serializable
    data class SinglePhotoView(
        val albumInfo: AlbumInfo,
        val mediaItemId: Long,
        val nextMediaItemId: Long?,
        val type: ScreenType
    )

    @Serializable
    data class SingleAlbumView(
        val albumInfo: AlbumInfo
    )

    @Serializable
    object Trash {
        @Serializable
        object TrashedPhotoView

        @Serializable
        data class SingleTrashedPhotoView(
            val mediaItemId: Long
        )
    }


    @Serializable
    data class SingleHiddenPhotoView(
        val mediaItemId: Long
    )

    @Serializable
    data class ImageEditor(
        val absolutePath: String,
        val uri: String,
        val dateTaken: Long,
        val albumInfo: AlbumInfo,
        val type: ScreenType
    )

    @Serializable
    data class VideoEditor(
        val uri: String,
        val absolutePath: String,
        val albumInfo: AlbumInfo,
        val type: ScreenType
    )

    @OptIn(ExperimentalUuidApi::class)
    @Serializable
    data class ImmichAlbum(
        val id: String
    )
}
