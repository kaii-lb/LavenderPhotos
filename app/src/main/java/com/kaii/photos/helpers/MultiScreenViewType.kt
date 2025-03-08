package com.kaii.photos.helpers

import com.kaii.photos.compose.ViewProperties
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
	DataAndBackup
}


object Screens {
	@Serializable
	data class SinglePhotoView(
	    val albums: List<String>,
	    val mediaItemId: Long,
		val loadsFromMainViewModel: Boolean
	)

	@Serializable
	data class SingleAlbumView(
		val albums: List<String>
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
