package com.kaii.photos.compose.dialogs

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.R
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.CheckBoxButtonRow
import com.kaii.photos.compose.FullWidthDialogButton
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.createDirectoryPicker
import kotlinx.coroutines.launch

@Composable
fun ConfirmationDialog(
    showDialog: MutableState<Boolean>,
    dialogTitle: String,
    confirmButtonLabel: String,
    action: () -> Unit
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val modifier = if (isLandscape)
        Modifier.width(256.dp)
    else
        Modifier

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            modifier = modifier,
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        action()
                    }
                ) {
                    Text(
                        text = confirmButtonLabel,
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}

@Composable
fun ConfirmationDialogWithBody(
    showDialog: MutableState<Boolean>,
    dialogTitle: String,
    dialogBody: String,
    confirmButtonLabel: String,
    showCancelButton: Boolean = true,
    action: () -> Unit
) {
    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val modifier = if (isLandscape)
        Modifier.width(256.dp)
    else
        Modifier

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            modifier = modifier,
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        action()
                    }
                ) {
                    Text(
                        text = confirmButtonLabel,
                        fontSize = TextUnit(14f, TextUnitType.Sp)
                    )
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = TextUnit(16f, TextUnitType.Sp)
                )
            },
            text = {
                Text(
                    text = dialogBody,
                    fontSize = TextUnit(14f, TextUnitType.Sp)
                )
            },
            dismissButton = {
                if (showCancelButton) {
                    Button(
                        onClick = {
                            showDialog.value = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = TextUnit(14f, TextUnitType.Sp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}

@Composable
fun TextEntryDialog(
    title: String,
    placeholder: String? = null,
    onConfirm: (text: String) -> Boolean,
    onValueChange: (text: String) -> Boolean,
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val keyboardController = LocalSoftwareKeyboardController.current
        var text by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }

        TextField(
            value = text,
            onValueChange = {
                text = it
                showError = onValueChange(it)
            },
            maxLines = 1,
            singleLine = true,
            placeholder = {
                if (placeholder != null) {
                    Text(
                        text = placeholder,
                        fontSize = TextUnit(16f, TextUnitType.Sp)
                    )
                }
            },
            suffix = {
                if (showError) {
                    Row {
                        val coroutineScope = rememberCoroutineScope()

                        Icon(
                            painter = painterResource(id = R.drawable.error_2),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        LavenderSnackbarController.pushEvent(
                                            LavenderSnackbarEvents.MessageEvent(
                                                message = "Paths need to be relative",
                                                iconResId = R.drawable.error_2,
                                                duration = SnackbarDuration.Short
                                            )
                                        )
                                    }
                                }
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                }
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FullWidthDialogButton(
            text = "Confirm",
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single,
            enabled = !showError
        ) {
            showError = !onConfirm(text)
        }
    }
}

@Composable
fun ExplanationDialog(
    title: String,
    explanation: String,
    showDialog: MutableState<Boolean>,
    showPreviousDialog: MutableState<Boolean>? = null
) {
    ExplanationDialogBase(
        title = title,
        body = {
            Text(
                text = explanation,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.wrapContentSize()
            )
        },
        showDialog = showDialog,
        showPreviousDialog = showPreviousDialog
    )
}

@Composable
fun AnnotatedExplanationDialog(
    title: String,
    annotatedExplanation: AnnotatedString,
    showDialog: MutableState<Boolean>,
    showPreviousDialog: MutableState<Boolean>? = null
) {
    ExplanationDialogBase(
        title = title,
        body = {
            Text(
                text = annotatedExplanation,
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.wrapContentSize()
            )
        },
        showDialog = showDialog,
        showPreviousDialog = showPreviousDialog
    )
}

@Composable
private fun ExplanationDialogBase(
    title: String,
    body: @Composable () -> Unit,
    showDialog: MutableState<Boolean>,
    showPreviousDialog: MutableState<Boolean>? = null
) {
    showPreviousDialog?.value = false

    LavenderDialogBase(
        modifier = Modifier
            .animateContentSize(),
        onDismiss = {
            showDialog.value = false
        }
    ) {
        Text(
            text = title,
            fontSize = TextUnit(18f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.wrapContentSize()
        )

        Spacer(modifier = Modifier.height(24.dp))

        body()

        Spacer(modifier = Modifier.height(24.dp))

        FullWidthDialogButton(
            text = "Okay",
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single
        ) {
            showDialog.value = false
            showPreviousDialog?.value = true
        }
    }
}

@Composable
fun AlbumPathsDialog(
	albumInfo: AlbumInfo,
	onConfirm: (selectedPaths: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
	val selectedPaths = remember { mutableStateListOf<String>().apply { addAll(albumInfo.paths)  } }

	LavenderDialogBase(
		modifier = Modifier
			.animateContentSize(),
		onDismiss = onDismiss
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth(1f)
		) {
			Text(
			    text = albumInfo.name,
			    fontSize = TextUnit(18f, TextUnitType.Sp),
			    fontWeight = FontWeight.Bold,
			    color = MaterialTheme.colorScheme.onSurface,
			    modifier = Modifier
			    	.wrapContentSize()
			    	.align(Alignment.Center)
			)

            val activityLauncher = createDirectoryPicker { path ->
                if (path != null) {
                    mainViewModel.settings.AlbumsList.editInPlace(
                        albumId = albumInfo.id,
                        newInfo = albumInfo.copy(
                        	paths = albumInfo.paths.toMutableList().apply {
                        		if (!contains(path)) {
                        			add(path)
                        		}
                        	}
                        )
                    )
                }
            }

			IconButton(
			    onClick = {
			        activityLauncher.launch(null)
			    },
			    modifier = Modifier
			        .align(Alignment.CenterEnd)
			        .padding(0.dp, 0.dp, 0.dp, 4.dp)
			) {
			    Icon(
			        painter = painterResource(id = R.drawable.add),
			        contentDescription = "add a new album",
			        modifier = Modifier
			            .size(24.dp)
			    )
			}
		}

		Spacer(modifier = Modifier.height(24.dp))

		val groupedPaths = remember(albumInfo.paths) {
			albumInfo.paths.groupBy {
				(it.removeSuffix("/") + "/").split("/")[0]
			}
		}

		LazyColumn (
			modifier = Modifier
                .heightIn(max = 250.dp)
                .fillMaxWidth(1f)
		) {
			itemsIndexed(
                items = groupedPaths.keys.toList()
			) { index, group ->
                val rowPosition = when {
                    groupedPaths.size == 1 -> {
                        RowPosition.Single
                    }

                    index == 0 -> {
                        RowPosition.Top
                    }

                    index == groupedPaths.size - 1 -> {
                        RowPosition.Bottom
                    }

                    else -> {
                        RowPosition.Middle
                    }
                }

                val expanded = remember { mutableStateOf(false) }

                DialogExpandableItem(
                    text = group,
                    iconResId = R.drawable.edit, // TODO: change to drop down icon
                    position = rowPosition,
                    expanded = expanded
                ) {
                    val paths = groupedPaths[group]
                    paths?.forEach { path ->
                        CheckBoxButtonRow(
                            text = path,
                            checked = selectedPaths.contains(path)
                        ) {
                            if (selectedPaths.contains(path)) {
                                selectedPaths.remove(path)
                            } else {
                                selectedPaths.add(path)
                            }
                        }
                    }
                }
			}
		}


		Spacer(modifier = Modifier.height(24.dp))

		FullWidthDialogButton(
		    text = "Confirm",
		    color = MaterialTheme.colorScheme.primary,
		    textColor = MaterialTheme.colorScheme.onPrimary,
		    position = RowPosition.Single
		) {
			onConfirm(selectedPaths)
            onDismiss()
		}
	}
}
