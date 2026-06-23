package com.kaii.photos.data.parsers

import android.util.Log
import com.kaii.photos.domain.news.News

class LnmParser {
    companion object {
        private val TAG = LnmParser::class.qualifiedName
    }

    fun parseLine(line: String, id: Int): News {
        try {
            return when (line.firstOrNull()) {
                '#' -> parseSection(line, id)
                '+' -> parseCategory(line, id)
                '-' -> parseItem(line, id)
                '!' -> parseNote(line, id)

                else -> throw IllegalArgumentException("Cannot parse char at line $id: ${line.firstOrNull()}")
            }
        } catch (e: Throwable) {
            Log.d(TAG, e.message.toString())
            e.printStackTrace()
            throw e
        }
    }

    private fun parseSection(section: String, id: Int): News.Section {
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

    private fun parseCategory(category: String, id: Int): News.Category {
        // string is of the format: + category=Feature|Fix|Improvement
        val title = category.substring(11)

        return News.Category(News.Category.Type.valueOf(title), id)
    }

    private fun parseItem(item: String, id: Int): News.Item {
        // string is of the format: - [issueNumber=0_PADDED_6_DIGIT_NUMBER] title=VARIABLE_LENGTH_STRING
        val issueNumberIndex = item.indexOf("issueNumber=")
        val titleIndex = item.indexOf("title=")

        val title = item.substring(titleIndex + 6)

        val issueNumber = if (issueNumberIndex != -1) {
            item.substring(issueNumberIndex + 12, titleIndex - 1).toInt()
        } else null

        return News.Item(title, issueNumber, id)
    }

    private fun parseNote(note: String, id: Int): News.Note {
        // string is of the format: ! urgency=Normal|Critical info=VARIABLE_LENGTH_STRING
        val infoIndex = note.indexOf("info=")
        val urgency = note.substring(10, infoIndex - 1)
        val info = note.substring(infoIndex + 5)

        return News.Note(info, News.Note.Urgency.valueOf(urgency), id)
    }
}