package com.kaii.photos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import com.kaii.photos.compose.single_photo.GlideView
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import me.saket.telephoto.zoomable.rememberZoomableState

class TinyImageTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
          val fakeMediaItem = MediaStoreData(
                type = MediaType.Image,
                id = 1L,
                uri = "android.resource://${packageName}/${R.drawable.tiny_image}".toUri(),
                displayName = "tiny_image.jpg",
                mimeType = "image/jpeg",
            )

            GlideView(
                model = fakeMediaItem.uri,
                item = fakeMediaItem,
                zoomableState = rememberZoomableState(),
                window = window,
                appBarsVisible = remember { mutableStateOf(true) },
            )
        }
    }
}
