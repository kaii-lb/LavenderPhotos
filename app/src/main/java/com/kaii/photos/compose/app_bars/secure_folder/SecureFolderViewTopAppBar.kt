package com.kaii.photos.compose.app_bars.secure_folder

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaii.photos.R
import com.kaii.photos.compose.app_bars.IsSelectingTopBar
import com.kaii.photos.compose.app_bars.getAppBarContentTransition
import com.kaii.photos.helpers.grid_management.SelectionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderViewTopAppBar(
    selectionManager: SelectionManager,
    onBackClicked: () -> Unit
) {
    val isSelecting by selectionManager.enabled.collectAsStateWithLifecycle(initialValue = false)

    AnimatedContent(
        targetState = isSelecting,
        transitionSpec = {
            getAppBarContentTransition(isSelecting)
        },
        label = "SecureFolderGridViewBottomBarAnimatedContent"
    ) { target ->
        if (!target) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { onBackClicked() },
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
                title = {
                    Text(
                        text = stringResource(id = R.string.secure_folder),
                        fontSize = TextUnit(18f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(160.dp)
                    )
                }
            )
        } else {
            IsSelectingTopBar(
                selectionManager = selectionManager,
                showTags = false,
                showTagDialog = false,
                setShowTagDialog = {}
            )
        }
    }
}