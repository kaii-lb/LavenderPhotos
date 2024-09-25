package com.kaii.photos.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.MainActivity
import com.kaii.photos.R
import com.kaii.photos.datastore
import com.kaii.photos.datastore.getAlbumsList
import com.kaii.photos.datastore.addToAlbumsList
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.signature
import com.kaii.photos.models.album_grid.AlbumsViewModel
import com.kaii.photos.models.album_grid.AlbumsViewModelFactory
import kotlinx.coroutines.runBlocking
import java.io.File

@Composable
fun AlbumGridView(navController: NavHostController) {
    Column (
        modifier = Modifier
			.fillMaxSize(1f)
			.background(CustomMaterialTheme.colorScheme.background)
			.padding(8.dp, 0.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
		// val listOfDirs = emptyList<String>().toMutableList()
		val context = LocalContext.current
		runBlocking {
			context.datastore.addToAlbumsList("DCIM/Camera")
			context.datastore.addToAlbumsList("Pictures/Screenshot")
			context.datastore.addToAlbumsList("Pictures/Whatsapp")
			context.datastore.addToAlbumsList("Pictures/100PINT/Pins")
			context.datastore.addToAlbumsList("Movies")
			context.datastore.addToAlbumsList("LavenderPhotos/Restored Files")
			context.datastore.addToAlbumsList("Download")
			context.datastore.addToAlbumsList("Pictures/Instagram")
		}

		val listOfDirs = runBlocking {
			val list = context.datastore.getAlbumsList()
			list
		}

		val albumsViewModel: AlbumsViewModel = viewModel(
			factory = AlbumsViewModelFactory(LocalContext.current, listOfDirs.toList())
		)
		val mediaStoreData = albumsViewModel.mediaStoreData.collectAsState()
		val actualData = mediaStoreData.value

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize(1f),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Top
        ) {
        	item (
        		span = { GridItemSpan(maxLineSpan) }
        	) {
        		CategoryList(navController)
        	}

            items(
                count = listOfDirs.size
            ) { index ->
				val folder = File("/storage/emulated/0/" + listOfDirs[index])
				val neededDir = listOfDirs[index]

				if (actualData.isNotEmpty()) {
					val mediaItem = actualData[neededDir] ?: MediaStoreData()

				    val requestBuilderTransform =
				        { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
				            requestBuilder.load(item.uri).centerCrop()
				        }

					val preloadingData =
				        rememberGlidePreloadingData(
				            listOf(mediaItem),
				            Size(100f, 100f),
				            requestBuilderTransform = requestBuilderTransform
                        )
					val (mediaStoreItem, preloadRequestBuilder) = preloadingData[0]

					AlbumGridItem(
						navController,
						folder.name,
						neededDir,
						mediaStoreItem,
						preloadRequestBuilder
					)
				}
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridItem(
	navController: NavHostController,
	title: String,
	neededDir: String,
	item: MediaStoreData,
	preloadRequestBuilder: RequestBuilder<Drawable>
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
					MainActivity.mainViewModel.setSelectedAlbumDir(neededDir)
					navController.navigate(MultiScreenViewType.SingleAlbumView.name)
				},

				onDoubleClick = { /*ignore double clicks*/ },

				onLongClick = {
					// TODO: select item
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
                modifier = Modifier
					.aspectRatio(1f)
					.clip(RoundedCornerShape(16.dp)),
            ) {
                it.thumbnail(preloadRequestBuilder).signature(item.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
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
private fun CategoryList(navController: NavHostController) {
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
            onClick = { /*TODO*/ },
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
        			painter = painterResource(id = R.drawable.favorite),
					contentDescription = "Favorites Button",
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
	            	text = "Favorites",
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
				navController.navigate(MultiScreenViewType.TrashedPhotoView.name)
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


@Composable
private fun AlbumDialog(showDialog: MutableState<Boolean>) {
    if (showDialog.value) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

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
                        text = "Albums",
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

				Column (
					modifier = Modifier
						.padding(12.dp)
						.wrapContentHeight()
				) {
					DialogClickableItem(
						text = "Select",
						iconResId = R.drawable.check_item,
						position = DialogItemPosition.Top,
						action = {}
					)
									
					DialogClickableItem(
						text = "Rename Album",
						iconResId = R.drawable.edit,
						position = DialogItemPosition.Middle,
						action = {}
					)

					DialogClickableItem (
						text = "Remove album from list",
						iconResId = R.drawable.delete,
						position = DialogItemPosition.Bottom,
						action = {
							coroutineScope.launch {
								showDialog.value = false
	                           	navController.popBackStack()
	                           	context.datastore.removeFromAlbumsList(dir)
	                       	}
						}
					)
				}
            }
        }
    }
}
