package com.kaii.photos.compose.grids

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.albums.AlbumGridItem
import com.kaii.photos.compose.widgets.rememberDeviceOrientation
import com.kaii.photos.datastore.AlbumGroup
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.AlbumType
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.datastore.state.rememberAlbumGridState
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.models.album_group.AlbumGroupViewModel
import com.kaii.photos.models.album_group.AlbumGroupViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumGroup(
    id: String,
    name: String,
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
                }
            )
        }
    ) { paddingValues ->
        val isLandscape by rememberDeviceOrientation()

        val albumGridState = rememberAlbumGridState()
        val singleAlbums by albumGridState.singleAlbums.collectAsStateWithLifecycle()
        val columnSize by viewModel.albumColumnSize.collectAsStateWithLifecycle()
        val immichInfo by viewModel.immichInfo.collectAsStateWithLifecycle()
        val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

        var albums by remember { mutableStateOf(emptyList<AlbumGridState.Album.Single>()) }

        LaunchedEffect(singleAlbums, group) {
            withContext(Dispatchers.IO) {
                albums = singleAlbums.filter {
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(
                if (!isLandscape) {
                    columnSize
                } else {
                    columnSize * 2
                }
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
            items(
                count = albums.size,
                key = { key ->
                    albums[key].id
                },
            ) { index ->
                val album = albums[index]

                AlbumGridItem(
                    album = album,
                    isSelected = false,
                    info = immichInfo,
                    modifier = Modifier
                        .wrapContentSize()
                        .animateItem(
                            fadeInSpec = tween(
                                durationMillis = 250
                            ),
                            fadeOutSpec = tween(
                                durationMillis = 250
                            ),
                            placementSpec = tween(durationMillis = 250)
                        )
                ) {
                    navController.navigate(
                        route =
                            when (album.info.album) {
                                is AlbumType.Cloud -> {
                                    Screens.Immich.GridView(album = album.info.album)
                                }

                                is AlbumType.Custom -> {
                                    Screens.CustomAlbum.GridView(album = album.info.album)
                                }

                                else -> {
                                    Screens.Album.GridView(album = album.info.album as AlbumType.Folder)
                                }
                            }
                    )
                }
            }
        }
    }
}