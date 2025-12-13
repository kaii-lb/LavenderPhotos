package com.kaii.photos.compose.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.FullWidthDialogButton
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.RadioButtonRow
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.LookAndFeel
import com.kaii.photos.datastore.PhotoGrid
import com.kaii.photos.datastore.Storage
import com.kaii.photos.datastore.StoredDrawable
import com.kaii.photos.datastore.TrashBin
import com.kaii.photos.helpers.DisplayDateFormat
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MediaItemSortMode.Companion.presentableName
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TopBarDetailsFormat
import com.kaii.photos.helpers.createDirectoryPicker
import com.kaii.photos.reorderable_lists.ReorderableItem
import com.kaii.photos.reorderable_lists.ReorderableLazyList
import com.kaii.photos.reorderable_lists.rememberReorderableListState
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun AddTabDialog(
    tabList: List<BottomBarTab>,
    dismissDialog: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = dismissDialog
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
        ) {
            Text(
                text = stringResource(id = R.string.tabs_add),
                fontSize = TextUnit(18f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.Center)
            )

            IconButton(
                onClick = {
                    dismissDialog()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "close this dialog",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val iconList = listOf(
            StoredDrawable.Albums,
            StoredDrawable.PhotoGrid,
            StoredDrawable.SecureFolder,
            StoredDrawable.Search,
            StoredDrawable.Favourite,
            StoredDrawable.Star,
            StoredDrawable.Bolt,
            StoredDrawable.Face,
            StoredDrawable.Pets,
            StoredDrawable.Motorcycle,
            StoredDrawable.Motorsports
        )

        val state = rememberLazyListState()

        val selectedItem by remember {
            derivedStateOf {
                if (state.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val width = state.layoutInfo.viewportSize.width
                    val itemHalfWidth = state.layoutInfo.visibleItemsInfo[0].size / 2
                    val center = width / 2

                    val itemIndex = state.layoutInfo.visibleItemsInfo.find {
                        println("CENTER $center OFFSET ${it.offset} HALF $itemHalfWidth")
                        it.offset == 0 || it.offset in (center - itemHalfWidth)..(center + itemHalfWidth)
                    }?.index

                    if (itemIndex != null) iconList[itemIndex] else null
                } else null
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
        ) {
            LazyRow(
                state = state,
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(48.dp),
                flingBehavior = rememberSnapFlingBehavior(
                    lazyListState = state,
                    snapPosition = SnapPosition.Center
                ),
                contentPadding = PaddingValues(
                    start = this.maxWidth / 2 - 56.dp / 2,
                    end = this.maxWidth / 2 - 56.dp / 2
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 16.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                items(
                    count = iconList.size
                ) { index ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 2.dp,
                                color = if (selectedItem == iconList[index]) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = iconList[index].nonFilled),
                            contentDescription = "Custom icon for custom tab",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(48.dp * 2.5f)
                    .height(56.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(48.dp * 2.5f)
                    .height(56.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        var tabName by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        val focus = remember { FocusRequester() }

        var hideKb by remember { mutableStateOf(false) }
        val kbController = LocalSoftwareKeyboardController.current

        LaunchedEffect(hideKb) {
            if (hideKb) kbController?.hide()
            hideKb = false
        }

        TextField(
            value = tabName,
            onValueChange = {
                tabName = it
            },
            placeholder = {
                Text(
                    text = stringResource(id = R.string.tabs_name),
                    fontSize = TextUnit(14f, TextUnitType.Sp)
                )
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = TextUnit(16f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            colors = TextFieldDefaults.colors().copy(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedIndicatorColor = Color.Transparent,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTrailingIconColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
            ),
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
                    hideKb = true
                },
            ),
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.checkmark_thin),
                    contentDescription = "Apply tab name",
                    modifier = Modifier
                        .clickable {
                            focusManager.clearFocus()
                            hideKb = true
                        }
                )
            },
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .focusRequester(focus)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val selectedAlbums = remember { mutableStateListOf<String>() }
        val coroutineScope = rememberCoroutineScope()

        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(16.dp, 0.dp, 8.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = stringResource(id = R.string.tabs_albums_incl),
                fontSize = TextUnit(14f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
            )

            val activityLauncher = createDirectoryPicker { path, basePath ->
                if (path != null && basePath != null) {
                    val absolutePath = basePath + path

                    if (!selectedAlbums.contains(absolutePath)) selectedAlbums.add(absolutePath)
                }
            }

            IconButton(
                onClick = {
                    activityLauncher.launch(null)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "Add a new album to this tab",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalSeparator()

        Spacer(modifier = Modifier.height(2.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(1f)
                .heightIn(max = 160.dp)
                .wrapContentHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(
                count = selectedAlbums.size
            ) { index ->
                InfoRow(
                    text = selectedAlbums[index].split("/").last(),
                    iconResId = R.drawable.close
                ) {
                    selectedAlbums.removeAt(index)
                }
            }

            if (selectedAlbums.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(40.dp)
                            .padding(16.dp, 8.dp, 8.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = stringResource(id = R.string.none),
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val mainViewModel = LocalMainViewModel.current
        val resources = LocalResources.current
        FullWidthDialogButton(
            text = stringResource(id = R.string.tabs_confirm),
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single
        ) {
            if (tabList.size < 8) {
                if (selectedItem != null && selectedAlbums.isNotEmpty() && tabName != "") {
                    mainViewModel.settings.DefaultTabs.setTabList(
                        tabList.toMutableList().apply {
                            add(
                                BottomBarTab(
                                    name = tabName,
                                    albumPaths = selectedAlbums,
                                    icon = selectedItem!!,
                                    id = tabList.size,
                                    isCustom = true
                                )
                            )
                        }
                    )

                    dismissDialog()
                } else {
                    coroutineScope.launch {
                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = resources.getString(R.string.tabs_empty_params),
                                icon = R.drawable.error_2,
                                duration = SnackbarDuration.Short
                            )
                        )
                    }
                }
            } else {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.tabs_max_reached),
                            icon = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SortModeSelectorDialog(
    currentSortMode: MediaItemSortMode,
    dismiss: () -> Unit
) {
    LavenderDialogBase(onDismiss = dismiss) {
        TitleCloseRow(title = stringResource(id = R.string.sort_mode)) {
            dismiss()
        }

        var chosenSortMode by remember { mutableStateOf(currentSortMode) }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
        ) {
            items(
                count = MediaItemSortMode.entries.size - 1 // ignore "Disabled"
            ) { index ->
                val sortMode =
                    MediaItemSortMode.entries.filter { it != MediaItemSortMode.Disabled }[index] // cursed syntax

                RadioButtonRow(
                    text = sortMode.presentableName,
                    checked = chosenSortMode == sortMode
                ) {
                    chosenSortMode = sortMode
                }
            }
        }

        val mainViewModel = LocalMainViewModel.current
        ConfirmCancelRow(
            onConfirm = {
                mainViewModel.settings.PhotoGrid.setSortMode(chosenSortMode)
                dismiss()
            }
        )
    }
}

@Composable
fun DeleteIntervalDialog(
    showDialog: MutableState<Boolean>,
    initialValue: Int
) {
    var deleteInterval by remember { mutableIntStateOf(initialValue) }
    val mainViewModel = LocalMainViewModel.current

    SelectableButtonListDialog(
        title = stringResource(id = R.string.trash_auto_delete_interval),
        body = stringResource(id = R.string.trash_auto_delete_notice),
        showDialog = showDialog,
        buttons = {
            RadioButtonRow(
                text = "3" + stringResource(id = R.string.trash_days),
                checked = deleteInterval == 3
            ) {
                deleteInterval = 3
            }

            RadioButtonRow(
                text = "10" + stringResource(id = R.string.trash_days),
                checked = deleteInterval == 10
            ) {
                deleteInterval = 10
            }

            RadioButtonRow(
                text = "15" + stringResource(id = R.string.trash_days),
                checked = deleteInterval == 15
            ) {
                deleteInterval = 15
            }

            RadioButtonRow(
                text = "30" + stringResource(id = R.string.trash_days),
                checked = deleteInterval == 30
            ) {
                deleteInterval = 30
            }

            RadioButtonRow(
                text = "60" + stringResource(id = R.string.trash_days),
                checked = deleteInterval == 60
            ) {
                deleteInterval = 60
            }
        },
        onConfirm = {
            mainViewModel.settings.TrashBin.setAutoDeleteInterval(deleteInterval)
        }
    )
}

@Composable
fun ThumbnailSizeDialog(
    showDialog: MutableState<Boolean>,
    initialValue: Int
) {
    var thumbnailSize by remember { mutableIntStateOf(initialValue) }
    val mainViewModel = LocalMainViewModel.current

    SelectableButtonListDialog(
        title = stringResource(id = R.string.settings_storage_thumbnails_resolution),
        body = stringResource(id = R.string.settings_storage_thumbnails_notice),
        showDialog = showDialog,
        buttons = {
            RadioButtonRow(
                text = "32x32" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 32
            ) {
                thumbnailSize = 32
            }

            RadioButtonRow(
                text = "64x64" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 64
            ) {
                thumbnailSize = 64
            }

            RadioButtonRow(
                text = "128x128" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 128
            ) {
                thumbnailSize = 128
            }

            RadioButtonRow(
                text = "256x256" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 256
            ) {
                thumbnailSize = 256
            }

            RadioButtonRow(
                text = "512x512" + stringResource(id = R.string.pixels),
                checked = thumbnailSize == 512
            ) {
                thumbnailSize = 512
            }
        },
        onConfirm = {
            mainViewModel.settings.Storage.setThumbnailSize(thumbnailSize)
        }
    )
}

@Composable
fun TabCustomizationDialog(
    currentTab: MutableState<BottomBarTab>,
    closeDialog: () -> Unit
) {
    val mainViewModel = LocalMainViewModel.current
    val tabList by mainViewModel.settings.DefaultTabs.getTabList()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()

    LavenderDialogBase(
        onDismiss = closeDialog
    ) {
        TitleCloseRow(title = stringResource(id = R.string.tabs_customize)) {
            closeDialog()
        }

        Column(
            modifier = Modifier
                .wrapContentSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            val resources = LocalResources.current

            DefaultTabs.extendedList.forEach { tab ->
                InfoRow(
                    text = tab.name,
                    iconResId = if (tab in tabList) R.drawable.delete else R.drawable.add,
                    opacity = if (tab in tabList) 1f else 0.5f
                ) {
                    mainViewModel.settings.DefaultTabs.setTabList(
                        tabList.toMutableList().apply {
                            if (tab in tabList && tabList.size > 1) {
                                remove(tab)
                                if (currentTab.value == tab) currentTab.value =
                                    tabList[0] // handle tab removal
                            } else if (tab in tabList) {
                                coroutineScope.launch {
                                    LavenderSnackbarController.pushEvent(
                                        LavenderSnackbarEvents.MessageEvent(
                                            message = resources.getString(R.string.tabs_min_reached),
                                            icon = R.drawable.error_2,
                                            duration = SnackbarDuration.Short
                                        )
                                    )
                                }
                            }

                            if (tab !in tabList && tabList.size < 8) {
                                add(tab)
                            } else if (tab !in tabList) {
                                coroutineScope.launch {
                                    LavenderSnackbarController.pushEvent(
                                        LavenderSnackbarEvents.MessageEvent(
                                            message = resources.getString(R.string.tabs_max_reached),
                                            icon = R.drawable.error_2,
                                            duration = SnackbarDuration.Short
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }

            tabList.forEach { tab ->
                if (tab !in DefaultTabs.extendedList) {
                    InfoRow(
                        text = tab.name,
                        iconResId = R.drawable.delete
                    ) {
                        if (tabList.size > 1) {
                            mainViewModel.settings.DefaultTabs.setTabList(
                                tabList.toMutableList().apply {
                                    remove(tab)
                                    if (currentTab.value == tab) currentTab.value =
                                        tabList[0] // handle tab removal
                                }
                            )
                        } else {
                            coroutineScope.launch {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.MessageEvent(
                                        message = resources.getString(R.string.tabs_min_reached),
                                        icon = R.drawable.error_2,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalSeparator()
        Spacer(modifier = Modifier.height(16.dp))

        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            AddTabDialog(
                tabList = tabList,
                dismissDialog = {
                    showDialog = false
                }
            )
        }

        val resources = LocalResources.current
        FullWidthDialogButton(
            text = stringResource(id = R.string.tabs_add),
            color = MaterialTheme.colorScheme.primary,
            position = RowPosition.Single,
            textColor = MaterialTheme.colorScheme.onPrimary
        ) {
            if (tabList.size < 8) {
                showDialog = true
            } else {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = resources.getString(R.string.tabs_max_reached),
                            icon = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultTabSelectorDialog(
    tabList: List<BottomBarTab>,
    defaultTab: BottomBarTab,
    dismissDialog: () -> Unit
) {
    var selectedTab by remember(defaultTab) { mutableStateOf(defaultTab) }
    val tabListDynamic = remember { mutableStateListOf<BottomBarTab>().apply { addAll(tabList) } }

    LavenderDialogBase(
        onDismiss = dismissDialog
    ) {
        TitleCloseRow(title = stringResource(id = R.string.tabs_default)) {
            dismissDialog()
        }

        // val state = rememberLazyListState()
        // val itemOffset = remember { mutableFloatStateOf(0f) }
        // var selectedItem: BottomBarTab? by remember { mutableStateOf(null) }

        val listState = rememberLazyListState()

        val reorderableState = rememberReorderableListState(listState) { fromIndex, toIndex ->
            val newList = tabListDynamic.toMutableList()
            newList.add(toIndex, newList.removeAt(fromIndex))

            tabListDynamic.clear()
            tabListDynamic.addAll(newList.distinctBy { it.name })
        }

        ReorderableLazyList(
            listState = listState,
            reorderableState = reorderableState
        ) {
            items(
                count = tabListDynamic.size,
                key = { index ->
                    tabListDynamic[index].name
                }
            ) { index ->
                ReorderableItem(
                    index = index,
                    reorderableState = reorderableState
                ) {
                    val tab = tabListDynamic[index]

                    ReorderableRadioButtonRow(
                        text = tab.name,
                        checked = selectedTab == tab
                    ) {
                        selectedTab = tab
                    }
                }
            }
        }

        val mainViewModel = LocalMainViewModel.current
        ConfirmCancelRow(
            onConfirm = {
                mainViewModel.settings.DefaultTabs.setTabList(tabListDynamic)
                mainViewModel.settings.DefaultTabs.setDefaultTab(selectedTab)

                dismissDialog()
            }
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun DateFormatDialog(
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.look_and_feel_date_format),
            closeOffset = 12.dp
        ) {
            onDismiss()
        }

        Spacer(modifier = Modifier.height(12.dp))
        val mainViewModel = LocalMainViewModel.current
        val currentDate = remember {
            Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toJavaLocalDate()
        }

        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = DisplayDateFormat.entries
            ) { index, item ->
                PreferencesRow(
                    title = stringResource(id = item.description),
                    summary = currentDate.format(item.format),
                    position =
                        when (index) {
                            0 -> RowPosition.Top
                            DisplayDateFormat.entries.size - 1 -> RowPosition.Bottom
                            else -> RowPosition.Middle
                        },
                    iconResID = item.icon
                ) {
                    mainViewModel.settings.LookAndFeel.setDisplayDateFormat(item)
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun TopBarDetailsFormatDialog(
    onDismiss: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = onDismiss
    ) {
        TitleCloseRow(
            title = stringResource(id = R.string.look_and_feel_image_details_format),
            closeOffset = 12.dp
        ) {
            onDismiss()
        }

        Spacer(modifier = Modifier.height(12.dp))

        val context = LocalContext.current
        val mainViewModel = LocalMainViewModel.current
        val currentDate = remember {
            Clock.System.now()
                .epochSeconds
        }

        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = TopBarDetailsFormat.entries
            ) { index, item ->
                PreferencesRow(
                    title = stringResource(id = item.description),
                    summary = item.format(context, "Screenshot 20251210.png", currentDate),
                    position =
                        when (index) {
                            0 -> RowPosition.Top
                            TopBarDetailsFormat.entries.size - 1 -> RowPosition.Bottom
                            else -> RowPosition.Middle
                        },
                    iconResID = item.icon
                ) {
                    mainViewModel.settings.LookAndFeel.setTopBarDetailsFormat(item)
                    onDismiss()
                }
            }
        }
    }
}