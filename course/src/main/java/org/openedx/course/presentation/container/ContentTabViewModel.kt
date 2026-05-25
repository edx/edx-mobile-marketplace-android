package org.openedx.course.presentation.container

import org.openedx.core.BaseViewModel
import org.openedx.core.system.ResourceManager
import org.openedx.course.presentation.CourseAnalytics

class ContentTabViewModel(
    val courseId: String,
    private val courseTitle: String,
    private val analytics: CourseAnalytics,
    resourceManager: ResourceManager,
) : BaseViewModel() {

    fun logTabClickEvent(contentTab: CourseContentTab) {
       /* analytics.logEvent(
            CourseAnalyticsEvent.COURSE_CONTENT_TAB_CLICK.eventName,
            buildMap {
                put(
                    CourseAnalyticsKey.NAME.key,
                    CourseAnalyticsEvent.COURSE_CONTENT_TAB_CLICK.biValue
                )
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COURSE_NAME.key, courseTitle)
                put(CourseAnalyticsKey.TAB_NAME.key, contentTab.name)
            }
        )*/
    }
}
