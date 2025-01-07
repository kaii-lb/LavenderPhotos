package com.kaii.photos.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.R
import com.kaii.photos.helpers.CustomMaterialTheme

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
            .height(62.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CustomMaterialTheme.colorScheme.primary else Color.Transparent)
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
            color = if (selected) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ShowSelectedState(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    AnimatedVisibility(
        visible = showIcon,
        enter =
        scaleIn(
            animationSpec = tween(
                durationMillis = 150
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 150
            )
        ),
        exit =
        scaleOut(
            animationSpec = tween(
                durationMillis = 150
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 150
            )
        ),
        modifier = modifier
    ) {
        Box(
            modifier = modifier
                .padding(2.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isSelected) R.drawable.file_is_selected_background else R.drawable.file_not_selected_background),
                contentDescription = "file is selected indicator",
                tint =
                if (isSelected)
                    CustomMaterialTheme.colorScheme.primary
                else {
                    if (isSystemInDarkTheme()) CustomMaterialTheme.colorScheme.onBackground else CustomMaterialTheme.colorScheme.background
                },
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .align(Alignment.Center)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter =
                scaleIn(
                    animationSpec = tween(
                        durationMillis = 150
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 150
                    )
                ),
                exit =
                scaleOut(
                    animationSpec = tween(
                        durationMillis = 150
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 150
                    )
                ),
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.file_is_selected_foreground),
                    contentDescription = "file is selected indicator",
                    tint = CustomMaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

// @Composable
// fun DirectoryTreeItem(
// 	path: String,
// 	tree: SnapshotStateList<String>
// ) {
// 	val isSelected by remember { derivedStateOf {
// 		tree.contains(path)
// 	}}
//
// 	Box (
// 		modifier = Modifier
// 			.fillMaxWidth(1f)
// 			.clip(RoundedCornerShape(16.dp))
// 			.background(CustomMaterialTheme.colorScheme.surfaceContainer)
// 			.clickable {
// 				if (isSelected) {
// 					tree.remove(path)
// 				} else {
// 					tree.add(path)
// 				}
// 			}
// 	) {
// 		Text(
// 			text = path.removeSuffix("/").substringAfter("/"),
// 			fontSize = TextUnit(14f, TextUnitType.Sp),
//             color = if (isSelected) CustomMaterialTheme.colorScheme.onPrimary else CustomMaterialTheme.colorScheme.onSurface,
//             modifier = Modifier
//                 .wrapContentSize()
//                 .align(Alignment.CenterStart)
// 		)
//
// 	    ShowSelectedState(
// 	        isSelected = isSelected,
// 	        modifier = Modifier
// 	            .align(Alignment.CenterEnd)
// 	    )
// 	}
// }
