package com.kaii.photos.compose

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.darkenColor

@Composable
fun DialogClickableItem(text: String, iconResId: Int, position: RowPosition, action: (() -> Unit)? = null) {
	val buttonHeight = 40.dp

	val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position)

	val modifier = if (action != null) Modifier.clickable { action() } else Modifier

	Row (
		modifier = Modifier
			.fillMaxWidth(1f)
			.height(buttonHeight)
			.clip(shape)
			.background(CustomMaterialTheme.colorScheme.surfaceVariant)
			.wrapContentHeight(align = Alignment.CenterVertically)
			.then(modifier)
			.padding(8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Start
	) {
		Icon (
			painter = painterResource(id = iconResId),
            contentDescription = "icon describing: $text",
			modifier = Modifier
				.size(20.dp)
		)

		Spacer (modifier = Modifier.width(12.dp))
		
		Text (
			text = text,
			fontSize = TextUnit(16f, TextUnitType.Sp),
			textAlign = TextAlign.Start,
		)
	}

	Spacer (
		modifier = Modifier
			.height(spacerHeight)
			.background(CustomMaterialTheme.colorScheme.surface)
	)
}

/** Do not use background colors for your composable */
@Composable
fun DialogExpandableItem(text: String, iconResId: Int, position: RowPosition, content: @Composable () -> Unit) {
	val buttonHeight = 40.dp

	val (firstShape, firstSpacerHeight) = getDefaultShapeSpacerForPosition(position)
	var shape by remember { mutableStateOf(firstShape) }
	var spacerHeight by remember { mutableStateOf(firstSpacerHeight) }
	var expanded by remember { mutableStateOf(false) }

	Row (
		modifier = Modifier
			.fillMaxWidth(1f)
			.height(buttonHeight)
			.clip(shape)
			.background(CustomMaterialTheme.colorScheme.surfaceVariant)
			.wrapContentHeight(align = Alignment.CenterVertically)
			.clickable {
				if (!expanded) {
					expanded = true
					shape = firstShape.copy(
						bottomEnd = CornerSize(0.dp),
						bottomStart = CornerSize(0.dp)
					)
					spacerHeight = 0.dp
				} else {
					expanded = false
					shape = firstShape
					spacerHeight = firstSpacerHeight
				}
			}
			.padding(8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Start
	) {
		Icon (
			painter = painterResource(id = iconResId),
			contentDescription = "icon describing: $text",
			modifier = Modifier
				.size(20.dp)
		)

		Spacer (modifier = Modifier.width(12.dp))

		Text (
			text = text,
			fontSize = TextUnit(16f, TextUnitType.Sp),
			textAlign = TextAlign.Start,
		)
	}

	var neededHeight by remember { mutableStateOf(100.dp) }
	val height by androidx.compose.animation.core.animateDpAsState(
		targetValue = if (expanded) 128.dp else 0.dp,
		label = "height of other options",
		animationSpec = tween(
			durationMillis = 500
		)
	)

	Column (
		modifier = Modifier
			.height(height)
			.fillMaxWidth(1f)
			.background(darkenColor(CustomMaterialTheme.colorScheme.surfaceVariant, 0.3f))
	) {
		// Column (
		// 	modifier = Modifier
		// 		.wrapContentHeight()
		// 		.onGloballyPositioned {
		// 			neededHeight = it.size.height.dp
		// 		}
		// ) {
			content()
		// }
	}

	Spacer (
		modifier = Modifier
			.height(spacerHeight)
			.background(CustomMaterialTheme.colorScheme.surface)
	)
}

private fun getDefaultShapeSpacerForPosition(position: RowPosition) : Pair<RoundedCornerShape, Dp> {
	val shape: RoundedCornerShape
	val spacerHeight: Dp

	when(position) {
		RowPosition.Top -> {
			shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)
			spacerHeight = 2.dp
		}
		RowPosition.Middle -> {
			shape = RoundedCornerShape(0.dp)
			spacerHeight = 2.dp
		}
		RowPosition.Bottom -> {
			shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
			spacerHeight = 0.dp
		}
		RowPosition.Single -> {
			shape = RoundedCornerShape(16.dp)
			spacerHeight = 0.dp
		}
	}

	return Pair(shape, spacerHeight)
}
