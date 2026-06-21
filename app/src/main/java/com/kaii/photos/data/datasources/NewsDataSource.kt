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
    private var id = 0

    suspend fun getPage(page: Int, size: Int): NewsPageResponse = withContext(Dispatchers.IO) {
        val allNews = cachedNews ?: parseFile().also { cachedNews = it }

        val startIndex = (page - 1) * size
        val pageData = allNews.drop(startIndex).take(size).map { line ->
            id += 1
            parser.parseLine(line, id)
        }

        NewsPageResponse(
            data = pageData,
            page = page,
            isEndOfList = startIndex + size >= allNews.size
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