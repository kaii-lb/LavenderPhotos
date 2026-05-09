package com.kaii.photos.compose.immich.backup_options_page

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kaii.photos.LocalNavController
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.user_action.ConfirmationDialogWithBody
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.screens.ImmichBackupOptionsStateImpl
import kotlinx.coroutines.launch

@Composable
fun ImmichBackupOptionsPage(
    state: ImmichBackupOptionsStateImpl,
    modifier: Modifier = Modifier,
    navController: NavController = LocalNavController.current
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val albums by state.albums.collectAsStateWithLifecycle()
    val searchQuery by state.query.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            val context = LocalContext.current

            TopBar(
                query = { searchQuery },
                isRefreshing = { state.isLoading },
                pullDistance = { pullToRefreshState.distanceFraction },
                navController = navController,
                onQueryChange = state::search,
                confirm = {
                    state.confirm(context)
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val coroutineScope = rememberCoroutineScope()

        val showDialog = remember { mutableStateOf(false) }
        ConfirmationDialogWithBody(
            showDialog = showDialog,
            dialogTitle = stringResource(id = R.string.immich_backup_option_changes_unsaved),
            dialogBody = stringResource(id = R.string.immich_backup_option_changes_unsaved_desc),
            confirmButtonLabel = stringResource(id = R.string.refresh)
        ) {
            coroutineScope.launch {
                state.refresh()
            }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(all = 16.dp)
                .pullToRefresh(
                    isRefreshing = state.isLoading,
                    state = pullToRefreshState,
                    onRefresh = {
                        if (state.hasUnsavedChanges) {
                            showDialog.value = true
                        } else {
                            coroutineScope.launch {
                                state.refresh()
                            }
                        }
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val assetCount by state.assetCount.collectAsStateWithLifecycle(initialValue = 0)
            ImmichBackupOptionsPageHeader(
                selectedCount = state::selectedCount,
                assetCount = { assetCount },
                modifier = Modifier
                    .fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp)),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(
                    items = albums,
                    key = { _, item ->
                        item.id
                    }
                ) { index, album ->
                    ImmichAlbumListItem(
                        album = album,
                        selected = {
                            state.selected(id = album.id)
                        },
                        position =
                            when {
                                albums.size == 1 -> RowPosition.Single
                                index == 0 -> RowPosition.Top
                                index == albums.size - 1 -> RowPosition.Bottom
                                else -> RowPosition.Middle
                            },
                        onToggle = {
                            state.toggle(id = album.id)
                        },
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .animateItem()
                    )
                }

                if (albums.isEmpty()) {
                    item(
                        key = "nothing_shown_card"
                    ) {
                        Column(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(durationMillis = AnimationConstants.DURATION_SHORT),
                                    fadeOutSpec = tween(durationMillis = AnimationConstants.DURATION_SHORT),
                                    placementSpec = tween(durationMillis = AnimationConstants.DURATION_SHORT)
                                )
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(all = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(
                                space = 8.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.cloud_off),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                            )

                            Text(
                                text = stringResource(id = R.string.immich_backup_options_empty),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}