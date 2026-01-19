package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.CourseEnrollmentDetailsEntity
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.extension.isNotNull
import java.util.Date

@Parcelize
data class CourseEnrollmentDetails(
    val id: String,
    val courseUpdates: String,
    val courseHandouts: String,
    val discussionUrl: String,
    val courseAccessDetails: CourseAccessDetails,
    val certificate: Certificate?,
    val enrollmentDetails: EnrollmentDetails,
    val courseInfoOverview: CourseInfoOverview,
) : Parcelable {

    val hasAccess: Boolean
        get() = courseAccessDetails.coursewareAccess?.hasAccess ?: false

    val isAuditAccessExpired: Boolean
        get() = courseAccessDetails.auditAccessExpires.isNotNull() &&
                Date().after(courseAccessDetails.auditAccessExpires)

    val isUpgradeable: Boolean
        get() = enrollmentDetails.isAuditMode &&
                courseInfoOverview.isStarted &&
                enrollmentDetails.isUpgradeDeadlinePassed.not() &&
                courseInfoOverview.productInfo.isNotNull()

    fun mapToRoomEntity() = CourseEnrollmentDetailsEntity(
        id = id,
        courseUpdates = courseUpdates,
        courseHandouts = courseHandouts,
        discussionUrl = discussionUrl,
        courseAccessDetails = courseAccessDetails.mapToRoomEntity(),
        certificate = certificate?.mapToRoomEntity(),
        enrollmentDetails = enrollmentDetails.mapToRoomEntity(),
        courseInfoOverview = courseInfoOverview.mapToRoomEntity(),
    )
}

enum class CourseAccessError {
    NONE, AUDIT_EXPIRED_NOT_UPGRADABLE, AUDIT_EXPIRED_UPGRADABLE, NOT_YET_STARTED, UNKNOWN
}

fun CourseEnrollmentDetails.toPurchaseFlowData(
    iapFlow: IAPFlow,
    screenName: String
): PurchaseFlowData {
    return PurchaseFlowData(
        iapFlow = iapFlow,
        screenName = screenName,
        courseId = this.id,
        courseName = courseInfoOverview.name,
        orgName = courseInfoOverview.org,
        orgLogo = courseInfoOverview.orgLogo,
        courseExpiresDate = courseAccessDetails.auditAccessExpires,
        isSelfPaced = courseInfoOverview.isSelfPaced,
        productInfo = courseInfoOverview.productInfo
    )
}
