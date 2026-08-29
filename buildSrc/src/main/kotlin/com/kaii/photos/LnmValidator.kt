class LnmValidator() {
    private enum class Status {
        Latest,
        Broken,
        None
    }

    private enum class Urgency {
        Normal,
        Critical
    }

    private enum class Type {
        Features,
        Fixes,
        Improvements
    }

    private val versionRegex = Regex("""^v\d+\.\d+\.\d+(-beta|-hotfix)?$""")
    private val dateRegex = Regex("""^\d{2}-\d{2}-\d{4}$""")
    private val issueNumberRegex = Regex("""^\d{6}$""")

    private var alreadyHasLatest = false

    fun validateLine(line: String): Boolean =
        try {
            val trimmed = line.trim()
            when (trimmed.firstOrNull()) {
                '#' -> parseSection(trimmed)
                '+' -> parseCategory(trimmed)
                '-' -> parseItem(trimmed)
                '!' -> parseNote(trimmed)
                else -> true
            }
        } catch (_: Throwable) {
            false
        }

    private fun parseSection(section: String): Boolean {
        // string is of the format: # version=vX.Y.Z[-beta|-hotfix] date=DD-MM-YYYY [status=None|Broken|Latest]
        val dateIndex = section.indexOf("date=")
        if (dateIndex == -1) return false

        val version = section.substring(10, dateIndex - 1)
        val statusIndex = section.indexOf("status=")

        val date =
            if (statusIndex == -1) section.substring(dateIndex + 5)
            else section.substring(dateIndex + 5, statusIndex - 1)

        val status =
            if (statusIndex != -1) section.substring(statusIndex + 7)
            else "None"

        if (status == "Latest") {
        	if (alreadyHasLatest) return false
        	alreadyHasLatest = true
        }

        return version.matches(versionRegex) &&
                (date == "Unknown" || date.matches(dateRegex)) &&
                status in Status.entries.map { it.name }
    }

    private fun parseCategory(category: String): Boolean {
        // string is of the format: + category=Features|Fixes|Improvements
        val title = category.substring(11)
        return title in Type.entries.map { it.name }
    }

    private fun parseItem(item: String): Boolean {
        // string is of the format: - [issueNumber=0_PADDED_6_DIGIT_NUMBER] title=VARIABLE_LENGTH_STRING
        val issueNumberIndex = item.indexOf("issueNumber=")
        val titleIndex = item.indexOf("title=")

        if (titleIndex == -1) return false

        if (issueNumberIndex != -1) {
            if (issueNumberIndex != 2) return false
            val issueNumber = item.substring(issueNumberIndex + 12, titleIndex - 1)
            if (!issueNumber.matches(issueNumberRegex)) return false
        }

        val title = item.substring(titleIndex + 6)
        return title.isNotEmpty()
    }

    private fun parseNote(note: String): Boolean {
        // string is of the format: ! urgency=Normal|Critical info=VARIABLE_LENGTH_STRING
        val infoIndex = note.indexOf("info=")
        if (infoIndex == -1) return false

        val urgency = note.substring(10, infoIndex - 1)
        val info = note.substring(infoIndex + 5)

        return urgency in Urgency.entries.map { it.name } && info.isNotEmpty()
    }
}
