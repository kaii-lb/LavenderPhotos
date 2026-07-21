package com.kaii.photos.repositories

import com.kaii.photos.data.datasources.LatestNewsDataSource
import com.kaii.photos.data.parsers.HTMLToLnmParser
import com.kaii.photos.data.parsers.LnmParser
import com.kaii.photos.data.providers.AppVersionProvider
import com.kaii.photos.domain.news.News

class LatestNewsRepository(
    private val dataSource: LatestNewsDataSource,
    private val hTMLToLnmParser: HTMLToLnmParser,
    private val lnmParser: LnmParser,
    private val versionProvider: AppVersionProvider
) {
    suspend fun getNews(): List<News> {
        val markdown = dataSource.fetch() ?: return emptyList()
        val lnm = hTMLToLnmParser.parse(markdown)

        return lnm.lineSequence().mapIndexedNotNull { id, line ->
            if (line.isNotBlank()) lnmParser.parseLine(line, id) else null
        }.toList()
    }

    suspend fun hasUpdate(): Boolean {
        val latestVersion = dataSource.getLatestVersion()
            ?.replace("v" , "")
            ?.replace(".", "")
            ?.toLong()
            ?: 0

        return versionProvider.getCurrentVersionCode() < latestVersion
    }
}