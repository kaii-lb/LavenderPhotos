package com.kaii.photos.compose.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kaii.photos.R
import com.kaii.photos.compose.ConfirmCancelRow
import com.kaii.photos.helpers.ExtendedMaterialTheme
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.darkenColor
import kotlinx.coroutines.delay

@Composable
fun DialogClickableItem(
    text: String,
    @DrawableRes iconResId: Int,
    position: RowPosition,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    action: (() -> Unit)? = null
) {
    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position)

    val clickableModifier = if (action != null && enabled) Modifier.clickable { action() } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth(1f)
            .height(40.dp)
            .clip(shape)
            .background(
            	if (enabled) MaterialTheme.colorScheme.surfaceVariant
            	else darkenColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), 0.1f)
            )
            .wrapContentHeight(align = Alignment.CenterVertically)
            .then(clickableModifier)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = "icon describing: $text",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            textAlign = TextAlign.Start,
            modifier = Modifier
            	.wrapContentSize()
        )
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(MaterialTheme.colorScheme.surface)
    )
}

/** Do not use background colors for your composable
currently you need to calculate dp height of your composable manually */
@Composable
fun DialogExpandableItem(
	text: String,
	@DrawableRes iconResId: Int,
	position: RowPosition,
	expanded: MutableState<Boolean>,
	content: @Composable ColumnScope.() -> Unit
) {
    val buttonHeight = 40.dp

    val (firstShape, firstSpacerHeight) = getDefaultShapeSpacerForPosition(position)
    var shape by remember { mutableStateOf(firstShape) }
    var spacerHeight by remember { mutableStateOf(firstSpacerHeight) }

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(buttonHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .wrapContentHeight(align = Alignment.CenterVertically)
            .clickable {
                expanded.value = !expanded.value
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = "icon describing: $text",
            modifier = Modifier
                .size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
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
        enter = expandVertically(
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
        Column(
            modifier = Modifier
                .wrapContentHeight()
                // .clip(RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp))
                .background(darkenColor(MaterialTheme.colorScheme.surfaceVariant, 0.2f))
        ) {
            content()
        }
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(MaterialTheme.colorScheme.surface)
    )
}

fun getDefaultShapeSpacerForPosition(
    position: RowPosition,
    cornerRadius: Dp = 16.dp,
    innerCornerRadius: Dp = 0.dp,
    spacerHeight: Dp = 2.dp
): Pair<RoundedCornerShape, Dp> {
    val shape: RoundedCornerShape
    val height: Dp

    when (position) {
        RowPosition.Top -> {
            shape = RoundedCornerShape(cornerRadius, cornerRadius, innerCornerRadius, innerCornerRadius)
            height = spacerHeight
        }

        RowPosition.Middle -> {
            shape = RoundedCornerShape(innerCornerRadius)
            height = spacerHeight
        }

        RowPosition.Bottom -> {
            shape = RoundedCornerShape(innerCornerRadius, innerCornerRadius, cornerRadius, cornerRadius)
            height = 0.dp
        }

        RowPosition.Single -> {
            shape = RoundedCornerShape(cornerRadius)
            height = 0.dp
        }
    }

    return Pair(shape, height)
}

@Composable
fun AnimatableText(
	first: String,
	second: String,
	state: Boolean,
	modifier: Modifier,
) {
    AnimatedContent(
        targetState = state,
        label = "Dialog name animated content",
        transitionSpec = {
            (expandHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                ),
                expandFrom = Alignment.Start
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 350,
                )
            )).togetherWith(
                shrinkHorizontally(
                    animationSpec = tween(
                        durationMillis = 350
                    ),
                    shrinkTowards = Alignment.End
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 350,
                    )
                )
            )
        },
        modifier = modifier
        	.wrapContentHeight()
    ) { showFirst ->
        if (showFirst) {
            Text(
                text = first,
                fontWeight = FontWeight.Bold,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                modifier = modifier
                    .wrapContentSize()
            )
        } else {
            Text(
                text = second,
                fontWeight = FontWeight.Bold,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                modifier = modifier
                    .wrapContentSize()
            )
        }
    }
}

@Composable
fun DialogInfoText(
	firstText: String,
	secondText: String,
	iconResId: Int,
	color: Color = ExtendedMaterialTheme.colorScheme.expandableDialogBackground,
	contentColor: Color = MaterialTheme.colorScheme.onSurface,
	onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .height(36.dp)
            .background(color)
            .padding(10.dp, 4.dp)
            .clickable {
		    	if (onClick == null) {
		    		val clipboardManager =
		    		    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		    		val clipData = ClipData.newPlainText(firstText, secondText)
		    		clipboardManager.setPrimaryClip(clipData)
		    	} else onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = "$firstText: $secondText",
            tint = contentColor,
            modifier = Modifier
                .size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        val state = rememberScrollState()
        Text(
            text = if (firstText == "") "" else "$firstText: ",
            color = contentColor,
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
            color = contentColor,
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

@Composable
fun LoadingDialog(
    title: String,
    body: String
) {
    LavenderDialogBase(
        onDismiss = {} // never allow dismissal
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = body,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceDim,
            strokeCap = StrokeCap.Round,
            gapSize = 2.dp,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(1f)
        )
    }
}

@Composable
fun LavenderDialogBase(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(ExtendedMaterialTheme.colorScheme.dialogSurface) // brightenColor(MaterialTheme.colorScheme.surface, 0.1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
fun InfoRow(
    text: String,
    @DrawableRes iconResId: Int,
    opacity: Float = 1f,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(40.dp)
            .padding(16.dp, 8.dp, 8.dp, 8.dp)
            .alpha(opacity),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = text,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
            onClick = {
                onRemove()
            }
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "Remove this tab",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(20.dp)
            )
        }
    }
}

@Composable
fun SelectableButtonListDialog(
    title: String,
    body: String? = null,
    showDialog: MutableState<Boolean>,
    buttons: @Composable ColumnScope.() -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            showDialog.value = false
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(brightenColor(MaterialTheme.colorScheme.surface, 0.1f))
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .wrapContentSize()
            )

            if (body != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = body,
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(12.dp, 0.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .wrapContentSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                buttons()
            }


            ConfirmCancelRow(
                onConfirm = {
                    onConfirm()
                    showDialog.value = false
                }
            )
        }
    }
}

@Composable
fun ReorderableRadioButtonRow(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            painter = painterResource(id = R.drawable.reorderable),
            contentDescription = "this item can be dragged and reordered",
            modifier = Modifier
                .size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))
    }
}
