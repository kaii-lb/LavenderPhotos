package com.kaii.photos

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.kaii.photos.activities.PhotoGridView
import com.kaii.photos.ui.theme.PhotosTheme


/** Displays media store data in a recycler view. */
@OptIn(ExperimentalGlideComposeApi::class)
class HorizontalGalleryFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val galleryViewModel: GalleryViewModel by viewModels()
    return ComposeView(requireContext()).apply {
        setContent {
           PhotosTheme {
               Content(galleryViewModel)
           }
        }
     }
  }

	@Composable
    private fun Content(viewModel: GalleryViewModel) {
    	val mediaStoreData = viewModel.mediaStoreData.collectAsState()
        Scaffold (
            modifier = Modifier
                .fillMaxSize(1f),
            topBar = {
                TopBar()
            },
            bottomBar = { BottomBar() }
        ) { padding ->
            Column (
                modifier = Modifier
                    .padding(padding)
            ) {
            	Column (
            		modifier = Modifier
                    	.padding(8.dp)	
            	) {
	                DeviceMedia(mediaStoreData.value)
            	}
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar() {
        TopAppBar(
            title = {
                Row {
                    Text(
                        text = "Lavender ",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Photos",
                        fontWeight = FontWeight.Normal
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { /*TODO*/ },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings Button",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
        )
    }

    @Preview
    @Composable
    private fun BottomBar() {
        BottomAppBar (
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            contentPadding = PaddingValues(0.dp),
        ) {
			val buttonHeight = 56.dp
			val buttonWidth = 64.dp
			val iconSize = 28.dp
			val textSize = 14f
        
            Row (
                modifier = Modifier
                    .fillMaxSize(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column (
                    modifier = Modifier
                    	.width(buttonWidth)
                        .height(buttonHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                	Row (
						modifier = Modifier
	                        .height(iconSize + 4.dp)
	                        .width(iconSize * 2f)
	                        .clip(RoundedCornerShape(1000.dp))
	                        .background(MaterialTheme.colorScheme.secondaryContainer),
						verticalAlignment = Alignment.CenterVertically,
                    	horizontalArrangement = Arrangement.Center
                	) {
		                Icon(
		                    painter = painterResource(id = R.drawable.photogrid_filled),
		                    contentDescription = "button",
		                    modifier = Modifier
		                        .size(iconSize)
		                )

                	}
	                Text (
	                    text = "Photos",
	                    fontSize = TextUnit(textSize, TextUnitType.Sp),
	                    modifier = Modifier
	                        .wrapContentSize()
	                )
                }

                Column (
                    modifier = Modifier
                    	.width(buttonWidth)
                        .height(buttonHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
	                Icon(
	                    painter = painterResource(id = R.drawable.locked_folder),
	                    contentDescription = "button",
	                    modifier = Modifier
	                        .size(iconSize)
	                )

	                Text (
	                    text = "Secure",
	                    fontSize = TextUnit(textSize, TextUnitType.Sp),
	                    modifier = Modifier
	                        .wrapContentSize()
	                )
                }

                Column (
                    modifier = Modifier
                    	.width(buttonWidth)
                        .height(buttonHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
	                Icon(
	                    painter = painterResource(id = R.drawable.albums),
	                    contentDescription = "button",
	                    modifier = Modifier
	                        .size(iconSize)
	                )

	                Spacer(modifier = Modifier.weight(1f))

	                Text (
	                    text = "Albums",
	                    fontSize = TextUnit(textSize, TextUnitType.Sp),
	                    modifier = Modifier
	                        .wrapContentSize()
	                )
                }

                Column (
                    modifier = Modifier
                    	.width(buttonWidth)
                        .height(buttonHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
	                Icon(
	                    painter = painterResource(id = R.drawable.search),
	                    contentDescription = "button",
	                    modifier = Modifier
	                        .size(iconSize)
	                )

	                Text (
	                    text = "Search",
	                    fontSize = TextUnit(textSize, TextUnitType.Sp),
	                    modifier = Modifier
	                        .wrapContentSize()
	                )
                }
            }
        }
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

    LazyColumn(
    	modifier = Modifier.fillMaxSize(1f),
    	horizontalAlignment = Alignment.Start,
    	verticalArrangement = Arrangement.Center
   	) {
      items(preloadingData.size) { i ->
      	println(i)
	    val (mediaStoreItem, preloadRequestBuilder) = preloadingData[i]
       	MediaStoreView(mediaStoreItem, preloadRequestBuilder)
      }
    }

  }

  private fun MediaStoreData.signature() = MediaStoreSignature(mimeType, dateModified, orientation)

  @Composable
  fun MediaStoreView(
    item: MediaStoreData,
    preloadRequestBuilder: RequestBuilder<Drawable>,
  ) {
		Column (
              modifier = Modifier
				.fillMaxWidth(1f/3f)
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
	      		it.thumbnail(preloadRequestBuilder).signature(item.signature())
	    	}
       }

  }

  companion object {
    private const val THUMBNAIL_DIMENSION = 50
    private val THUMBNAIL_SIZE = Size(THUMBNAIL_DIMENSION.toFloat(), THUMBNAIL_DIMENSION.toFloat())
  }
}
