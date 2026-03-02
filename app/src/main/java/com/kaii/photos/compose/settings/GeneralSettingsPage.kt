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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
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
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.di.appModule
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.grid_management.MediaItemSortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsPage(modifier: Modifier = Modifier) {
    val settings = LocalContext.current.appModule.settings

    val allAlbums by settings.albums.get().collectAsStateWithLifecycle(initialValue = emptyList())
    val mainPhotosPaths by settings.mainPhotosView.getAlbums().collectAsStateWithLifecycle(initialValue = emptyList())
    val shouldShowEverything by settings.mainPhotosView.getShowEverything().collectAsStateWithLifecycle(initialValue = false)
    val autoDetectAlbums by settings.albums.getAutoDetect().collectAsStateWithLifecycle(initialValue = true)
    val currentSortMode by settings.photoGrid.getSortMode().collectAsStateWithLifecycle(initialValue = MediaItemSortMode.DateTaken)
    val tabList by settings.defaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = emptyList())
    val defaultTab by settings.defaultTabs.getDefaultTab().collectAsStateWithLifecycle(initialValue = DefaultTabs.TabTypes.photos)
    val checkForUpdatesOnStartup by settings.versions.getCheckUpdatesOnStartup().collectAsStateWithLifecycle(initialValue = false)

    GeneralSettingsPageImpl(
        allAlbums = allAlbums,
        mainPhotosPaths = mainPhotosPaths,
        shouldShowEverything = shouldShowEverything,
        autoDetectAlbums = autoDetectAlbums,
        currentSortMode = currentSortMode,
        tabList = tabList,
        defaultTab = defaultTab,
        checkForUpdatesOnStartup = checkForUpdatesOnStartup,
        modifier = modifier,
        setShowEverything = settings.mainPhotosView::setShowEverything,
        addMainPhotosAlbum = settings.mainPhotosView::addAlbum,
        clearMainPhotosAlbums = settings.mainPhotosView::clear,
        setAutoDetect = settings.albums::setAutoDetect,
        resetAlbums = settings.albums::reset,
        setSortMode = settings.photoGrid::setSortMode,
        setCheckUpdatesOnStartup = settings.versions::setCheckUpdatesOnStartup,
        setTabList = settings.defaultTabs::setTabList,
        setDefaultTab = settings.defaultTabs::setDefaultTab
    )
}

@Preview
@Composable
private fun GeneralSettingsPagePreview(modifier: Modifier = Modifier) {
    GeneralSettingsPageImpl(
        allAlbums = emptyList(),
        mainPhotosPaths = emptyList(),
        shouldShowEverything = false,
        autoDetectAlbums = false,
        currentSortMode = MediaItemSortMode.DateTaken,
        tabList = DefaultTabs.defaultList,
        defaultTab = DefaultTabs.TabTypes.photos,
        checkForUpdatesOnStartup = false,
        modifier = modifier,
        setShowEverything = {},
        addMainPhotosAlbum = {},
        clearMainPhotosAlbums = {},
        setAutoDetect = {},
        resetAlbums = {},
        setSortMode = {},
        setCheckUpdatesOnStartup = {},
        setTabList = {},
        setDefaultTab = {}
    )
}

@Composable
private fun GeneralSettingsPageImpl(
    allAlbums: List<AlbumInfo>,
    mainPhotosPaths: Collection<String>,
    shouldShowEverything: Boolean,
    autoDetectAlbums: Boolean,
    currentSortMode: MediaItemSortMode,
    tabList: List<BottomBarTab>,
    defaultTab: BottomBarTab,
    checkForUpdatesOnStartup: Boolean,
    modifier: Modifier,
    setShowEverything: (value: Boolean) -> Unit,
    addMainPhotosAlbum: (relativePath: String) -> Unit,
    clearMainPhotosAlbums: () -> Unit,
    setAutoDetect: (value: Boolean) -> Unit,
    resetAlbums: () -> Unit,
    setSortMode: (mode: MediaItemSortMode) -> Unit,
    setCheckUpdatesOnStartup: (value: Boolean) -> Unit,
    setTabList: (list: List<BottomBarTab>) -> Unit,
    setDefaultTab: (tab: BottomBarTab) -> Unit
) {
    Scaffold(
        topBar = {
            GeneralSettingsTopBar()
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                PreferencesSeparatorText(stringResource(id = R.string.albums))
            }

            item {
                val selectedAlbums = remember { mutableStateListOf<String>() }
                val showAlbumsSelectionDialog = remember { mutableStateOf(false) }

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.albums_main_list),
                    iconResID = R.drawable.photogrid,
                    position = RowPosition.Single,
                    checked = shouldShowEverything,
                    showBackground = false,
                    summary =
                        if (!shouldShowEverything) stringResource(id = R.string.albums_main_list_desc_1)
                        else stringResource(id = R.string.albums_main_list_desc_2),
                    onRowClick = {
                        selectedAlbums.clear()
                        selectedAlbums.addAll(
                            if (shouldShowEverything) {
                                val flat = allAlbums.flatMap { it.paths }.fastMap { it.removeSuffix("/") }

                                flat - mainPhotosPaths.map { it.removeSuffix("/") }.toSet()
                            } else {
                                mainPhotosPaths.map { it.removeSuffix("/") }
                            }
                        )

                        showAlbumsSelectionDialog.value = true
                    },
                    onSwitchClick = setShowEverything
                )

                if (showAlbumsSelectionDialog.value) {
                    SelectableButtonListDialog(
                        title = stringResource(id = R.string.albums_selected),
                        body =
                            if (!shouldShowEverything) stringResource(id = R.string.albums_main_list_selected)
                            else stringResource(id = R.string.albums_main_list_selected_inverse),
                        showDialog = showAlbumsSelectionDialog,
                        onConfirm = {
                            clearMainPhotosAlbums()

                            selectedAlbums.forEach { album ->
                                addMainPhotosAlbum(album)
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
                                        if (selectedAlbums.contains(associatedAlbum.mainPath) && (selectedAlbums.size > 1 || shouldShowEverything)) {
                                            selectedAlbums.remove(associatedAlbum.mainPath.removeSuffix("/"))
                                        } else {
                                            selectedAlbums.add(associatedAlbum.mainPath.removeSuffix("/"))
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                val context = LocalContext.current

                val isAlreadyLoading = remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val findingAlbums = stringResource(id = R.string.finding_albums_on_device)
                val foundAlbums = stringResource(id = R.string.albums_found)

                PreferencesSwitchRow(
                    title = stringResource(id = R.string.albums_auto_detect),
                    summary = stringResource(id = R.string.albums_auto_detect_desc),
                    iconResID = R.drawable.albums_search,
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = autoDetectAlbums
                ) { checked ->
                    // as to keep the MutableState alive even if the user leaves the screen
                    context.appModule.scope.launch(Dispatchers.IO) {
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

                                isAlreadyLoading.value = false
                            }

                            setAutoDetect(checked)

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
                        resetAlbums()
                        setAutoDetect(false)

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
                var showSortModeSelectorDialog by remember { mutableStateOf(false) }

                if (showSortModeSelectorDialog) {
                    SortModeSelectorDialog(
                        currentSortMode = currentSortMode,
                        setSortMode = setSortMode,
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
                    checked = currentSortMode != MediaItemSortMode.Disabled && currentSortMode != MediaItemSortMode.DisabledLastModified,
                    onRowClick = {
                        showSortModeSelectorDialog = true
                    },
                    onSwitchClick = { checked ->
                        if (checked) {
                            setSortMode(MediaItemSortMode.DateTaken)
                        } else {
                            setSortMode(
                                if (currentSortMode == MediaItemSortMode.DateModified) MediaItemSortMode.DisabledLastModified
                                else MediaItemSortMode.Disabled
                            )
                        }
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

                if (showDefaultTabAlbumDialog) {
                    DefaultTabSelectorDialog(
                        tabList = tabList,
                        defaultTab = defaultTab,
                        setTabList = setTabList,
                        setDefaultTab = setDefaultTab,
                        dismissDialog = {
                            showDefaultTabAlbumDialog = false
                        }
                    )
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
                        tabList = tabList,
                        setTabList = setTabList,
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
                PreferencesSwitchRow(
                    title = stringResource(id = R.string.updates_check),
                    iconResID = R.drawable.update,
                    summary = stringResource(id = R.string.updates_check_desc),
                    position = RowPosition.Single,
                    showBackground = false,
                    checked = checkForUpdatesOnStartup,
                    onSwitchClick = setCheckUpdatesOnStartup
                )
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





