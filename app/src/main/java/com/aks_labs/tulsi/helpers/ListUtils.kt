package com.aks_labs.tulsi.helpers

import android.util.Log
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "LIST_UTILS"
// fun LazyListState.getItemAtOffset(
//     offset: Int
// ) = layoutInfo.visibleItemsInfo.find { item ->
//         offset in item.offset..item.offset + item.size
//     }?.index

@Suppress("UNCHECKED_CAST")
fun <T : Any> LazyListState.getItemAtOffset(
    offset: Int,
    keys: List<T>
): Int? {
    var key: T? = null
    val relativeOffset = offset - layoutInfo.viewportStartOffset

    val possibleItem = layoutInfo.visibleItemsInfo.find { item ->
        relativeOffset >= item.offset && relativeOffset < item.offset + item.size
    }

    if (possibleItem != null) {
        key = possibleItem.key as? T
    }

    val found = keys.find {
        it == key
    } ?: return null

    return keys.indexOf(found)
}

@Composable
fun Modifier.dragReorderable(
    state: LazyListState,
    keys: List<Any>,
    itemOffset: MutableFloatState,
    onItemSelected: (itemIndex: Int?) -> Unit,
    onMove: (currentIndex: Int, targetIndex: Int) -> Unit
): Modifier {
    val coroutineScope = rememberCoroutineScope()

    return this.pointerInput(Unit) {
        var selectedItemIndex: Int? = null

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                state.getItemAtOffset(
                    offset = offset.y.roundToInt(),
                    keys = keys
                )?.let {
                    Log.d(TAG, "Item index start : $it")
                    selectedItemIndex = it
                }
            },

            onDrag = { change, offset ->
                change.consume()
                itemOffset.floatValue += offset.y

                run {
                    val targetItemIndex = state.getItemAtOffset(
                        offset = offset.y.roundToInt(),
                        keys = keys
                    )

                    Log.d(TAG, "Target item index: $targetItemIndex")

                    if (targetItemIndex != null && selectedItemIndex != null) {
                        val currentItem = state.layoutInfo.visibleItemsInfo.find {
                            it.key == keys[selectedItemIndex!!]
                        }
                        val targetItem = state.layoutInfo.visibleItemsInfo.find {
                            it.key == keys[targetItemIndex]
                        }

                        if (currentItem != null && targetItem != null) {
                            onMove(selectedItemIndex!!, targetItemIndex)

                            // itemOffset.floatValue =
                            //     change.position.y - (targetItem.offset + targetItem.size / 2)
                        } else if (currentItem != null) {
                            val startOffset = currentItem.offset + itemOffset.floatValue
                            val endOffset =
                                currentItem.offset + currentItem.size + itemOffset.floatValue

                            val offsetToTop = startOffset - state.layoutInfo.viewportStartOffset
                            val offsetToBottom = endOffset - state.layoutInfo.viewportEndOffset

                            val scroll = when {
                                offsetToTop < 0 -> offsetToTop.coerceAtMost(0f)
                                offsetToBottom > 0 -> offsetToBottom.coerceAtLeast(0f)
                                else -> 0f
                            }

                            if (scroll != 0f && (state.canScrollBackward || state.canScrollForward)) coroutineScope.launch {
                                state.scrollBy(scroll)
                            }
                        }
                    }

                    // if (targetItemIndex != null && currentItem != null) {
                    //     onMove(currentIndex, targetItemIndex)
                    //     onItemSelected(selectedItemIndex)
                    //
                    //     selectedItemIndex = targetItemIndex
                    //     val targetItem = state.layoutInfo.visibleItemsInfo[targetItemIndex]
                    //     itemOffset.floatValue += currentItem.offset - targetItem.offset
                    //
                }
            },

            onDragCancel = {
                selectedItemIndex = null
                onItemSelected(null)
                itemOffset.floatValue = 0f
            },

            onDragEnd = {
                selectedItemIndex = null
                onItemSelected(null)
                itemOffset.floatValue = 0f
            }
        )
    }
}


