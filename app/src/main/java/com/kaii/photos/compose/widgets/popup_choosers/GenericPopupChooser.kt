package com.kaii.photos.compose.widgets.popup_choosers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.FolderIsEmpty
import com.kaii.photos.compose.widgets.ClearableTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GenericPopupChooser(
    sheetState: SheetState,
    query: () -> String,
    onQueryChanged: (String) -> Unit,
    itemList: () -> List<T>,
    key: (item: T) -> Any,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(id = R.string.search),
    showSearchBar: Boolean = true,
    onDismiss: () -> Unit,
    content: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    ModalBottomSheet(
        sheetState = sheetState,
        tonalElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true
        ),
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismiss,
        contentWindowInsets = {
            WindowInsets.safeContent.add(WindowInsets(bottom = 8.dp))
        },
        modifier = modifier
            .statusBarsPadding()
    ) {
        AnimatedVisibility(
            visible = sheetState.currentValue != SheetValue.Hidden && showSearchBar,
            enter = expandVertically(
                expandFrom = Alignment.Top
            ) + fadeIn(),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top
            ) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ClearableTextField(
                value = query(),
                onValueChange = onQueryChanged,
                placeholder = placeholder,
                icon = R.drawable.search,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                onClear = {
                    onQueryChanged("")
                },
                onConfirm = {}
            )
        }

        if (itemList().isEmpty()) {
            FolderIsEmpty(
                emptyText = stringResource(id = R.string.search_nothing_found),
                emptyIconResId = R.drawable.error,
                backgroundColor = Color.Transparent
            )
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                    .clip(RoundedCornerShape(size = 24.dp)),
                verticalArrangement = Arrangement.spacedBy(
                    space = 2.dp,
                    alignment = Alignment.Top
                ),
                horizontalAlignment = Alignment.Start
            ) {
                items(
                    count = itemList().size,
                    key = {
                        key(itemList()[it])
                    }
                ) { index ->
                    content(index, itemList()[index])
                }
            }
        }
    }
}