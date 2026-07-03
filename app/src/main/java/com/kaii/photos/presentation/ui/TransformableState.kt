package com.kaii.photos.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class TransformableState(
    val applyTransformation: Boolean
) {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
}

@Composable
fun retainTransformableState(applyTransformation: Boolean = true): TransformableState {
    return retain {
        TransformableState(applyTransformation)
    }
}