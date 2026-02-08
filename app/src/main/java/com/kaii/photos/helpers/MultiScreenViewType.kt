package com.kaii.photos.helpers

import com.kaii.photos.datastore.AlbumInfo
import kotlinx.serialization.Serializable

enum class MultiScreenViewType {
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

interface Screens {
    @Serializable
    object Album : Screens {
        @Serializable
        data class GridView(
            val albumInfo: AlbumInfo
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val albumInfo: AlbumInfo,
            val index: Int,
            val nextMediaItemId: Long?
        ) : Screens
    }

    @Serializable
    object Trash : Screens {
        @Serializable
        object GridView : Screens

        @Serializable
        data class SinglePhoto(
            val index: Int
        ) : Screens
    }

    @Serializable
    object Favourites : Screens {
        @Serializable
        object GridView : Screens

        @Serializable
        object MigrationPage : Screens

        @Serializable
        data class SinglePhoto(
            val index: Int,
            val nextMediaItemId: Long?
        ) : Screens
    }

    @Serializable
    object SecureFolder : Screens {
        @Serializable
        object GridView : Screens

        @Serializable
        data class SinglePhoto(
            val index: Int
        ) : Screens
    }

    @Serializable
    data class ImageEditor(
        val absolutePath: String,
        val uri: String,
        val dateTaken: Long,
        val albumInfo: AlbumInfo,
        val type: ScreenType
    ) : Screens

    @Serializable
    data class VideoEditor(
        val uri: String,
        val absolutePath: String,
        val albumInfo: AlbumInfo,
        val type: ScreenType
    ) : Screens

    @Serializable
    object Immich : Screens {
        @Serializable
        data class GridView(
            val albumInfo: AlbumInfo
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val albumInfo: AlbumInfo,
            val index: Int,
            val nextMediaItemId: Long?,
        ) : Screens
    }

    @Serializable
    object CustomAlbum : Screens {
        @Serializable
        data class GridView(
            val albumInfo: AlbumInfo
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val index: Int,
            val albumInfo: AlbumInfo,
            val nextMediaItemId: Long?,
        ) : Screens
    }

    @Serializable
    object MainPages : Screens {
        @Serializable
        object MainGrid : Screens {
            @Serializable
            data class GridView(
                val albumInfo: AlbumInfo
            ) : Screens

            @Serializable
            data class SinglePhoto(
                val albumInfo: AlbumInfo,
                val index: Int,
                val nextMediaItemId: Long?,
            ) : Screens
        }

        @Serializable
        object Secure : Screens

        @Serializable
        object Albums : Screens

        @Serializable
        object Search : Screens {
            @Serializable
            data class SinglePhoto(
                val index: Int,
                val nextMediaItemId: Long?,
            ) : Screens
        }

        @Serializable
        object Trash : Screens

        @Serializable
        object Favourites : Screens
    }
}
