package com.kaii.photos.compose.widgets.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.Placeholder
import com.bumptech.glide.integration.compose.placeholder
import com.kaii.photos.R
import com.kaii.photos.domain.about.ContributorItem.Contributor
import com.kaii.photos.domain.about.SocialButton
import com.kaii.photos.presentation.ui.ColorCreator
import com.kaii.photos.presentation.ui.theme.ThemeConfiguration
import com.kaii.photos.ui.theme.PhotosTheme

@Preview
@Composable
private fun ContributorItemPreview() {
    PhotosTheme(theme = ThemeConfiguration.Default) {
        ContributorItem(
            name = "kaii-lb",
            avatarUrl = "https://avatars.githubusercontent.com/u/70664258?v=4",
            title = Contributor.Title.Maintainer,
            description = "Building apps that redefine what's possible, without compromising safety and privacy",
            socials = listOf(
                SocialButton(
                    link = "https://github.com/kaii-lb",
                    icon = SocialButton.Icon.Github
                )
            ),
            openLink = {}
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ContributorItem(
    name: String,
    avatarUrl: Any,
    title: Contributor.Title,
    description: String,
    socials: List<SocialButton>,
    modifier: Modifier = Modifier,
    openLink: (link: String) -> Unit
) {
    val colorCreator = remember {
        ColorCreator()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 32.dp))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.Start
            )
        ) {
            GlideImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                loading = avatarPlaceholder(name, colorCreator),
                failure = avatarPlaceholder(name, colorCreator),
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.Start
            )
        ) {
            Text(
                text = stringResource(
                    id = when (title) {
                        Contributor.Title.Maintainer -> R.string.contributor_maintainer
                        Contributor.Title.MajorContributor -> R.string.contributor_major
                        Contributor.Title.Contributor -> R.string.contributor
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .offset(y = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            socials.forEach { social ->
                IconButton(
                    onClick = {
                        openLink(social.link)
                    },
                    shapes = IconButtonDefaults.shapes(
                        shape = CircleShape,
                        pressedShape = RoundedCornerShape(size = 10.dp)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = social.icon.drawable),
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun avatarPlaceholder(
    name: String,
    colorCreator: ColorCreator
): Placeholder {
    return placeholder {
        val color = remember { colorCreator.generateColor() }
        val onColor = remember { colorCreator.onColorFor(color) }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color)
                .padding(all = 12.dp)
        ) {
            Text(
                text = name[0].uppercase(),
                color = onColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .wrapContentSize()
            )
        }
    }
}