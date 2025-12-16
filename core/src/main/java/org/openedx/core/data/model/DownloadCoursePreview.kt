package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.DownloadCoursePreview as EntityDownloadCoursePreview
import org.openedx.core.domain.model.DownloadCoursePreview as DomainDownloadCoursePreview

data class DownloadCoursePreview(
    @SerializedName("course_id")
    val id: String,
    @SerializedName("course_name")
    val name: String?,
    @SerializedName("course_image")
    val image: String?,
    @SerializedName("total_size")
    val totalSize: Long?,
) {
    fun mapToDomain(): DomainDownloadCoursePreview {
        return DomainDownloadCoursePreview(
            id = id,
            name = name ?: "",
            image = image ?: "",
            totalSize = totalSize ?: 0,
        )
    }

    fun mapToRoomEntity(): EntityDownloadCoursePreview {
        return EntityDownloadCoursePreview(
            id = id,
            name = name,
            image = image,
            totalSize = totalSize,
        )
    }
}
