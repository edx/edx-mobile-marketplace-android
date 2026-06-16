package org.openedx.notifications.utils

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.openedx.core.domain.model.CourseStructure
import org.openedx.notifications.domain.interactor.CourseProgressAnalyzer

/**
 * Main manager for course progress notifications
 * Monitors course progress and triggers notifications based on defined conditions
 * Handles both foreground and background scenarios
 */
class CourseProgressNotificationManager(
    private val context: Context,
) {

    private val progressAnalyzer = CourseProgressAnalyzer()
    private val managerScope = CoroutineScope(Dispatchers.Default)

    // Cache to track previous progress state (in-memory LRU cache for 10 courses)
    private val progressCache = LruCache<String, Map<String, Double>>(10)

    companion object {
        private const val TAG = "CourseProgressNotifier"
    }

    init {
        // Schedule background work for periodic checks (inactivity reminders, etc.)
    }

    /**
     * Called when a course is viewed to record activity
     * Should be called from CourseOutlineViewModel or similar
     */
    fun onCourseViewed(courseId: String) {
    }

    /**
     * Main entry point: Call this when course structure/progress is updated
     * Analyzes changes and triggers appropriate notifications
     * Works across app lifecycle (foreground and background)
     */
    fun checkAndNotifyProgressUpdates(
        courseId: String,
        courseName: String,
        courseStructure: CourseStructure
    ) {
        println("DEBUG_MANAGER: checkAndNotifyProgressUpdates called for course=$courseId")
        managerScope.launch {
            try {
                // Get previous progress state from cache
                val previousProgress = progressCache.get(courseId) ?: emptyMap()

                println("DEBUG_MANAGER: Previous progress: $previousProgress")

                // Analyze progress changes
                progressAnalyzer.analyzeProgressChanges(
                    courseId = courseId,
                    courseName = courseName,
                    courseStructure = courseStructure,
                    previousProgress = previousProgress
                ).collect { event ->
                    println("DEBUG_MANAGER: Event collected: ${event.eventType}")
                    // Check if this notification hasn't been sent before (duplicate prevention)
                    val alreadySent = notificationPreferences.hasNotificationBeenSent(
                        courseId = event.courseId,
                        blockId = event.blockId,
                        eventType = event.eventType
                    )
                    println("DEBUG_MANAGER: Duplicate check - alreadySent=$alreadySent")

                    if (!alreadySent) {
                        // Send the notification
                        println("DEBUG_MANAGER: Sending notification for ${event.eventType}")
                        val notificationId = notificationSender.sendNotification(event)
                        println("DEBUG_MANAGER: Notification sent with ID=$notificationId")

                        // Mark as sent to prevent duplicates
                        notificationPreferences.markNotificationAsSent(
                            courseId = event.courseId,
                            blockId = event.blockId,
                            eventType = event.eventType
                        )
                    } else {
                        println("DEBUG_MANAGER: Skipping duplicate notification")
                    }
                }

                // Update cache with current progress
                val currentProgress = buildProgressMap(courseStructure)
                progressCache.put(courseId, currentProgress)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Clear progress cache for a course (useful when resetting course)
     */
    fun clearCourseCache(courseId: String) {
        progressCache.remove(courseId)
    }

    /**
     * Cleanup resources
     */
    fun shutdown() {
        progressCache.evictAll()
    }

    /**
     * Builds a progress map from course structure for comparison
     */
    private fun buildProgressMap(courseStructure: CourseStructure): Map<String, Double> {
        val map = mutableMapOf<String, Double>()

        courseStructure.blockData.forEach { block ->
            map[block.id] = block.completion
        }

        // Add overall course progress
        val courseProgress = if (courseStructure.blockData.isNotEmpty()) {
            val verticalBlocks = courseStructure.blockData.filter {
                it.type.name == "VERTICAL" || it.type.name == "UNIT"
            }
            if (verticalBlocks.isNotEmpty()) {
                verticalBlocks.map { it.completion }.average()
            } else {
                courseStructure.blockData.map { it.completion }.average()
            }
        } else {
            0.0
        }
        map["course_progress"] = courseProgress

        return map
    }

    /**
     * Manually trigger a progress check (useful for testing or forced updates)
     */
    fun forceProgressCheck(
        courseId: String,
        courseName: String,
        courseStructure: CourseStructure
    ) {
        // Clear previous progress to force new notifications
        progressCache.remove(courseId)
        checkAndNotifyProgressUpdates(courseId, courseName, courseStructure)
    }
}
