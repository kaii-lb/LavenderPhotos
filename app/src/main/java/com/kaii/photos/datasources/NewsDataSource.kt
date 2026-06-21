package com.kaii.photos.datasources

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface News {
    val id: Int

    data class Section(
        val version: String,
        val date: String,
        val status: Status,
        override val id: Int
    ) : News {
        enum class Status {
            Latest,
            Broken,
            None
        }
    }

    data class Category(
        val category: Type,
        override val id: Int
    ) : News {
        enum class Type {
            Feature,
            Fix,
            Improvement
        }
    }

    data class Item(
        val title: String,
        val issueNumber: Int?,
        override val id: Int
    ) : News

    data class Note(
        val info: String,
        val urgency: Urgency,
        override val id: Int
    ) : News {
        enum class Urgency {
            Normal,
            Critical
        }
    }
}

class NewsDataSource(
    private val context: Context
) {
    data class PageResponse(
        val data: List<News>,
        val page: Int,
        val isEndOfList: Boolean
    )

    private var cachedNews: List<String>? = null
    private var id = 0

    suspend fun getPage(page: Int, size: Int): PageResponse = withContext(Dispatchers.IO) {
        val allNews = cachedNews ?: parseFile().also { cachedNews = it }

        val startIndex = (page - 1) * size
        val pageData = allNews.drop(startIndex).take(size).map { line ->
            id += 1

            when (line.firstOrNull()) {
                '#' -> parseSection(line)
                '+' -> parseCategory(line)
                '-' -> parseItem(line)
                '!' -> parseNote(line)

                else -> throw IllegalArgumentException("Cannot parse char: ${line.firstOrNull()}")
            }
        }

        PageResponse(
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

    private fun parseSection(section: String): News.Section {
        // string is of the format: # version=vX.Y.Z[-beta|-hotfix] date=DD-MM-YYYY [status=None|Broken|Latest]
        val dateIndex = section.indexOf("date=")
        val version = section.substring(10, dateIndex)

        val statusIndex = section.indexOf("status=")
        val date =
            if (statusIndex == -1) section.substring(dateIndex + 5)
            else section.substring(dateIndex + 5, statusIndex - 1)

        val status =
            if (statusIndex != -1) section.substring(statusIndex + 7)
            else "None"

        return News.Section(version, date, News.Section.Status.valueOf(status), id)
    }

    private fun parseCategory(category: String): News.Category {
        // string is of the format: + category=Feature|Fix|Improvement
        val title = category.substring(11)

        return News.Category(News.Category.Type.valueOf(title), id)
    }

    private fun parseItem(item: String): News.Item {
        // string is of the format: - [issueNumber=0_PADDED_6_DIGIT_NUMBER] title=VARIABLE_LENGTH_STRING
        val issueNumberIndex = item.indexOf("issueNumber=")
        val title = item.substring(
            if (issueNumberIndex != -1) 20
            else 8
        )

        val issueNumber = if (issueNumberIndex != -1) {
            item.substring(14, 20).toInt()
        } else null

        return News.Item(title, issueNumber, id)
    }

    private fun parseNote(note: String): News.Note {
        // string is of the format: ! urgency=Normal|Critical info=VARIABLE_LENGTH_STRING
        val infoIndex = note.indexOf("info=")
        val urgency = note.substring(10, infoIndex - 1)
        val info = note.substring(infoIndex + 5)

        return News.Note(info, News.Note.Urgency.valueOf(urgency), id)
    }
}