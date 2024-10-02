package com.kaii.photos.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
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
import kotlinx.coroutines.delay

@Composable
fun DialogClickableItem(text: String, iconResId: Int, position: RowPosition, action: (() -> Unit)? = null) {
	val buttonHeight = 40.dp

	val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position)

	val clickableModifier = if (action != null) Modifier.clickable { action() } else Modifier

	Row (
		modifier = Modifier
			.fillMaxWidth(1f)
			.height(buttonHeight)
			.clip(shape)
			.background(CustomMaterialTheme.colorScheme.surfaceVariant)
			.wrapContentHeight(align = Alignment.CenterVertically)
			.then(clickableModifier)
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

/** Do not use background colors for your composable
currently you need to calculate dp height of your composable manually */
@Composable
fun DialogExpandableItem(text: String, iconResId: Int, position: RowPosition, expanded: MutableState<Boolean>, content: @Composable () -> Unit) {
	val buttonHeight = 40.dp

	val (firstShape, firstSpacerHeight) = getDefaultShapeSpacerForPosition(position)
	var shape by remember { mutableStateOf(firstShape) }
	var spacerHeight by remember { mutableStateOf(firstSpacerHeight) }

	Row (
		modifier = Modifier
			.fillMaxWidth(1f)
			.height(buttonHeight)
			.clip(shape)
			.background(CustomMaterialTheme.colorScheme.surfaceVariant)
			.wrapContentHeight(align = Alignment.CenterVertically)
			.clickable {
				expanded.value = !expanded.value
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

	LaunchedEffect(key1 = expanded.value) {
		if (expanded.value) {
			shape = firstShape.copy(
				bottomEnd = CornerSize(0.dp),
				bottomStart = CornerSize(0.dp)
			)
			spacerHeight = 0.dp
		} else {
			delay(150)
			shape = firstShape
			spacerHeight = firstSpacerHeight
		}		
	}

	AnimatedVisibility(
		visible = expanded.value,
		modifier = Modifier
			.fillMaxWidth(1f),
		enter = expandVertically (
			animationSpec = tween(
				durationMillis = 250
			),
			expandFrom = Alignment.Top
		) + fadeIn(
			animationSpec = tween(
				durationMillis = 250
			)			
		),
		exit = shrinkVertically(
			animationSpec = tween(
				durationMillis = 250
			),
			shrinkTowards = Alignment.Top
		) + fadeOut(
			animationSpec = tween(
				durationMillis = 250
			)			
		),			
	) {
		Column (
			modifier = Modifier
				.wrapContentHeight()
				.background(darkenColor(CustomMaterialTheme.colorScheme.surfaceVariant, 0.2f))
		) {
			content()
		}
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
