package com.kaii.photos.domain.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ContributorItem {
    data class Separator(
        @param:StringRes val title: Int
    ) : ContributorItem

    data class Contributor(
        val name: String,
        @param:StringRes val description: Int,
        val title: Title,
        @param:DrawableRes val avatarUrl: Int?,
        val contributions: Int?,
        val socials: List<SocialButton>
    ) : ContributorItem {
        enum class Title {
            Maintainer,
            MajorContributor,
            Contributor
        }
    }
}

@Serializable
data class GithubContributorResponse(
    val login: String,
    @SerialName("html_url") val htmlUrl: String,
    val contributions: Int
)