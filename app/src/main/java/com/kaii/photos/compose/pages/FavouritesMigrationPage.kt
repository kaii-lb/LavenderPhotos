package com.kaii.photos.compose.pages

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.ExplanationDialog
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.TextStylingConstants
import com.kaii.photos.helpers.permissions.favourites.MigrationState
import com.kaii.photos.helpers.permissions.favourites.rememberFavouritesMigrationState

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavouritesMigrationPage() {
    val migrationState = rememberFavouritesMigrationState()
    val state by migrationState.state.collectAsStateWithLifecycle()

    var showDialog by remember(state) { mutableStateOf(state == MigrationState.Declined) }
    if (showDialog) {
        ExplanationDialog(
            title = stringResource(id = R.string.favourites_migration_declined),
            explanation = stringResource(id = R.string.favourites_migration_declined_desc)
        ) {
            showDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    val navController = LocalNavController.current

                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_arrow),
                            contentDescription = stringResource(id = R.string.return_to_previous_page),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                title = {}
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(
                    space = 12.dp,
                    alignment = Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.favourite_filled),
                    contentDescription = stringResource(id = R.string.favourites_migration),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(96.dp)
                )

                Text(
                    text = stringResource(id = R.string.favourites_migration_notice),
                    fontSize = TextStylingConstants.LARGE_TEXT_SIZE.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 24.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.favourites_migration_desc),
                        fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 12.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    modifier = Modifier
                        .padding(top = 64.dp)
                        .animateContentSize(
                            animationSpec = AnimationConstants.expressiveTween(
                                durationMillis = AnimationConstants.DURATION
                            )
                        )
                ) {
                    Button(
                        onClick = {
                            migrationState.step()
                        },
                        enabled = state == MigrationState.NeedsPermission || state == MigrationState.Done
                    ) {
                        Text(
                            text = stringResource(
                                id =
                                    when (state) {
                                        MigrationState.NeedsPermission -> R.string.permissions_grant_long
                                        MigrationState.InProgress -> R.string.favourites_migration_in_progress_short
                                        else -> R.string.permissions_continue
                                    }
                            ),
                            fontSize = TextStylingConstants.MEDIUM_TEXT_SIZE.sp
                        )
                    }

                    if (state == MigrationState.InProgress) {
                        ContainedLoadingIndicator(
                            modifier = Modifier
                                .size(40.dp)
                        )
                    }
                }
            }
        }
    }
}