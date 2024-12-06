package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.CourseInfoOverviewDB
import org.openedx.core.data.model.room.CourseModeDB
import org.openedx.core.data.model.room.MediaDb
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.extension.isNotNullOrEmpty
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.CourseInfoOverview as DomainCourseInfoOverview

data class CourseInfoOverview(
    @SerializedName("name")
    val name: String,
    @SerializedName("number")
    val number: String,
    @SerializedName("org")
    val org: String,
    @SerializedName("start")
    val start: String?,
    @SerializedName("start_display")
    val startDisplay: String,
    @SerializedName("start_type")
    val startType: String,
    @SerializedName("end")
    val end: String?,
    @SerializedName("is_self_paced")
    val isSelfPaced: Boolean,
    @SerializedName("media")
    var media: Media?,
    @SerializedName("course_sharing_utm_parameters")
    val courseSharingUtmParameters: CourseSharingUtmParameters,
    @SerializedName("course_about")
    val courseAbout: String,
    @SerializedName("course_modes")
    val courseModes: List<CourseMode>?,
) {
    fun mapToDomain() = DomainCourseInfoOverview(
        name = name,
        number = number,
        org = org,
        start = TimeUtils.iso8601ToDate(start ?: ""),
        startDisplay = startDisplay,
        startType = startType,
        end = TimeUtils.iso8601ToDate(end ?: ""),
        isSelfPaced = isSelfPaced,
        media = media?.mapToDomain(),
        courseSharingUtmParameters = courseSharingUtmParameters.mapToDomain(),
        courseAbout = courseAbout,
        courseModes = courseModes?.map { it.mapToDomain() },
        productInfo = courseModes?.find {
            it.isVerifiedMode()
        }?.takeIf {
            it.androidSku.isNotNullOrEmpty() && it.storeSku.isNotNullOrEmpty()
        }?.run {
            ProductInfo(
                courseSku = androidSku!!,
                storeSku = storeSku!!,
                lmsUSDPrice = minPrice ?: 0.0
            )
        }
    )

    fun mapToRoomEntity(): CourseInfoOverviewDB {
        return CourseInfoOverviewDB(
            name = name,
            number = number,
            org = org,
            start = start ?: "",
            startDisplay = startDisplay,
            startType = startType,
            end = end ?: "",
            isSelfPaced = isSelfPaced,
            media = MediaDb.createFrom(media),
            courseSharingUtmParameters = courseSharingUtmParameters.mapToRoomEntity(),
            courseAbout = courseAbout,
            courseModes = courseModes?.map { CourseModeDB.createFrom(it.mapToDomain()) },
        )
    }
}
