package org.openedx.core.domain.model

import android.os.Parcelable
import com.google.gson.internal.bind.util.ISO8601Utils
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.CourseInfoOverviewDB
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.data.model.room.CourseInfoOverviewDb
import java.util.Date

@Parcelize
data class CourseInfoOverview(
    val name: String,
    val number: String,
    val org: String,
    val orgLogo: String,
    val start: Date?,
    val startDisplay: String?,
    val startDisplay: String,
    val startType: String,
    val end: Date?,
    val isSelfPaced: Boolean,
    var media: Media?,
    val courseSharingUtmParameters: CourseSharingUtmParameters,
    val courseAbout: String,
    val courseModes: List<CourseMode>?,
    val productInfo: ProductInfo?
) : Parcelable {

    val isStarted: Boolean
        get() = start?.before(Date()) ?: false

    fun mapToEntity() = CourseInfoOverviewDb(
    fun mapToRoomEntity() = CourseInfoOverviewDB(
        name = name,
        number = number,
        org = org,
        start = start,
        startDisplay = startDisplay ?: "",
        orgLogo = orgLogo,
        start = start?.let { ISO8601Utils.format(it) },
        startDisplay = startDisplay,
        startType = startType,
        end = end,
        end = end?.let { ISO8601Utils.format(it) },
        isSelfPaced = isSelfPaced,
        media = media?.mapToEntity(),
        courseSharingUtmParameters = courseSharingUtmParameters.mapToEntity(),
        courseAbout = courseAbout
        media = media?.mapToRoomEntity(),
        courseSharingUtmParameters = courseSharingUtmParameters.mapToRoomEntity(),
        courseAbout = courseAbout,
        courseModes = courseModes?.map { it.mapToRoomEntity() } ?: emptyList(),
        productInfo = productInfo?.mapToRoomEntity(),
    )
}
