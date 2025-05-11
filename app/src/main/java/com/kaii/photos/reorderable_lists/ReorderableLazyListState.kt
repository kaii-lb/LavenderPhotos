package com.kaii.photos.reorderable_lists

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ReorderableLazyListState(
    internal val state: LazyListState,
    private val coroutineScope: CoroutineScope,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    private var relativeDistance by mutableFloatStateOf(0f)
    private var initialItem by mutableStateOf<LazyListItemInfo?>(null)

    var currentIndex by mutableStateOf<Int?>(null)

    private val currentItemInfo by derivedStateOf {  state.layoutInfo.visibleItemsInfo
        .firstOrNull { it.index == currentIndex } }

    internal val currentOffset: Float
        get() = currentItemInfo?.let {
            (initialItem?.offset ?: 0) + relativeDistance - it.offset
        } ?: 0f

    private var previousIndex by mutableStateOf<Int?>(null)
    private var previousOffset = Animatable(0f)

    fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                val y = offset.y - state.layoutInfo.viewportStartOffset
                y.roundToInt() in item.offset..(item.offset + item.size)
            }?.also {
                currentIndex = it.index
                initialItem = it
            }
    }

    fun onDrag(offset: Offset) {
        relativeDistance += (offset.y - state.layoutInfo.viewportStartOffset)

        initialItem?.offset?.let { initialOffset ->
            val offsetTop = relativeDistance + initialOffset
            val offsetBottom = offsetTop + initialItem!!.size

            currentItemInfo?.let { current ->
                state.layoutInfo.visibleItemsInfo
                    .filterNot { item ->
                        (item.offset + item.size) < offsetTop || item.offset > offsetBottom || current.index == item.index
                    }
                    .firstOrNull { item ->
                        val delta = (offsetBottom - current.offset)
                        when {
                            delta > 0 -> {
                                offsetBottom > (item.offset + item.size)
                            }

                            else -> {
                                offsetTop < item.offset
                            }
                        }
                    }?.also { item ->
                        currentIndex?.let { current ->
                            coroutineScope.launch {
                                onMove.invoke(
                                    current,
                                    item.index
                                )
                            }
                        }

                        currentIndex = item.index
                    }
            }
        }
    }

    fun onDragStopped() {
        if (currentIndex != null) {
            previousIndex = currentIndex
            val startOffset = currentOffset

            coroutineScope.launch {
                previousOffset.snapTo(startOffset)
                previousOffset.animateTo(
                    0f,
                    tween(
                        easing = FastOutLinearInEasing
                    )
                )
                previousIndex = null
            }
        }

        initialItem = null
        relativeDistance = 0f
        currentIndex = null
    }

    fun checkForOverScroll(): Float {
        return initialItem?.let {
            val startOffset = it.offset + relativeDistance
            val endOffset = (it.offset + it.size) + relativeDistance

            return@let when {
                relativeDistance > 0 -> (endOffset - state.layoutInfo.viewportEndOffset+50f).takeIf { diff -> diff > 0 }
                relativeDistance < 0 -> (startOffset - state.layoutInfo.viewportStartOffset-50f).takeIf { diff -> diff < 0 }
                else -> null
            }
        } ?: 0f
    }
}

@Composable
fun rememberReorderableListState(
    lazyListState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): ReorderableLazyListState {
    val scope = rememberCoroutineScope()

    val state = remember(lazyListState) {
        ReorderableLazyListState(
            state = lazyListState,
            onMove = onMove,
            coroutineScope = scope
        )
    }

    return state
}