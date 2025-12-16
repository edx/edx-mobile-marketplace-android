package org.openedx.downloads.domain.interactor

import org.openedx.downloads.data.repository.DownloadRepository

class DownloadInteractor(
    private val repository: DownloadRepository
) {
    fun getDownloadCoursesPreview(refresh: Boolean) = repository.getDownloadCoursesPreview(refresh)

    suspend fun getDownloadModelsByCourseIds(courseId: String) =
        repository.getDownloadModelsByCourseIds(courseId)

    suspend fun getCourseStructureFromCache(courseId: String) =
        repository.getCourseStructureFromCache(courseId)

    suspend fun getCourseStructure(courseId: String) = repository.getCourseStructure(courseId)
}
