package org.openedx.course.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.openedx.core.ApiConstants
import org.openedx.core.data.api.CourseApi
import org.openedx.core.data.model.BlocksCompletionBody
import org.openedx.core.data.model.room.VideoProgressEntity
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseDatesResult
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseEnrollmentDetailsSource
import org.openedx.core.domain.model.CourseProgress
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.extension.channelFlowWithAwait
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.data.storage.CourseDao
import java.util.concurrent.ConcurrentHashMap

class CourseRepository(
    private val api: CourseApi,
    private val courseDao: CourseDao,
    private val downloadDao: DownloadDao,
    private val preferencesManager: CorePreferences,
    private val networkConnection: NetworkConnection,
) {
    private val courseStructure = mutableMapOf<String, CourseStructure>()
    private val needsRefresh = ConcurrentHashMap.newKeySet<String>()
    private val courseStatusMap = mutableMapOf<String, CourseComponentStatus>()
    private val courseDatesMap = mutableMapOf<String, CourseDatesResult>()

    suspend fun removeDownloadModel(id: String) {
        downloadDao.removeDownloadModel(id)
    }
    private val structureCache = CoalescingCache<String, CourseStructure>(
        fetch = { courseId ->
            val response = api.getCourseStructure(
                "stale-if-error=0",
                "v4",
                preferencesManager.user?.username,
                courseId
            )
            courseDao.insertCourseStructureEntity(response.mapToRoomEntity())
            response.mapToDomain()
        },
        persist = { courseId, _ -> needsRefresh.remove(courseId) }
    )

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
    private val progressCache = CoalescingCache<String, CourseProgress>(
        fetch = { courseId ->
            val response = api.getCourseProgress(courseId)
            courseDao.insertCourseProgressEntity(response.mapToRoomEntity(courseId))
            response.mapToDomain()
        }
    )
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
    suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        return structureCache.getCached(courseId)
            ?: courseDao.getCourseStructureById(courseId)?.mapToDomain()?.also {
                structureCache.setCached(courseId, it)
            }
            ?: throw NoCachedDataException()
    }

    suspend fun getEnrollmentDetailsFlow(
        courseId: String,
    ): Flow<CourseEnrollmentDetailsSource> = channelFlowWithAwait {
        var hasEnrollmentDetails = false
        getCourseEnrollmentDetailsFromCache(courseId)?.let {
            hasEnrollmentDetails = true
            trySend(CourseEnrollmentDetailsSource.Local(it))
        }

        if (networkConnection.isOnline()) {
            getEnrollmentDetails(courseId).let {
                courseDao.insertCourseEnrollmentDetailsEntity(it.mapToRoomEntity())
                hasEnrollmentDetails = true
                trySend(CourseEnrollmentDetailsSource.Remote(it))
            }
        }

        var throwable: Throwable? = null
        if (!hasEnrollmentDetails) {
            throwable = NoCachedDataException()
        }
        close(throwable)
    }

    private suspend fun getCourseEnrollmentDetailsFromCache(
        courseId: String,
    ): CourseEnrollmentDetails? {
        return courseDao.getCourseEnrollmentDetailsById(id = courseId)?.mapToDomain()
    }

    suspend fun getEnrollmentDetails(courseId: String): CourseEnrollmentDetails {
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

    fun getCourseProgress(
        courseId: String,
        isRefresh: Boolean,
        getOnlyCacheIfExist: Boolean
    ): Flow<CourseProgress> = flow {
        if (!isRefresh) {
            progressCache.getCached(courseId)?.let { emit(it) }
        }

        if (!isRefresh && progressCache.getCached(courseId) == null) {
            courseDao.getCourseProgressById(courseId)?.mapToDomain()?.let {
                progressCache.setCached(courseId, it)
                emit(it)
            }
        }

        val shouldRefresh = isRefresh || needsRefresh.contains(courseId)
        val hasCache = progressCache.getCached(courseId) != null
        val shouldFetch = shouldRefresh || !hasCache || !getOnlyCacheIfExist

        if (!networkConnection.isOnline() && !hasCache) {
            throw NoCachedDataException()
        }
        if (networkConnection.isOnline() && shouldFetch) {
            emit(progressCache.getOrFetch(courseId, forceRefresh = true))
        }
    }

    suspend fun getCourseDates(courseId: String) =
        api.getCourseDates(courseId).getCourseDatesResult()

    suspend fun resetCourseDates(courseId: String) =
        api.resetCourseDates(mapOf(ApiConstants.COURSE_KEY to courseId)).mapToDomain()

    suspend fun getHandouts(courseId: String) = api.getHandouts(courseId).mapToDomain()

    suspend fun getAnnouncements(courseId: String) =
        api.getAnnouncements(courseId).map { it.mapToDomain() }
    suspend fun getVideoProgress(blockId: String): VideoProgressEntity {
        return courseDao.getVideoProgressByBlockId(blockId)
            ?: VideoProgressEntity(blockId, "", null, null)
    }


    suspend fun getAllDownloadModels() = downloadDao.readAllDataNonFlow().map { it.mapToDomain() }

    suspend fun saveVideoProgress(
        blockId: String,
        videoUrl: String,
        videoTime: Long,
        duration: Long
    ) {
        val videoProgressEntity = VideoProgressEntity(blockId, videoUrl, videoTime, duration)
        courseDao.insertVideoProgressEntity(videoProgressEntity)
    }
}
