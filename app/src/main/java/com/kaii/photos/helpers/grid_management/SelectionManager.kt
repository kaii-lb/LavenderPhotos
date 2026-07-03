package com.kaii.photos.helpers.grid_management

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.kaii.photos.R
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.epochToDayStart
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.mediastore.MediaType
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class SelectionManager(
    private var sortMode: MediaItemSortMode,
    private val scope: CoroutineScope,
    private val context: Context,
    private val getMediaInDate: suspend (Long) -> Map<Long, SelectedItem>
) {
    @Serializable
    data class SelectedItem(
        val id: Long,
        val uri: String,
        val immichUrl: String?,
        val isImage: Boolean,
        val parentPath: String
    ) {
        val immichId: String?
            get() = immichUrl?.split("/")?.dropLast(1)?.last()

        val isCloud: Boolean
            get() = uri.startsWith("/api")
    }

    private var _selection by mutableStateOf<Map<Long, Map<Long, SelectedItem>>>(emptyMap())
    val selection = snapshotFlow { _selection.values.flatMap { it.values } }

    private var _sections by mutableStateOf<List<Long>>(emptyList())

    private var manualEnable by mutableStateOf(false)
    val enabled = snapshotFlow { _selection.values.any { it.values.isNotEmpty() } || manualEnable }

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
            _selection[getKey(item)]?.containsKey(item.item.id) == true
        } else {
            _sections.contains(getKey(item))
        }

    fun enterSelectMode() {
        manualEnable = true
    }

    fun clear() {
        _selection = emptyMap()
        _sections = emptyList()
        manualEnable = false
    }

    fun addAll(
        items: List<PhotoLibraryUIModel?>
    ) = scope.launch(Dispatchers.IO) {
        // hardcoded android limit for handling uris
        if (_selection.values.sumOf { it.values.size } >= 2000) {
            scope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
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
            snapshot[key] = (snapshot[key] ?: emptyMap()).toMutableMap().apply {
                val media = list.associate {
                    it.item.id to
                            SelectedItem(
                                id = it.item.id,
                                uri = it.item.uri,
                                immichUrl = it.item.immichUrl,
                                isImage = it.item.type == MediaType.Image,
                                parentPath = it.item.parentPath
                            )
                }

                snapshot[key] = ((snapshot[key] ?: emptyMap()) + media)

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

            val concurrentMap = ConcurrentHashMap(snapshot[key] ?: emptyMap())
            snapshot[key] = concurrentMap.apply {
                val ids = list.fastMap { it.id }
                values.forEach {
                    if (it.id in ids) remove(it.id)
                }
            }
        }

        added.groupBy { getMediaKey(it) }.forEach { (key, list) ->
            snapshot[key] = (snapshot[key] ?: emptyMap()) + list.associate {
                it.id to
                        SelectedItem(
                            id = it.id,
                            uri = it.uri,
                            immichUrl = it.immichUrl,
                            isImage = it.type == MediaType.Image,
                            parentPath = it.parentPath
                        )
            }
            snapshot[key] = snapshot[key]!!

            val maxCount = getMediaInDate(epochToDayStart(key)).size

            if (snapshot[key]!!.size == maxCount) {
                sections.add(key)
            } else {
                sections.remove(key)
            }
        }

        // hardcoded android limit for handling uris
        if (snapshot.values.sumOf { it.values.size } >= 2000) {
            scope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
                        message = context.resources.getString(R.string.media_select_limit_reached),
                        icon = R.drawable.lists,
                        duration = SnackbarDuration.Short
                    )
                )
            }

            return@launch
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

        // sortMode.isDisabled -> 0 // seems to cause animation state issues

        else -> item.getDateTakenDay()
    }

    private fun toggleMedia(item: MediaStoreData) {
        val key = getMediaKey(item)

        if (_selection[key]?.containsKey(item.id) == true) {
            remove(item, key)
        } else {
            add(item, key)
        }
    }

    private fun toggleSection(timestamp: Long) = scope.launch(Dispatchers.IO) {
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableList()

        if (timestamp in sections) {
            snapshot[timestamp] = emptyMap()
            sections.removeAll { it == timestamp }
        } else {
            snapshot[timestamp] = getMediaInDate(epochToDayStart(timestamp))

            // hardcoded android limit for handling uris
            if (snapshot[timestamp]!!.size >= 2000) {
                scope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvent.MessageEvent(
                            message = context.resources.getString(R.string.media_select_limit_reached),
                            icon = R.drawable.lists,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }

            sections.add(timestamp)
        }

        _selection = snapshot
        _sections = sections
    }

    private fun add(
        item: MediaStoreData,
        key: Long
    ) = scope.launch(Dispatchers.IO) {
        // hardcoded android limit for handling uris
        if (_selection.values.sumOf { it.values.size } >= 2000) {
            scope.launch {
                LavenderSnackbarController.pushEvent(
                    LavenderSnackbarEvent.MessageEvent(
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

        val list = (snapshot[key] ?: emptyMap()) + mapOf(
            item.id to
                    SelectedItem(
                        id = item.id,
                        uri = item.uri,
                        immichUrl = item.immichUrl,
                        isImage = item.type == MediaType.Image,
                        parentPath = item.parentPath
                    )
        )
        snapshot[key] = list

        val maxCount = getMediaInDate(epochToDayStart(key)).size

        if (list.size == maxCount) {
            sections.add(key)
        }

        _selection = snapshot
        _sections = sections
    }

    private fun remove(item: MediaStoreData, key: Long) {
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableList()

        snapshot[key] = (snapshot[key] ?: emptyMap()).toMutableMap().apply {
            remove(item.id)
        }
        sections.remove(key)

        _selection = snapshot
        _sections = sections
    }

    fun setSortMode(mode: MediaItemSortMode) {
        sortMode = mode
    }
}