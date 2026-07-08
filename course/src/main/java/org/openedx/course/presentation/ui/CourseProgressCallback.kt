package org.openedx.course.presentation.ui

interface CourseProgressCallback {

    /**
     * Shows an immediate, generic notification (e.g., "Course Started").
     */
    fun onCourseNotification(courseId: String, title: String, message: String)

    /**
     * Schedules a 30-second reminder notification for when a user leaves a course.
     */
    fun scheduleCourseProgressNotification(courseId: String, courseName: String)

    /**
     * Cancels a pending 30-second reminder notification.
     */
    fun cancelCourseProgressNotification(courseId: String)
}