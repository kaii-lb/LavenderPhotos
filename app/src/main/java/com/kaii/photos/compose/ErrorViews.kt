package com.kaii.photos.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.R
import com.kaii.photos.helpers.ImageFunctions
import com.kaii.photos.helpers.MediaItemSortMode

enum class ViewProperties(
    val emptyText: String,
    val emptyIconResId: Int,
    val prefix: String,
    val operation: ImageFunctions,
    val sortMode: MediaItemSortMode
) {
    Trash(
        emptyText = "Trashed items show up here",
        emptyIconResId = R.drawable.delete,
        prefix = "Trashed On ",
        operation = ImageFunctions.LoadTrashedImage,
        sortMode = MediaItemSortMode.LastModified
    ),
    Album(
        emptyText = "This album is empty",
        emptyIconResId = R.drawable.error,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage,
        sortMode = MediaItemSortMode.DateTaken
    ),
    SearchLoading(
        emptyText = "Search for some photos!",
        emptyIconResId = R.drawable.search,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage,
        sortMode = MediaItemSortMode.DateTaken
    ),
    SearchNotFound(
        emptyText = "Unable to find any matches",
        emptyIconResId = R.drawable.error,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage,
        sortMode = MediaItemSortMode.DateTaken
    ),
    SecureFolder(
        emptyText = "Add items here to secure them",
        emptyIconResId = R.drawable.locked_folder,
        prefix = "Secured On ",
        operation = ImageFunctions.LoadSecuredImage,
        sortMode = MediaItemSortMode.LastModified
    ),
    Favourites(
        emptyText = "Add your most precious memories",
        emptyIconResId = R.drawable.favourite,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage,
        sortMode = MediaItemSortMode.LastModified
    )
}

@Composable
fun FolderDoesntExist() {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .background(CustomMaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text (
            text = "This folder doesn't exist!",
            fontSize = TextUnit(18f, TextUnitType.Sp),
            modifier = Modifier
                .wrapContentSize()
        )

        Spacer (modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FolderIsEmpty(
	emptyText: String, 
	emptyIconResId: Int, 
	backgroundColor: Color = CustomMaterialTheme.colorScheme.background
) {
    Column (
        modifier = Modifier
            .fillMaxSize(1f)
            .background(backgroundColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlideImage(
            model = emptyIconResId,
            contentDescription = "folder doesn't exist icon",
            colorFilter = ColorFilter.tint(CustomMaterialTheme.colorScheme.onBackground),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(56.dp)
        )

		Spacer(modifier = Modifier.height(16.dp))

        Text (
            text = emptyText,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            color = CustomMaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
        )
    }
}
