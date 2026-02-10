package com.kaii.photos.compose.app_bars

import android.view.Window
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.helpers.AnimationConstants

/** please only use dialogComposable for its intended purpose */
@Composable
fun BottomAppBarItem(
    text: String,
    iconResId: Int,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    textSize: Float = 14f,
    enabled: Boolean = true,
    showRipple: Boolean = true,
    cornerRadius: Dp = 1000.dp,
    color: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    action: (() -> Unit)? = null,
    dialogComposable: @Composable (() -> Unit)? = null
) {
    val clickModifier = if (action != null && enabled) {
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
            .wrapContentWidth()
            .padding(horizontal = 8.dp)
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
                .background(if (enabled || color == Color.Transparent) color else color.copy(alpha = 0.6f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "button",
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(iconSize)
            )
        }

        Text(
            text = text,
            fontSize = TextUnit(textSize, TextUnitType.Sp),
            color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
        )
    }
}

fun getAppBarContentTransition(slideLeft: Boolean) = run {
    if (slideLeft) {
        (slideInHorizontally(
            animationSpec = AnimationConstants.expressiveSpring()
        ) { width -> width } + fadeIn(
            animationSpec = AnimationConstants.expressiveSpring()
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = AnimationConstants.expressiveSpring()
            ) { width -> -width } + fadeOut(
                animationSpec = AnimationConstants.expressiveSpring()
            )
        )
    } else {
        (slideInHorizontally(
            animationSpec = AnimationConstants.expressiveSpring()
        ) { width -> -width } + fadeIn(
            animationSpec = AnimationConstants.expressiveSpring()
        )).togetherWith(
            slideOutHorizontally(
                animationSpec = AnimationConstants.expressiveSpring()
            ) { width -> width } + fadeOut(
                animationSpec = AnimationConstants.expressiveSpring()
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
    val mainViewModel = LocalMainViewModel.current
    val isAmoled by mainViewModel.settings.lookAndFeel.getFollowDarkMode().collectAsStateWithLifecycle(initialValue = null)
    if (isAmoled == null) return

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = if (isAmoled == 3) {
                Color.Black
            } else {
                TopAppBarDefaults.topAppBarColors().containerColor
            }
        ),
        navigationIcon = navigationIcon,
        title = {
            AnimatedContent(
                targetState = alternated,
                transitionSpec = {
                    if (alternated) {
                        (slideInVertically(
                            animationSpec = AnimationConstants.expressiveSpring()
                        ) { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically(
                                animationSpec = AnimationConstants.expressiveSpring()
                            ) { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically(
                            animationSpec = AnimationConstants.expressiveSpring()
                        ) { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically(
                                animationSpec = AnimationConstants.expressiveSpring()
                            ) { height -> height } + fadeOut())
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
                        (slideInVertically(
                            animationSpec = AnimationConstants.expressiveSpring()
                        ) { height -> height } + fadeIn()).togetherWith(
                            slideOutVertically(
                                animationSpec = AnimationConstants.expressiveSpring()
                            ) { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically(
                            animationSpec = AnimationConstants.expressiveSpring()
                        ) { height -> -height } + fadeIn()).togetherWith(
                            slideOutVertically(
                                animationSpec = AnimationConstants.expressiveSpring()
                            ) { height -> height } + fadeOut())
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

fun ComponentActivity.lavenderEdgeToEdge(
    isDarkMode: Boolean,
    navBarColor: Color,
    statusBarColor: Color
) {
    enableEdgeToEdge(
        navigationBarStyle =
            if (isDarkMode) SystemBarStyle.dark(navBarColor.toArgb())
            else SystemBarStyle.light(navBarColor.toArgb(), navBarColor.toArgb()),
        statusBarStyle =
            if (isDarkMode) SystemBarStyle.dark(statusBarColor.toArgb())
            else SystemBarStyle.light(statusBarColor.toArgb(), statusBarColor.toArgb())
    )
}
