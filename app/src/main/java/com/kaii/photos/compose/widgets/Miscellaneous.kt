package com.kaii.photos.compose.widgets

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.kaii.lavender.immichintegration.state_managers.LoginState
import com.kaii.photos.R
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.profilePicture
import com.kaii.photos.mediastore.PhotoLibraryUIModel
import java.io.File

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
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
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
                text = selectedItemsList.filter { it !is PhotoLibraryUIModel.Section && (it as PhotoLibraryUIModel.Media).item != MediaStoreData.dummyItem }.size.toString(),
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
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    mediaCount: State<Int>,
    sectionCount: State<Int>,
    getAllMedia: () -> List<MediaStoreData>
) {
    Row(
        modifier = Modifier
            .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val isTicked by remember {
            derivedStateOf {
                selectedItemsList.size == mediaCount.value + sectionCount.value
            }
        }
        val selectAllEnabled by remember {
            derivedStateOf {
                mediaCount.value < 2000
            }
        }

        IconButton(
            onClick = {
                if (isTicked) {
                    selectedItemsList.clear()
                    selectedItemsList.add(PhotoLibraryUIModel.Media(item = MediaStoreData.dummyItem))
                } else {
                    selectedItemsList.clear()

                    selectedItemsList.addAll(getAllMedia().map { PhotoLibraryUIModel.Media(it) })
                }
            },
            enabled = selectAllEnabled, // limit of 2000 since android can't handle more uris
            modifier = Modifier
                .clip(RoundedCornerShape(1000.dp))
                .size(42.dp)
                .background(if (isTicked) MaterialTheme.colorScheme.primary else Color.Transparent)
        ) {
            Icon(
                painter = painterResource(R.drawable.checklist),
                contentDescription = "select all items",
                tint = when {
                    selectAllEnabled && isTicked -> {
                        MaterialTheme.colorScheme.onPrimary
                    }

                    selectAllEnabled -> {
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UpdatableProfileImage(
    loggedIn: Boolean,
    pfpUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pfpPath = remember(loggedIn) {
        if (loggedIn) {
            val file = File(context.profilePicture)

            if (file.exists()) file.absolutePath else R.drawable.account_circle
        } else R.drawable.account_circle
    }

    GlideImage(
        model = pfpPath,
        contentDescription = "User profile picture",
        contentScale = ContentScale.Crop,
        modifier = modifier
    ) {
        it
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .signature(ObjectKey(if (pfpPath is String) pfpUrl else 0))
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AnimatedImmichBackupIcon(
    state: LoginState,
    modifier: Modifier = Modifier
) {
    // TODO:
    // val immichUploadCount by immichViewModel.immichUploadedMediaCount.collectAsStateWithLifecycle()
    // val immichUploadTotal by immichViewModel.immichUploadedMediaTotal.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // val size by animateDpAsState(
        //     targetValue = if (immichUploadTotal != 0) 22.dp else 28.dp,
        //     animationSpec = tween(
        //         durationMillis = 400
        //     )
        // )

        UpdatableProfileImage(
            loggedIn = state is LoginState.LoggedIn,
            pfpUrl = (state as? LoginState.LoggedIn)?.pfpUrl ?: "",
            modifier = Modifier
                .size(28.dp) // TODO
                .clip(CircleShape)
                .zIndex(2f)
        )

        AnimatedVisibility(
            visible = false, // immichUploadTotal != 0,
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
            // val percentage by animateFloatAsState(
            //     targetValue = immichUploadCount.toFloat() / (if (immichUploadTotal == 0) 1 else immichUploadTotal),
            //     animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
            // )

            // CircularProgressIndicator(
            //     progress = {
            //         percentage
            //     },
            //     color = MaterialTheme.colorScheme.primary,
            //     trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            //     strokeCap = StrokeCap.Round,
            //     strokeWidth = 3.dp,
            //     modifier = Modifier
            //         .size(32.dp)
            // )
        }
    }
}

@Composable
fun AnimatedLoginIcon(
    state: LoginState,
    onClick: () -> Unit
) {
    AnimatedContent(
        targetState = state is LoginState.LoggedIn && state.pfpUrl.isNotBlank(),
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
    ) { visible ->
        if (visible) {
            // TODO
            AnimatedImmichBackupIcon(
                state = state,
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

fun Modifier.shimmerEffect(
    containerColor: Color = Color.DarkGray,
    highlightColor: Color = Color.Gray,
    durationMillis: Int = AnimationConstants.DURATION_EXTRA_LONG,
    delayMillis: Int = 0
) = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition()
    val startOffset by transition.animateFloat(
        initialValue = -3f * size.width,
        targetValue = 3f * size.width,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis
            )
        )
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                containerColor,
                highlightColor,
                containerColor
            ),
            start = Offset(startOffset, 0f),
            end = Offset(startOffset + size.width, size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}