package com.kaii.photos.compose

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.R
import com.kaii.photos.MainActivity.Companion.mainViewModel
import com.kaii.photos.compose.dialogs.SelectingMoreOptionsDialog
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType

// private const val TAG = "MISCELLANEOUS"

@Composable
fun SplitButton(
    enabled: Boolean = true,
    secondaryContentMaxWidth: Dp = 1000.dp,
    primaryContentPadding: PaddingValues = PaddingValues(11.dp),
    secondaryContentPadding: PaddingValues = PaddingValues(0.dp, 5.dp, 4.dp, 5.dp),
    primaryContainerColor: Color = MaterialTheme.colorScheme.primary,
    secondaryContainerColor: Color = MaterialTheme.colorScheme.primary,
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
                    tint = MaterialTheme.colorScheme.onSurface,
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
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                    MaterialTheme.colorScheme.primary
                else {
                    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
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
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun SelectViewTopBarLeftButtons(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
) {
    SplitButton(
        primaryContentPadding = PaddingValues(16.dp, 0.dp, 12.dp, 0.dp),
        secondaryContentPadding = PaddingValues(8.dp, 8.dp, 12.dp, 8.dp),
        secondaryContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        primaryContent = {
            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = "clear selection button",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(24.dp)
            )
        },
        secondaryContent = {
            Text(
                text = selectedItemsList.filter { it.type != MediaType.Section && it != MediaStoreData.dummyItem }.size.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = TextUnit(18f, TextUnitType.Sp),
                modifier = Modifier
                    .wrapContentSize()
                    .animateContentSize()
            )
        },
        primaryAction = {
            selectedItemsList.clear()
        },
        secondaryAction = {
            selectedItemsList.clear()
        }
    )
}

@Composable
fun SelectViewTopBarRightButtons(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>
) {
    val groupedMedia = mainViewModel.groupedMedia.collectAsStateWithLifecycle(initialValue = emptyList())

    val selectedItemsWithoutSection by remember {
        derivedStateOf {
            selectedItemsList.filter {
                it.type != MediaType.Section && it != MediaStoreData()
            }
        }
    }

    Row(
        modifier = Modifier
            .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val allItemsList by remember { derivedStateOf { groupedMedia.value ?: emptyList() } }
        val isTicked by remember {
            derivedStateOf {
                selectedItemsList.size == allItemsList.size
            }
        }

        IconButton(
            onClick = {
                if (groupedMedia.value != null) {
                    if (isTicked) {
                        selectedItemsList.clear()
                        selectedItemsList.add(MediaStoreData())
                    } else {
                        selectedItemsList.clear()

                        selectedItemsList.addAll(allItemsList)
                    }
                }
            },
            modifier = Modifier
                .clip(RoundedCornerShape(1000.dp))
                .size(42.dp)
                .background(if (isTicked) MaterialTheme.colorScheme.primary else Color.Transparent)
        ) {
            Icon(
                painter = painterResource(R.drawable.checklist),
                contentDescription = "select all items",
                tint = if (isTicked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(24.dp)
            )
        }

        if (currentView.value != DefaultTabs.TabTypes.secure) {
            val showMoreOptionsDialog = remember { mutableStateOf(false) }

            if (showMoreOptionsDialog.value) {
                SelectingMoreOptionsDialog(
                    showDialog = showMoreOptionsDialog,
                    selectedItems = selectedItemsWithoutSection,
                    currentView = currentView
                ) {
                    selectedItemsList.clear()
                }
            }

            IconButton(
                onClick = {
                    showMoreOptionsDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_options),
                    contentDescription = "show more options for selected items",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    }
}

/** return true if the device is in landscape mode, false otherwise */
@Composable
fun rememberDeviceOrientation(): MutableState<Boolean> {
    val localConfig = LocalConfiguration.current
    val isLandscape = remember { mutableStateOf(localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    LaunchedEffect(localConfig) {
        isLandscape.value = localConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    return isLandscape
}

@Composable
fun HorizontalSeparator() {
    Box (
        modifier = Modifier
            .height(1.dp)
            .padding(16.dp, 0.dp)
            .fillMaxWidth(1f)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(1000.dp))
    )
}

@Composable
fun ConfirmCancelRow(
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .height(56.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (onCancel != null) {
            FilledTonalButton(
                onClick = {
                    onCancel()
                },
            ) {
                Text(
                    text = "Cancel",
                    fontSize = TextUnit(14f, TextUnitType.Sp)
                )
            }
        }

        Spacer (modifier = Modifier.width(8.dp))

        FilledTonalButton( // maybe use normal button for it?
            onClick = {
                onConfirm()
            }
        ) {
            Text(text = "Confirm")
        }
    }
}

@Composable
fun TitleCloseRow(
	title: String,
	onClose: () -> Unit
) {
	Box (
	    modifier = Modifier
	        .fillMaxWidth(1f)
	        .padding(8.dp, 0.dp),
	) {
	    Text(
	        text = title,
	        fontSize = TextUnit(18f, TextUnitType.Sp),
	        color = MaterialTheme.colorScheme.onSurface,
	        modifier = Modifier
	            .wrapContentSize()
	            .align(Alignment.Center)
	    )


	    IconButton(
	        onClick = {
	            onClose()
	        },
	        modifier = Modifier
	        	.align(Alignment.CenterEnd)
	    ) {
	        Icon(
	            painter = painterResource(id = R.drawable.close),
	            contentDescription = "Close this dialog",
	            tint = MaterialTheme.colorScheme.onSurface
	        )
	    }
	}
}
