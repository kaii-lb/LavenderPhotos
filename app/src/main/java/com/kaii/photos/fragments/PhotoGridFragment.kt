package com.kaii.photos.fragments

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.MediaStoreSignature
import com.kaii.photos.gallery_model.GalleryViewModel
import com.kaii.photos.gallery_model.GalleryViewModelFactory
import com.kaii.photos.mediastore.MediaStoreData
import java.time.ZoneId
import java.util.Date

private const val THUMBNAIL_DIMENSION = 50
private val THUMBNAIL_SIZE = Size(THUMBNAIL_DIMENSION.toFloat(), THUMBNAIL_DIMENSION.toFloat())

private fun MediaStoreData.signature() = MediaStoreSignature(mimeType, dateModified, orientation)

@Composable
fun PhotoGrid() {
	val galleryViewModel: GalleryViewModel = viewModel(
		factory = GalleryViewModelFactory(LocalContext.current.applicationContext)
	)
	val mediaStoreData = galleryViewModel.mediaStoreData.collectAsState()

	DeviceMedia(mediaStoreData.value)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceMedia(mediaStoreData: List<MediaStoreData>) {
    val requestBuilderTransform =
        { item: MediaStoreData, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder.load(item.uri).signature(item.signature()).centerCrop()
        }

    val preloadingData =
        rememberGlidePreloadingData(
            mediaStoreData,
            THUMBNAIL_SIZE,
            requestBuilderTransform = requestBuilderTransform,
        )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(1f)
    ) {
        items(
        	count = preloadingData.size,
        	key = {
        		println("URI STRING ${mediaStoreData[it].uri.toString()}_$it")
        		mediaStoreData[it].uri.toString() + "_$it"
        	}
       	) { i ->
            val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]
            MediaStoreView(mediaStoreItem,
                preloadRequestBuilder,
                Modifier.aspectRatio(1f)
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaStoreView(
    item: MediaStoreData,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    modifier: Modifier,
) {
    Column(
        modifier = Modifier
			.then (modifier)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlideImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
       			.fillMaxSize(1f)            
       			.clip(RoundedCornerShape(8.dp)),
        ) {
            it.thumbnail(preloadRequestBuilder).signature(item.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
        }
    }

}
