package com.kaii.photos.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
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
				durationMillis = 350
			),
			expandFrom = Alignment.Top
		) + fadeIn(
			animationSpec = tween(
				durationMillis = 350
			)			
		),
		exit = shrinkVertically(
			animationSpec = tween(
				durationMillis = 350
			),
			shrinkTowards = Alignment.Top
		) + fadeOut(
			animationSpec = tween(
				durationMillis = 350
			)			
		),			
	) {
		Column (
			modifier = Modifier
				.wrapContentHeight()
				.clip(RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp))
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

@Composable
fun AnimatableText(first: String, second: String, state: Boolean, modifier: Modifier) {
	AnimatedContent(
		targetState = state,
		label = "Dialog name animated content",
		transitionSpec = {
			(expandHorizontally (
				animationSpec = tween(
					durationMillis = 250
				),
				expandFrom = Alignment.Start
			) + fadeIn(
				animationSpec = tween(
					durationMillis = 250,
				)
			)).togetherWith(
				shrinkHorizontally (
					animationSpec = tween(
						durationMillis = 250
					),
					shrinkTowards = Alignment.End
				) + fadeOut(
					animationSpec = tween(
						durationMillis = 250,
					)
				)
			)
		},
		modifier = Modifier
			.then(modifier)
	) { showFirst ->
		if (showFirst) {
			Text(
				text = first,
				fontWeight = FontWeight.Bold,
				fontSize = TextUnit(18f, TextUnitType.Sp),
				modifier = Modifier
					.then(modifier)
			)
		} else {
			Text(
				text = second,
				fontWeight = FontWeight.Bold,
				fontSize = TextUnit(18f, TextUnitType.Sp),
				modifier = Modifier
					.then(modifier)
			)
		}
	}
}
/** [extraAction] used to reset a [DialogExpandableItem]'s on click*/
@Composable
fun AnimatableTextField(
	state: MutableState<Boolean>,
	string: MutableState<String>,
	doAction: MutableState<Boolean>,
	rowPosition: RowPosition,
	modifier: Modifier = Modifier,
	extraAction: MutableState<Boolean>? = null,
	resetAction: () -> Unit
) {
	var waitForKB by remember { mutableStateOf(false) }
	val focus = remember { FocusRequester() }
	val focusManager = LocalFocusManager.current

	AnimatedContent (
		targetState = state.value,
		label = string.value,
		modifier = Modifier
			.then(modifier),
		transitionSpec = {
			(expandHorizontally (
				animationSpec = tween(
					durationMillis = 250
				),
				expandFrom = Alignment.Start
			) + fadeIn(
				animationSpec = tween(
					durationMillis = 250,
				)
			)).togetherWith(
				shrinkHorizontally (
					animationSpec = tween(
						durationMillis = 250
					),
					shrinkTowards = Alignment.End
				) + fadeOut(
					animationSpec = tween(
						durationMillis = 250,
					)
				)
			)
		}
	) { showFirst ->
		if (showFirst) {
			TextField(
				value = string.value,
				onValueChange = {
					string.value = it
				},
				keyboardActions = KeyboardActions(
					onDone = {
						focusManager.clearFocus()
						doAction.value = true
						waitForKB = true
					}
				),
				textStyle = LocalTextStyle.current.copy(
					fontSize = TextUnit(16f, TextUnitType.Sp),
					textAlign = TextAlign.Start,
					color = CustomMaterialTheme.colorScheme.onSurface,
				),
				keyboardOptions = KeyboardOptions(
					capitalization = KeyboardCapitalization.None,
					autoCorrectEnabled = false,
					keyboardType = KeyboardType.Ascii,
					imeAction = ImeAction.Done,
					showKeyboardOnFocus = true
				),
				trailingIcon = {
					IconButton(
						onClick = {
							focusManager.clearFocus()
							doAction.value = false
							waitForKB = true
						}
					) {
						Icon(
							painter = painterResource(id = R.drawable.close),
							contentDescription = "Cancel filename change button"
						)
					}
				},
				shape = RoundedCornerShape(16.dp),
				colors = TextFieldDefaults.colors().copy(
					unfocusedContainerColor = CustomMaterialTheme.colorScheme.surfaceVariant,
					unfocusedIndicatorColor = Color.Transparent,
					unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
					focusedIndicatorColor = Color.Transparent,
					focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
					focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceVariant
				),
				modifier = Modifier
					.focusRequester(focus)
					.fillMaxWidth(1f)
			)

			LaunchedEffect(Unit) {
				delay(500)
				focus.requestFocus()

			}

			LaunchedEffect(waitForKB) {
				if (!waitForKB) return@LaunchedEffect

				delay(200)

				resetAction()
				state.value = false
				waitForKB = false
			}
		} else {
			Column (
				modifier = Modifier
					.wrapContentHeight()
			) {
				DialogClickableItem(
					text = "Rename",
					iconResId = R.drawable.edit,
					position = rowPosition,
				) {
					state.value = true
					extraAction?.value = false
				}
			}
		}
	}
}

@Composable
fun DialogInfoText(firstText: String, secondText: String, iconResId: Int) {
	val context = LocalContext.current
	
	Row (
		modifier = Modifier
			.height(36.dp)
			.padding(10.dp, 4.dp)
			.clickable {
				val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
				val clipData = ClipData.newPlainText(firstText, secondText)
				clipboardManager.setPrimaryClip(clipData)
			},
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon (
			painter = painterResource(id = iconResId),
			contentDescription = "$firstText: $secondText",
			tint = CustomMaterialTheme.colorScheme.onSurface,
			modifier = Modifier
				.size(16.dp)
		)

		Spacer(modifier = Modifier.width(8.dp))

		val state = rememberScrollState()
		Text(
			text = "$firstText: ",
			color = CustomMaterialTheme.colorScheme.onSurface,
			style = TextStyle(
				textAlign = TextAlign.Start,
				fontSize = TextUnit(14f, TextUnitType.Sp),
			),
			maxLines = 1,
			softWrap = true,
			modifier = Modifier
				.wrapContentWidth()
		)
				
		Text(
			text = secondText,
			color = CustomMaterialTheme.colorScheme.onSurface,
			style = TextStyle(
				textAlign = TextAlign.Start,
				fontSize = TextUnit(14f, TextUnitType.Sp),
			),
			maxLines = 1,
			softWrap = true,
			modifier = Modifier
				.weight(1f)
				.horizontalScroll(state)
		)
	}
}
