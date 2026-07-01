package org.openedx.course.domain.interactor

import kotlinx.coroutines.flow.Flow
import org.openedx.core.BlockType
import org.openedx.core.domain.interactor.CourseInteractor
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseEnrollmentDetails
import org.openedx.core.domain.model.CourseEnrollmentDetailsSource
import org.openedx.core.domain.model.CourseStructure
import org.openedx.course.data.repository.CourseRepository

@Suppress("TooManyFunctions")
class CourseInteractor(
    private val repository: CourseRepository
) : CourseInteractor {

    suspend fun getCourseStructureFlow(
        courseId: String,
        forceRefresh: Boolean = true,
    ): Flow<CourseStructure?> {
        return repository.getCourseStructureFlow(courseId, forceRefresh)
    }

    override suspend fun getCourseStructure(
        courseId: String,
        isNeedRefresh: Boolean
    ): CourseStructure {
        return repository.getCourseStructure(courseId, isNeedRefresh)
    }
    override suspend fun getCourseStructureFromCache(courseId: String): CourseStructure {
        return repository.getCourseStructureFromCache(courseId)
    }
    suspend fun getEnrollmentDetailsFlow(courseId: String): Flow<CourseEnrollmentDetailsSource?> {
        return repository.getEnrollmentDetailsFlow(courseId)
    }

    suspend fun getEnrollmentDetails(courseId: String): CourseEnrollmentDetails {
        return repository.getEnrollmentDetails(courseId)
    }

    suspend fun getCourseStructureForVideos(
        courseId: String,
        isNeedRefresh: Boolean = false
    ): CourseStructure {
        val courseStructure = repository.getCourseStructure(courseId, isNeedRefresh)
        val blocks = courseStructure.blockData
        val videoBlocks = blocks.filter { it.type == BlockType.VIDEO }
        val resultBlocks = ArrayList<Block>()
        videoBlocks.forEach { videoBlock ->
            val verticalBlock = blocks.firstOrNull { it.descendants.contains(videoBlock.id) }
            if (verticalBlock != null) {
                val sequentialBlock =
                    blocks.firstOrNull { it.descendants.contains(verticalBlock.id) }
                if (sequentialBlock != null) {
                    val chapterBlock =
                        blocks.firstOrNull { it.descendants.contains(sequentialBlock.id) }
                    if (chapterBlock != null) {
                        resultBlocks.add(videoBlock)
                        val verticalIndex = resultBlocks.indexOfFirst { it.id == verticalBlock.id }
                        if (verticalIndex == -1) {
                            resultBlocks.add(verticalBlock.copy(descendants = listOf(videoBlock.id)))
                        } else {
                            val block = resultBlocks[verticalIndex]
                            resultBlocks[verticalIndex] =
                                block.copy(descendants = block.descendants + videoBlock.id)
                        }
                        if (!resultBlocks.contains(sequentialBlock)) {
                            resultBlocks.add(sequentialBlock)
                        }
                        if (!resultBlocks.contains(chapterBlock)) {
                            resultBlocks.add(chapterBlock)
                        }
                    }
                }

            }
        }
        return courseStructure.copy(blockData = resultBlocks.toList())
    }

    suspend fun getCourseStatusFlow(courseId: String) = repository.getCourseStatusFlow(courseId)

    suspend fun getCourseDatesFlow(courseId: String) = repository.getCourseDatesFlow(courseId)

    suspend fun getCourseDates(courseId: String) = repository.getCourseDates(courseId)

    suspend fun resetCourseDates(courseId: String) = repository.resetCourseDates(courseId)

    suspend fun getHandouts(courseId: String) = repository.getHandouts(courseId)

    suspend fun getAnnouncements(courseId: String) = repository.getAnnouncements(courseId)

    suspend fun removeDownloadModel(id: String) = repository.removeDownloadModel(id)

    fun getDownloadModels() = repository.getDownloadModels()
    override suspend fun getAllDownloadModels() = repository.getAllDownloadModels()
    fun getCourseProgress(courseId: String, isRefresh: Boolean, getOnlyCacheIfExist: Boolean) =
        repository.getCourseProgress(courseId, isRefresh, getOnlyCacheIfExist)

    suspend fun getVideoProgress(blockId: String) = repository.getVideoProgress(blockId)
}
