package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.DrawableItem
import com.kaii.photos.helpers.DrawableText
import com.kaii.photos.helpers.RowPosition
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetEditingViewDrawableTextBottomSheet(
    showBottomSheet: MutableState<Boolean>,
    modifications: SnapshotStateList<DrawableItem>,
    textMeasurer: TextMeasurer
) {
    val drawableText = modifications.findLast { it is DrawableText } as DrawableText? ?: return
    var hasSavedName by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                keyboardController?.hide()

                if (!hasSavedName) {
                    modifications.remove(drawableText)
                }

                showBottomSheet.value = false
            },
            sheetState = sheetState,
            containerColor = CustomMaterialTheme.colorScheme.background,
            contentWindowInsets = { WindowInsets.ime.add(WindowInsets.navigationBars) },
        ) {
            val text = remember { mutableStateOf(drawableText.text) }

            val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current).dp
            val height = remember(hasSavedName) { if (imeHeight > 72.dp || hasSavedName) imeHeight - 96.dp else 72.dp }

            val localTextStyle = LocalTextStyle.current
            val defaultStyle = DrawableText.Styles.Default.style

            TextFieldWithConfirm(
                text = text,
                placeholder = "Enter Text",
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(1f)
                    .height(height)
            ) {
                hasSavedName = true

                val textLayout = textMeasurer.measure(
                    text = text.value,
                    style = localTextStyle.copy(
                        color = drawableText.paint.color,
                        fontSize = TextUnit(
                            drawableText.paint.strokeWidth,
                            TextUnitType.Sp
                        ),
                        textAlign = defaultStyle.textAlign,
                        platformStyle = defaultStyle.platformStyle,
                        lineHeightStyle = defaultStyle.lineHeightStyle,
                        baselineShift = defaultStyle.baselineShift
                    )
                )

                keyboardController?.hide()
                modifications.remove(drawableText)

                drawableText.text = text.value
                drawableText.size = textLayout.size
                modifications.add(drawableText)

                coroutineScope.launch {
                    sheetState.hide()
                    showBottomSheet.value = false
                    hasSavedName = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CroppingRatioBottomSheet(
    showBottomSheet: MutableState<Boolean>,
    originalImageRatio: Float,
    onSetCroppingRatio: (ratio: Float) -> Unit
) {
    var ratio by remember { mutableFloatStateOf(0f) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet.value = false
            },
            sheetState = sheetState,
            containerColor = CustomMaterialTheme.colorScheme.background,
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
                ClickableRow(
                    title = "Freeform",
                    position = RowPosition.Top,
                    selected = ratio == 0f
                ) {
                    ratio = 0f
                    onSetCroppingRatio(0f)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "Image Ratio",
                    position = RowPosition.Middle,
                    selected = ratio == originalImageRatio
                ) {
                    ratio = originalImageRatio
                    onSetCroppingRatio(originalImageRatio)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "Square",
                    position = RowPosition.Middle,
                    selected = ratio == 1f
                ) {
                    ratio = 1f
                    onSetCroppingRatio(1f)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "21:9",
                    position = RowPosition.Middle,
                    selected = ratio == 21f / 9f
                ) {
                    ratio = 21f / 9f
                    onSetCroppingRatio(21f / 9f)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "9:16 (vertical)",
                    position = RowPosition.Middle,
                    selected = ratio == 9f / 16f
                ) {
                    ratio = 9f / 16f
                    onSetCroppingRatio(9f / 16f)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "16:9 (horizontal)",
                    position = RowPosition.Middle,
                    selected = ratio == 16f / 9f
                ) {
                    ratio = 16f / 9f
                    onSetCroppingRatio(16f / 9f)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "5:4",
                    position = RowPosition.Middle,
                    selected = ratio == 1.25f
                ) {
                    ratio = 1.25f
                    onSetCroppingRatio(1.25f)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "4:3",
                    position = RowPosition.Middle,
                    selected = ratio == 4f / 3f
                ) {
                    ratio = 4f / 3f
                    onSetCroppingRatio(4f / 3f)
                    showBottomSheet.value = false
                }

                ClickableRow(
                    title = "3:2",
                    position = RowPosition.Bottom,
                    selected = ratio == 1.5f
                ) {
                    ratio = 1.5f
                    onSetCroppingRatio(1.5f)
                    showBottomSheet.value = false
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

    Column(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth(1f)
            .clip(shape)
            .background(CustomMaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                action()
            }
            .padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            color = CustomMaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )

        if (selected) {
            Icon(
                painter = painterResource(id = R.drawable.file_is_selected_foreground),
                contentDescription = "this cropping ratio is selected",
                tint = CustomMaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.End)
            )
        }
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(CustomMaterialTheme.colorScheme.background)
    )
}
