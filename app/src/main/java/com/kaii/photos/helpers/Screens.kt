package com.kaii.photos.helpers

import android.net.Uri
import android.os.Bundle
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.widgets.ExpressivePINFieldState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface Screens {
    @Serializable
    object Album : Screens {
        @Serializable
        data class GridView(
            val album: AlbumType.Folder
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val album: AlbumType.Folder,
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
        val file: FileOperationItemMetadata,
        val album: AlbumType
    ) : Screens

    @Serializable
    data class VideoEditor(
        val file: FileOperationItemMetadata,
        val album: AlbumType
    ) : Screens

    @Serializable
    object Immich : Screens {
        @Serializable
        object Dashboard : Screens

        @Serializable
        object Account : Screens

        @Serializable
        object Login : Screens

        @Serializable
        object BackupOptions : Screens

        @Serializable
        data class ShareAlbumPage(
            val albumImmichId: String,
            val albumTitle: String,
            val itemCount: Int,
            val latestImage: String
        ) : Screens

        @Serializable
        data class GridView(
            val album: AlbumType.Cloud
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val album: AlbumType.Cloud,
            val index: Int
        ) : Screens
    }

    @Serializable
    object CustomAlbum : Screens {
        @Serializable
        data class GridView(
            val album: AlbumType.Custom
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val index: Int,
            val album: AlbumType.Custom
        ) : Screens
    }

    @Serializable
    object MainPages : Screens {
        @Serializable
        object MainGrid : Screens {
            @Serializable
            object GridView : Screens

            @Serializable
            object SettingsDialog : Screens

            @Serializable
            data class SinglePhoto(
                val album: AlbumType.Folder,
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
            object PrivacyAndSecurity : Screens {
                @Serializable
                data class ScreenLock(
                    val action: ExpressivePINFieldState.Action,
                    val password: ByteArray?,
                    val salt: ByteArray?
                ) : Screens {
                    override fun equals(other: Any?): Boolean {
                        if (this === other) return true
                        if (javaClass != other?.javaClass) return false

                        other as ScreenLock

                        if (action != other.action) return false
                        if (!password.contentEquals(other.password)) return false
                        if (!salt.contentEquals(other.salt)) return false

                        return true
                    }

                    override fun hashCode(): Int {
                        var result = action.hashCode()
                        result = 31 * result + (password?.contentHashCode() ?: 0)
                        result = 31 * result + (salt?.contentHashCode() ?: 0)
                        return result
                    }
                }
            }

            @Serializable
            object LookAndFeel : Screens {
                @Serializable
                object ColorAndStyle
            }

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

            @Serializable
            object AboutPage : Screens
        }
    }

    @Serializable
    object Startup : Screens {
        @Serializable
        object PermissionsPage : Screens

        @Serializable
        object ProcessingPage : Screens

        @Serializable
        object ScreenLock : Screens

        @Serializable
        object PrivacyModeActive : Screens
    }

    @Serializable
    data class AlbumGroup(
        val id: String,
        val name: String
    ) : Screens

    @Serializable
    object SAFFolder : Screens {
        @Serializable
        data class GridView(
            val album: AlbumType.SAFFolder
        ) : Screens

        @Serializable
        data class SinglePhoto(
            val album: AlbumType.SAFFolder,
            val index: Int
        ) : Screens
    }

    class FileOperationItemMetadataNavType : androidx.navigation.NavType<FileOperationItemMetadata>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): FileOperationItemMetadata? {
            return bundle.getString(key)?.let { Json.decodeFromString<FileOperationItemMetadata>(it) }
        }

        override fun parseValue(value: String): FileOperationItemMetadata {
            return Json.decodeFromString(Uri.decode(value))
        }

        override fun put(bundle: Bundle, key: String, value: FileOperationItemMetadata) {
            bundle.putString(key, Json.encodeToString(value))
        }

        override fun serializeAsValue(value: FileOperationItemMetadata): String {
            return Uri.encode(Json.encodeToString(value))
        }
    }
}
