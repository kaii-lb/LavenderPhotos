package com.kaii.photos.helpers.grid_management

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.database.entities.epochToDayStart
import com.kaii.photos.domain.files.FileOperationItemMetadata
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.presentation.selection.SelectionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds

class SelectionManager(
    @Volatile
    private var sortMode: MediaItemSortMode,
    private val scope: CoroutineScope,
    private val getMediaInDate: suspend (Long, MediaItemSortMode) -> Map<Long, SelectedItem>
) {
    @Serializable
    data class SelectedItem(
        val id: Long,
        val uri: String,
        val immichUrl: String?,
        val isImage: Boolean,
        val absolutePath: String,
        val parentPath: String
    ) {
        val immichId: String?
            get() = immichUrl?.split("/")?.dropLast(1)?.last()

        val isCloud: Boolean
            get() = uri.startsWith("/api")

        fun toFileOperationMetadata() =
            FileOperationItemMetadata(
                id = id,
                uri = uri,
                absolutePath = absolutePath,
                isImage = isImage,
                immichUrl = immichUrl,
                parentPath = parentPath
            )
    }

    @Volatile
    private var singleSelectMode = false

    private val timeZone = TimeZone.getDefault()
    private val mutationMutex = Mutex()
    private val selectionLock = Any()
    private var selectionGeneration = 0L

    private val eventChannel = Channel<SelectionEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    private var _selection by mutableStateOf<Map<Long, Map<Long, SelectedItem>>>(emptyMap())
    val selection = snapshotFlow { _selection.values.flatMap { it.values } }

    private var _sections by mutableStateOf<Set<Long>>(emptySet())

    private var manualEnable by mutableStateOf(false)
    val enabled = snapshotFlow { manualEnable || _selection.values.any { it.values.isNotEmpty() } }

    @OptIn(FlowPreview::class)
    val count = snapshotFlow { _selection.values.sumOf { it.values.size } }.debounce(25.milliseconds)

    fun toggle(item: PhotoLibraryUIModel) {

        if (item is PhotoLibraryUIModel.MediaImpl) {
            if (singleSelectMode) {
                val wasSelected = isSelected(item)
                clear()
                if (!wasSelected) toggleMedia(item = item.item)
            } else {
                toggleMedia(item = item.item)
            }
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
        synchronized(selectionLock) {
            selectionGeneration += 1
            _selection = emptyMap()
            _sections = emptySet()
            manualEnable = false
        }
    }

    fun setSingleSelectModeActive(active: Boolean) {
        singleSelectMode = active
    }

    fun addAll(
        items: List<PhotoLibraryUIModel?>
    ) = launchSelectionMutation { generation ->
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableSet()
        val grouped = items.fastMapNotNull { it as? PhotoLibraryUIModel.MediaImpl }.groupBy { getKey(it) }

        grouped.forEach { (key, list) ->
            val media = list.associate {
                it.item.id to SelectedItem(
                    id = it.item.id, uri = it.item.uri, immichUrl = it.item.immichUrl,
                    isImage = it.item.type == MediaType.Image,
                    absolutePath = it.item.absolutePath, parentPath = it.item.parentPath
                )
            }

            val merged = (snapshot[key] ?: emptyMap()) + media
            snapshot[key] = merged

            val maxCount = getMediaInDate(epochToDayStart(key, timeZone), sortMode).size
            if (merged.size == maxCount) sections.add(key)
        }

        publishSelection(generation, snapshot, sections)
    }

    fun addMedia(item: MediaStoreData) {
        val key = getMediaKey(item)

        add(item, key)
    }

    fun updateSelection(
        added: List<MediaStoreData>,
        removed: List<MediaStoreData>
    ) = launchSelectionMutation { generation ->
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableSet()

        removed.groupBy { getMediaKey(it) }.forEach { (key, list) ->
            sections.remove(key)

            val ids = list.fastMap { it.id }.toSet()
            val updated = (snapshot[key] ?: emptyMap()).filterKeys { it !in ids }
            if (updated.isEmpty()) snapshot.remove(key) else snapshot[key] = updated
        }

        added.groupBy { getMediaKey(it) }.forEach { (key, list) ->
            snapshot[key] = (snapshot[key] ?: emptyMap()) + list.associate {
                it.id to
                        SelectedItem(
                            id = it.id,
                            uri = it.uri,
                            immichUrl = it.immichUrl,
                            isImage = it.type == MediaType.Image,
                            absolutePath = it.absolutePath,
                            parentPath = it.parentPath
                        )
            }

            val maxCount = getMediaInDate(epochToDayStart(key, timeZone), sortMode).size

            if (snapshot[key]!!.size == maxCount) {
                sections.add(key)
            } else {
                sections.remove(key)
            }
        }

        publishSelection(generation, snapshot, sections)
    }

    private fun getKey(item: PhotoLibraryUIModel) =
        if (item is PhotoLibraryUIModel.MediaImpl) {
            getMediaKey(item.item)
        } else {
            (item as PhotoLibraryUIModel.Section).timestamp
        }

    private fun getMediaKey(item: MediaStoreData) = when {
        sortMode == MediaItemSortMode.MonthTaken -> item.getMonthTaken(timeZone)

        sortMode.isDateModified -> item.getDateModifiedDay(timeZone)

        // sortMode.isDisabled -> 0 // seems to cause animation state issues

        else -> item.getDateTakenDay(timeZone)
    }

    private fun toggleMedia(item: MediaStoreData) {
        val key = getMediaKey(item)

        if (_selection[key]?.containsKey(item.id) == true) {
            remove(item, key)
        } else {
            add(item, key)
        }
    }

    private fun toggleSection(timestamp: Long) = launchSelectionMutation { generation ->
        if (singleSelectMode) return@launchSelectionMutation

        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableSet()

        if (timestamp in sections) {
            snapshot.remove(timestamp)
            sections.remove(timestamp)
        } else {
            val media = getMediaInDate(epochToDayStart(timestamp, timeZone), sortMode)

            snapshot[timestamp] = media
            sections.add(timestamp)
        }

        publishSelection(generation, snapshot, sections)
    }

    private fun add(
        item: MediaStoreData,
        key: Long
    ) = launchSelectionMutation { generation ->
        val snapshot = _selection.toMutableMap()
        val sections = _sections.toMutableSet()

        val list = (snapshot[key] ?: emptyMap()) + mapOf(
            item.id to SelectedItem(
                id = item.id,
                uri = item.uri,
                immichUrl = item.immichUrl,
                isImage = item.type == MediaType.Image,
                absolutePath = item.absolutePath,
                parentPath = item.parentPath
            )
        )
        snapshot[key] = list

        val maxCount = getMediaInDate(epochToDayStart(key, timeZone), sortMode).size

        if (list.size == maxCount) {
            sections.add(key)
        }

        publishSelection(generation, snapshot, sections)
    }

    private fun launchSelectionMutation(block: suspend (Long) -> Unit) =
        currentSelectionGeneration().let { generation ->
            scope.launch(Dispatchers.IO) {
                mutationMutex.withLock {
                    block(generation)
                }
            }
        }

    private fun currentSelectionGeneration() = synchronized(selectionLock) {
        selectionGeneration
    }

    private fun publishSelection(
        generation: Long,
        selection: Map<Long, Map<Long, SelectedItem>>,
        sections: Set<Long>
    ) = synchronized(selectionLock) {
        if (generation != selectionGeneration) return@synchronized

        if (selection.values.sumOf { it.size } <= 2000) {
            _selection = selection
            _sections = sections
        } else {
            eventChannel.trySend(SelectionEvent.LimitReached)
        }
    }

    private fun remove(item: MediaStoreData, key: Long) = launchSelectionMutation { generation ->
        val selection = _selection.toMutableMap()
        val sections = _sections.toMutableSet()

        val updated = (selection[key] ?: emptyMap()) - item.id

        if (updated.isEmpty()) selection.remove(key)
        else selection[key] = updated

        sections.remove(key)

        publishSelection(generation, selection, sections)
    }

    fun setSortMode(mode: MediaItemSortMode) {
        sortMode = mode
    }
}
