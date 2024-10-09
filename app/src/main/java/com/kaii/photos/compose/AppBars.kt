package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.helpers.MainScreenViewType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppTopBar(showDialog: MutableState<Boolean>) {
    TopAppBar(
        title = {
            Row {
                Text(
                    text = "Lavender ",
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
                Text(
                    text = "Photos",
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings Button",
                    tint = CustomMaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.background
        ),
    )
}

@Composable
fun MainAppBottomBar(currentView: MutableState<MainScreenViewType>) {
    BottomAppBar(
        containerColor = CustomMaterialTheme.colorScheme.surfaceContainer,
        contentColor = CustomMaterialTheme.colorScheme.onPrimaryContainer,
        contentPadding = PaddingValues(0.dp),
    ) {
        val buttonHeight = 56.dp
        val buttonWidth = 64.dp
        val iconSize = 24.dp
        val textSize = 14f

        Row(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // should find a better way
            val unselectedColor = CustomMaterialTheme.colorScheme.surfaceContainer
            val selectedColor = CustomMaterialTheme.colorScheme.secondaryContainer
            var photoGridColor by remember { mutableStateOf(unselectedColor) }
            var lockedFolderColor by remember { mutableStateOf(unselectedColor) }
            var albumGridColor by remember { mutableStateOf(unselectedColor) }
            var searchPageColor by remember { mutableStateOf(unselectedColor) }
            // for the love of god find a better way
            var photoGridIcon by remember { mutableIntStateOf(R.drawable.photogrid_filled) }
            var lockedFolderIcon by remember { mutableIntStateOf(R.drawable.locked_folder) }
            var albumGridIcon by remember { mutableIntStateOf(R.drawable.albums) }

            when (currentView.value) {
                MainScreenViewType.PhotosGridView -> {
                    photoGridColor = selectedColor
                    lockedFolderColor = unselectedColor
                    albumGridColor = unselectedColor
                    searchPageColor = unselectedColor

                    photoGridIcon = R.drawable.photogrid_filled
                    lockedFolderIcon = R.drawable.locked_folder
                    albumGridIcon = R.drawable.albums
                }
                MainScreenViewType.SecureFolder -> {
                    photoGridColor = unselectedColor
                    lockedFolderColor = selectedColor
                    albumGridColor = unselectedColor
                    searchPageColor = unselectedColor

                    photoGridIcon = R.drawable.photogrid
                    lockedFolderIcon = R.drawable.locked_folder_filled
                    albumGridIcon = R.drawable.albums
                }
                MainScreenViewType.AlbumsGridView -> {
                    photoGridColor = unselectedColor
                    lockedFolderColor = unselectedColor
                    albumGridColor = selectedColor
                    searchPageColor = unselectedColor

                    photoGridIcon = R.drawable.photogrid
                    lockedFolderIcon = R.drawable.locked_folder
                    albumGridIcon = R.drawable.albums_filled
                }
                MainScreenViewType.SearchPage -> {
                    photoGridColor = unselectedColor
                    lockedFolderColor = unselectedColor
                    albumGridColor = unselectedColor
                    searchPageColor = selectedColor

                    photoGridIcon = R.drawable.photogrid
                    lockedFolderIcon = R.drawable.locked_folder
                    albumGridIcon = R.drawable.albums
                }
            }

            // photo grid button
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(buttonHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (currentView.value != MainScreenViewType.PhotosGridView) {
                            currentView.value = MainScreenViewType.PhotosGridView
                        }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .height(iconSize + 8.dp)
                        .width(iconSize * 2.25f)
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(1000.dp))
                        .background(photoGridColor),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = photoGridIcon),
                        contentDescription = "button",
                        modifier = Modifier
                            .size(iconSize)
                    )

                }
                Text(
                    text = "Photos",
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomCenter)
                )
            }

            // locked folder button
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(buttonHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (currentView.value != MainScreenViewType.SecureFolder) {
                            currentView.value = MainScreenViewType.SecureFolder
                        }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .height(iconSize + 8.dp)
                        .width(iconSize * 2.25f)
                        .clip(RoundedCornerShape(1000.dp))
                        .align(Alignment.TopCenter)
                        .background(lockedFolderColor),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = lockedFolderIcon),
                        contentDescription = "button",
                        modifier = Modifier
                            .size(iconSize)
                    )
                }

                Text(
                    text = "Secure",
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomCenter)
                )
            }

            // album grid button
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(buttonHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (currentView.value != MainScreenViewType.AlbumsGridView) {
                            currentView.value = MainScreenViewType.AlbumsGridView
                        }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .height(iconSize + 8.dp)
                        .width(iconSize * 2.25f)
                        .clip(RoundedCornerShape(1000.dp))
                        .align(Alignment.TopCenter)
                        .background(albumGridColor),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = albumGridIcon),
                        contentDescription = "button",
                        modifier = Modifier
                            .size(iconSize)
                    )
                }

                Text(
                    text = "Albums",
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomCenter)
                )
            }

            // search page button
            Box(
                modifier = Modifier
                    .width(buttonWidth)
                    .height(buttonHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (currentView.value != MainScreenViewType.SearchPage) {
                            currentView.value = MainScreenViewType.SearchPage
                        }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .height(iconSize + 8.dp)
                        .width(iconSize * 2.25f)
                        .clip(RoundedCornerShape(1000.dp))
                        .align(Alignment.TopCenter)
                        .background(searchPageColor),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "button",
                        modifier = Modifier
                            .size(iconSize)
                    )
                }

                Text(
                    text = "Search",
                    fontSize = TextUnit(textSize, TextUnitType.Sp),
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(selectedItemsList: SnapshotStateList<String>) {
	TopAppBar(
        title = {
            Row (
            	verticalAlignment = Alignment.CenterVertically,
            	horizontalArrangement = Arrangement.SpaceEvenly
            ){
            	Button(
            		onClick = {
            			selectedItemsList.clear()
            		},
            		modifier = Modifier
            			.width(56.dp)
            	) {
            		Icon(
            			painter = painterResource(id = R.drawable.close),
            			contentDescription = "clear selection button",
            			modifier = Modifier
            				// .size(48.dp)
            		)
            	}

				Spacer(modifier = Modifier.width(16.dp))
            	
				Text(
                    text = selectedItemsList.size.toString(),
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(20f, TextUnitType.Sp)
                )            		
            }
        },
        actions = {
            IconButton(
                onClick = {
                    // showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_options),
                    contentDescription = "show more options for selected items",
                    tint = CustomMaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                    	.size(24.dp)
                )
            }        	
        },
        scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.background
        ),
    )
}
