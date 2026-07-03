package com.kaii.photos.models.search_page

import android.app.PendingIntent
import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.kaii.photos.PhotosApplication
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.search.SearchManager
import com.kaii.photos.models.BaseViewModel
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
import com.kaii.photos.repositories.TagRepository
import io.github.kaii_lb.lavender.immichintegration.clients.ApiClient
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel(
    context: Context
) : BaseViewModel() {
    override val scope: CoroutineScope = PhotosApplication.appModule.scope
    override val apiClient: ApiClient = PhotosApplication.appModule.apiClient

    private val db = MediaDatabase.getInstance(context.applicationContext)
    override val repo = SearchRepository(
        db = db,
        taggedItemsDao = db.taggedItemsDao(),
        scope = viewModelScope,
        info = ImmichBasicInfo.Empty,
        sortMode = MediaItemSortMode.DateTaken,
        format = DisplayDateFormat.Default,
        client = PhotosApplication.appModule.apiClient
    )

    private val searchManager = SearchManager(
        searchRepo = repo,
        tagRepo = TagRepository(
            dao = MediaDatabase.getInstance(context.applicationContext).tagDao()
        )
    )

    init {
        viewModelScope.launch {
            displayDateFormat.collect { update(format = it) }
        }

        viewModelScope.launch {
            sortMode.collect { update(sortMode = it) }
        }

        viewModelScope.launch {
            immichInfo.collect { update(info = it) }
        }
    }

    val mediaFlow = searchManager.mediaFlow
    val gridMediaFlow = searchManager.gridMediaFlow

    val tags = searchManager.tags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )

    val searchQuery = searchManager.searchQuery.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ""
    )

    val searchMode = searchManager.searchMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = SearchMode.Name
    )

    val searchingForTags = searchManager.searchingForTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    val selectedTags = searchManager.selectedTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun search(
        query: String
    ) = searchManager.search(query)

    private fun update(
        sortMode: MediaItemSortMode? = null,
        format: DisplayDateFormat? = null,
        info: ImmichBasicInfo? = null
    ) = searchManager.update(sortMode, format, info)

    fun setSearchMode(mode: SearchMode) = searchManager.setSearchMode(mode)
    fun setSearchingForTags(value: Boolean) = searchManager.setSearchingForTags(value)
    fun toggleTagSelected(tag: Tag) = searchManager.toggleTagSelected(tag)
    fun clearSelectedTags() = searchManager.clearSelectedTags()

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            searchManager.deleteTag(tag)
        }
    }

    fun clear() = searchManager.clear()

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

            searchManager.copy(context, list, destination, preserveDate.value, null) {
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

            searchManager.move(context, list, destination, preserveDate.value) {
                percentage.floatValue = it.toFloat() / list.size
                body.value = context.resources.getString(
                    R.string.media_move_snackbar_body,
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

    override fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = searchManager.renameItem(context, uri, newName)

    override fun secure(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.secure_encrypting),
                    icon = R.drawable.secure_folder,
                    isLoading = isLoading
                )
            )

            searchManager.secure(context, list)

            isLoading.value = false
        }
    }

    override fun restore(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        scope.launch {
            val isLoading = mutableStateOf(true)

            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvent.LoadingEvent(
                    message = context.resources.getString(R.string.secure_decrypting),
                    icon = R.drawable.unlock,
                    isLoading = isLoading
                )
            )

            searchManager.restore(context, list)

            isLoading.value = false
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

            searchManager.setTrashed(context, list, trashed) {
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
            searchManager.delete(context, list)
        }
    }

    override fun setFavourite(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        favourite: Boolean
    ): PendingIntent? {
        scope.launch {
            searchManager.setFavourite(context, favourite, list)
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

            searchManager.share(context, list)
            isLoading.value = false
        }
    }

    override fun renameAlbum(context: Context, newName: String) {
        throw IllegalAccessError("Cannot rename album in search views!")
    }
}

