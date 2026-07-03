package com.kaii.photos.reorderable_lists

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class SortableGridState(
    private val gridState: LazyGridState,
    private val density: Density,
    private val albums: () -> List<AlbumGridState.Album>,
    private val hasPrefix: () -> Boolean,
    private val isAlbumGroup: Boolean,
    private val sortMode: () -> AlbumSortMode,
    private val autoDetect: () -> Boolean,
    private val scrollThreshold: Float,
    private val setAlbums: (list: List<AlbumGridState.Album>) -> Unit,
    private val setAlbumSortMode: (sortMode: AlbumSortMode) -> Unit,
    private val setAlbumOrder: (order: List<String>) -> Unit,
    private val addAlbumToGroup: (albumId: String, groupId: String) -> Unit,
    private val toggleAlbumPin: (album: AlbumGridState.Album) -> Unit,
    private val deleteAlbum: (album: AlbumGridState.Album) -> Unit
) {
    enum class DeleteAlbumState {
        Unselected,
        Deleting,
        NotAllowed
    }

    enum class PinAlbumState {
        Unselected,
        Pinning
    }

    private val defaultScale = 1f
    private val selectedScale = 0.9f
    private val addingToGroupScale = 0.65f

    private var targetItemIndex: Int? = null
    private var lastSortMode = sortMode()
    private var lastAlbumOrder = albums().map { it.id }

    var scrollSpeed by mutableFloatStateOf(0f)
        private set

    var selectedItem by mutableStateOf<AlbumGridState.Album?>(null)
        private set
    var itemOffset by mutableStateOf(Offset.Zero)
        private set
    var itemScale by mutableFloatStateOf(1f)
        private set

    var deleteAlbumState by mutableStateOf(DeleteAlbumState.Unselected)
        private set
    var pinAlbumState by mutableStateOf(PinAlbumState.Unselected)
        private set

    fun onDragStart(offset: Offset) {
        lastSortMode = sortMode()
        lastAlbumOrder = albums().map { it.id }

        gridState.layoutInfo.visibleItemsInfo
            .find { item ->
                IntRect(
                    offset = item.offset,
                    size = item.size
                ).contains(offset.round())
                        && !item.key.toString().startsWith("FavAndTrash")
                        && !item.key.toString().startsWith("DeleteThisAlbum")
                        && !item.key.toString().startsWith("PinDeleteRow")
            }?.let { item ->
                val indexOffset = if (hasPrefix()) 2 else 1

                val index = item.index - indexOffset

                if (index in albums().indices) {
                    selectedItem = albums()[index]
                    itemScale = selectedScale

                    var itemCenter = item.offset.toOffset() + item.size.center.toOffset()

                    gridState.layoutInfo.visibleItemsInfo.find {
                        it.key == "PinDeleteRow" && it.row != -1
                    }?.let {
                        with(density) {
                            itemCenter += Offset(x = 0f, y = 104.dp.toPx())
                        }
                    }

                    itemOffset = offset - itemCenter
                }
            } ?: run { selectedItem = null }
    }

    fun onDrag(change: PointerInputChange, offset: Offset) {
        change.consume()
        itemOffset += offset

        val targetItem = gridState.layoutInfo.visibleItemsInfo
            .find { item ->
                IntRect(
                    offset = item.offset,
                    size = item.size
                ).contains(change.position.round())
            }

        val currentLazyItem =
            gridState.layoutInfo.visibleItemsInfo.find {
                it.key == selectedItem?.id
            }

        val distanceFromBottom = gridState.layoutInfo.viewportSize.height - change.position.y
        val distanceFromTop = change.position.y // for clarity

        scrollSpeed = when {
            distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
            distanceFromTop < scrollThreshold -> -scrollThreshold + distanceFromTop
            else -> 0f
        }

        if (targetItem == null) return

        if (targetItem.key == "PinDeleteRow" && currentLazyItem != null) {
            itemScale = selectedScale

            val rect = Rect(
                offset = targetItem.offset.toOffset(),
                size = targetItem.size.toSize().copy(
                    width = targetItem.size.width / 2f
                )
            )

            if (rect.contains(change.position)) {
                pinAlbumState = PinAlbumState.Pinning
                deleteAlbumState = DeleteAlbumState.Unselected
            } else {
                pinAlbumState = PinAlbumState.Unselected

                val item = selectedItem
                deleteAlbumState =
                    if (item is AlbumGridState.Album.Single) {
                        if (item.info.album is AlbumType.Cloud ||
                            (item.info.album is AlbumType.Folder && autoDetect())
                        ) {
                            DeleteAlbumState.NotAllowed
                        } else {
                            DeleteAlbumState.Deleting
                        }
                    } else {
                        DeleteAlbumState.Deleting
                    }
            }

            return
        } else {
            pinAlbumState = PinAlbumState.Unselected
            deleteAlbumState = DeleteAlbumState.Unselected
        }

        val snapshot = albums()
        if (
            currentLazyItem != null &&
            targetItem.key in snapshot.map { it.id } &&
            !isAlbumGroup
        ) {
            targetItemIndex = snapshot.indexOfFirst { it.id == targetItem.key }
            val newList = snapshot.toMutableList()

            if (snapshot[targetItemIndex!!] is AlbumGridState.Album.Group && selectedItem is AlbumGridState.Album.Single) {
                itemScale = addingToGroupScale
            } else {
                itemScale = selectedScale
                newList.removeAll { it.id == selectedItem?.id }
                newList.add(targetItemIndex!!, selectedItem!!)

                val oldOffset = currentLazyItem.offset.toOffset()
                val newOffset = targetItem.offset.toOffset()

                itemOffset += oldOffset - newOffset

                setAlbums(newList)
            }
        }
    }

    fun onDragCancel() {
        selectedItem = null
        itemOffset = Offset.Zero
        itemScale = defaultScale
        scrollSpeed = 0f
        pinAlbumState = PinAlbumState.Unselected
        deleteAlbumState = DeleteAlbumState.Unselected
    }

    fun onDragEnd() {
        if (pinAlbumState == PinAlbumState.Pinning && selectedItem != null) {
            toggleAlbumPin(selectedItem!!)
            onDragCancel()
            return
        }

        if (deleteAlbumState == DeleteAlbumState.Deleting && selectedItem != null) {
            deleteAlbum(selectedItem!!)
            onDragCancel()
            return
        }

        if (!isAlbumGroup &&
            targetItemIndex != null &&
            albums()[targetItemIndex!!] is AlbumGridState.Album.Group &&
            selectedItem is AlbumGridState.Album.Single
        ) {
            val targetItem = albums()[targetItemIndex!!]

            addAlbumToGroup(
                selectedItem!!.id,
                targetItem.id
            )

            setAlbums(albums().toMutableList().filter { it.id != selectedItem?.id })
            setAlbumSortMode(lastSortMode)
            onDragCancel()
            return
        }

        if (!isAlbumGroup && lastAlbumOrder != albums().map { it.id }) {
            setAlbumSortMode(AlbumSortMode.Custom)
            setAlbumOrder(albums().map { it.id })
            onDragCancel()
        }

        onDragCancel() // all failed case
    }

    fun scrollBy(scrolledBy: Float) {
        itemOffset += Offset(x = 0f, y = scrolledBy)
    }
}

@Composable
fun rememberSortableGridState(
    gridState: LazyGridState,
    albums: () -> List<AlbumGridState.Album>,
    hasPrefix: () -> Boolean,
    isAlbumGroup: Boolean,
    sortMode: () -> AlbumSortMode,
    autoDetect: () -> Boolean,
    setAlbums: (list: List<AlbumGridState.Album>) -> Unit,
    setAlbumSortMode: (sortMode: AlbumSortMode) -> Unit,
    setAlbumOrder: (order: List<String>) -> Unit,
    addAlbumToGroup: (id: String, groupId: String) -> Unit,
    toggleAlbumPin: (album: AlbumGridState.Album) -> Unit,
    deleteAlbum: (album: AlbumGridState.Album) -> Unit
): SortableGridState {
    val density = LocalDensity.current

    val state = retain(gridState) {
        SortableGridState(
            gridState = gridState,
            density = density,
            albums = albums,
            hasPrefix = hasPrefix,
            isAlbumGroup = isAlbumGroup,
            sortMode = sortMode,
            autoDetect = autoDetect,
            scrollThreshold = with(density) { 60.dp.toPx() },
            setAlbums = setAlbums,
            setAlbumSortMode = setAlbumSortMode,
            setAlbumOrder = setAlbumOrder,
            addAlbumToGroup = addAlbumToGroup,
            toggleAlbumPin = toggleAlbumPin,
            deleteAlbum = deleteAlbum
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.scrollSpeed }.collectLatest { scrollSpeed ->
            if (scrollSpeed != 0f) {
                while (isActive) {
                    val scrolledBy = gridState.scrollBy(scrollSpeed)

                    state.scrollBy(scrolledBy)

                    delay(16.milliseconds)
                }
            }
        }
    }

    return state
}