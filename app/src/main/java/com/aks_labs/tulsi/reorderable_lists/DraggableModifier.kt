package com.aks_labs.tulsi.reorderable_lists

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

@Composable
fun LazyItemScope.ReorderableItem(
    index: Int,
    reorderableState: ReorderableLazyListState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDragging = reorderableState.currentIndex == index

    val localModifier = Modifier
        .zIndex(if (isDragging) 1f else 0f)
        .graphicsLayer {
            translationY = if (isDragging) reorderableState.currentOffset else 0f
        }
        .animateItem(
            placementSpec =
                if (isDragging) null
                else tween(
                    durationMillis = 350
                )
        )

    Column(
        modifier = modifier
            .then(localModifier)
    ) {
        content()
    }
}

