package com.kaii.photos.helpers

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

fun LazyListState.getItemAtOffset(
    offset: Int
) = layoutInfo.visibleItemsInfo.find { item ->
        offset in item.offset..item.offset + item.size
    }?.index

@Composable
fun Modifier.dragReorderable(
    state: LazyListState,
    itemOffset: MutableFloatState,
    onItemSelected: (itemIndex: Int?) -> Unit,
    onMove: (currentIndex: Int, targetIndex: Int) -> Unit
): Modifier {
    val coroutineScope = rememberCoroutineScope()

    return this.pointerInput(Unit) {
        var selectedItemIndex: Int? = null

        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                state.getItemAtOffset(offset = offset.y.toInt())?.let {
                    selectedItemIndex = it
                    onItemSelected(it)
                }
            },

            onDrag = { change, offset ->
                change.consume()
                itemOffset.floatValue += offset.y

                val currentIndex = selectedItemIndex ?: return@detectDragGesturesAfterLongPress
                val currentItem = state.layoutInfo.visibleItemsInfo.find { it.index == currentIndex } ?: return@detectDragGesturesAfterLongPress

                val startOffset = currentItem.offset + itemOffset.floatValue
                val endOffset = currentItem.offset + currentItem.size + itemOffset.floatValue
                val middleOffset = startOffset + (endOffset - startOffset) / 2

                val targetItemIndex = state.getItemAtOffset(middleOffset.toInt())?.let {
                    if (it != currentIndex) it else null
                }

                if (targetItemIndex != null) {
                    onMove(currentIndex, targetItemIndex)
                    onItemSelected(selectedItemIndex)

                    selectedItemIndex = targetItemIndex
                    val targetItem = state.layoutInfo.visibleItemsInfo[targetItemIndex]
                    itemOffset.value += currentItem.offset - targetItem.offset
                } else {
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
