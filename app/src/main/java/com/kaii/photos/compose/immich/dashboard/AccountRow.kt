package com.kaii.photos.compose.immich.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.UpdatableProfileImage
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.domain.immich.ImmichLoginState
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountRow(
    immichInfo: () -> ImmichBasicInfo,
    userInfo: () -> ImmichLoginState,
    isLoadingInfo: () -> Boolean,
    pullToRefreshState: PullToRefreshState
) {
    val resources = LocalResources.current
    val title by remember {
        derivedStateOf {
            when (val info = userInfo()) {
                is ImmichLoginState.LoggedIn -> {
                    resources.getString(R.string.immich_login_found) + " " + info.user.name
                }

                is ImmichLoginState.LoggedOut -> {
                    resources.getString(R.string.immich_login_unavailable)
                }

                else -> {
                    resources.getString(R.string.immich_login_unreachable)
                }
            }
        }
    }

    val summary by remember {
        derivedStateOf {
            when (val info = userInfo()) {
                is ImmichLoginState.LoggedIn -> {
                    resources.getString(R.string.immich_email) + " " + info.user.email
                }

                is ImmichLoginState.LoggedOut -> {
                    resources.getString(R.string.immich_login_unavailable_desc)
                }

                else -> {
                    resources.getString(R.string.immich_login_unreachable_desc)
                }
            }
        }
    }

    val navController = LocalNavController.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.CenterVertically)
            .clickable(enabled = immichInfo().endpoint.isNotBlank() && userInfo() !is ImmichLoginState.ServerUnreachable && !isLoadingInfo()) {
                navController.navigate(
                    if (userInfo() is ImmichLoginState.LoggedIn) Screens.Immich.Account
                    else Screens.Immich.Login
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UpdatableProfileImage(
            immichInfo = immichInfo,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                textAlign = TextAlign.Start,
                color =
                    if (immichInfo().endpoint != "" && !isLoadingInfo()) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Text(
                text = summary,
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
                color =
                    if (immichInfo().endpoint != "" && !isLoadingInfo()) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        val animatedScale by animateFloatAsState(
            targetValue = if (isLoadingInfo()) 1f else pullToRefreshState.distanceFraction
        )

        AnimatedVisibility(
            visible = isLoadingInfo() || pullToRefreshState.distanceFraction > 0f,
            enter = fadeIn() + scaleIn(animationSpec = AnimationConstants.expressiveTween()),
            exit = fadeOut() + scaleOut(animationSpec = AnimationConstants.expressiveTween()),
            modifier = Modifier
                .scale(animatedScale)
        ) {
            ContainedLoadingIndicator(
                modifier = Modifier
                    .size(32.dp)
            )
        }
    }
}