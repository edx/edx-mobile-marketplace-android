package org.openedx.notifications.domain.interactor


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.notifications.domain.model.CourseProgressEvent
import org.openedx.notifications.domain.model.ProgressEventType

/**
 * Analyzes course progress and generates notification events
 */
class CourseProgressAnalyzer {

    companion object {
        // Thresholds for progress notifications
        private const val THRESHOLD_25 = 0.25
        private const val THRESHOLD_50 = 0.50
        private const val THRESHOLD_75 = 0.75
        private const val THRESHOLD_100 = 1.0
    }

    /**
     * Analyzes course structure and generates progress events
     * Returns a flow of progress events that should trigger notifications
     */
    fun analyzeProgressChanges(
        courseId: String,
        courseName: String,
        courseStructure: CourseStructure,
        previousProgress: Map<String, Double>,
    ): Flow<CourseProgressEvent> = flow {
        // Calculate section and course completion
        val sectionProgress = calculateSectionProgress(courseStructure.blockData)
        val courseProgress = calculateCourseProgress(courseStructure.blockData)

        // Debug: Log progress info
        println("DEBUG_ANALYZER: Analyzing course=$courseId, blockData count=${courseStructure.blockData.size}")
        println("DEBUG_ANALYZER: Course progress=$courseProgress, sectionProgress=$sectionProgress")

        // Check for unit completion
        courseStructure.blockData.forEach { block ->
            val previousBlockProgress = previousProgress[block.id] ?: 0.0
            val currentBlockProgress = block.completion

            // Debug: Log block progress
            if (currentBlockProgress > previousBlockProgress) {
                println("DEBUG_ANALYZER: Block ${block.id} progress changed: $previousBlockProgress → $currentBlockProgress")
            }

            // Unit completed (use >= 0.99 instead of == 1.0 for flexibility)
            if (previousBlockProgress < 0.99 && currentBlockProgress >= 0.99) {
                println("DEBUG_ANALYZER: UNIT_COMPLETED: ${block.displayName}")
                emit(CourseProgressEvent(
                    courseId = courseId,
                    courseName = courseName,
                    blockId = block.id,
                    blockName = block.displayName,
                    eventType = ProgressEventType.UNIT_COMPLETED,
                    progressPercentage = 100.0
                ))
            }
        }

        // Check for section progress thresholds
        sectionProgress.forEach { (sectionId, progress) ->
            val previousSectionProgress = previousProgress[sectionId] ?: 0.0

            if (previousSectionProgress < THRESHOLD_25 && progress >= THRESHOLD_25) {
                emit(CourseProgressEvent(
                    courseId = courseId,
                    courseName = courseName,
                    blockId = sectionId,
                    blockName = "Section Progress",
                    eventType = ProgressEventType.SECTION_PROGRESS_THRESHOLD_25,
                    progressPercentage = (progress * 100).coerceIn(25.0, 99.9)
                ))
            }
            if (previousSectionProgress < THRESHOLD_50 && progress >= THRESHOLD_50) {
                emit(CourseProgressEvent(
                    courseId = courseId,
                    courseName = courseName,
                    blockId = sectionId,
                    blockName = "Section Progress",
                    eventType = ProgressEventType.SECTION_PROGRESS_THRESHOLD_50,
                    progressPercentage = (progress * 100).coerceIn(50.0, 99.9)
                ))
            }
            if (previousSectionProgress < THRESHOLD_75 && progress >= THRESHOLD_75) {
                emit(CourseProgressEvent(
                    courseId = courseId,
                    courseName = courseName,
                    blockId = sectionId,
                    blockName = "Section Progress",
                    eventType = ProgressEventType.SECTION_PROGRESS_THRESHOLD_75,
                    progressPercentage = (progress * 100).coerceIn(75.0, 99.9)
                ))
            }
            if (previousSectionProgress < THRESHOLD_100 && progress == THRESHOLD_100) {
                emit(CourseProgressEvent(
                    courseId = courseId,
                    courseName = courseName,
                    blockId = sectionId,
                    blockName = "Section Completed",
                    eventType = ProgressEventType.SECTION_PROGRESS_THRESHOLD_100,
                    progressPercentage = 100.0
                ))
            }
        }

        // Check for course progress thresholds
        val previousCourseProgress = previousProgress["course_progress"] ?: 0.0

        if (previousCourseProgress < THRESHOLD_25 && courseProgress >= THRESHOLD_25) {
            emit(CourseProgressEvent(
                courseId = courseId,
                courseName = courseName,
                blockId = courseId,
                blockName = courseName,
                eventType = ProgressEventType.COURSE_PROGRESS_THRESHOLD_25,
                progressPercentage = (courseProgress * 100).coerceIn(25.0, 99.9)
            ))
        }
        if (previousCourseProgress < THRESHOLD_50 && courseProgress >= THRESHOLD_50) {
            emit(CourseProgressEvent(
                courseId = courseId,
                courseName = courseName,
                blockId = courseId,
                blockName = courseName,
                eventType = ProgressEventType.COURSE_PROGRESS_THRESHOLD_50,
                progressPercentage = (courseProgress * 100).coerceIn(50.0, 99.9)
            ))
        }
        if (previousCourseProgress < THRESHOLD_75 && courseProgress >= THRESHOLD_75) {
            emit(CourseProgressEvent(
                courseId = courseId,
                courseName = courseName,
                blockId = courseId,
                blockName = courseName,
                eventType = ProgressEventType.COURSE_PROGRESS_THRESHOLD_75,
                progressPercentage = (courseProgress * 100).coerceIn(75.0, 99.9)
            ))
        }
        if (previousCourseProgress < THRESHOLD_100 && courseProgress == THRESHOLD_100) {
            emit(CourseProgressEvent(
                courseId = courseId,
                courseName = courseName,
                blockId = courseId,
                blockName = "$courseName - Completed!",
                eventType = ProgressEventType.COURSE_PROGRESS_THRESHOLD_100,
                progressPercentage = 100.0
            ))
        }
    }

    /**
     * Calculates overall section completion (average of subsection completions)
     */
    private fun calculateSectionProgress(blocks: List<Block>): Map<String, Double> {
        val sectionProgress = mutableMapOf<String, Double>()

        blocks.filter { it.type.name == "CHAPTER" || it.type.name == "SEQUENTIAL" }.forEach { block ->
            val childBlocks = blocks.filter { block.descendants.contains(it.id) }
            if (childBlocks.isNotEmpty()) {
                val avgCompletion = childBlocks.map { it.completion }.average()
                sectionProgress[block.id] = avgCompletion
            } else {
                sectionProgress[block.id] = block.completion
            }
        }

        return sectionProgress
    }

    /**
     * Calculates overall course completion percentage
     */
    private fun calculateCourseProgress(blocks: List<Block>): Double {
        if (blocks.isEmpty()) return 0.0

        // Filter to leaf blocks (units/verticals) to avoid double counting
        val verticalBlocks = blocks.filter { it.type.name == "VERTICAL" || it.type.name == "UNIT" }

        return if (verticalBlocks.isNotEmpty()) {
            verticalBlocks.map { it.completion }.average()
        } else {
            blocks.map { it.completion }.average()
        }
    }
}
