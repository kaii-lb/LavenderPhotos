package com.kaii.photos.helpers

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

internal interface NavigationView {
	val albums: List<String>

	fun hasSameAlbumsAs(other: List<String>) = albums.toSet() == other.toSet()
}

object Screens {
	@Serializable
	data class SinglePhotoView(
		override val albums: List<String>,
		val mediaItemId: Long,
		val loadsFromMainViewModel: Boolean
	) : NavigationView

	@Serializable
	data class SingleAlbumView(
		override val albums: List<String>
	) : NavigationView

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
