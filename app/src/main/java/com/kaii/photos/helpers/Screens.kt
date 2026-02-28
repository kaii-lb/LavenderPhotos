package com.kaii.photos.helpers

import com.kaii.photos.datastore.AlbumInfo
import kotlinx.serialization.Serializable

interface Screens {
    @Serializable
    object Album {
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
    object Trash {
        @Serializable
        object GridView : Screens

        @Serializable
        data class SinglePhoto(
            val index: Int
        ) : Screens
    }

    @Serializable
    object Favourites {
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
    object SecureFolder {
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
    object Immich {
        @Serializable
        object InfoPage: Screens

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
    object CustomAlbum {
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
    object MainPages {
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
        object MainPage : Screens {
            @Serializable
            object General : Screens

            @Serializable
            object PrivacyAndSecurity : Screens

            @Serializable
            object LookAndFeel : Screens

            @Serializable
            object Behaviour : Screens

            @Serializable
            object MemoryAndStorage : Screens

            @Serializable
            object Debugging : Screens
        }

        @Serializable
        object Misc {
            @Serializable
            object DataAndBackup : Screens

            @Serializable
            object UpdatePage : Screens

            @Serializable
            object LicensesPage : Screens

            @Serializable
            object ExtendedLicensePage : Screens
        }
    }

    @Serializable
    data class TagEditor(val mediaId: Long)
}
