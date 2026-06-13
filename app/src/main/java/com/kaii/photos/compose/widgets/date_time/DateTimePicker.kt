package com.kaii.photos.compose.widgets.date_time

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kaii.photos.R
import com.kaii.photos.compose.pages.FullWidthDialogButton
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.widgets.DateTimePickerState
import com.kaii.photos.widgets.rememberDateTimePickerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Preview
@Composable
private fun DateTimePickerPreview() {
    DateTimePicker(
        mediaItem = MediaStoreData.dummyItem,
        onDismiss = {}
    )
}

@Composable
fun DateTimePicker(
    mediaItem: MediaStoreData,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val state = rememberDateTimePickerState(
        mediaItem = mediaItem
    )

    DateTimePicker(
        state = state,
        modifier = modifier,
        onDismiss = onDismiss,
        onConfirm = {
            state.writeDate()
        }
    )
}

@Composable
fun DateTimePicker(
    state: DateTimePickerState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (instant: Instant) -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = modifier
                    .widthIn(max = 400.dp)
                    .dropShadow(
                        shape = RoundedCornerShape(
                            topStart = 56.dp, topEnd = 56.dp,
                            bottomStart = 32.dp, bottomEnd = 32.dp
                        ),
                        shadow = Shadow(
                            radius = 8.dp,
                            color = Color.Black.copy(alpha = 0.2f)
                        )
                    )
                    .padding(all = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var showTimePicker by remember { mutableStateOf(false) }

                val animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                AnimatedContent(
                    targetState = showTimePicker,
                    transitionSpec = {
                        val enter = slideInVertically(animationSpec = animationSpec) { it }
                        val exit = scaleOut(targetScale = 0.5f)
                        enter.togetherWith(exit)
                    }
                ) { visible ->
                    if (visible) {
                        TimePicker(state = state)
                    } else {
                        DatePicker(state = state)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(all = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }

                    val backButtonWidth by animateDpAsState(
                        targetValue = if (showTimePicker) 48.dp else 0.dp,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                    )

                    FilledTonalIconButton(
                        onClick = {
                            state.setIsLoading(loading = false)
                            state.setIsError(error = false)
                            showTimePicker = false
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(backButtonWidth)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(id = R.string.exif_date_changer_go_back)
                        )
                    }

                    val color by animateColorAsState(
                        targetValue =
                            if (state.isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.secondaryContainer,
                        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
                    )

                    val coroutineScope = rememberCoroutineScope()
                    FullWidthDialogButton(
                        text = stringResource(
                            id = when {
                                state.isError -> R.string.error

                                showTimePicker -> R.string.media_confirm

                                else -> R.string.next
                            }
                        ),
                        color = color,
                        textColor = MaterialTheme.colorScheme.contentColorFor(color),
                        position = RowPosition.Single,
                        enabled = !state.isLoading && !state.isError
                    ) {
                        if (showTimePicker) {
                            state.setIsLoading(loading = true)
                            onConfirm(state.getDateTime())

                            coroutineScope.launch {
                                var counter = 0
                                while (state.isLoading && counter < 60) {
                                    delay(1.seconds)
                                    counter += 1
                                }

                                onDismiss()
                            }
                        } else {
                            showTimePicker = true
                        }
                    }
                }
            }
        }
    }
}