package com.kaii.photos.compose.settings

import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.CheckBoxButtonRow
import com.kaii.photos.compose.ConfirmCancelRow
import com.kaii.photos.compose.FullWidthDialogButton
import com.kaii.photos.compose.HorizontalSeparator
import com.kaii.photos.compose.LavenderDialogBase
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.compose.RadioButtonRow
import com.kaii.photos.compose.TitleCloseRow
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Editing
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.datastore.PhotoGrid
import com.kaii.photos.datastore.StoredDrawable
import com.kaii.photos.datastore.Versions
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.ExtendedMaterialTheme
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MediaItemSortMode.Companion.presentableName
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.createDirectoryPicker
import com.kaii.photos.helpers.dragReorderable
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsPage(currentTab: MutableState<BottomBarTab>) {
    Scaffold(
        topBar = {
            GeneralSettingsTopBar()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                PreferencesSeparatorText("Video")
            }

            item {
                val shouldAutoPlay by mainViewModel.settings.Video.getShouldAutoPlay().collectAsStateWithLifecycle(initialValue = true)
                val muteOnStart by mainViewModel.settings.Video.getMuteOnStart().collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Auto Play Videos",
                    summary = "Start playing videos as soon as they appear on screen",
                    iconResID = R.drawable.auto_play,
                    checked = shouldAutoPlay,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Video.setShouldAutoPlay(checked)
                    }
                )

                PreferencesSwitchRow(
                    title = "Videos Start Muted",
                    summary = "Don't play audio when first starting video playback",
                    iconResID = R.drawable.volume_mute,
                    checked = muteOnStart,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Video.setMuteOnStart(checked)
                    }
                )
            }


            item {
                PreferencesSeparatorText("Editing")
            }

            item {
                val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault().collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Overwrite on save",
                    summary = "Default to overwriting instead of saving a copy when editing media",
                    iconResID = R.drawable.storage,
                    checked = overwriteByDefault,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Editing.setOverwriteByDefault(checked)
                    }
                )
            }

            item {
                PreferencesSeparatorText("Albums")
            }

            item {
                val mainPhotosAlbums by mainViewModel.settings.MainPhotosView.getAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
                val allAlbums by mainViewModel.settings.AlbumsList.getAlbumsList().collectAsStateWithLifecycle(initialValue = emptyList())

                val showAlbumsSelectionDialog = remember { mutableStateOf(false) }
                val selectedAlbums = remember { mutableStateListOf<String>() }

                PreferencesRow(
                    title = "Main Albums List",
                    iconResID = R.drawable.photogrid,
                    position = RowPosition.Single,
                    showBackground = false,
                    summary = "Select albums that will have their photos displayed in the main photo view"
                ) {
                    selectedAlbums.clear()
                    selectedAlbums.addAll(
                        mainPhotosAlbums.map {
                            it.apply {
                                removeSuffix("/")
                            }
                        }
                    )

                    showAlbumsSelectionDialog.value = true
                }

                if (showAlbumsSelectionDialog.value) {
                    SelectableButtonListDialog(
                        title = "Selected Albums",
                        body = "Albums selected here will show up in the main photo view",
                        showDialog = showAlbumsSelectionDialog,
                        onConfirm = {
                            mainViewModel.settings.MainPhotosView.clear()

                            selectedAlbums.forEach { album ->
                                mainViewModel.settings.MainPhotosView.addAlbum(album.removeSuffix("/"))
                            }
                        },
                        buttons = {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .height(384.dp)
                            ) {
                                items(
                                    count = allAlbums.size
                                ) { index ->
                                    val associatedAlbum = allAlbums[index]

                                    CheckBoxButtonRow(
                                        text = associatedAlbum,
                                        checked = selectedAlbums.contains(associatedAlbum)
                                    ) {
                                        if (selectedAlbums.contains(associatedAlbum) && selectedAlbums.size > 1) {
                                            selectedAlbums.remove(associatedAlbum)
                                        } else {
                                            selectedAlbums.add(associatedAlbum)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect().collectAsStateWithLifecycle(initialValue = false)
                val isAlreadyLoading = remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                PreferencesSwitchRow(
                    title = "Automatically detect albums",
                    summary = "Detects all the folders with media on the device and adds them to the album list",
                    iconResID = R.drawable.albums_search,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = autoDetectAlbums
                ) { checked ->
                    // as to keep the MutableState alive even if the user leaves the screen
                    mainViewModel.launch {
                        if (!isAlreadyLoading.value) {
                            isAlreadyLoading.value = true

                            if (checked) {
                                LavenderSnackbarController.pushEvent(
                                    LavenderSnackbarEvents.LoadingEvent(
                                        message = if (isAlreadyLoading.value) "Finding albums..." else "Found all albums!",
                                        iconResId = R.drawable.albums_search,
                                        isLoading = isAlreadyLoading
                                    )
                                )
                                val albums = mainViewModel.settings.AlbumsList.getAllAlbumsOnDevice()
                                mainViewModel.settings.AlbumsList.setAlbumsList(albums)
                            }
                            mainViewModel.settings.AlbumsList.setAutoDetect(checked)

                            isAlreadyLoading.value = false
                        }
                    }
                }

                PreferencesRow(
                    title = "Clear album list",
                    summary = "Remove all albums except for Camera and Download",
                    iconResID = R.drawable.albums_clear,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    coroutineScope.launch {
                        mainViewModel.settings.AlbumsList.setAlbumsList(
                            listOf(
                                Environment.DIRECTORY_DCIM + "/Camera",
                                Environment.DIRECTORY_DOWNLOADS
                            )
                        )

                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = "Cleared album list",
                                duration = SnackbarDuration.Short,
                                iconResId = R.drawable.albums
                            )
                        )
                    }
                }
            }

            item {
            	val currentSortMode by mainViewModel.settings.PhotoGrid.getSortMode().collectAsStateWithLifecycle(initialValue = MediaItemSortMode.DateTaken)
				var showSortModeSelectorDialog by remember { mutableStateOf(false) }

				if (showSortModeSelectorDialog) {
					SortModeSelectorDialog(
						currentSortMode = currentSortMode,
						dismiss = {
							showSortModeSelectorDialog = false
						}
					)
				}

            	PreferencesSwitchRow(
            		title = "Media Sorting",
            		summary = "Sets the sorting of photos and videos in grids",
            		iconResID = R.drawable.sorting,
            		position = RowPosition.Single,
            		showBackground = false,
            		checked = currentSortMode != MediaItemSortMode.Disabled,
            		onRowClick = {
            			showSortModeSelectorDialog = true
            		},
            		onSwitchClick = { checked ->
	                    if (checked) mainViewModel.settings.PhotoGrid.setSortMode(MediaItemSortMode.DateTaken)
	                    else mainViewModel.settings.PhotoGrid.setSortMode(MediaItemSortMode.Disabled)
            		}
            	)
            }

            item {
                PreferencesSeparatorText(
                    text = "Tabs"
                )
            }

            item {
                var showDefaultTabAlbumDialog by remember { mutableStateOf(false) }
                val tabList by mainViewModel.settings.DefaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = emptyList())
                val defaultTab by mainViewModel.settings.DefaultTabs.getDefaultTab().collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)

                if (showDefaultTabAlbumDialog) {
                    DefaultTabSelectorDialog(
                        tabList = tabList,
                        defaultTab = defaultTab
                    ) {
                        showDefaultTabAlbumDialog = false
                    }
                }

                PreferencesRow(
                    title = "Default Tab",
                    summary = "Lavender Photos will auto-open this tab at startup",
                    iconResID = R.drawable.folder_open,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    showDefaultTabAlbumDialog = true
                }
            }

            item {
                var showDialog by remember { mutableStateOf(false) }

                if (showDialog) {
                    TabCustomizationDialog(
                    	currentTab = currentTab,
                        closeDialog = {
                            showDialog = false
                        }
                    )
                }

                PreferencesRow(
                    title = "Customize Tabs",
                    summary = "Change what tabs are available in the bottom bar",
                    iconResID = R.drawable.edit,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    showDialog = true
                }
            }

            item {
                PreferencesSeparatorText(
                    text = "Updates"
                )
            }

            item {
                val checkForUpdatesOnStartup by mainViewModel.settings.Versions.getCheckUpdatesOnStartup().collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = "Check for Updates",
                    iconResID = R.drawable.update,
                    summary = "Notifies of new version when you open the app. Does not auto-install the update",
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = checkForUpdatesOnStartup
                ) {
                    mainViewModel.settings.Versions.setCheckUpdatesOnStartup(it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsTopBar() {
    val navController = LocalNavController.current

    TopAppBar(
        title = {
            Text(
                text = "General",
                fontSize = TextUnit(22f, TextUnitType.Sp)
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = "Go back to previous page",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun DefaultTabSelectorDialog(
    tabList: List<BottomBarTab>,
    defaultTab: BottomBarTab,
    dismissDialog: () -> Unit
) {
    var selectedTab by remember(defaultTab) { mutableStateOf(defaultTab) }

    LavenderDialogBase(
        onDismiss = dismissDialog
    ) {
        TitleCloseRow(title = "Default Tab") {
        	dismissDialog()
        }

        val state = rememberLazyListState()
        val itemOffset = remember { mutableFloatStateOf(0f) }
        var selectedItem: BottomBarTab? by remember { mutableStateOf(null) }

        LazyColumn(
            state = state,
            modifier = Modifier
                .wrapContentSize()
                .dragReorderable(
                    state = state,
                    itemOffset = itemOffset,
                    onItemSelected = { index ->
                        selectedItem =
                            if (index != null) tabList[index]
                            else null
                    },
                    onMove = { currentIndex, targetIndex ->
                        val list = tabList.toMutableList()
                        val item = list[currentIndex]
                        list.remove(item)
                        list.add(targetIndex, item)

                        val mapped = list.mapIndexed { index, new ->
                            new.copy(
                                index = index
                            )
                        }

                        mainViewModel.settings.DefaultTabs.setTabList(mapped)
                    }
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            items(
                count = tabList.size,
                key = { key ->
                    tabList[key].name
                },
            ) { index ->
                val tab = tabList[index]

                ReorderableRadioButtonRow(
                    text = tab.name,
                    checked = selectedTab == tab,
                    modifier =
                    Modifier
                        .zIndex(
                            if (selectedItem == tab) 1f
                            else 0f
                        )
                        .graphicsLayer {
                            if (selectedTab == tab) {
                                translationY = itemOffset.floatValue
                            }
                        }
                        .animateItem(placementSpec = if (selectedItem == tab) null else tween(durationMillis = 250))
                ) {
                    selectedTab = tab
                }
            }
        }

        ConfirmCancelRow(
            onConfirm = {
                mainViewModel.settings.DefaultTabs.setDefaultTab(selectedTab)
                dismissDialog()
            }
        )
    }
}

@Composable
private fun ReorderableRadioButtonRow(
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
fun TabCustomizationDialog(
	currentTab: MutableState<BottomBarTab>,
    closeDialog: () -> Unit
) {
    val tabList by mainViewModel.settings.DefaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()

    LavenderDialogBase(
        onDismiss = closeDialog
    ) {
		TitleCloseRow(title = "Customize Tabs") {
			closeDialog()
		}

        Column(
            modifier = Modifier
                .wrapContentSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
        	DefaultTabs.defaultList.forEach { tab ->
        		InfoRow(
        			text = tab.name,
        			iconResId = if (tab in tabList) R.drawable.delete else R.drawable.add,
        			opacity = if (tab in tabList) 1f else 0.5f
        		) {
        			mainViewModel.settings.DefaultTabs.setTabList(
        			    tabList.toMutableList().apply {
        			    	if (tab in tabList && tabList.size > 1) {
        			    		remove(tab)
        			    		if (currentTab.value == tab) currentTab.value = tabList[0] // handle tab removal
        			    	} else if (tab in tabList) {
        			    		coroutineScope.launch {
        			    			LavenderSnackbarController.pushEvent(
        			    				LavenderSnackbarEvents.MessageEvent(
        			    					message = "At least one tab needs to exist",
        			    					iconResId = R.drawable.error_2,
        			    					duration = SnackbarDuration.Short
        			    				)
        			    			)
        			    		}
        			    	}

        			    	if (tab !in tabList && tabList.size < 5) {
        			    		add(tab)
        			    	} else if (tab !in tabList) {
        			    		coroutineScope.launch {
        			    			LavenderSnackbarController.pushEvent(
        			    				LavenderSnackbarEvents.MessageEvent(
        			    					message = "Maximum of 5 tabs allowed",
        			    					iconResId = R.drawable.error_2,
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
            	if (tab !in DefaultTabs.defaultList) {
	                InfoRow(
						text = tab.name,
						iconResId = R.drawable.delete
					) {
						if (tabList.size > 1) {
							mainViewModel.settings.DefaultTabs.setTabList(
							    tabList.toMutableList().apply {
							        remove(tab)
							        if (currentTab.value == tab) currentTab.value = tabList[0] // handle tab removal
							    }
							)
						} else {
							coroutineScope.launch {
								LavenderSnackbarController.pushEvent(
									LavenderSnackbarEvents.MessageEvent(
										message = "At least one tab needs to exist",
										iconResId = R.drawable.error_2,
										duration = SnackbarDuration.Short
									)
								)
							}
						}
					}
            	}
            }
        }

		Spacer (modifier = Modifier.height(4.dp))
        HorizontalSeparator()
        Spacer (modifier = Modifier.height(16.dp))

        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            AddTabDialog(
                tabList = tabList,
                dismissDialog = {
                    showDialog = false
                }
            )
        }

        FullWidthDialogButton(
            text = "Add a tab",
            color = MaterialTheme.colorScheme.primary,
            position = RowPosition.Single,
            textColor = MaterialTheme.colorScheme.onPrimary
        ) {
            if (tabList.size < 5) {
                showDialog = true
            } else {
                coroutineScope.launch {
                    LavenderSnackbarController.pushEvent(
                        LavenderSnackbarEvents.MessageEvent(
                            message = "Maximum of 5 tabs allowed",
                            iconResId = R.drawable.error_2,
                            duration = SnackbarDuration.Short
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
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
private fun AddTabDialog(
	tabList: List<BottomBarTab>,
    dismissDialog: () -> Unit
) {
    LavenderDialogBase(
        onDismiss = dismissDialog
    ) {
    	Box (
    		modifier = Modifier
    			.fillMaxWidth(1f)
    	) {
    		Text(
    		    text = "Add a Tab",
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
                    start = maxWidth / 2 - 56.dp / 2,
                    end = maxWidth / 2 - 56.dp / 2
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
                                ExtendedMaterialTheme.colorScheme.dialogSurface,
                                ExtendedMaterialTheme.colorScheme.dialogSurface,
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
                                ExtendedMaterialTheme.colorScheme.dialogSurface,
                                ExtendedMaterialTheme.colorScheme.dialogSurface
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
                    text = "Tab name",
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
                    painter = painterResource(id = R.drawable.file_is_selected_foreground),
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

        Row (
			modifier = Modifier
				.fillMaxWidth(1f)
                .padding(16.dp, 0.dp, 8.dp, 0.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Start
		) {
			Text(
				text = "Albums included",
				fontSize = TextUnit(14f, TextUnitType.Sp),
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier
					.weight(1f)
			)

            val activityLauncher = createDirectoryPicker { path ->
                if (path != null) selectedAlbums.add(path)
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

		Spacer (modifier = Modifier.height(4.dp))

        HorizontalSeparator()

		Spacer (modifier = Modifier.height(2.dp))

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
        			        text = "None",
        			        fontSize = TextUnit(14f, TextUnitType.Sp),
        			        color = MaterialTheme.colorScheme.onSurface,
        			        overflow = TextOverflow.Ellipsis
        			    )
       			    }
        		}
        	}
        }

        Spacer (modifier = Modifier.height(16.dp))

		val coroutineScope = rememberCoroutineScope()

        FullWidthDialogButton(
            text = "Add Tab",
            color = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            position = RowPosition.Single
        ) {
        	if (tabList.size < 5) {
	        	if (selectedItem != null && selectedAlbums.isNotEmpty() && tabName != "") {
		        	mainViewModel.settings.DefaultTabs.setTabList(
		        	    tabList.toMutableList().apply {
		        	    	add(
		        	    		BottomBarTab(
		        	    			name = tabName,
		        	    			albumPaths = selectedAlbums,
		        	    			index = tabList.size,
		        	    			icon = selectedItem!!,
		        	    		)
		        	    	)
		        	    }
		        	)

		            dismissDialog()
	        	} else {
	        		coroutineScope.launch {
		        		LavenderSnackbarController.pushEvent(
		        			LavenderSnackbarEvents.MessageEvent(
		        				message = "Tab parameters cannot be empty",
		        				iconResId = R.drawable.error_2,
		        				duration = SnackbarDuration.Short
		        			)
		        		)
	        		}
	        	}
        	} else {
        		coroutineScope.launch {
	        		LavenderSnackbarController.pushEvent(
	        			LavenderSnackbarEvents.MessageEvent(
	        				message = "Can't add more than 5 tabs",
	        				iconResId = R.drawable.error_2,
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
		TitleCloseRow(title = "Media Sort Mode") {
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
				val sortMode = MediaItemSortMode.entries.filter { it != MediaItemSortMode.Disabled }[index] // cursed syntax

				RadioButtonRow(
					text = sortMode.presentableName,
					checked = chosenSortMode == sortMode
				) {
					chosenSortMode = sortMode
				}
			}
		}

        ConfirmCancelRow(
            onConfirm = {
                mainViewModel.settings.PhotoGrid.setSortMode(chosenSortMode)
                dismiss()
            }
        )
	}
}
