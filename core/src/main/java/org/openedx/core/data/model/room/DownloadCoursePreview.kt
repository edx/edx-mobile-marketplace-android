package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.domain.model.DownloadCoursePreview as DomainDownloadCoursePreview

@Entity(tableName = "download_course_preview_table")
data class DownloadCoursePreview(
    @PrimaryKey
    @ColumnInfo("course_id")
    val id: String,
    @ColumnInfo("course_name")
    val name: String?,
    @ColumnInfo("course_image")
    val image: String?,
    @ColumnInfo("total_size")
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
}
