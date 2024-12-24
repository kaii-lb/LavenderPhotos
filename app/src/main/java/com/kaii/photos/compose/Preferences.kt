package com.kaii.photos.compose

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    modifier: Modifier = Modifier,
    summary: String? = null,
    goesToOtherPage: Boolean = false,
    showBackground: Boolean = true,
    titleTextSize: Float = 18f,
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
            .background(if (showBackground) CustomMaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .then(clickable)
            .padding(16.dp, 12.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResID),
            contentDescription = "an icon describing: $title",
            tint = CustomMaterialTheme.colorScheme.onBackground,
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
                color = CustomMaterialTheme.colorScheme.onSurface
            )

            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 3,
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
            tint = CustomMaterialTheme.colorScheme.onBackground,
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
                color = CustomMaterialTheme.colorScheme.onSurface
            )

            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    textAlign = TextAlign.Start,
                    color = darkenColor(CustomMaterialTheme.colorScheme.onSurface, 0.15f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }


        Row (
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
	                    .background(CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
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
        color = CustomMaterialTheme.colorScheme.primary,
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
    Row (
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

        Spacer (modifier = Modifier.width(16.dp))

        Text (
            text = text,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = CustomMaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .wrapContentSize()
        )
    }
}
