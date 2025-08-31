package com.kaii.photos.compose.app_bars

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.MainActivity.Companion.immichViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.AnimatedLoginIcon
import com.kaii.photos.compose.widgets.SelectViewTopBarLeftButtons
import com.kaii.photos.compose.widgets.SelectViewTopBarRightButtons
import com.kaii.photos.compose.dialogs.AlbumAddChoiceDialog
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.mediastore.MediaStoreData

@Composable
fun MainAppTopBar(
    alternate: Boolean,
    showDialog: MutableState<Boolean>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>,
    isFromMediaPicker: Boolean = false
) {
    DualFunctionTopAppBar(
        alternated = alternate,
        title = {
            Row {
                Text(
                    text = stringResource(id = R.string.app_name_full).split(" ").first() + " ",
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
                Text(
                    text = stringResource(id = R.string.app_name_full).split(" ")[1], // second part
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = currentView.value == DefaultTabs.TabTypes.albums && !isFromMediaPicker,
                enter = scaleIn(
                    animationSpec = AnimationConstants.expressiveSpring()
                ),
                exit = scaleOut(
                    animationSpec = AnimationConstants.expressiveSpring()
                )
            ) {
                var showAlbumTypeDialog by remember { mutableStateOf(false) }
                if (showAlbumTypeDialog) {
                    AlbumAddChoiceDialog {
                        showAlbumTypeDialog = false
                    }
                }

                IconButton(
                    onClick = {
                        showAlbumTypeDialog = true
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = stringResource(id = R.string.album_add),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (!isFromMediaPicker) {
                val immichUserState by immichViewModel.immichUserLoginState.collectAsStateWithLifecycle()

                AnimatedLoginIcon(
                    immichUserLoginState = immichUserState
                ) {
                    showDialog.value = true
                }
            } else {
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        (context as Activity).finish()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close media picker",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        alternateTitle = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        alternateActions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppBottomBar(
    currentView: MutableState<BottomBarTab>,
    tabs: List<BottomBarTab>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(selectedItemsList.size) {
        expanded = selectedItemsList.isEmpty()
    }

    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(1f)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(all = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            collapsedShadowElevation = 12.dp,
            expandedShadowElevation = 12.dp,
            scrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
                exitDirection = FloatingToolbarExitDirection.Bottom // TODO: continue this
            ),
            leadingContent = {
                tabs.take(if (selectedItemsList.isNotEmpty()) 1 else 4).forEach { tab ->
                    ToggleButton(
                        checked = currentView.value == tab,
                        onCheckedChange = {
                            if (currentView.value != tab) {
                                selectedItemsList.clear()
                                currentView.value = tab
                            }
                        },
                        shapes = ToggleButtonDefaults.shapes(
                            shape = null,
                            pressedShape = CircleShape,
                            checkedShape = CircleShape
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        AnimatedVisibility(
                            visible = currentView.value == tab
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = tab.icon.filled),
                                    contentDescription = stringResource(id = R.string.tabs_navigate_to, tab.name)
                                )

                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }

                        Text(
                            text = tab.name,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }
        ) {
            if (selectedItemsList.isNotEmpty()) {
                SelectingBottomBarItems(
                    albumInfo = AlbumInfo(
                        id = currentView.value.id,
                        name = currentView.value.name,
                        paths = currentView.value.albumPaths,
                        isCustomAlbum = false
                    ),
                    selectedItemsList = selectedItemsList
                )
            } else if (!expanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    tabs.drop(4).forEach { tab ->
                        val height by animateDpAsState(
                            targetValue = if (expanded) 0.dp else 48.dp,
                            animationSpec = tween(
                                delayMillis = 300
                            )
                        )

                        ToggleButton(
                            checked = currentView.value == tab,
                            onCheckedChange = {
                                if (currentView.value != tab) {
                                    selectedItemsList.clear()
                                    currentView.value = tab
                                }
                            },
                            shapes = ToggleButtonDefaults.shapes(
                                shape = null,
                                pressedShape = CircleShape,
                                checkedShape = CircleShape
                            ),
                            contentPadding = PaddingValues(all = 10.dp),
                            modifier = Modifier
                                .height(height)
                        ) {
                            AnimatedVisibility(
                                visible = currentView.value == tab
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = tab.icon.filled),
                                        contentDescription = stringResource(id = R.string.tabs_navigate_to, tab.name)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }

                            Text(
                                text = tab.name,
                                fontSize = TextUnit(14f, TextUnitType.Sp),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                }
            }

            if (tabs.size > 4 && selectedItemsList.isEmpty()) {
                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.other_page_indicator),
                        contentDescription = "Show more tabs"
                    )
                }
            }
        }
    }
}
