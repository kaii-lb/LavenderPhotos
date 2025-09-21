package com.kaii.photos.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalAppDatabase
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.DefaultTabSelectorDialog
import com.kaii.photos.compose.dialogs.SelectableButtonListDialog
import com.kaii.photos.compose.dialogs.SortModeSelectorDialog
import com.kaii.photos.compose.dialogs.TabCustomizationDialog
import com.kaii.photos.compose.widgets.CheckBoxButtonRow
import com.kaii.photos.compose.widgets.PreferencesRow
import com.kaii.photos.compose.widgets.PreferencesSeparatorText
import com.kaii.photos.compose.widgets.PreferencesSwitchRow
import com.kaii.photos.datastore.AlbumsList
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.Editing
import com.kaii.photos.datastore.MainPhotosView
import com.kaii.photos.datastore.PhotoGrid
import com.kaii.photos.datastore.Versions
import com.kaii.photos.datastore.Video
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsPage(currentTab: MutableState<BottomBarTab>) {
    val mainViewModel = LocalMainViewModel.current

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
                PreferencesSeparatorText(stringResource(id = R.string.video))
            }

            item {
                val shouldAutoPlay by mainViewModel.settings.Video.getShouldAutoPlay()
                    .collectAsStateWithLifecycle(initialValue = true)
                val muteOnStart by mainViewModel.settings.Video.getMuteOnStart()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.video_auto_play),
                    summary = stringResource(id = R.string.video_auto_play_desc),
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
                    title = stringResource(id = R.string.video_start_muted),
                    summary = stringResource(id = R.string.video_start_muted_desc),
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
                PreferencesSeparatorText(stringResource(id = R.string.editing))
            }

            item {
                val overwriteByDefault by mainViewModel.settings.Editing.getOverwriteByDefault()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.editing_overwrite_on_save),
                    summary = stringResource(id = R.string.editing_overwrite_on_save_desc),
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
                val exitOnSave by mainViewModel.settings.Editing.getExitOnSave()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.editing_exit_on_save),
                    summary = stringResource(id = R.string.editing_exit_on_save_desc),
                    iconResID = R.drawable.exit,
                    checked = exitOnSave,
                    position = RowPosition.Single,
                    showBackground = false,
                    onRowClick = null,
                    onSwitchClick = { checked ->
                        mainViewModel.settings.Editing.setExitOnSave(checked)
                    }
                )
            }

            item {
                PreferencesSeparatorText(stringResource(id = R.string.albums))
            }

            item {
                val appDatabase = LocalAppDatabase.current
                val mainPhotosAlbums by mainViewModel.settings.MainPhotosView.getAlbums()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
                val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect().collectAsStateWithLifecycle(initialValue = true)
                val allAlbums by if (autoDetectAlbums) {
                    mainViewModel.settings.AlbumsList.getAutoDetectedAlbums(displayDateFormat, appDatabase).collectAsStateWithLifecycle(initialValue = emptyList())
                } else {
                    mainViewModel.settings.AlbumsList.getNormalAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
                }

                val showAlbumsSelectionDialog = remember { mutableStateOf(false) }
                val selectedAlbums = remember { mutableStateListOf<String>() }

                val shouldShowEverything by mainViewModel.showAllInMain.collectAsStateWithLifecycle()

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.albums_main_list),
                    iconResID = R.drawable.photogrid,
                    position = RowPosition.Single,
                    checked = shouldShowEverything,
                    showBackground = false,
                    summary =
                        if (!shouldShowEverything) stringResource(id = R.string.albums_main_list_desc_1)
                        else stringResource(id = R.string.albums_main_list_desc_2),
                    onRowClick = { checked ->
                        selectedAlbums.clear()
                        selectedAlbums.addAll(
                            mainPhotosAlbums.map {
                                it.apply {
                                    removeSuffix("/")
                                }
                            }
                        )
                        showAlbumsSelectionDialog.value = true
                    },
                    onSwitchClick = { checked ->
                        mainViewModel.settings.MainPhotosView.setShowEverything(checked)
                    }
                )

                if (showAlbumsSelectionDialog.value) {
                    SelectableButtonListDialog(
                        title = stringResource(id = R.string.albums_selected),
                        body =
                            if (!shouldShowEverything) stringResource(id = R.string.albums_main_list_selected)
                            else stringResource(id = R.string.albums_main_list_selected_inverse),
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
                                        text = associatedAlbum.name,
                                        checked = selectedAlbums.contains(associatedAlbum.mainPath)
                                    ) {
                                        if ((selectedAlbums.contains(associatedAlbum.mainPath) && selectedAlbums.size > 1) || shouldShowEverything) {
                                            selectedAlbums.remove(associatedAlbum.mainPath)
                                        } else {
                                            selectedAlbums.add(associatedAlbum.mainPath)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                val autoDetectAlbums by mainViewModel.settings.AlbumsList.getAutoDetect().collectAsStateWithLifecycle(initialValue = true)

                val customAlbums by mainViewModel.settings.AlbumsList.getCustomAlbums().collectAsStateWithLifecycle(initialValue = emptyList())

                val isAlreadyLoading = remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val findingAlbums = stringResource(id = R.string.finding_albums_on_device)
                val foundAlbums = stringResource(id = R.string.albums_found)

                val displayDateFormat by mainViewModel.displayDateFormat.collectAsStateWithLifecycle()
                val appDatabase = LocalAppDatabase.current
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.albums_auto_detect),
                    summary = stringResource(id = R.string.albums_auto_detect_desc),
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
                                        message = if (isAlreadyLoading.value) findingAlbums else foundAlbums,
                                        icon = R.drawable.albums_search,
                                        isLoading = isAlreadyLoading
                                    )
                                )

                                mainViewModel.settings.AlbumsList.getAllAlbumsOnDevice(displayDateFormat, appDatabase)
                                    .cancellable()
                                    .collectLatest { list ->
                                        mainViewModel.settings.AlbumsList.setAlbumsList(customAlbums + list)
                                        mainViewModel.settings.AlbumsList.setAutoDetect(true)

                                        isAlreadyLoading.value = false
                                    }
                            }

                            mainViewModel.settings.AlbumsList.setAutoDetect(checked)

                            isAlreadyLoading.value = false
                        }
                    }
                }

                val clearDone = stringResource(id = R.string.albums_clear_list_done)
                PreferencesRow(
                    title = stringResource(id = R.string.albums_clear_list),
                    summary = stringResource(id = R.string.albums_clear_list_desc),
                    iconResID = R.drawable.albums_clear,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    coroutineScope.launch {
                        mainViewModel.settings.AlbumsList.setAlbumsList(
                            mainViewModel.settings.AlbumsList.defaultAlbumsList
                        )
                        mainViewModel.settings.AlbumsList.setAutoDetect(false)

                        LavenderSnackbarController.pushEvent(
                            LavenderSnackbarEvents.MessageEvent(
                                message = clearDone,
                                duration = SnackbarDuration.Short,
                                icon = R.drawable.albums
                            )
                        )
                    }
                }
            }

            item {
                val currentSortMode by mainViewModel.settings.PhotoGrid.getSortMode()
                    .collectAsStateWithLifecycle(initialValue = MediaItemSortMode.DateTaken)
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
                    title = stringResource(id = R.string.albums_media_sorting),
                    summary = stringResource(id = R.string.albums_media_sorting_desc),
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
                    text = stringResource(id = R.string.tabs)
                )
            }

            item {
                var showDefaultTabAlbumDialog by remember { mutableStateOf(false) }
                val tabList by mainViewModel.settings.DefaultTabs.getTabList()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val defaultTab by mainViewModel.settings.DefaultTabs.getDefaultTab()
                    .collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)

                if (showDefaultTabAlbumDialog) {
                    DefaultTabSelectorDialog(
                        tabList = tabList,
                        defaultTab = defaultTab
                    ) {
                        showDefaultTabAlbumDialog = false
                    }
                }

                PreferencesRow(
                    title = stringResource(id = R.string.tabs_default),
                    summary = stringResource(id = R.string.tabs_default_desc),
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
                    title = stringResource(id = R.string.tabs_customize),
                    summary = stringResource(id = R.string.tabs_customize_desc),
                    iconResID = R.drawable.edit,
                    position = RowPosition.Single,
                    showBackground = false
                ) {
                    showDialog = true
                }
            }

            item {
                PreferencesSeparatorText(
                    text = stringResource(id = R.string.updates)
                )
            }

            item {
                val checkForUpdatesOnStartup by mainViewModel.settings.Versions.getCheckUpdatesOnStartup()
                    .collectAsStateWithLifecycle(initialValue = false)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.updates_check),
                    iconResID = R.drawable.update,
                    summary = stringResource(id = R.string.updates_check_desc),
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
                text = stringResource(id = R.string.settings_general),
                fontSize = TextUnit(TextStylingConstants.EXTRA_EXTRA_LARGE_TEXT_SIZE, TextUnitType.Sp)
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
                    contentDescription = stringResource(id = R.string.return_to_previous_page),
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





