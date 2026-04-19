package com.kaii.photos.compose.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.profilePicture
import com.kaii.photos.mediastore.ImmichInfo
import io.github.kaii_lb.lavender.immichintegration.state_managers.LoginState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Instant

@Composable
fun MainDialogUserInfo(
    loginState: LoginState,
    coroutineScope: CoroutineScope,
    immichInfo: () -> ImmichBasicInfo,
    dismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val navController = LocalNavController.current
        UpdatableProfileImage(
            userInfo = { loginState },
            immichInfo = immichInfo,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(enabled = immichInfo().accessToken.isNotBlank() || loginState !is LoginState.LoggedOut) {
                    coroutineScope.launch {
                        dismiss()
                        delay(AnimationConstants.DURATION_SHORT.toLong())
                        navController.navigate(Screens.Immich.Dashboard)
                    }
                },
        )

        val resources = LocalResources.current
        val originalName = retain(immichInfo(), loginState) {
            when (loginState) {
                is LoginState.LoggedIn -> {
                    loginState.name
                }

                else -> {
                    immichInfo().username.ifBlank {
                        resources.getString(R.string.immich_login_unavailable)
                    }
                }
            }
        }

        Text(
            text = stringResource(id = R.string.main_dialog_greeting, originalName),
            fontSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE.sp
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UpdatableProfileImage(
    userInfo: () -> LoginState,
    immichInfo: () -> ImmichBasicInfo,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    if (userInfo() is LoginState.LoggedIn || immichInfo().accessToken.isNotBlank()) {
        val context = LocalContext.current
        val epoch = File(context.profilePicture).lastModified() // TODO: remove when using build in [updatedAt]
        val actualPfpUrl = remember(immichInfo(), userInfo(), epoch) {
            val info = userInfo() as? LoginState.LoggedIn
            val formatted = Instant.fromEpochMilliseconds(epoch).toString()

            immichInfo().endpoint + "/api/users/${info?.userId ?: immichInfo().userId}/profile-image?updatedAt=${formatted}"
        }

        GlideImage(
            model = ImmichInfo(
                thumbnail = actualPfpUrl,
                original = actualPfpUrl,
                hash = "",
                accessToken = immichInfo().accessToken,
                useThumbnail = false
            ),
            contentDescription = "User profile picture",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(CircleShape)
        ) {
            it
                .signature(ObjectKey(actualPfpUrl))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(context.profilePicture) // TODO: probably remove this
        }
    } else {
        Icon(
            painter = painterResource(id = R.drawable.account_circle),
            contentDescription = "User's Immich profile picture",
            tint = tint,
            modifier = modifier
                .clip(CircleShape)
        )
    }
}