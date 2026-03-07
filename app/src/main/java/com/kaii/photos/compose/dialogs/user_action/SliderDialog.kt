package com.kaii.photos.compose.dialogs.user_action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.LavenderDialogBase
import com.kaii.photos.helpers.TextStylingConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderDialog(
    title: (Float) -> String,
    steps: Int = 0,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    startsAt: Float = 1f,
    onSetValue: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(startsAt) }

    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Text(
            text = title(sliderValue),
            fontSize = TextUnit(TextStylingConstants.LARGE_TEXT_SIZE, TextUnitType.Sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            valueRange = range,
            steps = steps,
            onValueChange = {
                sliderValue = it
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    drawTick = { _, _ -> },
                    modifier = Modifier
                        .height(32.dp)
                )
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                alignment = Alignment.End,
                space = 8.dp
            )
        ) {
            FilledTonalButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(id = R.string.media_cancel),
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp)
                )
            }

            Button(
                onClick = {
                    onSetValue(sliderValue)
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(id = R.string.media_confirm),
                    fontSize = TextUnit(TextStylingConstants.SMALL_TEXT_SIZE, TextUnitType.Sp)
                )
            }
        }
    }
}