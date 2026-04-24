package com.kaii.photos.models.multi_album

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
import com.kaii.photos.repositories.HybridRepository
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MultiAlbumViewModel(
    album: AlbumType.Folder,
    context: Context,
    override val scope: CoroutineScope = context.appModule.scope,
    override val apiClient: ApiClient = context.appModule.apiClient
) : BaseViewModel(context) {
    private val db = MediaDatabase.getInstance(context.applicationContext)
    override val repo =
        HybridRepository(
            mediaDao = db.mediaDao(),
            customDao = db.customDao(),
            syncTaskDao = db.taskDao(),
            client = context.appModule.apiClient,
            initialAlbum = album,
            scope = viewModelScope,
            info = immichInfo,
            sortMode = sortMode,
            format = displayDateFormat
        )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    fun changeAlbum(
        album: AlbumType.Folder
    ) = repo.changeAlbum(album = album)

    fun editAlbum(id: String, newInfo: AlbumType) {
        settings.albums.edit(id, newInfo)
    }

    fun removeAlbum(id: String) {
        settings.albums.remove(id)
    }

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

    override fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        origin: AlbumType,
        destination: AlbumType
    ) {
        scope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_move_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_move_snackbar_title),
                    body = body,
                    icon = R.drawable.cut,
                    percentage = percentage
                )
            )

            repo.move(context, list, null, destination, preserveDate.value) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_move_snackbar_body,
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

    override fun renameAlbum(
        context: Context,
        newName: String
    ) {
        scope.launch {
            repo.renameAlbum(context, newName)
        }
    }

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

    override fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            repo.delete(context, list)
        }
    }

    override fun setFavourite(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        favourite: Boolean
    ) = runBlocking { repo.setFavourite(context, favourite, list) } // this is okay since local media's setFavourite is not a blocking function

    override fun share(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            repo.share(context, list)
        }
    }
}

