package com.kaii.photos.data.parsers

class HTMLToLnmParser {
    fun parse(markdown: String): String {
        return markdown.lineSequence().joinToString("\n") { line ->
            when {
                line.startsWith("<b><h4>Note: ") || line.startsWith("<b><h4>Critical: ") -> parseNote(line)

                line.startsWith("<b><h4>- ") -> parseCategory(line)

                line.startsWith("- ") -> parseItem(line)

                line.startsWith("v") -> parseVersion(line)

                else -> "" // ignore errors while parsing HTML so the app doesn't blow up
            }
        }
    }

    /** vX.Y.Z - Hotfix - Broken DD-MM-YYYY */
    private fun parseVersion(line: String): String {
        val version = line
            .lowercase()
            .replace(" - ", "-")
            .replace("-broken", "")
            .substringBefore(' ')

        val status =
            if (line.lowercase().contains("broken")) "Broken"
            else "Latest"

        val date = line.substringAfterLast(" ")
            .split("-")
            .reversed()
            .joinToString("-")

        return "# version=$version date=$date status=$status"
    }

    /** <b><h4>Note: Note Info */
    private fun parseNote(line: String): String {
        val info = line
            .replace("<b><h4>Note: ", "")
            .replace("<b><h4>Critical: ", "")
            .replace("</h4></b>", "")

        val urgency =
            if (line.startsWith("<b><h4>Critical:")) "Critical"
            else "Normal"

        return "! urgency=$urgency info=$info"
    }

    /** <b><h4>- Feature */
    private fun parseCategory(line: String): String {
        val category = line.replace("<b><h4>- ", "").replace("</h4></b>", "")

        return "+ category=$category"
    }

    /** - Description. Fixes Issue: #123 */
    private fun parseItem(line: String): String {
        val issueNumberIndex = line.indexOf("Fixes Issue: ")

        val issueNumber = if (issueNumberIndex != -1) {
            line.substring(issueNumberIndex + 13)
                .drop(1)
                .padStart(6, '0') // Fixes Issue: #123 -> 123
        } else null

        val title =
            if (issueNumberIndex != -1) line.substring(0, issueNumberIndex).substringAfter("- ").trim()
            else line.substringAfter("- ").trim()

        return if (issueNumber != null) {
            "- issueNumber=${issueNumber} title=$title"
        } else {
            "- title=$title"
        }
    }
}