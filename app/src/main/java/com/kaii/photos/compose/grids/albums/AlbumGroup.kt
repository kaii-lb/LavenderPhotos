package com.kaii.photos.compose.grids.albums

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.AlbumGroupTopBar
import com.kaii.photos.compose.dialogs.AlbumGroupInfoDialog
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.models.album_group.AlbumGroupViewModel
import com.kaii.photos.models.album_group.AlbumGroupViewModelFactory
import com.kaii.photos.permissions.auth.rememberSecureFolderAuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumGroup(
    id: String,
    modifier: Modifier = Modifier,
    viewModel: AlbumGroupViewModel = viewModel(
        factory = AlbumGroupViewModelFactory(
            context = LocalContext.current,
            id = id
        )
    )
) {
    val navController = LocalNavController.current

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    var showInfoDialog by remember { mutableStateOf(false) }

    val groups by viewModel.groups.collectAsStateWithLifecycle()
    if (showInfoDialog) {
        AlbumGroupInfoDialog(
            sheetState = sheetState,
            group = { viewModel.group!! },
            groups = { groups },
            editGroup = viewModel::editGroup,
            deleteGroup = {
                coroutineScope.launch {
                    sheetState.hide()
                    viewModel.deleteGroup(it)
                    navController.popBackStack()
                    showInfoDialog = false
                }
            },
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    showInfoDialog = false
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            AlbumGroupTopBar(
                group = { viewModel.group },
                navController = navController,
                showInfoDialog = {
                    if (viewModel.group != null) {
                        coroutineScope.launch {
                            showInfoDialog = true
                            delay(50)
                            sheetState.expand()
                        }
                    }
                },
                setAlbums = { ids ->
                    viewModel.setGroupAlbums(
                        id = id,
                        albumIds = ids
                    )
                }
            )
        }
    ) { paddingValues ->
        val authManager = rememberSecureFolderAuthManager()
        val immichInfo by viewModel.immichInfo.collectAsStateWithLifecycle()
        val columnSize by viewModel.albumColumnSize.collectAsStateWithLifecycle()
        val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
        val albums by viewModel.albums.collectAsStateWithLifecycle()

        SortableGrid(
            albumList = { albums },
            sortMode = { sortMode },
            tabList = { emptyList() },
            columnSize = columnSize,
            immichInfo = { immichInfo },
            navController = navController,
            autoDetect = { false },
            modifier = Modifier.padding(paddingValues),
            isAlbumGroup = true,
            removeAlbumIcon = R.drawable.folder_off,
            setAlbumSortMode = viewModel::setAlbumSortMode,
            setAlbumOrder = viewModel::setAlbumOrder,
            addAlbumToGroup = { _, _ -> },
            authenticateSecureFolder = { authManager.authenticate() },
            toggleAlbumPin = {
                viewModel.toggleAlbumPin(
                    album = it as AlbumGridState.Album.Single
                )
            },
            deleteAlbum = {
                viewModel.deleteAlbum(
                    album = it as AlbumGridState.Album.Single,
                    group = viewModel.group!!
                )
            }
        )
    }
}