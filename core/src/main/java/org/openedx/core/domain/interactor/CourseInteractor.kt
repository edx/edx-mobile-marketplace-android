package org.openedx.core.domain.interactor

import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadModel

interface CourseInteractor {
    suspend fun getCourseStructure(
        courseId: String,
        isNeedRefresh: Boolean = false
    ): CourseStructure

    suspend fun getCourseStructureFromCache(courseId: String): CourseStructure

    suspend fun getAllDownloadModels(): List<DownloadModel>
}
