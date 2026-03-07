package com.kaii.photos.compose.pages.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaii.photos.LocalNavController
import com.kaii.photos.compose.grids.albums.SortableGrid
import com.kaii.photos.compose.widgets.albums.CategoryList
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.datastore.state.AlbumGridState
import com.kaii.photos.helpers.Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsGridView(
    deviceAlbums: State<List<AlbumGridState.Album>>,
    sortMode: AlbumSortMode,
    tabList: List<BottomBarTab>,
    columnSize: Int,
    immichInfo: ImmichBasicInfo,
    migrateFav: () -> Boolean,
    isMediaPicker: Boolean = false,
    setAlbumSortMode: (sortMode: AlbumSortMode) -> Unit,
    setAlbumOrder: (order: List<String>) -> Unit,
    addAlbumToGroup: (albumId: String, groupId: String) -> Unit
) {
    val navController = LocalNavController.current

    SortableGrid(
        albumList = deviceAlbums,
        sortMode = sortMode,
        tabList = tabList,
        columnSize = columnSize,
        immichInfo = immichInfo,
        navController = navController,
        prefix = {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "FavAndTrash"
            ) {
                CategoryList(
                    navigateToFavourites = {
                        navController.navigate(
                            if (migrateFav() && !isMediaPicker) Screens.Favourites.MigrationPage // TODO: handle media picker case
                            else Screens.Favourites.GridView
                        )
                    },
                    navigateToTrash = {
                        navController.navigate(Screens.Trash.GridView)
                    }
                )
            }
        },
        suffix = {
            // padding for floating bottom nav bar
            item(
                span = {
                    GridItemSpan(maxLineSpan)
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(120.dp)
                )
            }
        },
        setAlbumSortMode = setAlbumSortMode,
        setAlbumOrder = setAlbumOrder,
        addAlbumToGroup = addAlbumToGroup
    )
}

