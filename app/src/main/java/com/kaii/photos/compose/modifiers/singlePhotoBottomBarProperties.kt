package com.kaii.photos.compose.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.kaii.photos.presentation.single_photos_views.DismissAnchors
import com.kaii.photos.presentation.single_photos_views.DismissDragState
import com.kaii.photos.presentation.single_photos_views.DismissDragState.Companion.barScaleModifier

fun Modifier.singlePhotoBottomBarProperties(
    draggableState: DismissDragState<DismissAnchors>,
    secondFR: FocusRequester,
    thirdFR: FocusRequester
) = this then Modifier
    .barScaleModifier(draggableState)
    .focusRequester(thirdFR)
    .focusProperties {
        onExit = {
            secondFR.requestFocus()
        }
        up = secondFR
        down = thirdFR
    }