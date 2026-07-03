package com.kaii.photos.compose.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kaii.photos.R
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Do not use background colours for your composable
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
            delay(150.milliseconds)
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
                .clip(
                    if (position == RowPosition.Bottom || position == RowPosition.Single) RoundedCornerShape(
                        0.dp,
                        0.dp,
                        16.dp,
                        16.dp
                    )
                    else RoundedCornerShape(0.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
            shape =
                RoundedCornerShape(cornerRadius, cornerRadius, innerCornerRadius, innerCornerRadius)
            height = spacerHeight
        }

        RowPosition.Middle -> {
            shape = RoundedCornerShape(innerCornerRadius)
            height = spacerHeight
        }

        RowPosition.Bottom -> {
            shape =
                RoundedCornerShape(innerCornerRadius, innerCornerRadius, cornerRadius, cornerRadius)
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
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    usePlatformDefaultWidth: Boolean = true,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = usePlatformDefaultWidth
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(8.dp),
            verticalArrangement = verticalArrangement,
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
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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

@Composable
fun ConfirmCancelRow(
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null,
    confirmColors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    cancelColors: ButtonColors = ButtonDefaults.filledTonalButtonColors()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(56.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (onCancel != null) {
            FilledTonalButton(
                onClick = {
                    onCancel()
                },
                colors = cancelColors
            ) {
                Text(
                    text = stringResource(id = R.string.media_cancel),
                    fontSize = TextUnit(14f, TextUnitType.Sp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledTonalButton( // maybe use normal button for it?
            onClick = {
                onConfirm()
            },
            colors = confirmColors
        ) {
            Text(text = stringResource(id = R.string.media_confirm))
        }
    }
}

@Composable
fun TitleCloseRow(
    title: String,
    closeOffset: Dp = 0.dp,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(8.dp, 0.dp)
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.Center)
        )


        IconButton(
            onClick = {
                onClose()
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = closeOffset)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = "Close this dialog",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HorizontalSeparator() {
    Box(
        modifier = Modifier
            .height(1.dp)
            .padding(16.dp, 0.dp)
            .fillMaxWidth(1f)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(1000.dp))
    )
}

@Composable
fun TallDialogInfoRow(
    title: String,
    info: String,
    @DrawableRes icon: Int,
    position: RowPosition,
    modifier: Modifier = Modifier,
    containerColor: Color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    navigatesAway: Boolean = false,
    enabled: Boolean = true,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val (shape, spacerHeight) = remember(position) {
        getDefaultShapeSpacerForPosition(
            position = position,
            cornerRadius = 32.dp
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth(1f)
            .height(64.dp)
            .clip(shape)
            .background(containerColor.copy(alpha = if (enabled) 1f else 0.4f))
            .combinedClickable(
                enabled = enabled,

                onClick = onClick,

                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "$title: $info",
                tint = contentColor.copy(alpha = if (enabled) 1f else 0.4f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.4f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = info,
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                fontWeight = FontWeight.Normal,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.4f),
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            )

            if (navigatesAway) {
                Icon(
                    painter = painterResource(id = R.drawable.other_page_indicator),
                    contentDescription = stringResource(id = R.string.navigation_away),
                    tint = contentColor.copy(alpha = if (enabled) 1f else 0.4f)
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(spacerHeight)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 1f else 0.4f))
        )
    }
}
