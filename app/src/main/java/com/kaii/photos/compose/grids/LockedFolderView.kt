package com.kaii.photos.compose.grids

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.MainActivity
import com.kaii.photos.compose.CustomMaterialTheme
import com.kaii.photos.helpers.getDateTakenForMedia
import com.kaii.photos.helpers.MediaItemSortMode
import com.kaii.photos.helpers.MultiScreenViewType
import com.kaii.photos.helpers.getAppLockedFolderDirectory
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import com.kaii.photos.models.gallery_model.groupPhotosBy
import com.kaii.photos.models.main_activity.MainDataSharingModel
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

@Composable
fun LockedFolderView(navController: NavHostController) {
    Scaffold (
        topBar =  { TopBar(navController) },
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
            val secureFolder = File(LocalContext.current.getAppLockedFolderDirectory())
			val fileList = secureFolder.listFiles() ?: return@Column
			val mediaStoreData = emptyList<MediaStoreData>().toMutableList()
            fileList.forEachIndexed { index, file ->
                val mimeType = Files.probeContentType(Path(file.absolutePath))
                val dateTaken = getDateTakenForMedia(file.absolutePath)

                val item = MediaStoreData(
                    type = if (mimeType.lowercase().contains("image")) MediaType.Image
                        else if (mimeType.lowercase().contains("video")) MediaType.Video
                        else MediaType.Section,
                    id = file.hashCode()  * file.length() * file.lastModified(),
                    uri = file.absolutePath.toUri(),
                    mimeType = mimeType,
                    dateModified = System.currentTimeMillis() / 1000,
                    dateTaken = dateTaken,
                    dateAdded = dateTaken,
                    orientation = 0,
                    displayName = file.name,
                    absolutePath = file.absolutePath,
                    gridPosition = index
                )
                mediaStoreData.add(item)
            }

            val groupedMedia = groupPhotosBy(mediaStoreData, MediaItemSortMode.DateTaken)

			if (fileList.isEmpty()) {
				Column (
		   			modifier = Modifier
		   				.background(CustomMaterialTheme.colorScheme.background),
		   			verticalArrangement = Arrangement.Center,
		   			horizontalAlignment = Alignment.CenterHorizontally
		   		) {
		   			Text (
		   				text = "Add files here to hide them securely",
				    	fontSize = TextUnit(18f, TextUnitType.Sp)
		   			)
		   		}
			} else {
				LazyVerticalGrid(
			        columns = GridCells.Fixed(3),
			        modifier = Modifier
			        	.fillMaxSize(1f),
			    ) {
			        items(
			            count = groupedMedia.size,
	                    key = {
	                        // println("URI STRING ${mediaStoreData[it].uri}")
	                        groupedMedia[it].hashCode().toString()
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
	                    val item = groupedMedia[i]

	                    LockedFolderItem(
	                        navController = navController,
	                        item = item,
	                        mainViewModel = MainActivity.mainViewModel,
	                        groupedMedia = groupedMedia
	                    )
			        }
			    }
			}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(navController: NavHostController) {
    val title = "Locked Folder"
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CustomMaterialTheme.colorScheme.surfaceContainer
        ),
        navigationIcon = {
            IconButton(
                onClick = { navController.popBackStack() },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.back_arrow),
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
                onClick = { /* TODO */ },
            ) {
                Icon(
                    painter = painterResource(id = com.kaii.photos.R.drawable.settings),
                    contentDescription = "show more options for the locked folder",
                    tint = CustomMaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun LockedFolderItem(
    navController: NavHostController,
    item: MediaStoreData,
    mainViewModel: MainDataSharingModel,
    groupedMedia: List<MediaStoreData>
) {
    if (item.mimeType == null && item.type == MediaType.Section) {
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .aspectRatio(5.5f)
                .padding(16.dp, 8.dp)
                .background(CustomMaterialTheme.colorScheme.background),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text (
                text = item.displayName ?: "This was meant to be a dated section",
                fontSize = TextUnit(16f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = CustomMaterialTheme.colorScheme.onBackground,
            )
        }
    } else {
        Box (
            modifier = Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(CustomMaterialTheme.colorScheme.primary)
                .combinedClickable(
                    onClick = {
                        mainViewModel.setSelectedMediaData(item)
						mainViewModel.setGroupedMedia(groupedMedia)
                        navController.navigate(MultiScreenViewType.SingleHiddenPhotoVew.name)
                    },

                    onDoubleClick = { /*ignore double clicks*/ },

                    onLongClick = {
                        // TODO: select item
                    }
                ),
        ) {
            GlideImage(
                model = item.uri.path,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .align(Alignment.Center),
            )

			if (item.type == MediaType.Video) {
        		Icon (
        			painter = painterResource(id = com.kaii.photos.R.drawable.play_arrow),
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
