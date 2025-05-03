package com.kaii.photos.helpers

import com.kaii.photos.datastore.AlbumInfo
import kotlinx.serialization.Serializable

enum class MultiScreenViewType {
    MainScreen,
    TrashedPhotoView,
    LockedFolderView,
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
	PrivacyAndSecurity
}

object Screens {
	@Serializable
	data class SinglePhotoView(
		val albumInfo: AlbumInfo,
		val mediaItemId: Long,
		val loadsFromMainViewModel: Boolean
	) {
		fun hasSameAlbumsAs(other: List<String>) = albumInfo.paths.toSet() == other.toSet()
	}

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
	data class EditingScreen(
	    val absolutePath: String,
	    val uri: String,
	    val dateTaken: Long
	)
}
