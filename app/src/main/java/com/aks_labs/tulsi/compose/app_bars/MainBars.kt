package com.aks_labs.tulsi.compose.app_bars

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aks_labs.tulsi.MainActivity.Companion.mainViewModel
import com.aks_labs.tulsi.R
import com.aks_labs.tulsi.compose.SelectViewTopBarLeftButtons
import com.aks_labs.tulsi.compose.SelectViewTopBarRightButtons
import com.aks_labs.tulsi.compose.dialogs.AlbumAddChoiceDialog
import com.aks_labs.tulsi.compose.dialogs.ConfirmationDialog
import com.aks_labs.tulsi.compose.grids.MoveCopyAlbumListView
import com.aks_labs.tulsi.datastore.BottomBarTab
import com.aks_labs.tulsi.datastore.DefaultTabs
import com.aks_labs.tulsi.datastore.Permissions
import com.aks_labs.tulsi.helpers.GetPermissionAndRun
import com.aks_labs.tulsi.helpers.setTrashedOnPhotoList
import com.aks_labs.tulsi.mediastore.MediaStoreData
import com.aks_labs.tulsi.mediastore.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainAppTopBar(
    alternate: Boolean,
    showDialog: MutableState<Boolean>,
    selectedItemsList: SnapshotStateList<MediaStoreData>,
    currentView: MutableState<BottomBarTab>
) {
    DualFunctionTopAppBar(
        alternated = alternate,
        title = {
            Row {
                Text(
                    text = " Tulsi\uD83C\uDF3F ",
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
                Text(
                    text = "Photos",
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            }
        },
        actions = {
            // Grid view toggle for Photos and Search tabs
            AnimatedVisibility(
                visible = currentView.value == DefaultTabs.TabTypes.photos || currentView.value == DefaultTabs.TabTypes.search,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                val isGridView by mainViewModel.isGridViewMode.collectAsStateWithLifecycle(initialValue = true)

                IconButton(
                    onClick = {
                        mainViewModel.toggleGridViewMode()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = if (isGridView) R.drawable.grid_view else R.drawable.view_day),
                        contentDescription = if (isGridView) "Switch to date view" else "Switch to grid view",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Add album button for Albums tab
            AnimatedVisibility(
                visible = currentView.value == DefaultTabs.TabTypes.albums,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
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
                        contentDescription = "Add album",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    showDialog.value = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings Button",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        alternateTitle = {
            SelectViewTopBarLeftButtons(selectedItemsList = selectedItemsList)
        },
        alternateActions = {
            SelectViewTopBarRightButtons(
                selectedItemsList = selectedItemsList,
                currentView = currentView
            )
        },
    )
}

@Composable
fun MainAppBottomBar(
    currentView: MutableState<BottomBarTab>,
    tabs: List<BottomBarTab>,
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 30.dp)                                      //
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Floating bottom bar container
        Box(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(0.95f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(percent = 30),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    shape = RoundedCornerShape(percent = 30)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 7.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(percent = 30)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEach { tab ->
                    FloatingBottomBarItem(
                        tab = tab,
                        isSelected = currentView.value == tab,
                        onClick = {
                            if (currentView.value != tab) {
                                selectedItemsList.clear()
                                currentView.value = tab
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomBarItem(
    tab: BottomBarTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val mutableInteraction = remember { MutableInteractionSource() }
    val selectedColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "selectedColor"
    )
    val selectedIconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "selectedIconColor"
    )
    val selectedTextColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "selectedTextColor"
    )

    Column(
        modifier = Modifier
            .width(70.dp)
            .padding(vertical = 4.dp)
            .clickable(
                indication = null,
                interactionSource = mutableInteraction,
                onClick = { if (!isSelected) onClick() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selected background indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = selectedColor,
                        shape = RoundedCornerShape(percent = 100)
                    )
                    .clip(RoundedCornerShape(100))
            )

            // Icon
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = if (isSelected) tab.icon.filled else tab.icon.nonFilled),
                contentDescription = "Navigate to ${tab.name} page",
                tint = selectedIconColor
            )
        }

        // Tab name text
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = selectedTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MainAppSelectingBottomBar(
    selectedItemsList: SnapshotStateList<MediaStoreData>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 30.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Floating bottom bar container
        Box(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(0.95f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(percent = 30),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    shape = RoundedCornerShape(percent = 30)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        shape = RoundedCornerShape(percent = 30)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                val selectedItemsWithoutSection by remember {
                    derivedStateOf {
                        selectedItemsList.filter {
                            it.type != MediaType.Section && it != MediaStoreData()
                        }
                    }
                }

                BottomAppBarItem(
                    text = "Share",
                    iconResId = R.drawable.share,
                    action = {
                        coroutineScope.launch {
                            val hasVideos = selectedItemsWithoutSection.any {
                                it.type == MediaType.Video
                            }

                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND_MULTIPLE
                                type = if (hasVideos) "video/*" else "images/*"
                            }

                            val fileUris = ArrayList<Uri>()
                            selectedItemsWithoutSection.forEach {
                                fileUris.add(it.uri)
                            }

                            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)

                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    }
                )

                val show = remember { mutableStateOf(false) }
                var isMoving by remember { mutableStateOf(false) }
                MoveCopyAlbumListView(
                    show = show,
                    selectedItemsList = selectedItemsList,
                    isMoving = isMoving,
                    groupedMedia = null,
                    insetsPadding = WindowInsets.statusBars
                )

                BottomAppBarItem(
                    text = "Move",
                    iconResId = R.drawable.cut,
                    action = {
                        isMoving = true
                        show.value = true
                    }
                )

                BottomAppBarItem(
                    text = "Copy",
                    iconResId = R.drawable.copy,
                    action = {
                        isMoving = false
                        show.value = true
                    }
                )

                val showDeleteDialog = remember { mutableStateOf(false) }
                val runDeleteAction = remember { mutableStateOf(false) }

                GetPermissionAndRun(
                    uris = selectedItemsWithoutSection.map { it.uri },
                    shouldRun = runDeleteAction,
                    onGranted = {
                        mainViewModel.launch(Dispatchers.IO) {
                            setTrashedOnPhotoList(
                                context = context,
                                list = selectedItemsWithoutSection.map { Pair(it.uri, it.absolutePath) },
                                trashed = true
                            )

                            selectedItemsList.clear()
                        }
                    }
                )

                val confirmToDelete by mainViewModel.settings.Permissions.getConfirmToDelete()
                    .collectAsStateWithLifecycle(initialValue = true)
                BottomAppBarItem(
                    text = "Delete",
                    iconResId = R.drawable.delete,
                    cornerRadius = 16.dp,
                    action = {
                        if (confirmToDelete) showDeleteDialog.value = true
                        else runDeleteAction.value = true
                    },
                    dialogComposable = {
                        ConfirmationDialog(
                            showDialog = showDeleteDialog,
                            dialogTitle = "Move these items to Trash Bin?",
                            confirmButtonLabel = "Delete"
                        ) {
                            runDeleteAction.value = true
                        }
                    }
                )
            }
        }
    }
}

