package com.kaii.photos.compose.widgets.news

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.kaii.photos.models.news.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsPopup(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        sheetState = sheetState,
        tonalElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onDismissRequest = onDismiss,
        contentWindowInsets = {
            WindowInsets.safeContent.add(WindowInsets(bottom = 8.dp))
        },
        modifier = modifier
            .statusBarsPadding()
    ) {
        val list = viewModel.news.collectAsLazyPagingItems()

        NewsList(
            list = list,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(size = 32.dp))
        )
    }
}