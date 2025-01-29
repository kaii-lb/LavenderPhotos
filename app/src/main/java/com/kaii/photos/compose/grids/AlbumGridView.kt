package com.kaii.photos.compose.grids

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.LocalNavController
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.helpers.MainScreenViewType
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.baseInternalStorageDirectory
import com.kaii.photos.helpers.brightenColor
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.album_grid.AlbumsViewModel
import com.kaii.photos.models.album_grid.AlbumsViewModelFactory
import java.io.File


@Composable
fun AlbumsGridView(listOfDirs: List<String>, currentView: MutableState<MainScreenViewType>) {
	val context = LocalContext.current
	val navController = LocalNavController.current

	val albumsViewModel: AlbumsViewModel = viewModel(
		factory = AlbumsViewModelFactory(context, listOfDirs.toList())
	)

	val albumToThumbnailMapping by albumsViewModel.mediaStoreData.collectAsStateWithLifecycle()

    BackHandler(
        enabled = currentView.value == MainScreenViewType.AlbumsGridView && navController.currentBackStackEntry?.destination?.route == MultiScreenViewType.MainScreen.name
    ) {
        currentView.value = MainScreenViewType.PhotosGridView
    }

	Column (
        modifier = Modifier
			.fillMaxSize(1f)
			.background(MaterialTheme.colorScheme.background)
			.padding(8.dp, 0.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
		val localConfig = LocalConfiguration.current
	    var isLandscape by remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

	    LaunchedEffect(localConfig) {
	    	isLandscape = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
	    }

        LazyVerticalGrid(
			columns = GridCells.Fixed(
				if (!isLandscape) {
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
				val folder = File(baseInternalStorageDirectory + listOfDirs[index])
				val neededDir = listOfDirs[index]

				if (albumToThumbnailMapping.isNotEmpty()) {
					val mediaItem = albumToThumbnailMapping[neededDir] ?: MediaStoreData()

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
			.background(MaterialTheme.colorScheme.surfaceContainer)
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
							MaterialTheme.colorScheme.surfaceContainer,
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
				color = MaterialTheme.colorScheme.onSurface,
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
			.background(MaterialTheme.colorScheme.background),
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
                    tint = MaterialTheme.colorScheme.primary,
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
		         	color = MaterialTheme.colorScheme.onBackground,
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                    	.size(20.dp)
        		)

	            Text(
	            	text = "Trash ",
		         	fontSize = TextUnit(16f, TextUnitType.Sp),
		          	textAlign = TextAlign.Center,
		         	color = MaterialTheme.colorScheme.onBackground,
		         	modifier = Modifier
		         		.fillMaxWidth(1f)
	           	)
        	}
        }
    }
}


