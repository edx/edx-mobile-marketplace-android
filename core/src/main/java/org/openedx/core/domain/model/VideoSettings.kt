package org.openedx.core.domain.model

import org.openedx.core.R

data class VideoSettings(
    val wifiDownloadOnly: Boolean,
    val videoStreamingQuality: VideoQuality,
    val videoDownloadQuality: VideoQuality,
    val videoPlaybackSpeed: VideoPlaybackSpeed,
) {
    companion object {
        val default =
            VideoSettings(true, VideoQuality.AUTO, VideoQuality.AUTO, VideoPlaybackSpeed.SPEED_1_0X)
    }
}

enum class VideoQuality(
    val titleResId: Int,
    val desResId: Int = 0,
    val width: Int,
    val height: Int,
    val tagId: String = "", // for analytics
) {
    AUTO(
        titleResId = R.string.core_video_quality_auto,
        desResId = R.string.core_video_quality_auto_description,
        width = 0,
        height = 0,
        tagId = "auto",
    ),
    OPTION_360P(
        titleResId = R.string.core_video_quality_p360,
        desResId = R.string.core_video_quality_p360_description,
        width = 640,
        height = 360,
        tagId = "low",
    ),
    OPTION_540P(
        titleResId = R.string.core_video_quality_p540,
        desResId = 0,
        width = 960,
        height = 540,
        tagId = "medium",
    ),
    OPTION_720P(
        titleResId = R.string.core_video_quality_p720,
        desResId = R.string.core_video_quality_p720_description,
        width = 1280,
        height = 720,
        tagId = "high",
    );
}

enum class VideoPlaybackSpeed(val speedValue: Float) {
    SPEED_0_25X(0.25f), SPEED_0_50X(0.5f), SPEED_0_75X(0.75f),
    SPEED_1_0X(1.0f), SPEED_1_25X(1.25f), SPEED_1_50X(1.5f),
    SPEED_1_75X(1.75f), SPEED_2_0X(2.0f);

    companion object {
        fun getVideoPlaybackSpeed(speedValue: Float): VideoPlaybackSpeed {
            return entries.find { it.speedValue == speedValue } ?: SPEED_1_0X
        }
    }
}
