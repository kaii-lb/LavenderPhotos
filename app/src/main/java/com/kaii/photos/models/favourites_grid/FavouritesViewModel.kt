package com.kaii.photos.models.favourites_grid

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.BaseViewModel
import com.kaii.photos.repositories.FavouritesRepository
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FavouritesViewModel(
    context: Context,
    override val scope: CoroutineScope = context.appModule.scope,
    override val apiClient: ApiClient = context.appModule.apiClient
) : BaseViewModel(context) {
    private val db = MediaDatabase.getInstance(context.applicationContext)
    override val repo = FavouritesRepository(
        mediaDao = db.mediaDao(),
        customDao = db.customDao(),
        syncTaskDao = db.taskDao(),
        client = context.applicationContext.appModule.apiClient,
        scope = viewModelScope,
        info = settings.immich.getImmichBasicInfo(),
        sortMode = settings.photoGrid.getSortMode(),
        format = settings.lookAndFeel.getDisplayDateFormat()
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    override fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType
    ) {
        scope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_copy_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_copy_snackbar_title),
                    body = body,
                    icon = R.drawable.content_paste,
                    percentage = percentage
                )
            )

            repo.copy(context, list, destination, preserveDate.value, null) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_copy_snackbar_body,
                    it, list.size
                )
            }.let { success ->
                if (!success) {
                    delay(1000)
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = context.resources.getString(R.string.media_snackbar_operation_failed),
                            icon = R.drawable.delete,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = repo.renameItem(context, uri, newName)

    override fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        trashed: Boolean
    ) {
        scope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_delete_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_delete_snackbar_title),
                    body = body,
                    icon = R.drawable.delete,
                    percentage = percentage
                )
            )

            repo.setTrashed(context, list, trashed) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_delete_snackbar_body,
                    it, list.size
                )
            }.let { success ->
                if (!success) {
                    delay(1000)
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = context.resources.getString(R.string.media_snackbar_operation_failed),
                            icon = R.drawable.delete,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }

    override fun setFavourite(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        favourite: Boolean
    ) = if (list.first().isCloud) {
        scope.launch {
            repo.setFavourite(context, favourite, list)
        }
        null
    } else {
        // this is okay since local media's setFavourite is not a blocking function
        runBlocking {
            repo.setFavourite(context, favourite, list)
        }
    }

    override fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            repo.delete(context, list)
        }
    }

    override fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            val isLoading = mutableStateOf(true)

            if (list.any { it.isCloud }) {
                LavenderSnackbarController.pushEvent(
                    event = LavenderSnackbarEvent.LoadingEvent(
                        message = context.resources.getString(R.string.media_sharing),
                        icon = R.drawable.share,
                        isLoading = isLoading
                    )
                )
            }

            repo.share(context, list)
            isLoading.value = false
        }
    }

    override fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType,
        destination: AlbumType
    ) {
        throw IllegalAccessError("Cannot move favourites!")
    }

    override fun renameAlbum(context: Context, newName: String) {
        throw IllegalAccessError("Cannot rename album in favourites!")
    }
}
