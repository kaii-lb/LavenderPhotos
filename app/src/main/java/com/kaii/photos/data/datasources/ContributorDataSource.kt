package com.kaii.photos.data.datasources

import android.content.Context
import com.kaii.photos.R
import com.kaii.photos.domain.about.ContributorItem
import com.kaii.photos.domain.about.GithubContributorResponse
import com.kaii.photos.domain.about.ContributorItem.Contributor
import com.kaii.photos.domain.about.SocialButton
import com.kaii.photos.domain.about.majorContributors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ContributorDataSource(
    private val context: Context,
) {
    private var cachedContributors: String? = null
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val majorContributorsLogins = majorContributors.mapNotNull {
        (it as? Contributor)?.name
    }

    suspend fun getContributorList(): List<ContributorItem> = withContext(Dispatchers.IO) {
        val jsonText = cachedContributors ?: parseFile().also { cachedContributors = it }

        val contributors = json.decodeFromString<List<GithubContributorResponse>>(jsonText).mapNotNull { githubContributor ->
            if (githubContributor.login in majorContributorsLogins) return@mapNotNull null

            Contributor(
                name = githubContributor.login,
                description = R.string.contributors_community_member,
                title = Contributor.Title.Contributor,
                avatarUrl = githubContributor.avatarUrl,
                contributions = githubContributor.contributions,
                socials = listOf(
                    SocialButton(
                        link = githubContributor.htmlUrl,
                        icon = SocialButton.Icon.Github
                    )
                )
            )
        }

        return@withContext majorContributors + contributors.sortedByDescending { it.contributions }
    }

    private fun parseFile(): String {
        return context.assets.open("contributors.json")
            .bufferedReader()
            .use {
                it.readText()
            }
    }
}