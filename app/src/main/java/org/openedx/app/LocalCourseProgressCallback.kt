package org.openedx.app

import androidx.compose.runtime.staticCompositionLocalOf
import org.openedx.course.presentation.ui.CourseProgressCallback

// This creates a CompositionLocal key that our Composables can use to access the callback.
// We provide a default that does nothing to prevent crashes if it's not provided.
val LocalCourseProgressCallback = staticCompositionLocalOf<CourseProgressCallback> {
    object : CourseProgressCallback {
        override fun onCourseNotification(courseId: String, title: String, message: String) {}
        override fun scheduleCourseProgressNotification(courseId: String, courseName: String) {}
        override fun cancelCourseProgressNotification(courseId: String) {}
    }
}