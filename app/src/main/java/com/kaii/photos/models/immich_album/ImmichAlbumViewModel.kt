package com.kaii.photos.models.immich_album

import android.app.PendingIntent
import android.content.Context
import android.content.IntentSender
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.models.BaseViewModel
import com.kaii.photos.repositories.ImmichRepository
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ImmichAlbumViewModel(
    context: Context,
    private val album: AlbumType.Cloud,
    db: MediaDatabase = PhotosApplication.appModule.db,
    override val scope: CoroutineScope = PhotosApplication.appModule.scope,
    override val apiClient: ApiClient = PhotosApplication.appModule.apiClient
) : BaseViewModel() {
    override val repo = ImmichRepository(
        db = db,
        album = album,
        scope = viewModelScope,
        sortMode = settings.photoGrid.getSortMode(),
        format = settings.lookAndFeel.getDisplayDateFormat(),
        info = settings.immich.getImmichBasicInfo(),
        client = apiClient
    )

    val mediaFlow = repo.mediaFlow
    val gridMediaFlow = repo.gridMediaFlow

    var selectionManager by mutableStateOf(createSelectionManager(context.applicationContext, sortMode.value, album.id))
        private set

    init {
        viewModelScope.launch {
            launch {
                while (true) {
                    refresh()
                    delay(5000.milliseconds)
                }
            }

            launch {
                sortMode.collect {
                    selectionManager.setSortMode(it)
                }
            }
        }
    }

    fun refresh() = repo.refresh()

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
                delay(1000.milliseconds)
                if (success) {
                    percentage.floatValue = 1f
                    body.value = context.resources.getString(
                        R.string.media_copy_snackbar_body,
                        list.size, list.size
                    )
                } else {
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

            repo.setTrashed(context, list, trashed, null, null) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_delete_snackbar_body,
                    it, list.size
                )
            }.let { success ->
                delay(1000.milliseconds)
                if (success) {
                    percentage.floatValue = 1f
                    body.value = context.resources.getString(
                        R.string.media_copy_snackbar_body,
                        list.size, list.size
                    )
                } else {
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
    ): PendingIntent? {
        scope.launch {
            repo.setFavourite(context, favourite, list)
        }

        return null
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
        throw IllegalAccessError("Cannot move items in an immich album!")
    }

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ): IntentSender? {
        throw IllegalAccessError("Cannot rename items in an immich album!")
    }

    override fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        throw NotImplementedError("Secure folder functionality is not implemented in immich contexts")
    }

    override fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        throw NotImplementedError("Secure folder functionality is not implemented in immich contexts")
    }
}