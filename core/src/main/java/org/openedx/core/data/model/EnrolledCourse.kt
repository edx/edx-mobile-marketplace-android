package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.EnrolledCourseEntity
import org.openedx.core.data.model.room.discovery.ProgressDb
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrollmentMode
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.Progress as ProgressDomain

data class EnrolledCourse(
    @SerializedName("audit_access_expires")
    val auditAccessExpires: String?,
    @SerializedName("created")
    val created: String?,
    @SerializedName("mode")
    val mode: String?,
    @SerializedName("is_active")
    val isActive: Boolean?,
    @SerializedName("course")
    val course: EnrolledCourseData?,
    @SerializedName("certificate")
    val certificate: Certificate?,
    @SerializedName("course_progress")
    val progress: Progress?,
    @SerializedName("course_status")
    val courseStatus: CourseStatus?,
    @SerializedName("course_assignments")
    val courseAssignments: CourseAssignments?,
    @SerializedName("course_modes")
    val courseModes: List<CourseMode>?,
) {
    fun mapToDomain(): EnrolledCourse {
        return EnrolledCourse(
            auditAccessExpires = TimeUtils.iso8601ToDate(auditAccessExpires ?: ""),
            created = created ?: "",
            mode = mode ?: "",
            isActive = isActive ?: false,
            course = course?.mapToDomain()!!,
            certificate = certificate?.mapToDomain(),
            progress = progress?.mapToDomain() ?: ProgressDomain.DEFAULT_PROGRESS,
            courseStatus = courseStatus?.mapToDomain(),
            courseAssignments = courseAssignments?.mapToDomain(),
            productInfo = courseModes?.find {
                EnrollmentMode.VERIFIED.toString().equals(it.slug, ignoreCase = true)
            }?.takeIf {
                it.androidSku.isNullOrEmpty().not() && it.storeSku.isNullOrEmpty().not()
            }?.run {
                ProductInfo(
                    courseSku = androidSku!!,
                    storeSku = storeSku!!,
                    lmsUSDPrice = minPrice ?: 0.0
                )
            }
        )
    }

    fun mapToRoomEntity(): EnrolledCourseEntity {
        return EnrolledCourseEntity(
            courseId = course?.id ?: "",
            auditAccessExpires = auditAccessExpires ?: "",
            created = created ?: "",
            mode = mode ?: "",
            isActive = isActive ?: false,
            course = course?.mapToRoomEntity()!!,
            certificate = certificate?.mapToRoomEntity(),
            progress = progress?.mapToRoomEntity() ?: ProgressDb.DEFAULT_PROGRESS,
            courseStatus = courseStatus?.mapToRoomEntity(),
            courseAssignments = courseAssignments?.mapToRoomEntity()
        )
    }

    fun setStoreSku(storeProductPrefix: String) {
        courseModes?.forEach {
            it.setStoreProductSku(storeProductPrefix)
        }
    }
}
