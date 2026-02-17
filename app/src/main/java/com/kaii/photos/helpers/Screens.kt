package com.kaii.photos.helpers

import com.kaii.photos.datastore.AlbumInfo
import kotlinx.serialization.Serializable

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
            val index: Int
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
            val index: Int
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
        val albumInfo: AlbumInfo
    ) : Screens

    @Serializable
    data class VideoEditor(
        val uri: String,
        val absolutePath: String,
        val albumInfo: AlbumInfo
    ) : Screens

    @Serializable
    object Immich : Screens {
        @Serializable
        object InfoPage

        @Serializable
        data class GridView(
            val albumInfo: AlbumInfo
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val albumInfo: AlbumInfo,
            val index: Int
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
            val albumInfo: AlbumInfo
        ) : Screens
    }

    @Serializable
    object MainPages : Screens {
        @Serializable
        object MainGrid : Screens {
            @Serializable
            object GridView : Screens

            @Serializable
            data class SinglePhoto(
                val albumInfo: AlbumInfo,
                val index: Int
            ) : Screens
        }

        @Serializable
        object Search : Screens {
            @Serializable
            data class SinglePhoto(
                val index: Int
            ) : Screens
        }

        @Serializable
        object Trash : Screens

        @Serializable
        object Favourites : Screens
    }

    @Serializable
    object OpenWithView

    @Serializable
    object Settings {
        @Serializable
        object MainPage {
            @Serializable
            object List

            @Serializable
            object General

            @Serializable
            object PrivacyAndSecurity

            @Serializable
            object LookAndFeel

            @Serializable
            object Behaviour

            @Serializable
            object MemoryAndStorage

            @Serializable
            object Debugging
        }

        @Serializable
        object Misc {
            @Serializable
            object DataAndBackup

            @Serializable
            object AboutAndUpdates

            @Serializable
            object UpdatePage

            @Serializable
            object LicensesPage

            @Serializable
            object ExtendedLicensePage
        }
    }
}
