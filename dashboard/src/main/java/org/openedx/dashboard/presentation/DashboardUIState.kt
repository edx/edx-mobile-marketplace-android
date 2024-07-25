package org.openedx.dashboard.presentation

import org.openedx.core.domain.model.EnrolledCourse

sealed class DashboardUIState {
    data class Courses(val courses: List<EnrolledCourse>, val isIAPEnabled: Boolean) :
        DashboardUIState()

    object Empty : DashboardUIState()
    object Loading : DashboardUIState()
}
