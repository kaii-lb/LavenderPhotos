package com.kaii.photos.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.DocumentsContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.MainActivity.Companion.startForResult
import com.kaii.photos.R
import com.kaii.photos.compose.grids.MoveCopyAlbumListView
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.User
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.darkenColor
import com.kaii.photos.helpers.getBaseInternalStorageDirectory
import com.kaii.photos.helpers.getExifDataForMedia
import com.kaii.photos.helpers.rememberVibratorManager
import com.kaii.photos.helpers.renameDirectory
import com.kaii.photos.helpers.renameImage
import com.kaii.photos.helpers.vibrateShort
import com.kaii.photos.mediastore.MediaStoreData
import kotlinx.coroutines.delay

@Composable
fun DialogClickableItem(text: String, iconResId: Int, position: RowPosition, action: (() -> Unit)? = null) {
    val buttonHeight = 40.dp

    val (shape, spacerHeight) = getDefaultShapeSpacerForPosition(position)

    val clickableModifier = if (action != null) Modifier.clickable { action() } else Modifier

    Row(
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

    Spacer(
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

    Row(
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
                .clip(RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp))
                .background(darkenColor(CustomMaterialTheme.colorScheme.surfaceVariant, 0.2f))
        ) {
            content()
        }
    }

    Spacer(
        modifier = Modifier
            .height(spacerHeight)
            .background(CustomMaterialTheme.colorScheme.surface)
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
fun AnimatableText(first: String, second: String, state: Boolean, modifier: Modifier) {
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

@Composable
fun DialogInfoText(firstText: String, secondText: String, iconResId: Int) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .height(36.dp)
            .padding(10.dp, 4.dp)
            .clickable {
                val clipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText(firstText, secondText)
                clipboardManager.setPrimaryClip(clipData)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainAppDialog(
    showDialog: MutableState<Boolean>,
    currentView: MutableState<MainScreenViewType>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    val vibratorManager = rememberVibratorManager()
    val navController = LocalNavController.current

    if (showDialog.value) {
        Dialog(
            onDismissRequest = {
                showDialog.value = false
            }
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(brightenColor(CustomMaterialTheme.colorScheme.surface, 0.1f))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                ) {
                    IconButton(
                        onClick = {
                            showDialog.value = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close dialog button",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }

                    val splitBy = Regex("(?=[A-Z])")
                    Text(
                        text = currentView.value.name.split(splitBy)[1],
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val storedName = mainViewModel.settings.User.getUsername().collectAsStateWithLifecycle(initialValue = null).value ?: return@Row

					var originalName by remember { mutableStateOf(storedName) }

                    var username by remember {
                        mutableStateOf(
                            originalName
                        )
                    }

                    GlideImage(
                        model = R.drawable.cat_picture,
                        contentDescription = "User profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(1000.dp))
                    ) {
                        it.override(256)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val focus = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current
                    var changeName by remember { mutableStateOf(false) }
                    var backPressedCallbackEnabled by remember { mutableStateOf(false) }

                    LaunchedEffect(key1 = changeName) {
                        focusManager.clearFocus()

                        if (!changeName && username != originalName) {
                            username = originalName
                            return@LaunchedEffect
                        }

                        mainViewModel.settings.User.setUsername(username)
                        originalName = username
                        changeName = false
                    }

                    TextField(
                        value = username,
                        onValueChange = { newVal ->
                            username = newVal
                        },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = TextUnit(16f, TextUnitType.Sp),
                            textAlign = TextAlign.Start,
                            color = CustomMaterialTheme.colorScheme.onSurface,
                        ),
                        maxLines = 1,
                        colors = TextFieldDefaults.colors().copy(
                            unfocusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            unfocusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                            unfocusedTrailingIconColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedTextColor = CustomMaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = CustomMaterialTheme.colorScheme.surfaceVariant,
                            focusedTrailingIconColor = CustomMaterialTheme.colorScheme.onSurface,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done,
                            showKeyboardOnFocus = true
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                changeName = true
                            },
                        ),
                        trailingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = "Cancel filename change button",
                                modifier = Modifier
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    ) {
                                        focusManager.clearFocus()
                                        changeName = false
                                        username = originalName
                                    }
                            )
                        },
                        shape = RoundedCornerShape(1000.dp),
                        modifier = Modifier
                            .focusRequester(focus)
                            .onFocusChanged {
                                backPressedCallbackEnabled = it.isFocused
                            }
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .wrapContentHeight()
                ) {
                    if (currentView.value != MainScreenViewType.AlbumsGridView && currentView.value != MainScreenViewType.SecureFolder) {
                        DialogClickableItem(
                            text = "Select",
                            iconResId = R.drawable.check_item,
                            position = RowPosition.Top,
                        ) {
                            showDialog.value = false
                            selectedItemsList.clear()
                            selectedItemsList.add(MediaStoreData())
                            vibratorManager.vibrateShort()
                        }
                    }

                    if (currentView.value == MainScreenViewType.AlbumsGridView) {
                        DialogClickableItem(
                            text = "Add an album",
                            iconResId = R.drawable.add,
                            position = RowPosition.Top,
                        ) {
                            showDialog.value = false
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                putExtra(DocumentsContract.EXTRA_INITIAL_URI, "".toUri())
                            }

                            startForResult.launch(intent)
                        }
                    }

                    DialogClickableItem(
                        text = "Data & Backup",
                        iconResId = R.drawable.data,
                        position = if (currentView.value == MainScreenViewType.SecureFolder) RowPosition.Top else RowPosition.Middle,
                    )

                    DialogClickableItem(
                        text = "Settings",
                        iconResId = R.drawable.settings,
                        position = RowPosition.Middle,
                    ) {
                        showDialog.value = false
                        navController.navigate(MultiScreenViewType.SettingsMainView.name)
                    }

                    DialogClickableItem(
                        text = "About & Updates",
                        iconResId = R.drawable.info,
                        position = RowPosition.Bottom,
                    ) {
                        showDialog.value = false
                        navController.navigate(MultiScreenViewType.AboutAndUpdateView.name)
                    }
                }
            }
        }
    }
}

/** @param moveCopyInsetsPadding should only be used when showMoveCopyOptions is enabled */
@Composable
fun SinglePhotoInfoDialog(
    showDialog: MutableState<Boolean>,
    currentMediaItem: MediaStoreData,
    groupedMedia: MutableState<List<MediaStoreData>>,
    showMoveCopyOptions: Boolean = true,
    moveCopyInsetsPadding: WindowInsets? = WindowInsets.statusBars
) {
    val context = LocalContext.current
    val isEditingFileName = remember { mutableStateOf(false) }

    val localConfig = LocalConfiguration.current
    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val modifier = if (isLandscape)
        Modifier.width(328.dp)
    else
        Modifier.fillMaxWidth(0.85f)

    if (showDialog.value) {
        Dialog(
            onDismissRequest = {
                showDialog.value = false
                isEditingFileName.value = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
        ) {
            Column(
                modifier = Modifier
                    .then(modifier)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(brightenColor(CustomMaterialTheme.colorScheme.surface, 0.1f))
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                ) {
                    IconButton(
                        onClick = {
                            showDialog.value = false
                            isEditingFileName.value = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close dialog button",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }

                    AnimatableText(
                        first = "Rename",
                        second = "More Options",
                        state = isEditingFileName.value,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .wrapContentHeight()
                ) {
                    var originalFileName = currentMediaItem.displayName ?: "Broken File"
                    val fileName = remember { mutableStateOf(originalFileName) }
                    val saveFileName = remember { mutableStateOf(false) }

                    val expanded = remember { mutableStateOf(false) }

                    LaunchedEffect(key1 = saveFileName.value) {
                        if (!saveFileName.value) {
                            return@LaunchedEffect
                        }

                        val oldName = currentMediaItem.displayName ?: "Broken File"
                        val path = currentMediaItem.absolutePath

                        renameImage(context, currentMediaItem.uri, fileName.value)

                        originalFileName = fileName.value
                        val newGroupedMedia = groupedMedia.value.toMutableList()
                        // set currentMediaItem to new one with new name
                        val newMedia = currentMediaItem.copy(
                            displayName = fileName.value,
                            absolutePath = path.replace(oldName, fileName.value)
                        )

                        val index = groupedMedia.value.indexOf(currentMediaItem)
                        newGroupedMedia[index] = newMedia
                        groupedMedia.value = newGroupedMedia

                        saveFileName.value = false
                    }

                    AnimatableTextField(
                        state = isEditingFileName,
                        string = fileName,
                        doAction = saveFileName,
                        extraAction = expanded,
                        rowPosition = RowPosition.Top
                    ) {
                        fileName.value = originalFileName
                    }

                    val mediaData = getExifDataForMedia(currentMediaItem.absolutePath)
                    // should add a way to automatically calculate height needed for this
                    val addedHeight by remember { mutableStateOf(36.dp * mediaData.keys.size) }
                    val moveCopyHeight = if (showMoveCopyOptions) 82.dp else 0.dp // 40.dp is height of one single row
                    val height by animateDpAsState(
                        targetValue = if (!isEditingFileName.value && expanded.value) {
                            42.dp + addedHeight + moveCopyHeight
                        } else if (!isEditingFileName.value && !expanded.value) {
                            42.dp + moveCopyHeight
                        } else {
                            0.dp
                        },
                        label = "height of other options",
                        animationSpec = tween(
                            durationMillis = 350
                        )
                    )

                    Column(
                        modifier = Modifier
                            .height(height)
                            .fillMaxWidth(1f)
                    ) {
                        if (showMoveCopyOptions && moveCopyInsetsPadding != null) {
                            val show = remember { mutableStateOf(false) }
                            var isMoving by remember { mutableStateOf(false) }

                            val stateList = SnapshotStateList<MediaStoreData>()
                            stateList.add(currentMediaItem)

                            MoveCopyAlbumListView(
                                show = show,
                                selectedItemsList = stateList,
                                isMoving = isMoving,
                                groupedMedia = null,
                                insetsPadding = moveCopyInsetsPadding
                            )

                            DialogClickableItem(
                                text = "Copy to Album",
                                iconResId = R.drawable.copy,
                                position = RowPosition.Middle,
                            ) {
                                isMoving = false
                                show.value = true
                            }

                            DialogClickableItem(
                                text = "Move to Album",
                                iconResId = R.drawable.cut,
                                position = RowPosition.Middle,
                            ) {
                                isMoving = true
                                show.value = true
                            }
                        }

                        val infoComposable = @Composable {
                            LazyColumn(
                                modifier = Modifier
                                    .wrapContentHeight()
                            ) {
                                for (key in mediaData.keys) {
                                    item {
                                        val value = mediaData[key]

                                        val splitBy = Regex("(?=[A-Z])")
                                        val split = key.toString().split(splitBy)
                                        // println("SPLIT IS $split")
                                        val name = if (split.size >= 3) "${split[1]} ${split[2]}" else key.toString()

                                        DialogInfoText(
                                            firstText = name,
                                            secondText = value.toString(),
                                            iconResId = key.iconResInt,
                                        )
                                    }
                                }
                            }
                        }

                        DialogExpandableItem(
                            text = "More Info",
                            iconResId = R.drawable.info,
                            position = RowPosition.Bottom,
                            expanded = expanded
                        ) {
                            infoComposable()
                        }
                    }
                }
            }
        }
    }
}

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
                        containerColor = CustomMaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = CustomMaterialTheme.colorScheme.onTertiaryContainer
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
                            containerColor = CustomMaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = CustomMaterialTheme.colorScheme.onTertiaryContainer
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
fun SingleAlbumDialog(
    showDialog: MutableState<Boolean>,
    dir: String,
    navController: NavHostController,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    if (showDialog.value) {
        val title = dir.split("/").last()

        Dialog(
            onDismissRequest = {
                showDialog.value = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(brightenColor(CustomMaterialTheme.colorScheme.surface, 0.1f))
                    .padding(8.dp)
            ) {
                val isEditingFileName = remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f),
                ) {
                    IconButton(
                        onClick = {
                            showDialog.value = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close dialog button",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }

                    AnimatableText(
                        first = "Rename",
                        second = title,
                        state = isEditingFileName.value,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                val reverseHeight by animateDpAsState(
                    targetValue = if (isEditingFileName.value) 0.dp else 42.dp,
                    label = "height of other options",
                    animationSpec = tween(
                        durationMillis = 500
                    )
                )

                Column(
                    modifier = Modifier
                        .height(reverseHeight)
                        .padding(8.dp, 0.dp)
                ) {
                    DialogClickableItem(
                        text = "Select",
                        iconResId = R.drawable.check_item,
                        position = RowPosition.Top,
                    ) {
                        showDialog.value = false
                        selectedItemsList.clear()
                        selectedItemsList.add(MediaStoreData())
                    }
                }
                val fileName = remember { mutableStateOf(title) }
                val saveFileName = remember { mutableStateOf(false) }

                LaunchedEffect(key1 = saveFileName.value) {
                    if (!saveFileName.value) {
                        return@LaunchedEffect
                    }

                    renameDirectory("${getBaseInternalStorageDirectory()}$dir", fileName.value)

                    val mainViewModel = MainActivity.mainViewModel
                    val newDir = dir.replace(title, fileName.value)
                    mainViewModel.setSelectedAlbumDir(newDir)

                    mainViewModel.settings.AlbumsList.editInAlbumsList(dir, fileName.value)
                    showDialog.value = false

                    navController.popBackStack()
                    navController.navigate(MultiScreenViewType.SingleAlbumView.name)

                    saveFileName.value = false
                }

                AnimatableTextField(
                    state = isEditingFileName,
                    string = fileName,
                    doAction = saveFileName,
                    rowPosition = RowPosition.Middle,
                    modifier = Modifier
                        .padding(8.dp, 0.dp)
                ) {
                    fileName.value = title
                }

                Column(
                    modifier = Modifier
                        .height(reverseHeight + 6.dp)
                        .padding(8.dp, 0.dp, 8.dp, 6.dp)
                ) {
                    DialogClickableItem(
                        text = "Remove album from list",
                        iconResId = R.drawable.delete,
                        position = RowPosition.Bottom,
                    ) {
                        mainViewModel.settings.AlbumsList.removeFromAlbumsList(dir)
                        showDialog.value = false
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
