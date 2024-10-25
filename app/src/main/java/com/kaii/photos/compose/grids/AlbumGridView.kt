package com.kaii.photos.compose.grids

import android.content.res.Configuration
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.datastore
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.datastore.getAlbumsList
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.album_grid.AlbumsViewModel
import com.kaii.photos.models.album_grid.AlbumsViewModelFactory
import kotlinx.coroutines.runBlocking
import java.io.File

@Composable
fun AlbumsGridView(navController: NavHostController) {
	val context = LocalContext.current
	val listOfDirs = runBlocking {
		val list = context.datastore.getAlbumsList()
		list.toMutableList()
	}

	listOfDirs.sortByDescending {
		File("/storage/emulated/0/" + it).lastModified()
	}
	listOfDirs.find { it == "DCIM/Camera" }?.let { cameraItem ->
		listOfDirs.remove(cameraItem)
		listOfDirs.add(0, cameraItem)
	}

	val albumsViewModel: AlbumsViewModel = viewModel(
		factory = AlbumsViewModelFactory(context, listOfDirs.toList())
	)

	val mediaStoreData = albumsViewModel.mediaStoreData.collectAsState()
	val actualData = mediaStoreData.value

    Column (
        modifier = Modifier
			.fillMaxSize(1f)
			.background(CustomMaterialTheme.colorScheme.background)
			.padding(8.dp, 0.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyVerticalGrid(
			columns = GridCells.Fixed(
				if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
					2
				} else {
					4
				}
			),
            modifier = Modifier
                .fillMaxSize(1f),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
        	item (
        		span = { GridItemSpan(maxLineSpan) }
        	) {
        		CategoryList(
					navigateToFavourites = {
						navController.navigate(MultiScreenViewType.FavouritesGridView.name)
					},
					navigateToTrash = {
						navController.navigate(MultiScreenViewType.TrashedPhotoView.name)
					}
				)
        	}

            items(
                count = listOfDirs.size,
	            key = { key ->
	                listOfDirs[key]
	            },                
            ) { index ->
				val folder = File("/storage/emulated/0/" + listOfDirs[index])
				val neededDir = listOfDirs[index]

				if (actualData.isNotEmpty()) {
					val mediaItem = actualData[neededDir] ?: MediaStoreData()

					Row (
						modifier = Modifier
							.wrapContentSize()	
							.animateItem(
								fadeInSpec = tween(
									durationMillis = 250
								),
								fadeOutSpec = tween(
									durationMillis = 250
								)
							)
					) {
						AlbumGridItem(
							title = folder.name,
							item = mediaItem
						) {
							MainActivity.mainViewModel.setSelectedAlbumDir(neededDir)
							navController.navigate(MultiScreenViewType.SingleAlbumView.name)
						}
					}
				}
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridItem(
	title: String,
	item: MediaStoreData,
	onClick: () -> Unit
) {	
	Column (
		modifier = Modifier
			.wrapContentHeight()
			.fillMaxWidth(1f)
			.padding(6.dp)
			.clip(RoundedCornerShape(24.dp))
			.background(CustomMaterialTheme.colorScheme.surfaceContainer)
			.combinedClickable(
				onClick = {
					onClick()
				}
			),
		verticalArrangement = Arrangement.SpaceEvenly,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Column (
			modifier = Modifier
				.fillMaxSize(1f)
				.padding(8.dp, 8.dp, 8.dp, 4.dp)
				.clip(RoundedCornerShape(16.dp)),
			verticalArrangement = Arrangement.SpaceEvenly,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			GlideImage(
				model = item.uri,
				contentDescription = item.displayName,
				contentScale = ContentScale.Crop,
				failure = placeholder(R.drawable.broken_image),
				modifier = Modifier
					.aspectRatio(1f)
					.clip(RoundedCornerShape(16.dp))
					.background(
						brightenColor(
							CustomMaterialTheme.colorScheme.surfaceContainer,
							0.1f
						)
					),
			) {
				it.signature(item.signature())
			}

			Text(
				text = " $title",
				fontSize = TextUnit(14f, TextUnitType.Sp),
				textAlign = TextAlign.Start,
				color = CustomMaterialTheme.colorScheme.onSurface,
				maxLines = 1,
				modifier = Modifier
					.fillMaxWidth(1f)
					.padding(2.dp)
			)
		}
	}
}

@Composable
private fun CategoryList(
	navigateToTrash: () -> Unit,
	navigateToFavourites: () -> Unit
) {
	Row (
        modifier = Modifier
			.fillMaxWidth(1f)
			.wrapContentHeight()
			.padding(8.dp)
			.background(CustomMaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = {
				navigateToFavourites()
			},
            modifier = Modifier
				.weight(1f)
				.height(48.dp)
        ) {
        	Row (
        		modifier = Modifier.fillMaxWidth(1f),
        		verticalAlignment = Alignment.CenterVertically,
   		        horizontalArrangement = Arrangement.SpaceEvenly
        	) {
				Icon (
        			painter = painterResource(id = R.drawable.favourite),
					contentDescription = "Favourites Button",
                    tint = CustomMaterialTheme.colorScheme.primary,
                    modifier = Modifier
						.size(22.dp)
						.padding(0.dp, 2.dp, 0.dp, 0.dp)
        		)

				Spacer (
					modifier = Modifier
						.width(8.dp)
				)

	            Text(
	            	text = "Favourites",
		         	fontSize = TextUnit(16f, TextUnitType.Sp),
		          	textAlign = TextAlign.Center,
		         	color = CustomMaterialTheme.colorScheme.onBackground,
		         	modifier = Modifier
		         		.fillMaxWidth(1f)
	           	)
        	}
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedButton(
            onClick = {
				navigateToTrash()
			},
            modifier = Modifier
				.weight(1f)
				.height(48.dp)
        ) {
        	Row (
        		modifier = Modifier.fillMaxWidth(1f),
        		verticalAlignment = Alignment.CenterVertically,
   		        horizontalArrangement = Arrangement.SpaceEvenly
        	) {
        		Icon (
        			painter = painterResource(id = R.drawable.trash),
					contentDescription = "Trash Button",
                    tint = CustomMaterialTheme.colorScheme.primary,
                    modifier = Modifier
                    	.size(20.dp)
        		)

	            Text(
	            	text = "Trash ",
		         	fontSize = TextUnit(16f, TextUnitType.Sp),
		          	textAlign = TextAlign.Center,
		         	color = CustomMaterialTheme.colorScheme.onBackground,
		         	modifier = Modifier
		         		.fillMaxWidth(1f)
	           	)
        	}
        }
    }
}


