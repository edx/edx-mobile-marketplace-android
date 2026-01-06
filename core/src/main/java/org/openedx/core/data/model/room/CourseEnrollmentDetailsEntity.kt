
package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.room.discovery.CertificateDb
import org.openedx.core.data.model.room.discovery.CourseAccessDetailsDb
import org.openedx.core.data.model.room.discovery.CourseSharingUtmParametersDb
import org.openedx.core.data.model.room.discovery.EnrollmentDetailsDB
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseInfoOverview
import org.openedx.core.domain.model.CourseMode
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.utils.TimeUtils

@Entity(tableName = "course_enrollment_details_table")
data class CourseEnrollmentDetailsEntity(
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @Embedded
    val courseAccessDetails: CourseAccessDetailsDb,
    @Embedded
    val certificate: CertificateDb?,
    @Embedded
    val enrollmentDetails: EnrollmentDetailsDB,
    @Embedded
    val courseInfoOverview: CourseInfoOverviewDB,
    @ColumnInfo("courseUpdates")
    val courseUpdates: String,
    @ColumnInfo("courseHandouts")
    val courseHandouts: String,
    @ColumnInfo("discussionUrl")
    val discussionUrl: String,
) {
    fun mapToDomain(): CourseEnrollmentDetails {
        return CourseEnrollmentDetails(
            id = id,
            courseUpdates = courseUpdates,
            courseHandouts = courseHandouts,
            discussionUrl = discussionUrl,
            courseAccessDetails = courseAccessDetails.mapToDomain(),
            certificate = certificate?.mapToDomain(),
            enrollmentDetails = enrollmentDetails.mapToDomain(),
            courseInfoOverview = courseInfoOverview.mapToDomain()
        )
    }
}

data class CourseInfoOverviewDB(
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("number")
    val number: String,
    @ColumnInfo("org")
    val org: String,
    @ColumnInfo("orgLogo")
    val orgLogo: String?,
    @ColumnInfo("start")
    val start: String?,
    @ColumnInfo("startDisplay")
    val startDisplay: String,
    @ColumnInfo("startType")
    val startType: String,
    @ColumnInfo("end")
    val end: String?,
    @ColumnInfo("isSelfPaced")
    val isSelfPaced: Boolean,
    @Embedded
    var media: MediaDb?,
    @Embedded
    val courseSharingUtmParameters: CourseSharingUtmParametersDb,
    @ColumnInfo("courseAbout")
    val courseAbout: String,
    @ColumnInfo("courseModes")
    val courseModes: List<CourseModeDB>?,
    @Embedded
    val productInfo: ProductInfoDb?,
) {
    fun mapToDomain(): CourseInfoOverview {
        return CourseInfoOverview(
            name = name,
            number = number,
            org = org,
            orgLogo = orgLogo ?: "",
            start = TimeUtils.iso8601ToDate(start ?: ""),
            startDisplay = startDisplay,
            startType = startType,
            end = TimeUtils.iso8601ToDate(end ?: ""),
            isSelfPaced = isSelfPaced,
            media = media?.mapToDomain(),
            courseSharingUtmParameters = courseSharingUtmParameters.mapToDomain(),
            courseAbout = courseAbout,
            courseModes = courseModes?.map { it.mapToDomain() },
            productInfo = productInfo?.mapToDomain(),
        )
    }
}

data class CourseModeDB(
    @ColumnInfo("slug")
    val slug: String?,
    @ColumnInfo("minPrice")
    val minPrice: Double?,
    @ColumnInfo("storeSku")
    var storeSku: String?,
) {
    fun mapToDomain(): CourseMode {
        return CourseMode(
            slug = slug,
            minPrice = minPrice,
            storeSku = storeSku,
        )
    }
}

data class ProductInfoDb(
    @ColumnInfo("storeSku")
    val storeSku: String,
    @ColumnInfo("lmsUSDPrice")
    val lmsUSDPrice: Double,
) {
    fun mapToDomain() = ProductInfo(
        storeSku = storeSku,
        lmsUSDPrice = lmsUSDPrice,
    )
}
