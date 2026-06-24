package com.kaii.photos.domain.about

import com.kaii.photos.R

data class SocialButton(
    val link: String,
    val icon: Icon
) {
    enum class Icon(val drawable: Int) {
        Github(drawable = R.drawable.github_invertocat_black_clearspace),
        Telegram(drawable = R.drawable.telegram_logo)
    }
}
