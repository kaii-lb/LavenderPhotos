package com.kaii.photos.compose.editing_view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.R

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ColorFilterImagePreview(
    image: ImageBitmap,
    colorFilter: ColorFilter,
    name: String,
    modifier: Modifier = Modifier
) {
    GlideImage(
        model = image.asAndroidBitmap(),
        contentDescription = name,
        contentScale = ContentScale.Crop,
        failure = placeholder(R.drawable.broken_image),
        colorFilter = colorFilter,
        modifier = modifier
            .width(64.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        it.override(1024)
    }
}

@Composable
fun ColorFilterItem(
    text: String,
    image: ImageBitmap,
    colorMatrix: ColorMatrix,
    selected: Boolean = false,
    action: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                action()
            }
            .padding(4.dp, 4.dp, 4.dp, 0.dp)
    ) {
        ColorFilterImagePreview(
            image = image,
            colorFilter = ColorFilter.colorMatrix(colorMatrix),
            name = text,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )

        Text(
            text = text,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
        )
    }
}

enum class SliderStates {
    FontScaling,
    Zooming,
    SelectedTextScaling
}

