package com.kaii.photos.models.favourites_grid

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.file_management.GenericFileManager
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.repositories.FavouritesRepository
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavouritesViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings

    val useBlackBackground = settings.lookAndFeel.getUseBlackBackgroundForViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val confirmToDelete = settings.permissions.getConfirmToDelete().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val doNotTrash = settings.permissions.getDoNotTrash().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val preserveDate = settings.permissions.getPreserveDateOnMove().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val columnSize = settings.lookAndFeel.getColumnSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 3
    )

    val openVideosExternally = settings.behaviour.getOpenVideosExternally().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val cacheThumbnails = settings.storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val thumbnailSize = settings.storage.getThumbnailSize().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = 256
    )

    val useRoundedCorners = settings.lookAndFeel.getUseRoundedCorners().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TopBarDetailsFormat.FileName
    )

    val blurViews = settings.lookAndFeel.getBlurViews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    val useCache = settings.storage.getCacheThumbnails().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    private val db = MediaDatabase.getInstance(context.applicationContext)
    private val repo = FavouritesRepository(
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

    fun allowedAlbumTypesFor(
        action: GenericFileManager.Action
    ) = repo.allowedAlbumTypesFor(action)

    fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        targetAlbumId: String,
        overrideDisplayName: ((displayName: String) -> String)?
    ) {
        viewModelScope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_copy_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_operate_snackbar_body),
                    body = body,
                    icon = R.drawable.content_paste,
                    percentage = percentage
                )
            )

            repo.copy(context, list, targetAlbumId, preserveDate.value, overrideDisplayName) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_copy_snackbar_body,
                    it, list.size
                )
            }
        }
    }

    fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) {
        viewModelScope.launch {
            repo.renameItem(context, uri, newName)
        }
    }

    fun setTrashed(
        context: Context,
        list: List<String>,
        trashed: Boolean
    ) {
        viewModelScope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_delete_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_operate_snackbar_body),
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
            }
        }
    }

    fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<String>
    ) {
        viewModelScope.launch {
            val percentage = mutableFloatStateOf(0f)
            val body = mutableStateOf(
                context.resources.getString(
                    R.string.media_restore_snackbar_body,
                    0, list.size
                )
            )

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.ProgressEvent(
                    message = context.resources.getString(R.string.media_operate_snackbar_body),
                    body = body,
                    icon = R.drawable.untrash,
                    percentage = percentage
                )
            )

            repo.setFavourite(context, favourite, list) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_restore_snackbar_body,
                    it, list.size
                )
            }
        }
    }
}
