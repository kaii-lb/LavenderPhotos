package com.kaii.photos.helpers.grid_management

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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

    private var _selection by mutableStateOf<Map<Long, List<SelectedItem>>>(emptyMap())
    val selection = snapshotFlow { _selection.values.flatten() }

    private var _sections by mutableStateOf<List<Long>>(emptyList())

    val enabled = selection.map { it.isNotEmpty() }

    @OptIn(FlowPreview::class)
    val count = selection.map { it.size }.debounce(25.milliseconds)

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
        _selection = emptyMap()
        _sections = emptyList()
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

        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableList()
        val grouped = items.fastMapNotNull { it as? PhotoLibraryUIModel.MediaImpl }.groupBy { getKey(it) }

        grouped.forEach { (key, list) ->
            snapshot[key] = (snapshot[key] ?: emptyList()).toMutableList().apply {
                val media = list.fastMap {
                    SelectedItem(it.item.id, it.item.type == MediaType.Image)
                }

                snapshot[key] = ((snapshot[key] ?: emptyList()) + media).distinct()

                val maxCount = getMediaInDate(epochToDayStart(key)).size

                if (media.size == maxCount) {
                    sections.add(key)
                }
            }
        }

        _selection = snapshot
        _sections = sections
    }

    fun addMedia(item: MediaStoreData) {
        val key = getMediaKey(item)

        add(item, key)
    }

    fun updateSelection(
        added: List<MediaStoreData>,
        removed: List<MediaStoreData>
    ) = scope.launch(Dispatchers.IO) {
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableList()

        removed.groupBy { getMediaKey(it) }.forEach { (key, list) ->
            sections.remove(key)
            snapshot[key] = snapshot[key]?.toMutableList()?.apply { removeAll { item -> item.id in list.fastMap { it.id } } } ?: emptyList()
        }

        added.groupBy { getMediaKey(it) }.forEach { (key, list) ->
            snapshot[key] = (snapshot[key] ?: emptyList()) + list.map { SelectedItem(it.id, it.type == MediaType.Image) }
            snapshot[key] = snapshot[key]!!.distinct()

            val maxCount = getMediaInDate(epochToDayStart(key)).size

            if (snapshot[key]!!.size == maxCount) {
                sections.add(key)
            } else {
                sections.remove(key)
            }
        }

        _selection = snapshot
        _sections = sections
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
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableList()

        if (timestamp in sections) {
            snapshot[timestamp] = emptyList()
            sections.removeAll { it == timestamp }
        } else {
            snapshot[timestamp] = getMediaInDate(epochToDayStart(timestamp))
            sections.add(timestamp)
        }

        _selection = snapshot
        _sections = sections
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

        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableList()

        val list = (snapshot[key] ?: emptyList()) + listOf(SelectedItem(item.id, item.type == MediaType.Image))
        snapshot[key] = list.distinct()

        val maxCount = getMediaInDate(epochToDayStart(key)).size

        if (list.distinct().size == maxCount) {
            sections.add(key)
        }

        _selection = snapshot
        _sections = sections
    }

    private fun remove(item: MediaStoreData, key: Long) {
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableList()

        snapshot[key] = snapshot[key]!!.toMutableList().apply { removeIf { it.id == item.id } }
        sections.remove(key)

        _selection = snapshot
        _sections = sections
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

                    else -> dao.mediaInDateRange(timestamp = timestamp, paths = paths, dateModified = sortMode.isDateModified)
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