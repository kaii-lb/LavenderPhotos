package com.kaii.photos.compose.modifiers

import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch

fun Modifier.pagerKeyHandler(
    state: PagerState,
    isVideo: () -> Boolean,
    onFocusUp: () -> Unit,
    onFocusDown: () -> Unit
) = composed {
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    this then Modifier
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

            when (event.key) {
                Key.DirectionRight, Key.DirectionLeft -> {
                    if (isVideo()) return@onPreviewKeyEvent false

                    val next = (state.currentPage + 1).coerceAtMost(state.pageCount - 1)
                    val previous = (state.currentPage - 1).coerceAtLeast(0)

                    val targetPage = if (event.key == Key.DirectionRight) {
                        if (layoutDirection == LayoutDirection.Ltr) next
                        else previous
                    } else {
                        if (layoutDirection == LayoutDirection.Rtl) next
                        else previous
                    }

                    if (targetPage != state.currentPage) {
                        coroutineScope.launch {
                            state.animateScrollToPage(
                                page = targetPage,
                                animationSpec = tween(durationMillis = 50)
                            )
                        }
                        true
                    } else {
                        false
                    }
                }

                Key.DirectionUp -> {
                    onFocusUp()
                    true
                }

                Key.DirectionDown -> {
                    onFocusDown()
                    true
                }

                else -> false
            }
        }
}