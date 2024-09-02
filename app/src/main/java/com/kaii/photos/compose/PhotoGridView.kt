package com.kaii.photos.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height	
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.MediaStoreSignature
import com.kaii.photos.models.gallery_model.GalleryViewModel
import com.kaii.photos.models.gallery_model.GalleryViewModelFactory
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.R
import com.kaii.photos.models.main_activity.MainDataSharingModel
import com.kaii.photos.MainActivity
import java.time.LocalTime

private const val THUMBNAIL_DIMENSION = 50
private val THUMBNAIL_SIZE = Size(THUMBNAIL_DIMENSION.toFloat(), THUMBNAIL_DIMENSION.toFloat())

private fun MediaStoreData.signature() = MediaStoreSignature(mimeType, dateModified, orientation)

@Composable
fun PhotoGrid(navController: NavHostController, path: String) {
	val galleryViewModel: GalleryViewModel = viewModel(
		factory = GalleryViewModelFactory(LocalContext.current.applicationContext, path)
	)
	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

    val mainViewModel = MainActivity.mainViewModel

    DeviceMedia(mediaStoreData.value, navController, mainViewModel)
}

@Composable
fun DeviceMedia(
    mediaStoreData: List<MediaStoreData>,
    navController: NavHostController,
    mainViewModel: MainDataSharingModel
) {
    val groupedMedia = groupPhotosBy(mediaStoreData, true)

    val requestBuilderTransform =
        { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder.load(item.uri).signature(item.signature()).centerCrop()
        }

    val preloadingData =
        rememberGlidePreloadingData(
            groupedMedia,
            THUMBNAIL_SIZE,
            requestBuilderTransform = requestBuilderTransform,
        )

    val gridState = rememberLazyGridState()

	var showLoadingSpinner by remember { mutableStateOf(true) }

	if (showLoadingSpinner) {
		println("SPINNING STARTED AT ${LocalTime.now()}")
		Row (
			modifier = Modifier
				.fillMaxWidth(1f)
				.height(48.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center
		) {
			Row (
				modifier = Modifier
					.size(40.dp)	
					.clip(RoundedCornerShape(1000.dp))
					.background(MaterialTheme.colorScheme.surfaceContainer),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Center
			) {
				CircularProgressIndicator(
					modifier = Modifier
						.size(28.dp),
					color = MaterialTheme.colorScheme.primary,
					strokeWidth = 4.dp,
					strokeCap = StrokeCap.Round
				)
			}
		}
	}
	
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(1f),
        state = gridState
    ) {	
        items(
            count = preloadingData.size,
            key = {
                // println("URI STRING ${mediaStoreData[it].uri}")
                groupedMedia[it].uri.toString()
            },
            span = { index ->
                val item = groupedMedia[index]
                if (item.type == MediaType.Section) {
                    GridItemSpan(maxLineSpan)
                } else {
                    GridItemSpan(1)
                }
            }
        ) { i ->
            val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]

            MediaStoreItem(
            	mediaStoreItem,
                preloadRequestBuilder,
                navController,
                mainViewModel
            )

            if (i >= 0) {
            	showLoadingSpinner = false	
            	println("SPINNING ENDED AT ${LocalTime.now()}")
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaStoreItem(
    item: MediaStoreData,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    navController: NavHostController,
    mainViewModel: MainDataSharingModel
) {
    if (item.mimeType == null && item.type == MediaType.Section) {
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .aspectRatio(5.5f)
                .padding(16.dp, 8.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text (
                text = item.displayName ?: "This was meant to be a dated section",
                fontSize = TextUnit(16f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    } else {
        Box (
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(MaterialTheme.colorScheme.primary)
                .combinedClickable (
                    onClick = {
                        mainViewModel.setSelectedMediaData(item)
                        navController.navigate(MultiScreenViewType.SinglePhotoView.name)
                    },

                    onDoubleClick = { /*ignore double clicks*/ },

                    onLongClick = {
                        // TODO: select item
                    }
                ),
        ) {
            GlideImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .align(Alignment.Center),
            ) {
                it.thumbnail(preloadRequestBuilder).signature(item.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
            }

			if (item.type == MediaType.Video) {
        		Icon (
        			painter = painterResource(id = R.drawable.play_arrow),
					contentDescription = "file is video indicator",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
        		)
        	}            
        }
    }

}
