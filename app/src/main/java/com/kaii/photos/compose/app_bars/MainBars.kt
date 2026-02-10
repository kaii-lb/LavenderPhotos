package com.kaii.photos.compose.app_bars

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.lavender.immichintegration.state_managers.LoginState
import com.kaii.lavender.immichintegration.state_managers.rememberLoginState
import com.kaii.photos.LocalMainViewModel
import com.kaii.photos.R
import com.kaii.photos.compose.dialogs.AlbumAddChoiceDialog
import com.kaii.photos.compose.dialogs.MainAppDialog
import com.kaii.photos.compose.widgets.AnimatedLoginIcon
import com.kaii.photos.compose.widgets.SelectViewTopBarLeftButtons
import com.kaii.photos.compose.widgets.SelectViewTopBarRightButtons
import com.kaii.photos.datastore.AlbumInfo
import com.kaii.photos.datastore.BottomBarTab
import com.kaii.photos.datastore.DefaultTabs
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.helpers.AnimationConstants
import com.kaii.photos.helpers.profilePicture
import com.kaii.photos.models.loading.PhotoLibraryUIModel
import kotlinx.coroutines.launch

@Composable
fun MainAppTopBar(
    alternate: Boolean,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    pagerState: PagerState,
    mediaCount: IntState,
    sectionCount: IntState,
    isFromMediaPicker: Boolean = false
) {
    val context = LocalContext.current
    val mainViewModel = LocalMainViewModel.current

    val immichInfo by mainViewModel.settings.immich.getImmichBasicInfo().collectAsStateWithLifecycle(initialValue = ImmichBasicInfo.Empty)
    val tabList by mainViewModel.settings.defaultTabs.getTabList().collectAsStateWithLifecycle(initialValue = DefaultTabs.defaultList)

    val loginState = rememberLoginState(baseUrl = immichInfo.endpoint)
    val userInfo by loginState.state.collectAsStateWithLifecycle()

    val showDialog = rememberSaveable { mutableStateOf(false) }

    MainAppDialog(
        showDialog = showDialog,
        pagerState = pagerState,
        selectedItemsList = selectedItemsList,
        mainViewModel = mainViewModel,
        loginState = loginState
    )

    LaunchedEffect(immichInfo) {
        loginState.refresh(
            accessToken = immichInfo.accessToken,
            pfpSavePath = context.profilePicture,
            previousPfpUrl = (userInfo as? LoginState.LoggedIn)?.pfpUrl ?: ""
        )
    }

    DualFunctionTopAppBar(
        alternated = alternate,
        title = {
            val split = stringResource(id = R.string.app_name_full).split(" ")

            val firstName = split.first()

            val secondName =
                if (split.size >= 2) split[1]
                else ""

            Row {
                Text(
                    text = "$firstName ",
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
                Text(
                    text = secondName,
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = tabList[pagerState.currentPage] == DefaultTabs.TabTypes.albums && !isFromMediaPicker,
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
                AnimatedLoginIcon(
                    state = userInfo
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
                selectedItemsList = selectedItemsList,
                mediaCount = mediaCount,
                sectionCount = sectionCount
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainAppBottomBar(
    pagerState: PagerState,
    tabs: List<BottomBarTab>,
    selectedItemsList: SnapshotStateList<PhotoLibraryUIModel>,
    scrollBehaviour: FloatingToolbarScrollBehavior
) {
    val mainViewModel = LocalMainViewModel.current
    val defaultTab by mainViewModel.settings.defaultTabs.getDefaultTab()
        .collectAsStateWithLifecycle(initialValue = mainViewModel.settings.defaultTabs.defaultTabItem)
    val tabList by mainViewModel.settings.defaultTabs.getTabList()
        .collectAsStateWithLifecycle(initialValue = mainViewModel.settings.defaultTabs.defaultTabList)

    val state = rememberLazyListState(
        initialFirstVisibleItemIndex =
            tabs.indexOf(
                if (defaultTab == DefaultTabs.TabTypes.secure || defaultTab !in tabs) tabs.first() else defaultTab
            )
    )

    val currentTab by remember {
        derivedStateOf {
            tabList[pagerState.currentPage]
        }
    }

    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(1f)
    ) {
        val animatedShadowColor by animateColorAsState(
            targetValue = if (scrollBehaviour.state.offset == 0f) Color.Black else Color.Black.copy(alpha = 0f),
            animationSpec = tween(
                durationMillis = AnimationConstants.DURATION
            )
        )

        val windowInfo = LocalWindowInfo.current
        val localDensity = LocalDensity.current

        HorizontalFloatingToolbar(
            expanded = false,
            collapsedShadowElevation = 0.dp,
            expandedShadowElevation = 0.dp,
            scrollBehavior = scrollBehaviour,
            modifier = Modifier
                .offset(y = (-12).dp)
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.systemBars)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = animatedShadowColor,
                    spotColor = animatedShadowColor
                )
                .widthIn(max = with(localDensity) {
                    windowInfo.containerSize.width.toDp() * 0.9f
                }),
            content = {
                AnimatedContent(
                    targetState = selectedItemsList.isNotEmpty(),
                    transitionSpec = {
                        (slideInHorizontally() + fadeIn()).togetherWith(
                            scaleOut(
                                transformOrigin = TransformOrigin(1f, 0.5f)
                            ) + fadeOut()
                        )
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                ) { animationState ->
                    if (animationState) {
                        Row(
                            modifier = Modifier
                                .wrapContentHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            when (currentTab) {
                                DefaultTabs.TabTypes.trash -> {
                                    TrashPhotoGridBottomBarItems(
                                        selectedItemsList = selectedItemsList
                                    )
                                }

                                DefaultTabs.TabTypes.favourites -> {
                                    FavouritesBottomAppBarItems(
                                        selectedItemsList = selectedItemsList
                                    )
                                }

                                else -> {
                                    SelectingBottomBarItems(
                                        albumInfo = AlbumInfo(
                                            id = currentTab.id,
                                            name = currentTab.name,
                                            paths = currentTab.albumPaths,
                                            isCustomAlbum = false
                                        ),
                                        selectedItemsList = selectedItemsList
                                    )
                                }
                            }
                        }
                    } else {
                        val coroutineScope = rememberCoroutineScope()

                        LazyRow(
                            state = state
                        ) {
                            items(
                                items = tabs
                            ) { tab ->
                                ToggleButton(
                                    checked = currentTab == tab,
                                    onCheckedChange = {
                                        if (currentTab != tab) coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                page = tabList.indexOf(tab),
                                                animationSpec = AnimationConstants.defaultSpring()
                                            )
                                        }
                                    },
                                    shapes = ToggleButtonDefaults.shapes(
                                        shape = null,
                                        pressedShape = CircleShape,
                                        checkedShape = CircleShape
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                ) {
                                    AnimatedVisibility(
                                        visible = currentTab == tab
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxHeight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = tab.icon.filled),
                                                contentDescription = stringResource(id = R.string.tabs_navigate_to, tab.name),
                                                modifier = Modifier
                                                    .size(24.dp)
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
                }
            }
        )
    }
}
