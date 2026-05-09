package com.kaii.photos.compose.immich.backup_options_page

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaii.photos.R
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarController
import io.github.kaii_lb.lavender.snackbars.LavenderSnackbarEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopBar(
    query: () -> String,
    isRefreshing: () -> Boolean,
    pullDistance: () -> Float,
    navController: NavController,
    onQueryChange: (query: String) -> Unit,
    confirm: suspend () -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = stringResource(id = R.string.return_to_previous_page),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        title = {
            TextField(
                value = query(),
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.albums_search_for),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null
                    )
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    errorIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .clip(CircleShape)
            )
        },
        actions = {
            val resources = LocalResources.current

            AnimatedContent(
                targetState = pullDistance() > 0 || isRefreshing(),
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) { state ->
                if (state) {
                    ContainedLoadingIndicator(
                        modifier = Modifier
                            .size(48.dp * pullDistance().coerceAtMost(1.25f))
                            .sizeIn(minWidth = 16.dp, maxWidth = 56.dp)
                    )
                } else {
                    FilledTonalIconButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = confirm()
                                LavenderSnackbarController.pushEvent(
                                    event = LavenderSnackbarEvent.MessageEvent(
                                        message =
                                            if (success) resources.getString(R.string.immich_backup_option_changes_synced)
                                            else resources.getString(R.string.immich_login_unreachable),
                                        icon =
                                            if (success) R.drawable.image_arrow_up
                                            else R.drawable.error_2,
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.checkmark_thin),
                            contentDescription = stringResource(id = R.string.apply),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .padding(top = 8.dp)
    )
}