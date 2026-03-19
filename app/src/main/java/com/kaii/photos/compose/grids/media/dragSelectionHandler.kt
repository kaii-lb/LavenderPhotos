package com.kaii.photos.compose.grids.media

import android.content.ClipData
import android.content.Context
import android.os.Vibrator
import android.view.View
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.bumptech.glide.Glide
import com.kaii.photos.R
import com.kaii.photos.helpers.grid_management.BitmapUriShadowBuilder
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.helpers.paging.PhotoLibraryUIModel
import com.kaii.photos.helpers.vibrateLong
import com.kaii.photos.helpers.vibrateShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Modifier.dragSelectionHandler(
    state: LazyGridState,
    selectionManager: SelectionManager,
    vibratorManager: Vibrator?,
    pagingItems: LazyPagingItems<PhotoLibraryUIModel>,
    gridState: LazyGridState,
    isDragSelecting: MutableState<Boolean>,
    context: Context,
    thumbnailSettings: Pair<Boolean, Int>
) = composed {
    val localDensity = LocalDensity.current
    val resources = LocalResources.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val scrollSpeed = remember { mutableFloatStateOf(0f) }
    val selectedCount by selectionManager.count.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(scrollSpeed.floatValue) {
        if (scrollSpeed.floatValue != 0f) {
            while (isActive) {
                gridState.scrollBy(scrollSpeed.floatValue)
                delay(10)
            }
        }
    }

    pointerInput(pagingItems) {
        var initialKey: Int? = null
        var currentKey: Int? = null
        var isDragAndDropping = false

        val scrollThreshold = with(localDensity) {
            60.dp.toPx()
        }

        if (pagingItems.itemCount == 0) return@pointerInput

        val itemWidth = state.layoutInfo.visibleItemsInfo.firstOrNull {
            if (it.index in 0..<pagingItems.itemCount) {
                pagingItems[it.index] is PhotoLibraryUIModel.MediaImpl
            } else false
        }?.size?.width

        val numberOfHorizontalItems = itemWidth?.let { state.layoutInfo.viewportSize.width / it } ?: 1

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                isDragSelecting.value = true

                state.getGridItemAtOffset(
                    offset = offset,
                    keys = (0..<pagingItems.itemCount).map { pagingItems[it]?.itemKey() },
                    numberOfHorizontalItems = numberOfHorizontalItems
                )?.let { index ->
                    val item = pagingItems[index]

                    if (item is PhotoLibraryUIModel.MediaImpl) {
                        val selected = selectionManager.isSelected(item)

                        if (selected) {
                            isDragAndDropping = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val items = selectionManager.selection.first().fastMap { it.toUri() }

                                val clipData = ClipData.newUri(
                                    context.contentResolver,
                                    resources.getString(R.string.drag_and_drop_data),
                                    items.first()
                                )

                                items.drop(1).forEach {
                                    clipData.addItem(ClipData.Item(it))
                                }

                                val bitmaps = items.take(3).map { // limit to 3 so we don't overstress the rendering/loading of bitmaps
                                    Glide.with(context)
                                        .asBitmap()
                                        .override(thumbnailSettings.second)
                                        .centerCrop()
                                        .load(it)
                                        .submit()
                                        .get()
                                }

                                val shadow = BitmapUriShadowBuilder(
                                    view = view,
                                    bitmaps = bitmaps,
                                    count = items.size,
                                    density = Density(density)
                                )

                                view.startDragAndDrop(
                                    clipData,
                                    shadow,
                                    clipData,
                                    View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE
                                )
                            }
                        } else {
                            vibratorManager?.apply {
                                if (selectedCount == 0) vibrateLong()
                                else vibrateShort()
                            }

                            isDragAndDropping = false
                            initialKey = index
                            currentKey = index

                            selectionManager.addMedia(item.item)
                        }
                    }
                }
            },

            onDragCancel = {
                initialKey = null
                scrollSpeed.floatValue = 0f
                isDragSelecting.value = false
            },

            onDragEnd = {
                initialKey = null
                scrollSpeed.floatValue = 0f
                isDragSelecting.value = false
            },

            onDrag = { change, _ ->
                isDragSelecting.value = true

                if (initialKey != null && !isDragAndDropping) {
                    val distanceFromBottom = state.layoutInfo.viewportSize.height - change.position.y
                    val distanceFromTop = change.position.y // for clarity

                    scrollSpeed.floatValue = when {
                        distanceFromBottom < scrollThreshold -> scrollThreshold - distanceFromBottom
                        distanceFromTop < scrollThreshold -> -scrollThreshold + distanceFromTop
                        else -> 0f
                    }

                    state.getGridItemAtOffset(
                        offset = change.position,
                        keys = (0..<pagingItems.itemCount).map { pagingItems[it]?.itemKey() },
                        numberOfHorizontalItems = numberOfHorizontalItems
                    )?.let { index ->
                        if (currentKey != index) {
                            val toBeRemoved =
                                if (initialKey!! <= currentKey!!) {
                                    (initialKey!!..currentKey!!).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                } else {
                                    (currentKey!!..initialKey!!).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                }

                            val toBeAdded =
                                if (initialKey!! <= index) {
                                    (initialKey!!..index).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                } else {
                                    (index..initialKey!!).mapNotNull {
                                        (pagingItems[it] as? PhotoLibraryUIModel.MediaImpl)?.item
                                    }
                                }

                            selectionManager.updateSelection(added = toBeAdded, removed = toBeRemoved)

                            currentKey = index
                        }
                    }
                }
            }
        )
    }
}