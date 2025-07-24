package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.Media
import org.openedx.core.domain.model.BannerImage as DomainBannerImage
import org.openedx.core.domain.model.CourseImage as DomainCourseImage
import org.openedx.core.domain.model.CourseVideo as DomainCourseVideo
import org.openedx.core.domain.model.Image as DomainImage

data class Media(
    @SerializedName("banner_image")
    val bannerImage: BannerImage?,
    @SerializedName("course_image")
    val courseImage: CourseImage?,
    @SerializedName("course_video")
    val courseVideo: CourseVideo?,
    @SerializedName("image")
    val image: Image?,
) {

    fun mapToDomain(): Media {
        return Media(
            bannerImage = bannerImage?.mapToDomain(),
            courseImage = courseImage?.mapToDomain(),
            courseVideo = courseVideo?.mapToDomain(),
            image = image?.mapToDomain()
        )
    }

}

data class Image(
    @SerializedName("large")
    val large: String?,
    @SerializedName("raw")
    val raw: String?,
    @SerializedName("small")
    val small: String?,
) {
    fun mapToDomain(): DomainImage {
        return DomainImage(
            large = large ?: "",
            raw = raw ?: "",
            small = small ?: ""
        )
    }
}

data class CourseVideo(
    @SerializedName("uri")
    val uri: String?,
) {
    fun mapToDomain(): DomainCourseVideo {
        return DomainCourseVideo(
            uri = uri ?: ""
        )
    }
}

data class CourseImage(
    @SerializedName("uri")
    val uri: String?,
    @SerializedName("name")
    val name: String?,
) {
    fun mapToDomain(): DomainCourseImage {
        return DomainCourseImage(
            uri = uri ?: "",
            name = name ?: ""
        )
    }
}

data class BannerImage(
    @SerializedName("uri")
    val uri: String?,
    @SerializedName("uri_absolute")
    val uriAbsolute: String?,
) {
    fun mapToDomain(): DomainBannerImage {
        return DomainBannerImage(
            uri = uri ?: "",
            uriAbsolute = uriAbsolute ?: ""
        )
    }
}
