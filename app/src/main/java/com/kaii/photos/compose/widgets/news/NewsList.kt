package com.kaii.photos.compose.widgets.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kaii.photos.domain.news.News
import com.kaii.photos.helpers.RowPosition
import com.kaii.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.flow.flowOf
import kotlin.reflect.KClass

@Preview
@Composable
private fun NewsListPreview() {
    PhotosTheme(theme = 2) {
        NewsList(
            list = flowOf(
                PagingData.from(
                    listOf(
                        News.Section(
                            version = "v2.0.0",
                            date = "20-7-2026",
                            status = News.Section.Status.Latest,
                            id = 0
                        ),
                        News.Note(
                            info = "This is a note.",
                            urgency = News.Note.Urgency.Critical,
                            id = 1
                        ),
                        News.Category(
                            category = News.Category.Type.Features,
                            id = 2
                        ),
                        News.Item(
                            title = "This is a very long description for a very boring UI component",
                            issueNumber = 123,
                            id = 3
                        ),
                        News.Item(
                            title = "This is a very long description for a very boring UI component",
                            issueNumber = 123,
                            id = 4
                        ),
                        News.Item(
                            title = "This is a very long description for a very boring UI component",
                            issueNumber = 123,
                            id = 5
                        ),
                        News.Category(
                            category = News.Category.Type.Improvements,
                            id = 6
                        ),
                        News.Item(
                            title = "This is a very long description for a very boring UI component",
                            issueNumber = 123,
                            id = 7
                        ),
                        News.Category(
                            category = News.Category.Type.Fixes,
                            id = 8
                        ),
                        News.Item(
                            title = "This is a very long description for a very boring UI component",
                            issueNumber = 123,
                            id = 9
                        ),
                        News.Item(
                            title = "This is a very long description for a very boring UI component",
                            issueNumber = 123,
                            id = 10
                        ),
                        News.Section(
                            version = "v1.9.0",
                            date = "20-7-2026",
                            status = News.Section.Status.Broken,
                            id = 11
                        ),
                        News.Category(
                            category = News.Category.Type.Features,
                            id = 12
                        ),
                        News.Item(
                            title = "This is a very long description for a very boring UI component",
                            issueNumber = 123,
                            id = 13
                        )
                    )
                )
            ).collectAsLazyPagingItems(),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        )
    }
}

@Composable
fun NewsList(
    list: LazyPagingItems<News>,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = 2.dp,
            alignment = Alignment.Top
        )
    ) {
        items(
            count = list.itemCount,
            key = list.itemKey {
                it.id
            }
        ) { index ->
            val item = list[index]

            if (item != null) {
                ItemContent(
                    item = item,
                    position = calcRowPosition(index, list.itemCount) {
                        list[it]!!
                    },
                    topPaddingFor = { classType ->
                        if (index - 1 > 0) {
                            if (list[index - 1]!!::class == classType) 8.dp
                            else 0.dp
                        } else 8.dp
                    }
                )
            }
        }
    }
}

@Composable
fun NewsList(
    list: List<News>,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = 2.dp,
            alignment = Alignment.Top
        )
    ) {
        items(
            count = list.size,
            key = { list[it].id }
        ) { index ->
            val item = list[index]

            ItemContent(
                item = item,
                position = calcRowPosition(index, list.size) {
                    list[it]
                },
                topPaddingFor = { classType ->
                    if (index - 1 > 0) {
                        if (list[index - 1]::class == classType) 8.dp
                        else 0.dp
                    } else 8.dp
                }
            )
        }
    }
}

@Composable
private fun ItemContent(
    item: News,
    position: RowPosition,
    topPaddingFor: (classType: KClass<out News>) -> Dp
) {
    when (item) {
        is News.Section -> {
            NewsSection(
                version = item.version,
                date = item.date,
                status = item.status,
                modifier = Modifier
                    .padding(top = topPaddingFor(News.Section::class))
            )
        }

        is News.Category -> {
            NewsCategory(
                category = item.category
            )
        }

        is News.Item -> {
            NewsCard(
                title = item.title,
                issueNumber = item.issueNumber,
                position = position
            )
        }

        is News.Note -> {
            NewsNote(
                info = item.info,
                urgency = item.urgency,
                modifier = Modifier
                    .padding(top = topPaddingFor(News.Note::class))
            )
        }
    }
}

private fun calcRowPosition(index: Int, size: Int, getItem: (index: Int) -> News): RowPosition {
    val isPrevCard = index > 0 && getItem(index - 1) is News.Item
    val isNextCard = index < size - 1 && getItem(index + 1) is News.Item

    return when {
        isPrevCard && isNextCard -> RowPosition.Middle
        isPrevCard && !isNextCard -> RowPosition.Bottom
        !isPrevCard && isNextCard -> RowPosition.Top
        else -> RowPosition.Single
    }
}