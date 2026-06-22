package com.kaii.photos

import com.kaii.photos.data.parsers.HTMLToLnmParser
import org.junit.Test

class HTMLToLnmParserTest {
    // date is flipped because GitHub would supply the date as YYYY-MM-DD
    private val markdown = """
v2.0.0-hotfix - Broken 2026-06-21
<b><h4>Critical: Note Info</h4></b>
<b><h4>Note: Note Info</h4></b>
<h4><b>Features</b></h4>
<ul>
<li>This is a changelog item</li>
<li>This is a changelog item</li>
</ul>
<h4><b>Fixes</b></h4>
<ul>
<li>This is a changelog item. Fixes Issue: #123</li>
</ul>
<h4><b>Improvements</b></h4>
<ul>
<li>This is a changelog item</li>
</ul>
    """.trimIndent()

    private val lnm = """
# version=v2.0.0-hotfix date=21-06-2026 status=Broken
! urgency=Critical info=Note Info
! urgency=Normal info=Note Info
+ category=Features
- title=This is a changelog item
- title=This is a changelog item
+ category=Fixes
- issueNumber=000123 title=This is a changelog item
+ category=Improvements
- title=This is a changelog item
""".trimIndent()

    @Test
    fun test() {
        val parsed = HTMLToLnmParser().parse(markdown)

        assert(parsed == lnm)
    }
}