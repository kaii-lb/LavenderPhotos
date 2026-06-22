package com.kaii.photos

import com.kaii.photos.data.parsers.HTMLToLnmParser
import org.junit.Test

class HTMLToLnmParserTest {
    private val markdown = """
v2.0.0 - Hotfix - Broken 21-06-2026
<b><h4>Critical: Note Info</h4></b>
<b><h4>Note: Note Info</h4></b>
<b><h4>- Features</h4></b>
- This is a changelog item
<b><h4>- Fixes</h4></b>
- This is a changelog item. Fixes Issue: #123
<b><h4>- Improvements</h4></b>
- This is a changelog item
    """.trimIndent()

    private val lnm = """
# version=v2.0.0-hotfix date=21-06-2026 status=Broken
! urgency=Critical info=Note Info
! urgency=Normal info=Note Info
+ category=Features
- title=This is a changelog item
+ category=Fixes
- issueNumber=000123 title=This is a changelog item.
+ category=Improvements
- title=This is a changelog item
""".trimIndent()

    @Test
    fun test() {
        val parsed = HTMLToLnmParser().parse(markdown)

        assert(parsed == lnm)
    }
}