package com.kaii.photos.data.parsers

class HTMLToLnmParser {
    fun parse(markdown: String): String {
        return markdown.lineSequence()
            .filter {
                it != "<ul>" && it != "</ul>" && it.isNotEmpty()
            }
            .joinToString("\n") { line ->
                when {
                    line.startsWith("<b><h4>Note: ") || line.startsWith("<b><h4>Critical: ") -> parseNote(line)

                    line.startsWith("<h4><b>") -> parseCategory(line)

                    line.startsWith("<li>") -> parseItem(line)

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

    /** <h4><b>Feature</b></h4> */
    private fun parseCategory(line: String): String {
        val category = line
            .replace("<h4><b>", "")
            .replace("</b></h4>", "")
            .trim()

        return "+ category=$category"
    }

    /** <li>Description. Fixes Issue: #123</li> */
    private fun parseItem(line: String): String {
        val cleanLine = line
            .replace("<li>", "")
            .replace("</li>", "")
            .trim()

        val issueNumberIndex = cleanLine.indexOf("Fixes Issue: ")

        val issueNumber = if (issueNumberIndex != -1) {
            cleanLine.substring(issueNumberIndex + 13)
                .drop(1)
                .padStart(6, '0')
        } else null

        val title = if (issueNumberIndex != -1) {
            cleanLine.substring(0, issueNumberIndex).trim().removeSuffix(".")
        } else {
            cleanLine
        }

        return if (issueNumber != null) {
            "- issueNumber=${issueNumber} title=$title"
        } else {
            "- title=$title"
        }
    }
}