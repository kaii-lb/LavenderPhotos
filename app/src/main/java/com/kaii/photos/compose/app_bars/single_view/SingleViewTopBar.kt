package com.kaii.photos.compose.app_bars.single_view

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.database.entities.MediaStoreData
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.TopBarDetailsFormat
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalTime::class)
@Composable
fun SingleViewTopBar(
    mediaItem: () -> MediaStoreData,
    visible: Boolean,
    showInfoDialog: Boolean,
    privacyMode: Boolean,
    topBarDetailsFormat: TopBarDetailsFormat,
    isOpenWithDefaultView: Boolean,
    showTags: Boolean,
    showTagDialog: Boolean = false,
    expandInfoDialog: () -> Unit,
    expandTagDialog: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current
    BackHandler(
        enabled = privacyMode
    ) {
        coroutineScope.launch {
            LavenderSnackbarController.pushEvent(
                LavenderSnackbarEvents.MessageEvent(
                    message = resources.getString(R.string.privacy_scroll_mode_exit_tried),
                    icon = R.drawable.do_not_touch,
                    duration = SnackbarDuration.Short
                )
            )
        }
    }

    Row(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(4.dp, 0.dp)
            .wrapContentHeight()
            .fillMaxWidth(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.Start
        )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter =
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
            exit =
                scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(),
            modifier = Modifier
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.Start
                )
            ) {
                val navController = LocalNavController.current
                val context = LocalContext.current

                FilledIconButton(
                    onClick = {
                        if (isOpenWithDefaultView) {
                            (context as Activity).finish()
                        } else {
                            navController.popBackStack()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shapes = IconButtonDefaults.shapes(
                        shape = IconButtonDefaults.filledShape,
                        pressedShape = IconButtonDefaults.mediumPressedShape
                    ),
                    enabled = !privacyMode
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = stringResource(id = R.string.return_to_previous_page)
                    )
                }

                Text(
                    text = topBarDetailsFormat.format(context, mediaItem().displayName, mediaItem().dateTaken),
                    fontSize = TextStylingConstants.SMALL_TEXT_SIZE.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            expandInfoDialog()
                        }
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter =
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
            exit =
                scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (showTags) {
                    FilledIconToggleButton(
                        checked = showTagDialog,
                        onCheckedChange = {
                            expandTagDialog()
                        },
                        shapes = IconButtonDefaults.toggleableShapes(
                            shape = IconButtonDefaults.filledShape,
                            pressedShape = IconButtonDefaults.extraSmallPressedShape,
                            checkedShape = IconButtonDefaults.mediumSelectedRoundShape
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.sell),
                            contentDescription = stringResource(id = R.string.tags)
                        )
                    }
                }

                FilledIconToggleButton(
                    checked = showInfoDialog,
                    onCheckedChange = {
                        expandInfoDialog()
                    },
                    shapes = IconButtonDefaults.toggleableShapes(
                        shape = IconButtonDefaults.filledShape,
                        pressedShape = IconButtonDefaults.extraSmallPressedShape,
                        checkedShape = IconButtonDefaults.mediumSelectedRoundShape
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.more_options),
                        contentDescription = stringResource(id = R.string.show_options)
                    )
                }
            }
        }
    }
}