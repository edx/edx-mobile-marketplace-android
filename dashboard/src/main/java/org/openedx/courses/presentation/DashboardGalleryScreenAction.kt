package org.openedx.courses.presentation

import org.openedx.core.domain.model.EnrolledCourse

interface DashboardGalleryScreenAction {
    object SwipeRefresh : DashboardGalleryScreenAction

    object Reload : DashboardGalleryScreenAction

    object NavigateToDiscovery : DashboardGalleryScreenAction

    data class ViewAll(
        val isCardClicked: Boolean,
    ) : DashboardGalleryScreenAction

    data class OpenBlock(
        val enrolledCourse: EnrolledCourse,
        val blockId: String,
        val source: ActionSource,
    ) : DashboardGalleryScreenAction

    data class OpenCourse(
        val enrolledCourse: EnrolledCourse,
        val isPrimaryCourse: Boolean,
        val source: ActionSource,
    ) : DashboardGalleryScreenAction

    data class NavigateToDates(
        val enrolledCourse: EnrolledCourse,
        val source: ActionSource,
    ) : DashboardGalleryScreenAction
}

enum class ActionSource {
    PAST_ASSIGNMENT, UPCOMING_ASSIGNMENT, RESUME_BLOCK, START_COURSE, CARD
}
