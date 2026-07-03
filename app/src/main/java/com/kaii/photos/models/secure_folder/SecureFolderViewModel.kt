package com.kaii.photos.models.secure_folder

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.BaseViewModel
import com.kaii.photos.repositories.SecureRepository
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SecureFolderViewModel(
    context: Context
) : BaseViewModel() {
    override val scope: CoroutineScope = PhotosApplication.appModule.scope
    override val apiClient: ApiClient = PhotosApplication.appModule.apiClient

    override val repo = SecureRepository(
        context = context,
        scope = viewModelScope,
        sortMode = settings.photoGrid.getSortMode(),
        format = settings.lookAndFeel.getDisplayDateFormat(),
        info = settings.immich.getImmichBasicInfo()
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    val selectionManager = SelectionManager(
        sortMode = MediaItemSortMode.DateModified,
        scope = viewModelScope,
        context = context,
        getMediaInDate = { timestamp ->
            repo.getItemsForDate(timestamp, sortMode.value)
        }
    )

    init {
        repo.attachFileObserver()

        viewModelScope.launch {
            sortMode.collect {
                selectionManager.setSortMode(it)
            }
        }
    }

    override fun onCleared() {
        repo.detachFileObserver()
    }

    override fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        PhotosApplication.appModule.scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.secure_decrypting),
                    icon = R.drawable.unlock,
                    isLoading = isLoading
                )
            )

            repo.restore(context, list)

            isLoading.value = false
        }
    }

    override fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        PhotosApplication.appModule.scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.secure_decrypting),
                    icon = R.drawable.unlock,
                    isLoading = isLoading
                )
            )

            repo.share(context, list)

            isLoading.value = false
        }
    }

    override fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        PhotosApplication.appModule.scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.media_delete_snackbar_title),
                    icon = R.drawable.delete,
                    isLoading = isLoading
                )
            )

            repo.delete(context, list)

            isLoading.value = false
        }
    }

    override fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType
    ) {
        throw NotImplementedError("Cannot copy items in secure folder")
    }

    override fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType,
        destination: AlbumType
    ) {
        throw NotImplementedError("Cannot move items in secure folder")
    }

    override fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean
    ) {
        throw NotImplementedError("Cannot trash items in secure folder")
    }

    override fun setFavourite(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        favourite: Boolean
    ): PendingIntent? {
        throw NotImplementedError("Cannot favourite items in secure folder")
    }

    override fun renameAlbum(context: Context, newName: String) {
        throw NotImplementedError("Cannot rename the secure folder")
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        throw NotImplementedError("Cannot rename items in secure folder")
    }

    override fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        throw NotImplementedError("Cannot secure already secured items")
    }
}