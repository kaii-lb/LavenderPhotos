package com.kaii.photos.compose.widgets.albums

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.datastore.AlbumSortMode
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.permissions.auth.rememberSecureFolderAuthManager

@Composable
fun SortModeHeader(
    sortMode: AlbumSortMode,
    tabList: List<BottomBarTab>,
    @FloatRange(0.0, 1.0) progress: Float,
    modifier: Modifier = Modifier,
    setAlbumSortMode: (sortMode: AlbumSortMode) -> Unit
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth(1f)
            .padding(4.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Start
        )
    ) {
        item {
            OutlinedIconButton(
                onClick = {
                    setAlbumSortMode(sortMode.flip())
                },
                enabled = sortMode != AlbumSortMode.Custom
            ) {
                val animatedRotation by animateFloatAsState(
                    targetValue = if (sortMode.isDescending) -90f else 90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "Animate sort order arrow"
                )

                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = stringResource(id = R.string.sort_indicator),
                    modifier = Modifier
                        .rotate(animatedRotation)
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    setAlbumSortMode(AlbumSortMode.LastModified.byDirection(sortMode.isDescending))
                },
                colors =
                    if (sortMode == AlbumSortMode.LastModified.byDirection(sortMode.isDescending)) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(id = R.string.sort_date),
                    modifier = Modifier
                        .scale(progress)
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    setAlbumSortMode(AlbumSortMode.Alphabetically.byDirection(sortMode.isDescending))
                },
                colors =
                    if (sortMode == AlbumSortMode.Alphabetically.byDirection(sortMode.isDescending)) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(id = R.string.sort_name),
                    modifier = Modifier
                        .scale(progress)
                )
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    setAlbumSortMode(AlbumSortMode.Custom)
                },
                colors =
                    if (sortMode == AlbumSortMode.Custom) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(
                    text = stringResource(id = R.string.sort_custom),
                    modifier = Modifier
                        .scale(progress)
                )
            }
        }

        if (!tabList.contains(DefaultTabs.TabTypes.secure)) {
            item {
                val authManager = rememberSecureFolderAuthManager()
                OutlinedButton(
                    onClick = {
                        authManager.authenticate()
                    },
                    colors =
                        if (sortMode == AlbumSortMode.Custom) ButtonDefaults.buttonColors()
                        else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(
                        text = stringResource(id = R.string.secure_folder),
                        modifier = Modifier
                            .scale(progress)
                    )
                }
            }
        }
    }
}