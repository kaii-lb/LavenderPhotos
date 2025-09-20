package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.editing.CroppingAspectRatio
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CroppingRatioBottomSheet(
    show: MutableState<Boolean>,
    ratio: CroppingAspectRatio,
    originalImageRatio: Float,
    onSetCroppingRatio: (ratio: CroppingAspectRatio) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(show.value) {
        if (show.value) sheetState.expand()
        else sheetState.hide()
    }

    if (show.value) {
        ModalBottomSheet(
            onDismissRequest = {
                show.value = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = { WindowInsets.navigationBars },
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.Start
            ) {
                CroppingAspectRatio.entries.forEachIndexed { index, entry ->
                    if (entry == CroppingAspectRatio.ByImage) entry.ratio = originalImageRatio

                    ClickableRow(
                        title = stringResource(id = entry.title),
                        position =
                            when (index) {
                                0 -> RowPosition.Top
                                CroppingAspectRatio.entries.size - 1 -> RowPosition.Bottom
                                else -> RowPosition.Middle
                            },
                        selected = ratio == entry
                    ) {
                        onSetCroppingRatio(entry)

                        coroutineScope.launch {
                            delay(AnimationConstants.DURATION.toLong())
                            sheetState.hide()
                            show.value = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClickableRow(
    title: String,
    position: RowPosition,
    selected: Boolean,
    action: () -> Unit
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position, 32.dp)

    Box(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                action()
            }
            .padding(16.dp, 8.dp)
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.CenterStart)
        )

        if (selected) {
            Icon(
                painter = painterResource(id = R.drawable.checkmark_thin),
                contentDescription = stringResource(id = R.string.bottom_sheets_ratio_selected),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterEnd)
            )
        }
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(MaterialTheme.colorScheme.background)
    )
}
