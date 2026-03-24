package com.kaii.photos.models.search_page

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.Tag
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.file_management.GenericFileManager
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.search.SearchManager
import com.kaii.photos.repositories.SearchMode
import com.kaii.photos.repositories.SearchRepository
import com.kaii.photos.repositories.TagRepository
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SearchViewModel(
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

    val displayDateFormat = settings.lookAndFeel.getDisplayDateFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DisplayDateFormat.Default
    )

    val sortMode = settings.photoGrid.getSortMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MediaItemSortMode.DateTaken
    )

    val immichInfo = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = ImmichBasicInfo.Empty
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

    val topBarDetailsFormat = settings.lookAndFeel.getTopBarDetailsFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = TopBarDetailsFormat.FileName
    )

    val preserveDate = settings.permissions.getPreserveDateOnMove().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    val vibrateOnClick = settings.lookAndFeel.getVibrateOnMediaClick().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = true
    )

    private val db = MediaDatabase.getInstance(context.applicationContext)
    private val searchManager = SearchManager(
        searchRepo = SearchRepository(
            searchDao = db.searchDao(),
            taggedItemsDao = db.taggedItemsDao(),
            scope = viewModelScope,
            info = ImmichBasicInfo.Empty,
            sortMode = MediaItemSortMode.DateTaken,
            format = DisplayDateFormat.Default,
            mediaDao = db.mediaDao(),
            customDao = db.customDao(),
            syncTaskDao = db.taskDao(),
            client = context.appModule.apiClient
        ),
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

    fun allowedAlbumTypesFor(moving: Boolean) = searchManager.allowedAlbumTypesFor(moving)

    suspend fun getExifData(
        context: Context,
        media: MediaStoreData
    ) = searchManager.getExifData(context, media)

    fun runAction(
        context: Context,
        action: GenericFileManager.Action
    ) = when (action) {
        is GenericFileManager.Action.Copy -> {
            copy(
                context = context,
                list = action.list,
                destination = action.destination
            )
        }

        is GenericFileManager.Action.Move -> {
            move(
                context = context,
                list = action.list,
                destination = action.destination
            )
        }

        is GenericFileManager.Action.Trash -> {
            setTrashed(
                context = context,
                list = action.list,
                trashed = action.trashed
            )
        }

        is GenericFileManager.Action.Delete -> {
            delete(
                context = context,
                list = action.list
            )
        }

        is GenericFileManager.Action.Favourite -> {
            setFavourite(
                context = context,
                favourite = action.favourite,
                list = action.list
            )
        }

        is GenericFileManager.Action.RenameItem -> {
            renameItem(
                context = context,
                uri = action.uri,
                newName = action.newName
            )
        }

        else -> null
    }

    private fun copy(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType
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
            }
        }
    }

    private fun move(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
        destination: AlbumType
    ) {
        viewModelScope.launch {
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
            }
        }
    }

    private fun renameItem(
        context: Context,
        uri: String,
        newName: String
    ) = searchManager.renameItem(context, uri, newName)

    private fun setTrashed(
        context: Context,
        list: List<SelectionManager.SelectedItem>,
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
            }
        }
    }

    private fun delete(
        context: Context,
        list: List<SelectionManager.SelectedItem>
    ) {
        viewModelScope.launch {
            searchManager.delete(context, list)
        }
    }

    private fun setFavourite(
        context: Context,
        favourite: Boolean,
        list: List<SelectionManager.SelectedItem>
    ) = if (list.first().isCloud) {
        viewModelScope.launch {
            searchManager.setFavourite(context, favourite, list)
        }
        null
    } else {
        // this is okay since local media's setFavourite is not a blocking function
        runBlocking {
            searchManager.setFavourite(context, favourite, list)
        }
    }
}

