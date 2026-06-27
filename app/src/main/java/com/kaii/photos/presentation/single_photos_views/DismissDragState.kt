package com.kaii.photos.presentation.single_photos_views

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import com.kaii.photos.LocalNavController

class DismissDragState<T>(
    val state: AnchoredDraggableState<T>,
    val flingBehavior: FlingBehavior,
    screenHeightPx: Float
) {
    companion object {
        fun <T> Modifier.barScaleModifier(state: DismissDragState<T>) = this
            .graphicsLayer {
                scaleX = 1f - state.progress * 0.1f
                scaleY = 1f - state.progress * 0.1f
            }
    }

    val progress by derivedStateOf {
        val dragOffset = state.requireOffset().coerceAtLeast(0f)
        (dragOffset / screenHeightPx).coerceIn(0f, 1f)
    }
}

@Composable
fun <T> rememberDismissDragState(
    initialValue: T,
    anchors: DraggableAnchors<T>
): DismissDragState<T> {
    val state = remember {
        AnchoredDraggableState(
            initialValue = initialValue,
            anchors = anchors
        )
    }

    val flingBehaviour = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { total: Float ->
            total * 0.4f
        },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )

    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    return remember {
        DismissDragState(
            state = state,
            flingBehavior = flingBehaviour,
            screenHeightPx = screenHeightPx
        )
    }
}

@Composable
fun rememberDismissSinglePhotoState(): DismissDragState<DismissAnchors> {
    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val anchors = remember(screenHeightPx) {
        DraggableAnchors {
            DismissAnchors.Resting at 0f
            DismissAnchors.Dismissed at screenHeightPx
        }
    }

    val draggableState = rememberDismissDragState(
        initialValue = DismissAnchors.Resting,
        anchors = anchors
    )

    val navController = LocalNavController.current
    LaunchedEffect(draggableState.state.settledValue) {
        if (draggableState.state.settledValue == DismissAnchors.Dismissed) {
            navController.popBackStack()
        }
    }

    return draggableState
}