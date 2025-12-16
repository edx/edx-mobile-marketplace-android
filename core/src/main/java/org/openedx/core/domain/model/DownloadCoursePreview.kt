package org.openedx.core.domain.model

data class DownloadCoursePreview(
    val id: String,
    val name: String,
    val image: String,
    val totalSize: Long,
)
