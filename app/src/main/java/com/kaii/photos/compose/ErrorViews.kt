package com.kaii.photos.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.R
import com.kaii.photos.helpers.ImageFunctions

enum class ViewProperties(
    val emptyText: String,
    val emptyIconResId: Int,
    val prefix: String,
    val operation: ImageFunctions
) {
    Trash(
        emptyText = "Trashed items show up here",
        emptyIconResId = R.drawable.delete,
        prefix = "Trashed On ",
        operation = ImageFunctions.LoadTrashedImage
    ),
    Album(
        emptyText = "This album is empty",
        emptyIconResId = R.drawable.error,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage
    ),
    SearchLoading(
        emptyText = "Search for some photos!",
        emptyIconResId = R.drawable.search,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage
    ),
    SearchNotFound(
        emptyText = "Unable to find any matches",
        emptyIconResId = R.drawable.error,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage
    ),
    SecureFolder(
        emptyText = "Add items here to secure them",
        emptyIconResId = R.drawable.locked_folder,
        prefix = "Secured On ",
        operation = ImageFunctions.LoadSecuredImage
    ),
    Favourites(
        emptyText = "Add your most precious memories",
        emptyIconResId = R.drawable.favourite,
        prefix = "",
        operation = ImageFunctions.LoadNormalImage
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FolderIsEmpty(
    emptyText: String,
    emptyIconResId: Int,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(backgroundColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlideImage(
            model = emptyIconResId,
            contentDescription = "folder doesn't exist icon",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = emptyText,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
        )
    }
}

@Composable
fun ErrorPage(
    message: String,
    @DrawableRes iconResId: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize(1f)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = "Error page icon",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(56.dp)
        )

        Spacer (modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = TextUnit(16f, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
        )
    }
}