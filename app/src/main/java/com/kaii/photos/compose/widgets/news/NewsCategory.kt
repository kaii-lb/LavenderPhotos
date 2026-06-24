package com.kaii.photos.compose.widgets.news

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.compose.widgets.TextSeparator
import com.kaii.photos.domain.news.News
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun NewsCategoryPreview() {
    PhotosTheme(theme = 1) {
        NewsCategory(
            category = News.Category.Type.Features,
            modifier = Modifier
                .width(300.dp)
        )
    }
}

@Composable
fun NewsCategory(
    category: News.Category.Type,
    modifier: Modifier = Modifier
) {
    TextSeparator(
        text = when (category) {
            News.Category.Type.Features -> stringResource(id = R.string.news_category_feature)
            News.Category.Type.Improvements -> stringResource(id = R.string.news_category_improvement)
            News.Category.Type.Fixes -> stringResource(id = R.string.news_category_fix)
        },
        modifier = modifier
    )
}