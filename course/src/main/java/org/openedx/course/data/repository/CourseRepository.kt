package org.openedx.course.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openedx.core.ApiConstants
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.extension.channelFlowWithAwait
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.data.storage.CourseDao

class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val networkConnection: NetworkConnection,
) {
    private val courseStructure = mutableMapOf<String, CourseStructure>()

    private val courseStatusMap = mutableMapOf<String, CourseComponentStatus>()
    private val courseDatesMap = mutableMapOf<String, CourseDatesResult>()

    suspend fun removeDownloadModel(id: String) {
        downloadDao.removeDownloadModel(id)
    }

    fun getDownloadModels() = downloadDao.readAllData().map { list ->
        list.map { it.mapToDomain() }
    }

    suspend fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = true,
    ): Flow<CourseStructure> = channelFlowWithAwait {
        var hasCourseStructure = false
        val cachedCourseStructure =
            courseStructure[courseId] ?: courseDao.getCourseStructureById(courseId)?.mapToDomain()
        if (cachedCourseStructure != null) {
            hasCourseStructure = true
            trySend(cachedCourseStructure)
        }

        val fetchRemoteCourse = !hasCourseStructure || forceRefresh
        if (networkConnection.isOnline() && fetchRemoteCourse) {
            val response = api.getCourseStructure(
                cacheControlHeaderParam = ApiConstants.HEADER_STALE_IF_ERROR,
                blocksApiVersion = ApiConstants.BLOCKS_API_VERSION,
                username = preferencesManager.user?.username,
                courseId = courseId,
            )
            courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
            val courseDomainModel = response.mapToDomain()
            courseStructure[courseId] = courseDomainModel
            trySend(courseDomainModel)
            hasCourseStructure = true
        }

        if (!hasCourseStructure) {
            throw NoCachedDataException()
        }
    }

    suspend fun getCourseStructure(courseId: String, isNeedRefresh: Boolean): CourseStructure {
        if (!isNeedRefresh) courseStructure[courseId]?.let { return it }

        if (networkConnection.isOnline()) {
            val response = api.getCourseStructure(
                cacheControlHeaderParam = ApiConstants.HEADER_STALE_IF_ERROR,
                blocksApiVersion = ApiConstants.BLOCKS_API_VERSION,
                username = preferencesManager.user?.username,
                courseId = courseId
            )
            courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
            courseStructure[courseId] = response.mapToDomain()

        } else {
            val cachedCourseStructure = courseDao.getCourseStructureById(courseId)
            if (cachedCourseStructure != null) {
                courseStructure[courseId] = cachedCourseStructure.mapToDomain()
            } else {
                throw NoCachedDataException()
            }
        }

        return courseStructure[courseId]!!
    }

    suspend fun getEnrollmentDetailsFlow(
        courseId: String,
    ): Flow<CourseEnrollmentDetails> = channelFlowWithAwait {
        getCourseEnrollmentDetailsFromCache(courseId)?.let {
            trySend(it)
        }
        getEnrollmentDetails(courseId).let {
            courseDao.insertCourseEnrollmentDetailsEntity(it.mapToRoomEntity())
            trySend(it)
        }
    }

    private suspend fun getCourseEnrollmentDetailsFromCache(
        courseId: String,
    ): CourseEnrollmentDetails? {
        return courseDao.getCourseEnrollmentDetailsById(id = courseId)?.mapToDomain()
    }

    private suspend fun getEnrollmentDetails(courseId: String): CourseEnrollmentDetails {
        return api.getEnrollmentDetails(courseId = courseId).mapToDomain()
    }

    suspend fun getCourseStatusFlow(
        courseId: String,
    ): Flow<CourseComponentStatus> = channelFlowWithAwait {
        val localStatus = courseStatusMap[courseId]
        localStatus?.let { trySend(it) }

        if (networkConnection.isOnline()) {
            val username = preferencesManager.user?.username ?: ""
            val status = api.getCourseStatus(username, courseId).mapToDomain()
            courseStatusMap[courseId] = status
            trySend(status)
        } else {
            val status = localStatus ?: CourseComponentStatus("")
            trySend(status)
        }
    }

    suspend fun markBlocksCompletion(courseId: String, blocksId: List<String>) {
        val username = preferencesManager.user?.username ?: ""
        val blocksCompletionBody = BlocksCompletionBody(
            username,
            courseId,
            blocksId.associateWith { "1" }.toMap()
        )
        return api.markBlocksCompletion(blocksCompletionBody)
    }

    suspend fun getCourseDatesFlow(
        courseId: String,
    ): Flow<CourseDatesResult> = channelFlowWithAwait {
        val localDates = courseDatesMap[courseId]
        localDates?.let { trySend(it) }

        if (networkConnection.isOnline()) {
            val datesResult = api.getCourseDates(courseId).getCourseDatesResult()
            courseDatesMap[courseId] = datesResult
            trySend(datesResult)
        } else {
            val datesResult = localDates ?: CourseDatesResult(
                datesSection = linkedMapOf(),
                courseBanner = CourseDatesBannerInfo(
                    missedDeadlines = false,
                    missedGatedContent = false,
                    verifiedUpgradeLink = "",
                    contentTypeGatingEnabled = false,
                    hasEnded = false
                )
            )
            trySend(datesResult)
        }
    }

    suspend fun getCourseDates(courseId: String) =
        api.getCourseDates(courseId).getCourseDatesResult()

    suspend fun resetCourseDates(courseId: String) =
        api.resetCourseDates(mapOf(ApiConstants.COURSE_KEY to courseId)).mapToDomain()

    suspend fun getHandouts(courseId: String) = api.getHandouts(courseId).mapToDomain()

    suspend fun getAnnouncements(courseId: String) =
        api.getAnnouncements(courseId).map { it.mapToDomain() }
}
