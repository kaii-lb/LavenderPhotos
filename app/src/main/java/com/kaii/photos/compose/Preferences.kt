package com.kaii.photos.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.getDefaultShapeSpacerForPosition
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.darkenColor

@Composable
fun PreferencesRow(
    title: String,
    iconResID: Int,
    position: RowPosition,
    modifier: Modifier = Modifier,
    summary: String? = null,
    goesToOtherPage: Boolean = false,
    showBackground: Boolean = true,
    titleTextSize: Float = 18f,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    action: (() -> Unit)? = null
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, 24.dp)

    val clickable = if (action != null) {
        Modifier.clickable {
            action()
        }
    } else {
        Modifier
    }

    val clip = if (showBackground) Modifier.clip(shape) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight()
            .then(clip)
            .wrapContentHeight(align = Alignment.CenterVertically)
            .background(if (showBackground) backgroundColor else Color.Transparent)
            .then(clickable)
            .padding(16.dp, 12.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResID),
            contentDescription = "an icon describing: $title",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = TextUnit(titleTextSize, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = contentColor
            )

            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (goesToOtherPage) {
            Icon(
                painter = painterResource(id = R.drawable.other_page_indicator),
                contentDescription = "this preference row leads to another page",
                tint = contentColor,
                modifier = Modifier
                    .size(28.dp)
            )
        }
    }
}

@Composable
fun PreferencesSwitchRow(
    title: String,
    iconResID: Int,
    position: RowPosition,
    checked: Boolean,
    summary: String? = null,
    enabled: Boolean = true,
    showBackground: Boolean = true,
    onRowClick: ((checked: Boolean) -> Unit)? = null,
    onSwitchClick: (checked: Boolean) -> Unit
) {
    val (shape, _) = getDefaultShapeSpacerForPosition(position, 24.dp)

    val backgroundColor = when {
        enabled && showBackground -> {
            MaterialTheme.colorScheme.surfaceVariant
        }

        !enabled && showBackground -> {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }

        else -> {
            Color.Transparent
        }
    }

    val clickable = if (enabled) Modifier.clickable {
        if (onRowClick != null) onRowClick(!checked) else onSwitchClick(!checked)
    } else Modifier

    val clip = if (showBackground) Modifier.clip(shape) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight()
            .then(clip)
            .background(backgroundColor)
            .then(clickable)
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResID),
            contentDescription = "an icon describing: $title",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = darkenColor(MaterialTheme.colorScheme.onSurface, 0.15f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }


        Row(
            modifier = Modifier
                .padding(12.dp, 0.dp, 0.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (onRowClick != onSwitchClick && onRowClick != null) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.width(16.dp))
            }

            Switch(
                checked = checked,
                onCheckedChange = {
                    onSwitchClick(it)
                },
                enabled = enabled
            )
        }
    }
}

@Composable
fun PreferencesSeparatorText(text: String) {
    Text(
        text = text,
        fontSize = TextUnit(16f, TextUnitType.Sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(12.dp)
    )
}

@Composable
fun RadioButtonRow(
    text: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(40.dp)
            .background(Color.Transparent)
            .padding(12.dp, 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(
            selected = checked,
            onClick = {
                onClick()
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .wrapContentSize()
        )
    }
}

@Composable
fun CheckBoxButtonRow(
    text: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(40.dp)
            .background(Color.Transparent)
            .padding(12.dp, 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                onCheckedChange()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                onCheckedChange()
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        val state = rememberScrollState()
        Text(
            text = text,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier
                .wrapContentSize()
                .horizontalScroll(state)
        )
    }
}

/** @param trackIcons needs to be a list of 3 icon res ids */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesThreeStateSwitchRow(
    title: String,
    iconResID: Int,
    position: RowPosition,
    currentPosition: Int,
    summary: String? = null,
    enabled: Boolean = true,
    showBackground: Boolean = true,
    trackIcons: List<Int>,
    onStateChange: (state: Int) -> Unit
) {
    val width = 104.dp
    fun nextPosition() =
        when (currentPosition) {
            0 -> 1
            1 -> 2
            2 -> 0

            else -> 0
        }

    val (shape, _) = getDefaultShapeSpacerForPosition(position, 24.dp)

    val backgroundColor = when {
        enabled && showBackground -> {
            MaterialTheme.colorScheme.surfaceVariant
        }

        !enabled && showBackground -> {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }

        else -> {
            Color.Transparent
        }
    }

    val clickable = if (enabled) Modifier.clickable {
        onStateChange(
            nextPosition()
        )
    } else Modifier

    val clip = if (showBackground) Modifier.clip(shape) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .wrapContentHeight()
            .then(clip)
            .background(backgroundColor)
            .then(clickable)
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResID),
            contentDescription = "an icon describing: $title",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = darkenColor(MaterialTheme.colorScheme.onSurface, 0.15f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(12.dp, 0.dp, 0.dp, 0.dp)
        ) {
            val animatedSliderVal by animateFloatAsState(
                targetValue = currentPosition.toFloat(),
                animationSpec = tween(
                    durationMillis = 200
                ),
                label = "Animate look and feel dark theme slider value"
            )

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .width(width)
                    .border(2.dp, SwitchDefaults.colors().uncheckedBorderColor, CircleShape)
                    .background(SwitchDefaults.colors().uncheckedTrackColor, CircleShape)
                    .padding(8.dp, 0.dp)
                    .align(Alignment.Center)
            ) {
                if (trackIcons.size == 3) {
                    Icon(
                        painter = painterResource(id = trackIcons[0]),
                        contentDescription = null,
                        tint = SwitchDefaults.colors().uncheckedThumbColor,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterStart)
                    )

                    Icon(
                        painter = painterResource(id = trackIcons[1]),
                        contentDescription = null,
                        tint = SwitchDefaults.colors().uncheckedThumbColor,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )

                    Icon(
                        painter = painterResource(id = trackIcons[2]),
                        contentDescription = null,
                        tint = SwitchDefaults.colors().uncheckedThumbColor,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterEnd)
                    )
                }
            }

            Slider(
                value = animatedSliderVal,
                valueRange = 0f..2f,
                onValueChange = {
                    val snapTo = if (it >= 1.4f) {
                        2
                    } else if (it <= 0.6f) {
                        0
                    } else {
                        1
                    }

                    onStateChange(snapTo)
                },
                track = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(40.dp)
                    )
                },
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(SwitchDefaults.colors().checkedTrackColor, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = trackIcons[currentPosition]),
                            contentDescription = null,
                            tint = SwitchDefaults.colors().checkedThumbColor,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                },
                enabled = enabled,
                modifier = Modifier
                    .height(40.dp)
                    .width(width - 12.dp)
                    .align(Alignment.Center)
            )
        }
    }
}
