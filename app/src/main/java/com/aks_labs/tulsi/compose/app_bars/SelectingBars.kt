package com.aks_labs.tulsi.compose.app_bars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aks_labs.tulsi.compose.SelectViewTopBarLeftButtons
import com.aks_labs.tulsi.compose.SelectViewTopBarRightButtons
import com.aks_labs.tulsi.datastore.BottomBarTab
import com.aks_labs.tulsi.mediastore.MediaStoreData

@Composable
fun SelectableBottomAppBarItem(
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    action: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                action()
            }
    ) {
        AnimatedVisibility(
            visible = selected,
            enter =
                expandHorizontally(
                    animationSpec = tween(
                        durationMillis = 350
                    ),
                    expandFrom = Alignment.CenterHorizontally
                ) + fadeIn(),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 150
                )
            ),
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(1000.dp))
                .align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(64.dp)
                    .clip(
                        RoundedCornerShape(
                            1000.dp
                        )
                    )
                    .align(Alignment.TopCenter)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.2f
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .height(32.dp)
                .width(58.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            label()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsSelectingTopBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>
) {
    TopAppBar(
        title = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        actions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList,
                currentView = currentView
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
    )
}

@Composable
fun IsSelectingBottomAppBar(
    items: @Composable (RowScope.() -> Unit)
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items()
        }
    }
}

