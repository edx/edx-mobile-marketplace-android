package org.openedx.downloads.data.repository

import kotlinx.coroutines.flow.flow
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.CourseDao
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.module.db.DownloadDao

class DownloadRepository(
    private val api: CourseApi,
    private val dao: DownloadDao,
    private val courseDao: CourseDao,
    private val corePreferences: CorePreferences,
) {
    fun getDownloadCoursesPreview(refresh: Boolean) = flow {
        if (!refresh) {
            val cachedDownloadCoursesPreview = dao.getDownloadCoursesPreview()
            emit(cachedDownloadCoursesPreview.map { it.mapToDomain() })
        }
        val username = corePreferences.user?.username ?: ""
        val response = api.getDownloadCoursesPreview(username)
        val downloadCoursesPreview = response.map { it.mapToDomain() }
        emit(downloadCoursesPreview)
        val downloadCoursesPreviewEntity = response.map { it.mapToRoomEntity() }
        dao.insertDownloadCoursePreview(downloadCoursesPreviewEntity)
    }

    suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        val cachedCourseStructure = courseDao.getCourseStructureById(courseId)
        if (cachedCourseStructure != null) {
            return cachedCourseStructure.mapToDomain()
        } else {
            throw NoCachedDataException()
        }
    }

    suspend fun getCourseStructure(courseId: String): CourseStructure {
        try {
            val response = api.getCourseStructure(
                cacheControlHeaderParam = "stale-if-error=0",
                blocksApiVersion = "v4",
                username = corePreferences.user?.username,
                courseId = courseId
            )
            courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
            return response.mapToDomain()
        } catch (_: Exception) {
            return getCourseStructureFromCache(courseId)
        }
    }

    suspend fun getDownloadModelsByCourseIds(courseId: String) =
        dao.getDownloadModelsByCourseIds(courseId).map { it.mapToDomain() }
}
