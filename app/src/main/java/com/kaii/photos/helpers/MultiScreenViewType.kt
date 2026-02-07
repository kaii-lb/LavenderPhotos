package com.kaii.photos.helpers

import com.kaii.photos.datastore.AlbumInfo
import kotlinx.serialization.Serializable

enum class MultiScreenViewType {
    MainScreen,
    AboutAndUpdateView,
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
    ExtendedLicensePage
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
    object Album {
        @Serializable
        data class GridView(
            val albumInfo: AlbumInfo
        )

        @Serializable
        data class SinglePhoto(
            val albumInfo: AlbumInfo,
            val index: Int,
            val nextMediaItemId: Long?
        )
    }

    @Serializable
    object Trash {
        @Serializable
        object GridView

        @Serializable
        data class SinglePhoto(
            val index: Int
        )
    }

    @Serializable
    object Favourites {
        @Serializable
        object GridView

        @Serializable
        object MigrationPage

        @Serializable
        data class SinglePhoto(
            val mediaItemId: Long,
            val nextMediaItemId: Long?
        )
    }

    @Serializable
    object SecureFolder {
        @Serializable
        object GridView

        @Serializable
        data class SinglePhoto(
            val index: Int
        )
    }

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

    @Serializable
    object Immich {
        @Serializable
        data class GridView(
            val albumInfo: AlbumInfo
        )

        @Serializable
        data class SinglePhoto(
            val mediaItemId: Long,
            val albumInfo: AlbumInfo,
            val nextMediaItemId: Long?,
        )
    }

    @Serializable
    object CustomAlbum {
        @Serializable
        data class GridView(
            val albumInfo: AlbumInfo
        )

        @Serializable
        data class SinglePhoto(
            val mediaItemId: Long,
            val albumInfo: AlbumInfo,
            val nextMediaItemId: Long?,
        )
    }
}
