package org.openedx.downloads.presentation

import androidx.fragment.app.FragmentManager

interface DownloadsRouter {

    fun navigateToSettings(fm: FragmentManager)

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
    )
}
