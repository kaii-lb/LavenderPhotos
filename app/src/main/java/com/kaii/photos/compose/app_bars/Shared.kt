package com.kaii.photos.compose.app_bars

import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat

/** please only use dialogComposable for its intended purpose */
@Composable
fun BottomAppBarItem(
    text: String,
    iconResId: Int,
    modifier: Modifier = Modifier,
    buttonWidth: Dp = 64.dp,
    buttonHeight: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    textSize: Float = 14f,
    color: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    showRipple: Boolean = true,
    cornerRadius: Dp = 1000.dp,
    action: (() -> Unit)? = null,
    dialogComposable: (@Composable () -> Unit)? = null
) {
    val clickModifier = if (action != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = if (!showRipple) null else LocalIndication.current
        ) {
            action()
        }
    } else {
        Modifier
    }

    if (dialogComposable != null) dialogComposable()

    Box(
        modifier = Modifier
            .width(buttonWidth)
            .height(buttonHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .then(clickModifier)
            .then(modifier),
    ) {
        Row(
            modifier = Modifier
                .height(iconSize + 8.dp)
                .width(iconSize * 2.25f)
                .clip(RoundedCornerShape(1000.dp))
                .align(Alignment.TopCenter)
                .background(color),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "button",
                tint = contentColor,
                modifier = Modifier
                    .size(iconSize)
            )
        }

        Text(
            text = text,
            fontSize = TextUnit(textSize, TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
        )
    }
}

fun getAppBarContentTransition(slideLeft: Boolean) = run {
    if (slideLeft) {
        (slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            )
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) { width -> -width } + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                )
            )
        )
    } else {
        (slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350
            )
        ) { width -> -width } + fadeIn(
            animationSpec = tween(
                durationMillis = 350
            )
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 350
                )
            ) { width -> width } + fadeOut(
                animationSpec = tween(
                    durationMillis = 350
                )
            )
        )
    }
}



fun setBarVisibility(
    visible: Boolean,
    window: Window,
    onSetBarVisible: (isVisible: Boolean) -> Unit
) {
    onSetBarVisible(visible)

    window.insetsController?.apply {
        if (visible) {
            show(WindowInsetsCompat.Type.systemBars())
        } else {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // window.setDecorFitsSystemWindows(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualFunctionTopAppBar(
    alternated: Boolean,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    alternateTitle: @Composable () -> Unit,
    alternateActions: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = @Composable {}
) {
    TopAppBarDefaults.topAppBarColors()
    TopAppBar(
        navigationIcon = navigationIcon,
        title = {
            AnimatedContent(
                targetState = alternated,
                transitionSpec = {
                    if (alternated) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "Dual Function App Bar Animation"
            ) { alternate ->
                if (alternate) {
                    alternateTitle()
                } else {
                    title()
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState = alternated,
                transitionSpec = {
                    if (alternated) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically { height -> height } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "Dual Function App Bar Animation"
            ) { alternate ->
                if (alternate) {
                    alternateActions()
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        actions()
                    }
                }
            }
        },
    )
}