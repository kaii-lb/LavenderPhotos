package com.kaii.photos.fragments

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
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
import com.kaii.photos.gallery_model.GalleryViewModelFactory
import com.kaii.photos.gallery_model.GalleryViewModel
import com.kaii.photos.mediastore.MediaStoreData

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

@Composable
fun DeviceMedia(mediaStoreData: List<List<MediaStoreData>>) {
    val requestBuilderTransform =
        { item: List<MediaStoreData>, requestBuilder: RequestBuilder<Drawable> ->
        	requestBuilder.load(item[0].uri).signature(item[0].signature()).centerCrop()
        }
        

    val preloadingData =
        rememberGlidePreloadingData(
            mediaStoreData,
            THUMBNAIL_SIZE,
            requestBuilderTransform = requestBuilderTransform,
        )      

    LazyColumn(
        modifier = Modifier.fillMaxSize(1f),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        items(preloadingData.size) { i ->
            println(i)
            val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]

            Row (
            	modifier = Modifier.fillMaxSize(1f)
            ) {
	            if(1 <= mediaStoreItem.size) MediaStoreView(mediaStoreItem[0], preloadRequestBuilder, Modifier.weight(1f))
	            if(2 <= mediaStoreItem.size) MediaStoreView(mediaStoreItem[1], preloadRequestBuilder, Modifier.weight(1f))
	            if(3 <= mediaStoreItem.size) MediaStoreView(mediaStoreItem[2], preloadRequestBuilder, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaStoreView(
    item: MediaStoreData,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    ontop: Modifier,
) {
    Column(
        modifier = Modifier
			then(ontop)
            .aspectRatio(1f)
            .padding(4.dp)
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlideImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(1f)
        ) {
            it.thumbnail(preloadRequestBuilder).signature(item.signature()).diskCacheStrategy(DiskCacheStrategy.ALL)
        }
    }

}
