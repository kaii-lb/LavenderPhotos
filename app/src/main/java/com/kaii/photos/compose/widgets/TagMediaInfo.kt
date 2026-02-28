package com.kaii.photos.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kaii.photos.R
import com.kaii.photos.helpers.TextStylingConstants

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun TagMediaInfo(
    mediaUri: String,
    mediaName: String,
    mediaPath: String,
    mediaType: String,
    mediaFavourited: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .clip(shape = RoundedCornerShape(24.dp))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(all = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        GlideImage(
            model = mediaUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(shape = RoundedCornerShape(size = 16.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.tags_name, mediaName),
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
            )

            Text(
                text = stringResource(id = R.string.tags_type, mediaType),
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
            )

            Text(
                text = stringResource(id = R.string.tags_path, mediaPath),
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
            )

            Text(
                text = stringResource(id = if (mediaFavourited) R.string.tags_favourited_true else R.string.tags_favourited_false),
                fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
            )
        }
    }
}