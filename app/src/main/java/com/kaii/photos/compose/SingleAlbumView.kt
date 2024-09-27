package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.kaii.photos.MainActivity
import com.kaii.photos.compose.DialogClickableItem
import com.kaii.photos.R
import com.kaii.photos.datastore
import com.kaii.photos.datastore.removeFromAlbumsList
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.helpers.single_image_functions.ImageFunctions
import kotlinx.coroutines.launch

@Composable
fun SingleAlbumView(navController: NavHostController) {
    val mainViewModel = MainActivity.mainViewModel

    val albumDir = mainViewModel.selectedAlbumDir.collectAsState(initial = null).value
    println("ALBUM DIR IS $albumDir")
    if (albumDir == null) return
	
    Scaffold (
        topBar =  { TopBar(navController, albumDir) },
        containerColor = CustomMaterialTheme.colorScheme.background,
        contentColor = CustomMaterialTheme.colorScheme.onBackground
    ) { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
       		PhotoGrid(navController, ImageFunctions.LoadNormalImage, albumDir, MediaItemSortMode.DateTaken)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController, dir: String) {
	val title = dir.split("/").last()
	val showDialog = remember { mutableStateOf(false) }

    SingleAlbumDialog(showDialog, dir, navController)

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = { navController.popBackStack() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = "Go back to previous page",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontSize = TextUnit(18f, TextUnitType.Sp),
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
                    showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = "show more options for the album view",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@Composable
private fun SingleAlbumDialog(showDialog: MutableState<Boolean>, dir: String, navController: NavHostController) {
    if (showDialog.value) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val title = dir.split("/").last()

        Dialog(
            onDismissRequest = {
                showDialog.value = false
            }
        ) {
            Column (
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(brightenColor(CustomMaterialTheme.colorScheme.surface, 0.1f))
                    .padding(8.dp)
            ) {
                Box (
                    modifier = Modifier
                        .fillMaxWidth(1f),
                ) {
                    IconButton(
                        onClick = {
                            showDialog.value = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close dialog button",
                            modifier = Modifier
                            	.size(24.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

				Column (
					modifier = Modifier
						.padding(12.dp)
						.wrapContentHeight()
				) {
					DialogClickableItem(
						text = "Select",
						iconResId = R.drawable.check_item,
						position = RowPosition.Top,
						action = {}
					)
									
					DialogClickableItem(
						text = "Rename Album",
						iconResId = R.drawable.edit,
						position = RowPosition.Middle,
						action = {}
					)

					DialogClickableItem (
						text = "Remove album from list",
						iconResId = R.drawable.delete,
						position = RowPosition.Bottom,
						action = {
							coroutineScope.launch {
	                           	context.datastore.removeFromAlbumsList(dir)
								showDialog.value = false
	                           	navController.popBackStack()
	                       	}
						}
					)
				}
            }
        }
    }
}
