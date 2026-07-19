package com.kaii.photos.compose.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.kaii.photos.presentation.single_photos_views.DismissAnchors
import com.kaii.photos.presentation.single_photos_views.DismissDragState
import com.kaii.photos.presentation.single_photos_views.DismissDragState.Companion.barScaleModifier

fun Modifier.singlePhotoTopBarProperties(
    draggableState: DismissDragState<DismissAnchors>,
    firstFR: FocusRequester,
    secondFR: FocusRequester
) = this then Modifier
    .barScaleModifier(draggableState)
        .focusRequester(firstFR)
        .focusProperties() {
            up = firstFR
            down = secondFR
        }
