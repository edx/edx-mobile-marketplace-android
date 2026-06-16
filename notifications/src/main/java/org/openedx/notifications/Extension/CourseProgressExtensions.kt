package org.openedx.notifications.Extension


import org.openedx.core.domain.model.CourseStructure
import org.openedx.notifications.utils.CourseProgressNotificationManager

/**
 * Extension functions for integrating course progress notifications with course UI
 * These helpers make it easy to integrate into existing course modules
 */

/**
 * Convenience function to check and notify progress updates
 * Call this from CourseOutlineViewModel whenever course data is loaded/refreshed
 */
fun CourseProgressNotificationManager.onCourseDataUpdated(
    courseId: String,
    courseName: String,
    courseStructure: CourseStructure?
) {
    if (courseStructure != null) {
        checkAndNotifyProgressUpdates(
            courseId = courseId,
            courseName = courseName,
            courseStructure = courseStructure
        )
    }
}
