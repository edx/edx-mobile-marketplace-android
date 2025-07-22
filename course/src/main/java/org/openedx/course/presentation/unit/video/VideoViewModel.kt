package org.openedx.course.presentation.unit.video

import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.core.utils.Logger
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.presentation.CourseAnalytics

class VideoViewModel(
    courseId: String,
    blockId: String,
    private val courseRepository: CourseRepository,
    private val notifier: CourseNotifier,
    private val preferencesManager: CorePreferences,
    courseAnalytics: CourseAnalytics,
) : BaseVideoViewModel(courseId, blockId, courseAnalytics) {

    private val logger = Logger(TAG)

    var videoUrl = ""
    var currentVideoTime = 0L
    var videoDuration = 0L
    var isPlaying: Boolean? = null
    val videoSettings
        get() = preferencesManager.videoSettings

    private var isBlockAlreadyCompleted = false

    fun sendTime() {
        if (currentVideoTime != C.TIME_UNSET) {
            viewModelScope.launch {
                notifier.send(
                    CourseVideoPositionChanged(
                        videoUrl,
                        currentVideoTime,
                        videoDuration,
                        isPlaying ?: false
                    )
                )
            }
        }
    }

    fun markBlockCompleted(blockId: String) {
        if (!isBlockAlreadyCompleted) {
            logVideoCompletedEvent(videoUrl, videoDuration)
            viewModelScope.launch {
                try {
                    isBlockAlreadyCompleted = true
                    courseRepository.markBlocksCompletion(
                        courseId,
                        listOf(blockId)
                    )
                    notifier.send(CourseCompletionSet())
                } catch (e: Exception) {
                    logger.e(
                        throwable = e,
                        metadata = mapOf("courseId" to courseId, "blockId" to blockId)
                    )
                    isBlockAlreadyCompleted = false
                }
            }
        }
    }

    companion object {
        private const val TAG = "VideoViewModel"
    }
}
