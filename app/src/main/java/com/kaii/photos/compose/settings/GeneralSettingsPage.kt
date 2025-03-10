package com.kaii.photos.compose.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.CheckBoxButtonRow
import com.kaii.photos.compose.PreferencesRow
import com.kaii.photos.compose.PreferencesSeparatorText
import com.kaii.photos.compose.PreferencesSwitchRow
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.Editing
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.datastore.Permissions
import com.kaii.photos.datastore.Video
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.helpers.RowPosition
import com.kaii.lavender_snackbars.LavenderSnackbarController
import com.kaii.lavender_snackbars.LavenderSnackbarEvents
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.dragReorderable
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsPage() {
    val context = LocalContext.current

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    PreferencesSeparatorText("Permissions")
                }

                item {
                    val isMediaManager by mainViewModel.settings.Permissions.getIsMediaManager().collectAsStateWithLifecycle(initialValue = false)

                    val manageMediaLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { _ ->
                        val granted = MediaStore.canManageMedia(context)

                        mainViewModel.onPermissionResult(
                            permission = Manifest.permission.MANAGE_MEDIA,
                            isGranted = granted
                        )

                        mainViewModel.settings.Permissions.setIsMediaManager(granted)
                    }

                   PreferencesSwitchRow(
                        title = "Media Manager",
                        summary = "Better and faster trash/delete/copy/move",
                        iconResID = R.drawable.movie_edit,
                        checked = isMediaManager,
                        position = RowPosition.Single,
                        showBackground = false
                    ) {
                        val intent = Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                        manageMediaLauncher.launch(intent)
                    }
                }
            }

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
            	PreferencesSeparatorText(
            		text = "Tabs"
            	)
            }

            item {
            	val showDefaultTabAlbumDialog = remember { mutableStateOf(false) }

				if (showDefaultTabAlbumDialog.value) {
	           		DefaultTabSelectorDialog(
	           			showDialog = showDefaultTabAlbumDialog
	           		)
				}

            	PreferencesRow(
            		title = "Default Tab",
            		summary = "Lavender Photos will auto-open this tab at startup",
            		iconResID = R.drawable.folder_open,
            		position = RowPosition.Single,
            		showBackground = false
            	) {
            		showDefaultTabAlbumDialog.value = true
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
	showDialog: MutableState<Boolean>
) {
	val tabList by mainViewModel.settings.DefaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)
	val defaultTab by mainViewModel.settings.DefaultTabs.getDefaultTab().collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)
	var selectedTab by remember(defaultTab) { mutableStateOf(defaultTab) }

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
                text = "Default Tab",
                fontSize = TextUnit(18f, TextUnitType.Sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .wrapContentSize()
            )

            Spacer (modifier = Modifier.height(8.dp))

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

                            val mapped = list.mapIndexed { index, item ->
								item.copy(
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
                        		.offset {
	                            	IntOffset(x = 0, y = if (selectedItem == tab) itemOffset.floatValue.toInt() else 0)
	                          	}
                        		.animateItem(placementSpec = if (selectedItem == tab) null else tween(durationMillis = 250))
                    ) {
                        selectedTab = tab
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(56.dp)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = {
                        mainViewModel.settings.DefaultTabs.setDefaultTab(selectedTab)
                        showDialog.value = false
                    }
                ) {
                    Text(text = "Confirm")
                }
            }
        }
    }
}

@Composable
private fun ReorderableRadioButtonRow(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row (
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

        Spacer (modifier = Modifier.width(16.dp))

        Text (
            text = text,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
        )

        Spacer (modifier = Modifier.width(16.dp))

        Icon(
            painter = painterResource(id = R.drawable.reorderable),
            contentDescription = "this item can be dragged and reordered",
            modifier = Modifier
                .size(24.dp)
        )

        Spacer (modifier = Modifier.width(8.dp))
    }
}
