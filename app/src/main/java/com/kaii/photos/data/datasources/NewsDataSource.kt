package com.kaii.photos.data.datasources

import android.content.Context
import com.kaii.photos.data.parsers.LnmParser
import com.kaii.photos.domain.news.NewsPageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsDataSource(
    private val context: Context,
    private val parser: LnmParser
) {
    private var cachedNews: List<String>? = null

    suspend fun getPage(offset: Int, size: Int): NewsPageResponse = withContext(Dispatchers.IO) {
        val allNews = cachedNews ?: parseFile().also { cachedNews = it }

        val pageData = allNews.drop(offset).take(size).mapIndexed { index, line ->
            parser.parseLine(line, offset + index + 1)
        }

        NewsPageResponse(
            data = pageData,
            isEndOfList = offset + size >= allNews.size
        )
    }

    private fun parseFile(): List<String> {
        return context.assets.open("changelog.lnm")
            .bufferedReader()
            .useLines { lines ->
                lines.mapNotNull { line ->
                    if (line.isBlank() || line.startsWith("//")) null
                    else line.trim()
                }.toList()
            }
    }
}