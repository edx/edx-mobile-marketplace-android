package org.openedx.core.domain.model

sealed class CourseEnrollmentDetailsSource(open val data: CourseEnrollmentDetails) {
    data class Local(override val data: CourseEnrollmentDetails) :
        CourseEnrollmentDetailsSource(data)

    data class Remote(override val data: CourseEnrollmentDetails) :
        CourseEnrollmentDetailsSource(data)
}
