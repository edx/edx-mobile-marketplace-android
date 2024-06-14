package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.domain.ProductInfo
import java.util.Date

@Parcelize
data class EnrolledCourse(
    val auditAccessExpires: Date?,
    val created: String,
    val mode: String,
    val isActive: Boolean,
    val course: EnrolledCourseData,
    val certificate: Certificate?,
    val progress: Progress,
    val courseStatus: CourseStatus?,
    val courseAssignments: CourseAssignments?,
    val productInfo: ProductInfo?,
) : Parcelable {

    private val isAuditMode: Boolean
        get() = EnrollmentMode.AUDIT.toString().equals(mode, ignoreCase = true)
    val isUpgradeable: Boolean
        get() = isAuditMode &&
                course.isStarted &&
                course.isUpgradeDeadlinePassed.not() &&
                productInfo != null
}

/**
 * Method to filter the audit courses from the given enrolled course list.
 *
 * @return the list of all audit courses with non-null Skus.
 */
fun List<EnrolledCourse>.getAuditCourses(): List<EnrolledCourse> {
    return this.filter { it.isUpgradeable }.toList()
}
