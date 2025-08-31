package com.kaii.photos.compose.widgets

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.immich.ImmichUserLoginState
import com.kaii.photos.mediastore.MediaStoreData
import com.kaii.photos.mediastore.MediaType
import java.io.File

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
                    painter = painterResource(id = R.drawable.checkmark_thin),
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
) {
    val mainViewModel = LocalMainViewModel.current
    val groupedMedia = mainViewModel.groupedMedia.collectAsStateWithLifecycle(initialValue = emptyList())

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

        val isSelectAllEnabled by remember {
            derivedStateOf {
                groupedMedia.value?.size?.let { it < 5000 } == true
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
            enabled = isSelectAllEnabled, // arbitrary limit since android can't handle infinitely many uris
            modifier = Modifier
                .clip(RoundedCornerShape(1000.dp))
                .size(42.dp)
                .background(if (isTicked) MaterialTheme.colorScheme.primary else Color.Transparent)
        ) {
            Icon(
                painter = painterResource(R.drawable.checklist),
                contentDescription = "select all items",
                tint = when {
                    isSelectAllEnabled && isTicked -> {
                        MaterialTheme.colorScheme.onPrimary
                    }

                    isSelectAllEnabled -> {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    else -> {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    }
                },
                modifier = Modifier
                    .size(24.dp)
            )
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
    Box(
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
                    text = stringResource(id = R.string.media_cancel),
                    fontSize = TextUnit(14f, TextUnitType.Sp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledTonalButton( // maybe use normal button for it?
            onClick = {
                onConfirm()
            }
        ) {
            Text(text = stringResource(id = R.string.media_confirm))
        }
    }
}

@Composable
fun TitleCloseRow(
    title: String,
    closeOffset: Dp = 0.dp,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(8.dp, 0.dp)
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
                .offset(x = closeOffset)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = "Close this dialog",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun InvalidatableGlideImage(
    path: Any?,
    signature: ObjectKey,
    modifier: Modifier = Modifier
) {
    GlideImage(
        model = path,
        contentDescription = "User profile picture",
        contentScale = ContentScale.Crop,
        modifier = modifier
    ) {
        it
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .signature(signature)
    }
}

@Composable
fun AnimatedImmichBackupIcon(
    immichUserState: ImmichUserLoginState,
    modifier: Modifier = Modifier
) {
    val pfpPath by remember {
        derivedStateOf {
            if (immichUserState is ImmichUserLoginState.IsLoggedIn) {
                val file = File(immichUserState.info.profileImagePath)
                if (file.exists()) file.absolutePath
                else R.drawable.cat_picture
            } else R.drawable.cat_picture
        }
    }
    val pfpSignature by remember {
        derivedStateOf {
            if (pfpPath is String) {
                ObjectKey(File(pfpPath as String).lastModified())
            } else ObjectKey(0)
        }
    }
    val immichUploadCount by immichViewModel.immichUploadedMediaCount.collectAsStateWithLifecycle()
    val immichUploadTotal by immichViewModel.immichUploadedMediaTotal.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        val size by animateDpAsState(
            targetValue = if (immichUploadTotal != 0) 22.dp else 28.dp,
            animationSpec = tween(
                durationMillis = 400
            )
        )
        InvalidatableGlideImage(
            path = pfpPath,
            signature = pfpSignature,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .zIndex(2f)
        )

        AnimatedVisibility(
            visible = immichUploadTotal != 0,
            enter = scaleIn(
                animationSpec = tween(
                    durationMillis = 400
                )
            ),
            exit = scaleOut(
                animationSpec = tween(
                    durationMillis = 400
                )
            ),
            modifier = Modifier
                .zIndex(1f)
        ) {
            val percentage by animateFloatAsState(
                targetValue = immichUploadCount.toFloat() / (if (immichUploadTotal == 0) 1 else immichUploadTotal),
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
            )

            CircularProgressIndicator(
                progress = {
                    percentage
                },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(32.dp)
            )
        }
    }
}

@Composable
fun AnimatedLoginIcon(
    immichUserLoginState: ImmichUserLoginState,
    onClick: () -> Unit
) {
    AnimatedContent(
        targetState = immichUserLoginState is ImmichUserLoginState.IsLoggedIn,
        transitionSpec = {
            (scaleIn(
                animationSpec = AnimationConstants.expressiveSpring()
            ) + fadeIn()).togetherWith(
                scaleOut(
                    animationSpec = AnimationConstants.expressiveSpring()
                ) + fadeOut()
            ).using(SizeTransform(clip = false))
        },
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(end = 4.dp)
    ) { state ->
        if (state) {
            AnimatedImmichBackupIcon(
                immichUserState = immichUserLoginState,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .clickable {
                        onClick()
                    }
            )
        } else {
            IconButton(
                onClick = {
                    onClick()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = stringResource(id = R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}