package org.openedx.courses.presentation

import org.openedx.core.domain.model.EnrolledCourse

interface DashboardGalleryScreenAction {
    object SwipeRefresh : DashboardGalleryScreenAction
    object Reload : DashboardGalleryScreenAction
    object NavigateToDiscovery : DashboardGalleryScreenAction
    data class ViewAll(val isCardClicked: Boolean) : DashboardGalleryScreenAction
    data class OpenBlock(
        val enrolledCourse: EnrolledCourse,
        val blockId: String,
        val blockType: BlockType,
    ) : DashboardGalleryScreenAction

    data class OpenCourse(
        val enrolledCourse: EnrolledCourse,
        val isPrimaryCourse: Boolean,
    ) : DashboardGalleryScreenAction

    data class NavigateToDates(
        val enrolledCourse: EnrolledCourse,
        val blockType: BlockType,
    ) : DashboardGalleryScreenAction
}
