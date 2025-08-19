package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
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

    val isVerifiedMode: Boolean
        get() = EnrollmentMode.VERIFIED.toString().equals(mode, ignoreCase = true)

    val isUpgradeable: Boolean
        get() = isAuditMode &&
                course.isStarted &&
                course.isUpgradeDeadlinePassed.not() &&
                productInfo != null
}

fun EnrolledCourse.toPurchaseFlowData(iapFlow: IAPFlow, screenName: String): PurchaseFlowData {
    return PurchaseFlowData(
        iapFlow = iapFlow,
        screenName = screenName,
        courseId = course.id,
        courseName = course.name,
        orgName = course.org,
        orgLogo = course.orgLogo,
        courseExpiresDate = auditAccessExpires,
        isSelfPaced = course.isSelfPaced,
        productInfo = productInfo
    )
}
