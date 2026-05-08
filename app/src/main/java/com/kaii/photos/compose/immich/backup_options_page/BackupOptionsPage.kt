package com.kaii.photos.compose.immich.backup_options_page

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.screens.ImmichBackupOptionsStateImpl

@Composable
fun ImmichBackupOptionsPage(
    state: ImmichBackupOptionsStateImpl,
    modifier: Modifier = Modifier,
    navController: NavController = LocalNavController.current
) {
    val albums by state.albums.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by state.query.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            val context = LocalContext.current

            TopBar(
                query = { searchQuery },
                navController = navController,
                onQueryChange = state::search,
                confirm = {
                    state.confirm(context)
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(all = 16.dp),
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
                    items = albums
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
                        }
                    )
                }

                if (albums.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
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