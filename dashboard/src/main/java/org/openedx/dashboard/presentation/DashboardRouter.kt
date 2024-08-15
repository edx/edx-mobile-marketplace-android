package org.openedx.dashboard.presentation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String,
        openTab: String = "",
        resumeBlockId: String = ""
    )

    fun navigateToAllEnrolledCourses(fm: FragmentManager)

    fun getProgramFragment(): Fragment
}
