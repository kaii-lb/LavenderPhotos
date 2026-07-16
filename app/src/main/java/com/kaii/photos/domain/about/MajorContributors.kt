package com.kaii.photos.domain.about

import com.kaii.photos.R

val majorContributors: List<ContributorItem>
    get() = listOf(
        ContributorItem.Separator(
            title = R.string.contributors_separator_developer
        ),
        ContributorItem.Contributor(
            name = "kaii-lb",
            description = R.string.contributor_kaii_desc,
            title = ContributorItem.Contributor.Title.Maintainer,
            avatarUrl = R.drawable.kaii,
            contributions = null,
            socials = listOf(
                SocialButton(
                    link = "https://github.com/kaii-lb",
                    icon = SocialButton.Icon.Github
                ),
                SocialButton(
                    link = "https://t.me/kaii_lb/",
                    icon = SocialButton.Icon.Telegram
                )
            )
        ),
        ContributorItem.Separator(
            title = R.string.contributors_separator_major
        ),
        ContributorItem.Contributor(
            name = "KeXxDumb",
            description = R.string.contributor_kexxdumb_desc,
            title = ContributorItem.Contributor.Title.MajorContributor,
            avatarUrl = R.drawable.kexxdumb,
            contributions = null,
            socials = listOf(
                SocialButton(
                    link = "https://github.com/KeXxDumb",
                    icon = SocialButton.Icon.Github
                )
            )
        ),
        ContributorItem.Contributor(
            name = "stacsk",
            description = R.string.contributor_stacsk_desc,
            title = ContributorItem.Contributor.Title.MajorContributor,
            avatarUrl = R.drawable.stacsk,
            contributions = null,
            socials = listOf(
                SocialButton(
                    link = "https://github.com/stacsk",
                    icon = SocialButton.Icon.Github
                )
            )
        ),
        ContributorItem.Contributor(
            name = "tyrypyrking",
            description = R.string.contributor_tyrypyrking_desc,
            title = ContributorItem.Contributor.Title.MajorContributor,
            avatarUrl = R.drawable.tyrypyrking,
            contributions = null,
            socials = listOf(
                SocialButton(
                    link = "https://github.com/tyrypyrking",
                    icon = SocialButton.Icon.Github
                )
            )
        ),
        ContributorItem.Separator(
            title = R.string.contributors_separator_contributors
        ),
    )