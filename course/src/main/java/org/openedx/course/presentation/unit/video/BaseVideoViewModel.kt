package org.openedx.course.presentation.unit.video

import org.openedx.core.BaseViewModel
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey

open class BaseVideoViewModel(
    val courseId: String,
    val blockId: String,
    private val courseAnalytics: CourseAnalytics,
) : BaseViewModel() {

    fun logVideoLoadedEvent(videoUrl: String) {
        logVideoEvent(
            event = CourseAnalyticsEvent.VIDEO_LOADED,
            params = buildMap {
                put(CourseAnalyticsKey.VIDEO_URL.key, videoUrl)
            }
        )
    }

    fun logVideoSpeedEvent(
        videoUrl: String,
        oldSpeed: Float,
        newSpeed: Float,
        currentVideoTime: Long,
        duration: Long,
    ) {
        logVideoEvent(
            event = CourseAnalyticsEvent.VIDEO_CHANGE_SPEED,
            params = buildMap {
                put(CourseAnalyticsKey.VIDEO_URL.key, videoUrl)
                put(CourseAnalyticsKey.OLD_SPEED.key, oldSpeed)
                put(CourseAnalyticsKey.NEW_SPEED.key, newSpeed)
                put(CourseAnalyticsKey.CURRENT_TIME.key, currentVideoTime)
                put(CourseAnalyticsKey.DURATION.key, duration)
            }
        )
    }

    fun logVideoCompletedEvent(
        videoUrl: String,
        duration: Long,
    ) {
        logVideoEvent(
            event = CourseAnalyticsEvent.VIDEO_COMPLETED,
            params = buildMap {
                put(CourseAnalyticsKey.VIDEO_URL.key, videoUrl)
                put(CourseAnalyticsKey.DURATION.key, duration)
            }
        )
    }

    fun logPlayPauseEvent(
        videoUrl: String,
        isPlaying: Boolean,
        currentVideoTime: Long = 0,
        duration: Long = 0,
    ) {
        logVideoEvent(
            event = if (isPlaying) CourseAnalyticsEvent.VIDEO_PLAYED else CourseAnalyticsEvent.VIDEO_PAUSED,
            params = buildMap {
                put(CourseAnalyticsKey.VIDEO_URL.key, videoUrl)
                if (currentVideoTime > 0) {
                    put(CourseAnalyticsKey.CURRENT_TIME.key, currentVideoTime)
                }
                if (duration > 0) {
                    put(CourseAnalyticsKey.DURATION.key, duration)
                }
            }
        )
    }

    private fun logVideoEvent(event: CourseAnalyticsEvent, params: Map<String, Any?>) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                put(CourseAnalyticsKey.CATEGORY.key, CourseAnalyticsKey.VIDEOS.key)
                putAll(params)
            }
        )
    }

    fun logCastConnection(event: CourseAnalyticsEvent) {
        courseAnalytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.PLAY_MEDIUM.key, CourseAnalyticsKey.GOOGLE_CAST.key)
            }
        )
    }
}
