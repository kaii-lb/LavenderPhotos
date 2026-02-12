package com.kaii.photos.helpers.grid_management

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.epochToDayStart
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectionManager(
    private val sortMode: MediaItemSortMode,
    private val scope: CoroutineScope,
    private val context: Context,
    private val getMediaInDate: (Long) -> List<SelectedItem>
) {
    data class SelectedItem(
        val id: Long,
        val isImage: Boolean
    ) {
        fun toUri(): Uri {
            val uriParentPath =
                if (isImage) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            return ContentUris.withAppendedId(uriParentPath, id)
        }
    }

    private val _selection = mutableStateMapOf<Long, List<SelectedItem>>()
    val selection = snapshotFlow { _selection.values.flatten() }

    private val _sections = mutableStateListOf<Long>()

    val enabled = selection.map { it.isNotEmpty() }

    val count = selection.map { it.size }

    fun toggle(item: PhotoLibraryUIModel) {
        if (item is PhotoLibraryUIModel.MediaImpl) {
            toggleMedia(item = item.item)
        } else if (item is PhotoLibraryUIModel.Section) {
            toggleSection(timestamp = item.timestamp)
        }
    }

    fun isSelected(item: PhotoLibraryUIModel) =
        if (item is PhotoLibraryUIModel.MediaImpl) {
            _selection[getKey(item)]?.any { it.id == item.item.id } == true
        } else {
            _sections.contains(getKey(item))
        }

    fun clear() {
        _selection.clear()
        _sections.clear()
    }

    fun addAll(items: List<PhotoLibraryUIModel?>) = scope.launch(Dispatchers.IO) {
        // hardcoded android limit for handling uris
        if (_selection.values.flatten().size >= 2000) {
            scope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = context.resources.getString(R.string.media_select_limit_reached),
                        icon = R.drawable.lists,
                        duration = SnackbarDuration.Short
                    )
                )
            }

            return@launch
        }

        val grouped = items.fastMapNotNull { it as? PhotoLibraryUIModel.MediaImpl }.groupBy { getKey(it) }
        grouped.forEach { (key, list) ->
            _selection[key] = (_selection[key] ?: emptyList()).toMutableList().apply {
                val media = list.fastMap {
                    SelectedItem(it.item.id, it.item.type == MediaType.Image)
                }

                _selection[key] = ((_selection[key] ?: emptyList()) + media).distinct()

                val maxCount = getMediaInDate(epochToDayStart(key)).size

                if (media.size == maxCount) {
                    _sections.add(key)
                }
            }
        }
    }

    fun addMedia(item: MediaStoreData) {
        val key = getMediaKey(item)

        add(item, key)
    }

    fun updateSelection(items: List<MediaStoreData>) = scope.launch(Dispatchers.IO) {
        items.groupBy { getMediaKey(it) }.forEach { (key, list) ->
            _selection[key] = list.map { SelectedItem(it.id, it.type == MediaType.Image) }

            val maxCount = getMediaInDate(epochToDayStart(key)).size

            if (list.distinct().size == maxCount) {
                _sections.add(key)
            } else {
                _sections.remove(key)
            }
        }
    }

    private fun getKey(item: PhotoLibraryUIModel) =
        if (item is PhotoLibraryUIModel.MediaImpl) {
            getMediaKey(item.item)
        } else {
            (item as PhotoLibraryUIModel.Section).timestamp
        }

    private fun getMediaKey(item: MediaStoreData) = when {
        sortMode == MediaItemSortMode.MonthTaken -> item.getMonthTaken()

        sortMode.isDateModified -> item.getDateModifiedDay()

        sortMode.isDisabled -> 0

        else -> item.getDateTakenDay()
    }

    private fun toggleMedia(item: MediaStoreData) {
        val key = getMediaKey(item)

        if (_selection[key]?.any { it.id == item.id } == true) {
            remove(item, key)
        } else {
            add(item, key)
        }
    }

    private fun toggleSection(timestamp: Long) = scope.launch(Dispatchers.IO) {
        if (timestamp in _sections) {
            _selection[timestamp] = emptyList()
            _sections.removeAll { it == timestamp }
        } else {
            _selection[timestamp] = getMediaInDate(epochToDayStart(timestamp))
            _sections.add(timestamp)
        }
    }

    private fun add(item: MediaStoreData, key: Long) = scope.launch(Dispatchers.IO) {
        // hardcoded android limit for handling uris
        if (_selection.values.flatten().size >= 2000) {
            scope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvents.MessageEvent(
                        message = context.resources.getString(R.string.media_select_limit_reached),
                        icon = R.drawable.lists,
                        duration = SnackbarDuration.Short
                    )
                )
            }

            return@launch
        }

        val list = (_selection[key] ?: emptyList()) + listOf(SelectedItem(item.id, item.type == MediaType.Image))
        _selection[key] = list.distinct()

        val maxCount = getMediaInDate(epochToDayStart(key)).size

        if (list.distinct().size == maxCount) {
            _sections.add(key)
        }
    }

    private fun remove(item: MediaStoreData, key: Long) {
        _selection[key] = _selection[key]!!.toMutableList().apply { removeIf { it.id == item.id } }

        _sections.remove(key)
    }
}

@Composable
fun rememberSelectionManager(
    paths: Set<String>
): SelectionManager {
    val context = LocalContext.current
    val sortMode by LocalMainViewModel.current.sortMode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val set by rememberUpdatedState(paths)

    return remember(sortMode, context) {
        SelectionManager(
            sortMode = sortMode,
            scope = coroutineScope,
            context = context,
            getMediaInDate = { timestamp ->
                val dao = MediaDatabase.getInstance(context).mediaDao()

                when {
                    // search
                    set.isEmpty() -> dao.mediaInDateRange(timestamp = timestamp, dateModified = sortMode.isDateModified)

                    sortMode.isDateModified -> dao.mediaInDateModified(timestamp = timestamp, paths = paths)

                    else -> dao.mediaInDateTaken(timestamp = timestamp, paths = paths)
                }
            }
        )
    }
}

@Composable
fun rememberSelectionManager(
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>
): SelectionManager {
    val context = LocalContext.current
    val sortMode by LocalMainViewModel.current.sortMode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    return remember(sortMode, context) {
        SelectionManager(
            sortMode = sortMode,
            scope = coroutineScope,
            context = context,
            getMediaInDate = { timestamp ->
                (0..<pagingItems.itemCount).mapNotNull {
                    val item = (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item

                    if (item != null) {
                        val key = when {
                            sortMode == MediaItemSortMode.MonthTaken -> item.getMonthTaken()

                            sortMode.isDateModified -> item.getDateModifiedDay()

                            sortMode.isDisabled -> 0

                            else -> item.getDateTakenDay()
                        }

                        if (key in timestamp..timestamp + 86400) {
                            SelectionManager.SelectedItem(item.id, item.type == MediaType.Image)
                        } else null
                    } else null
                }
            }
        )
    }
}