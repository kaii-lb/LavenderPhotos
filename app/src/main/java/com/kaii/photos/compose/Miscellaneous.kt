package com.kaii.photos.compose

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme
import com.kaii.photos.mediastore.signature
import kotlinx.coroutines.launch

@Composable
fun SplitButton(
    enabled: Boolean = true,
    secondaryContentMaxWidth: Dp = 1000.dp,
    primaryContentPadding: PaddingValues = PaddingValues(11.dp),
    secondaryContentPadding: PaddingValues = PaddingValues(0.dp, 5.dp, 4.dp, 5.dp),
    primaryContainerColor: Color = CustomMaterialTheme.colorScheme.primary,
    secondaryContainerColor: Color = CustomMaterialTheme.colorScheme.primary,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable () -> Unit,
    primaryAction: () -> Unit,
    secondaryAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                primaryAction()
            },
            shape = RoundedCornerShape(1000.dp, 4.dp, 4.dp, 1000.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = primaryContainerColor
            ),
            contentPadding = primaryContentPadding,
            modifier = Modifier
            	.widthIn(min = 40.dp)
        ) {
            primaryContent()
        }

        Spacer(modifier = Modifier.width(4.dp))

        Button(
            onClick = secondaryAction,
            shape = RoundedCornerShape(4.dp, 1000.dp, 1000.dp, 4.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = secondaryContainerColor
            ),
            contentPadding = secondaryContentPadding,
            modifier = Modifier
                .widthIn(min = 20.dp, max = secondaryContentMaxWidth)
                .wrapContentSize()
                .animateContentSize()
        ) {
            secondaryContent()
        }
    }
}

@Composable
fun SelectableDropDownMenuItem(
    text: String,
    iconResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontSize = TextUnit(14f, TextUnitType.Sp),
            )
        },
        onClick = onClick,
        trailingIcon = {
            if (isSelected) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = "This save option is selected",
                    tint = CustomMaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    )
}

@Composable
fun SimpleTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .height(40.dp)
            .zIndex(2f)
            .clip(RoundedCornerShape(100.dp))
    ) {
        Text(
            text = text,
            color = if (selected) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface,
            fontSize = TextUnit(14f, TextUnitType.Sp)
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ColorFilterImagePreview(
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
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CustomMaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                action()
            }
            .padding(4.dp, 2.dp)
    ) {
        ColorFilterImagePreview(
        	image = image,
        	colorFilter = ColorFilter.colorMatrix(colorMatrix),
        	name = text,
        	modifier = Modifier
        		.align(Alignment.TopCenter)
       	)

		val localDensity = LocalDensity.current
        Text(
            text = text,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            color = if (selected) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
                .offset {
                	with(localDensity) {
	                	IntOffset(
	                		0,
	                		4.dp.toPx().toInt()
	                	)
                	}
                }
        )
    }
}
