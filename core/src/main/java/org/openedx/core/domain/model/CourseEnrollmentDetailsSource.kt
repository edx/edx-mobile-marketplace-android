package org.openedx.core.domain.model

sealed class CourseEnrollmentDetailsSource(open val data: CourseEnrollmentDetails) {
    data class Cache(override val data: CourseEnrollmentDetails) :
        CourseEnrollmentDetailsSource(data)

    data class Server(override val data: CourseEnrollmentDetails) :
        CourseEnrollmentDetailsSource(data)
}
