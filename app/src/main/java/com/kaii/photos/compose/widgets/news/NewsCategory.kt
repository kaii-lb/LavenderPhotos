package com.kaii.photos.compose.widgets.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaii.photos.R
import com.kaii.photos.domain.news.News
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun NewsCategoryPreview() {
    PhotosTheme(theme = 1) {
        NewsCategory(
            category = News.Category.Type.Feature,
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = when (category) {
                News.Category.Type.Feature -> stringResource(id = R.string.news_category_feature)
                News.Category.Type.Improvement -> stringResource(id = R.string.news_category_improvement)
                News.Category.Type.Fix -> stringResource(id = R.string.news_category_fix)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primaryFixed
        )
    }
}