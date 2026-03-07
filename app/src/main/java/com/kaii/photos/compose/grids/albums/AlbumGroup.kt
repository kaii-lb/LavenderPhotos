package com.kaii.photos.compose.grids.albums

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.AlbumGroupInfoDialog
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.models.album_group.AlbumGroupViewModel
import com.kaii.photos.models.album_group.AlbumGroupViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumGroup(
    id: String,
    name: String,
    albumGridState: AlbumGridState,
    modifier: Modifier = Modifier,
    viewModel: AlbumGroupViewModel = viewModel(
        factory = AlbumGroupViewModelFactory(context = LocalContext.current)
    )
) {
    val navController = LocalNavController.current
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    var group by remember { mutableStateOf<AlbumGroup?>(null) }

    LaunchedEffect(groups) {
        group = groups.find { it.id == id }
    }

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlbumGroupInfoDialog(
            sheetState = sheetState,
            group = { group!! },
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
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = "Go back to previous page",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Text(
                        text = group?.name ?: name,
                        fontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                showInfoDialog = true
                                delay(50)
                                sheetState.expand()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = stringResource(id = R.string.album_group_info),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val singleAlbums by albumGridState.singleAlbums.collectAsStateWithLifecycle()
        val columnSize by viewModel.albumColumnSize.collectAsStateWithLifecycle()
        val immichInfo by viewModel.immichInfo.collectAsStateWithLifecycle()
        val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

        val albums = remember { mutableStateOf(emptyList<AlbumGridState.Album.Single>()) }

        LaunchedEffect(singleAlbums, group, sortMode) {
            withContext(Dispatchers.IO) {
                albums.value = singleAlbums.filter {
                    it.id in (group?.albumIds ?: emptyList())
                }.let { inGroup ->
                    when (sortMode) {
                        AlbumSortMode.LastModified -> inGroup.sortedBy { it.date }
                        AlbumSortMode.LastModifiedDesc -> inGroup.sortedByDescending { it.date }
                        AlbumSortMode.Alphabetically -> inGroup.sortedBy { it.name }
                        AlbumSortMode.AlphabeticallyDesc -> inGroup.sortedByDescending { it.name }
                        AlbumSortMode.Custom -> inGroup
                    }
                }
            }
        }

        SortableGrid(
            albumList = albums,
            tabList = emptyList(),
            sortMode = sortMode,
            columnSize = columnSize,
            immichInfo = immichInfo,
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            isAlbumGroup = true,
            setAlbumSortMode = viewModel::setAlbumSortMode,
            setAlbumOrder = viewModel::setAlbumOrder,
            addAlbumToGroup = { _, _ ->}
        )
    }
}