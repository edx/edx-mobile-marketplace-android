package org.openedx.course.presentation.unit.video

import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import kotlinx.coroutines.launch
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.VideoPlaybackSpeed
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.core.utils.LocaleUtils
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

    var videoUrl = ""
    var currentVideoTime = 0L
    var videoDuration = 0L
    var isPlaying: Boolean? = null
    val videoSettings
        get() = preferencesManager.videoSettings

    private var isBlockAlreadyCompleted = false

    var transcripts = emptyMap<String, String>()
    var selectedLanguage: String = ""
    val subtitleConfigurations: List<MediaItem.SubtitleConfiguration>
        get() = transcripts
            .toSortedMap(
                compareBy { LocaleUtils.getLanguageByLanguageCode(it) }
            )
            .map { (language, uri) ->
                val selectionFlags =
                    if (language == selectedLanguage) C.SELECTION_FLAG_DEFAULT else 0

                MediaItem.SubtitleConfiguration.Builder(Uri.parse(uri))
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setSelectionFlags(selectionFlags)
                    .setLanguage(language)
                    .build()
            }

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
                    isBlockAlreadyCompleted = false
                }
            }
        }
    }

    fun getVideoQuality() = videoSettings.videoStreamingQuality

    fun setVideoPlaybackSpeed(speed: Float) {
        preferencesManager.videoSettings =
            videoSettings.copy(videoPlaybackSpeed = VideoPlaybackSpeed.getVideoPlaybackSpeed(speed))
    }
}
