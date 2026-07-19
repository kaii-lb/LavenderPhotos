package com.kaii.photos.compose.modifiers

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.kaii.photos.presentation.single_photos_views.DismissAnchors
import com.kaii.photos.presentation.single_photos_views.DismissDragState

fun Modifier.singlePhotoProperties(
    state: PagerState,
    draggableState: DismissDragState<DismissAnchors>,
    firstFR: FocusRequester,
    secondFR: FocusRequester,
    thirdFR: FocusRequester,
    isVideo: () -> Boolean
) = this then Modifier
    .focusable()
    .focusRequester(secondFR)
    .focusProperties {
        up = firstFR
        down = thirdFR
    }
    .pagerKeyHandler(
        state = state,
        isVideo = isVideo,
        onFocusUp = { firstFR.requestFocus() },
        onFocusDown = { thirdFR.requestFocus() }
    )
    .anchoredDraggable(
        state = draggableState.state,
        orientation = Orientation.Vertical,
        flingBehavior = draggableState.flingBehavior
    )