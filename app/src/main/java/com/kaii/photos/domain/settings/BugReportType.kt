package com.kaii.photos.domain.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kaii.photos.R

enum class BugReportType(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    @param:DrawableRes val icon: Int,
    val url: String
) {
    BugReport(
        title = R.string.debugging_report_issue_bug,
        summary = R.string.debugging_report_issue_bug_desc,
        icon = R.drawable.bug_report,
        url = "https://github.com/kaii-lb/LavenderPhotos/issues/new?template=bug_report.md"
    ),

    FeatureRequest(
        title = R.string.debugging_report_issue_feature,
        summary = R.string.debugging_report_issue_feature_desc,
        icon = R.drawable.light,
        url = "https://github.com/kaii-lb/LavenderPhotos/issues/new?template=feature_request.md"
    ),

    Other(
        title = R.string.debugging_report_issue_other,
        summary = R.string.debugging_report_issue_other_desc,
        icon = R.drawable.info,
        url = "https://github.com/kaii-lb/LavenderPhotos/issues/new?template=BLANK_ISSUE"
    )
}