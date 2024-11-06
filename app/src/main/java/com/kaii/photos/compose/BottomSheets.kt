package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.DrawableItem
import com.kaii.photos.helpers.DrawableText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetEditingViewDrawableTextBottomSheet(
    showBottomSheet: MutableState<Boolean>,
    modifications: SnapshotStateList<DrawableItem>
) {
    val drawableText = modifications.findLast { it is DrawableText } as DrawableText? ?: return
    var hasSavedName by remember { mutableStateOf(false) }

	val coroutineScope = rememberCoroutineScope()
	val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    if (showBottomSheet.value) {
    	keyboardController?.show()

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
			val height = remember(hasSavedName) { if (imeHeight > 72.dp || hasSavedName) imeHeight - 72.dp else 72.dp }

            TextFieldWithConfirm(
                text = text,
                placeholder = "Enter Text",
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(1f)
                    .height(height)
                    // .height(72.dp)
            ) {
                hasSavedName = true

                keyboardController?.hide()
                modifications.remove(drawableText)
                drawableText.text = text.value
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

@Composable
fun TextFieldWithConfirm(
    text: MutableState<String>,
    placeholder: String,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TextField(
            value = text.value,
            onValueChange = {
                text.value = it
            },
            maxLines = 1,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            prefix = {
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.edit),
                        contentDescription = "Edit Icon",
                        modifier = Modifier
                            .size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
                cursorColor = CustomMaterialTheme.colorScheme.primary,
                focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = CustomMaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onConfirm()
                    keyboardController?.hide()
                }
            ),
            shape = RoundedCornerShape(1000.dp, 0.dp, 0.dp, 1000.dp),
            modifier = Modifier
                .weight(1f)
        )

        Row(
            modifier = Modifier
                .height(56.dp)
                .width(32.dp)
                .clip(RoundedCornerShape(0.dp, 1000.dp, 1000.dp, 0.dp))
                .background(CustomMaterialTheme.colorScheme.surfaceContainer)
                .weight(0.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .clickable {
                        onConfirm()
                        keyboardController?.hide()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.file_is_selected_foreground),
                    contentDescription = "Confirm text change",
                    tint = CustomMaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    }

}
