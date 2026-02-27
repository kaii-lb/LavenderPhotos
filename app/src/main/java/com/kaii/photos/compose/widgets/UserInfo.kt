package com.kaii.photos.compose.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.kaii.lavender.immichintegration.state_managers.LoginState
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.Screens
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.profilePicture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MainDialogUserInfo(
    loginState: LoginState,
    coroutineScope: CoroutineScope,
    dismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val mainViewModel = LocalMainViewModel.current
    val alwaysShowInfo by mainViewModel.settings.immich.getAlwaysShowUserInfo().collectAsStateWithLifecycle(initialValue = false)
    val immichInfo by mainViewModel.settings.immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)

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
            loggedIn = alwaysShowInfo || loginState !is LoginState.LoggedOut,
            pfpUrl = if (alwaysShowInfo && loginState is LoginState.LoggedOut) context.profilePicture else (loginState as? LoginState.LoggedIn)?.pfpUrl ?: "",
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(enabled = alwaysShowInfo || loginState !is LoginState.LoggedOut) {
                    coroutineScope.launch {
                        dismiss()
                        delay(AnimationConstants.DURATION_SHORT.toLong())
                        navController.navigate(Screens.Immich.InfoPage)
                    }
                }
        )

        val originalName = retain(immichInfo, loginState) {
            when (loginState) {
                is LoginState.LoggedIn -> {
                    loginState.name
                }

                is LoginState.ServerUnreachable -> {
                    immichInfo.username.ifBlank {
                        resources.getString(R.string.immich_login_unavailable)
                    }
                }

                else -> {
                    if (alwaysShowInfo) immichInfo.username
                    else resources.getString(R.string.immich_login_unavailable)
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