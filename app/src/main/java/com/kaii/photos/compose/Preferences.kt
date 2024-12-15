package com.kaii.photos.compose

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.darkenColor

@Composable
fun PreferencesRow(
    title: String,
    iconResID: Int,
    position: RowPosition,
    summary: String? = null,
    goesToOtherPage: Boolean = false,
    showBackground: Boolean = true,
    titleTextSize: Float = 18f,
    height: Dp = 72.dp,
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
            .height(height)
            .then(clip)
            .wrapContentHeight(align = Alignment.CenterVertically)
            .background(if (showBackground) CustomMaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .then(clickable)
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResID),
            contentDescription = "an icon describing: $title",
            tint = CustomMaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .height(height)
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = TextUnit(titleTextSize, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = CustomMaterialTheme.colorScheme.onSurface
            )

            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (goesToOtherPage) {
            Icon(
                painter = painterResource(id = R.drawable.other_page_indicator),
                contentDescription = "this preference row leads to another page",
                tint = CustomMaterialTheme.colorScheme.onBackground,
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
    checked: State<Boolean>,
    summary: String? = null,
    enabled: Boolean = true,
    showBackground: Boolean = true,
    height: Dp = 72.dp,
    onSwitch: (checked: Boolean) -> Unit
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position, 24.dp)

    val backgroundColor = when {
        enabled && showBackground -> {
            CustomMaterialTheme.colorScheme.surfaceVariant
        }

        !enabled && showBackground -> {
            CustomMaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }

        else -> {
            Color.Transparent
        }
    }

    val clickable = if (enabled) Modifier.clickable {
        onSwitch(!checked.value)
    } else Modifier

	val clip = if (showBackground) Modifier.clip(shape) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(height)
            .then(clip)
            .wrapContentHeight(align = Alignment.CenterVertically)
            .background(backgroundColor)
            .then(clickable)
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResID),
            contentDescription = "an icon describing: $title",
            tint = CustomMaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .height(height)
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = CustomMaterialTheme.colorScheme.onSurface
            )

            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = darkenColor(CustomMaterialTheme.colorScheme.onSurface, 0.15f),
                    maxLines = 1,
                    modifier = Modifier
                    	.padding(0.dp, 0.dp, 8.dp, 0.dp)
                        .basicMarquee(
                        	iterations = 3,
                            animationMode = MarqueeAnimationMode.Immediately,
                            repeatDelayMillis = 3000,
                            initialDelayMillis = 3000
                        )
                )
            }
        }

        Switch(
            checked = checked.value,
            onCheckedChange = {
                onSwitch(it)
            },
            enabled = enabled
        )
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(CustomMaterialTheme.colorScheme.background)
    )
}
