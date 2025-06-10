package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.BannerImageDb
import org.openedx.core.data.model.room.CourseImageDb
import org.openedx.core.data.model.room.CourseVideoDb
import org.openedx.core.data.model.room.ImageDb
import org.openedx.core.data.model.room.MediaDb

@Parcelize
data class Media(
    val bannerImage: BannerImage? = null,
    val courseImage: CourseImage? = null,
    val courseVideo: CourseVideo? = null,
    val image: Image? = null
) : Parcelable {

    fun mapToRoomEntity() = MediaDb(
        bannerImage = bannerImage?.mapToRoomEntity(),
        courseImage = courseImage?.mapToRoomEntity(),
        courseVideo = courseVideo?.mapToRoomEntity(),
        image = image?.mapToRoomEntity(),
    )
}

@Parcelize
data class Image(
    val large: String,
    val raw: String,
    val small: String
) : Parcelable {

    fun mapToRoomEntity() = ImageDb(large, raw, small)
}

@Parcelize
data class CourseVideo(
    val uri: String
) : Parcelable {

    fun mapToRoomEntity() = CourseVideoDb(uri)
}

@Parcelize
data class CourseImage(
    val uri: String,
    val name: String
) : Parcelable {

    fun mapToRoomEntity() = CourseImageDb(uri, name)
}

@Parcelize
data class BannerImage(
    val uri: String,
    val uriAbsolute: String
) : Parcelable {

    fun mapToRoomEntity() = BannerImageDb(uri, uriAbsolute)
}
