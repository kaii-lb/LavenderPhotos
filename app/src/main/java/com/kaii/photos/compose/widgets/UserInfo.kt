package com.kaii.photos.compose.widgets

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import com.kaii.photos.mediastore.ImmichInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainDialogUserInfo(
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
            immichInfo = immichInfo,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(enabled = immichInfo().username.isNotBlank()) {
                    coroutineScope.launch {
                        dismiss()
                        delay(AnimationConstants.DURATION_SHORT.toLong())
                        navController.navigate(Screens.Immich.Dashboard)
                    }
                }
        )

        val resources = LocalResources.current
        Text(
            text = stringResource(id = R.string.main_dialog_greeting, immichInfo().username.ifBlank {
                resources.getString(R.string.immich_login_unavailable)
            }),
            fontSize = TextStylingConstants.EXTRA_LARGE_TEXT_SIZE.sp
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UpdatableProfileImage(
    immichInfo: () -> ImmichBasicInfo,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = immichInfo().username.isNotBlank(),
        modifier = modifier
            .clip(CircleShape)
    ) { state ->
        if (state) {
            val actualPfpUrl =
                "/api/users/${immichInfo().userId}" +
                        "/profile-image?updatedAt=${immichInfo().updatedAt}"

            GlideImage(
                model = ImmichInfo(
                    thumbnail = actualPfpUrl,
                    original = actualPfpUrl,
                    hash = "",
                    auth = immichInfo().auth,
                    endpoint = immichInfo().endpoint,
                    useThumbnail = false
                ),
                contentDescription = "User profile picture",
                contentScale = ContentScale.Crop
            ) {
                it
                    .signature(ObjectKey(actualPfpUrl))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.account_circle)
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.account_circle),
                contentDescription = "User's Immich profile picture",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}