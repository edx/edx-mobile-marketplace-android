package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class VideoPlayerConfig(
    @SerializedName("VIDEO_TRANSCRIPT_ENABLED")
    val isVideoTranscriptEnabled: Boolean = false,

    @SerializedName("BLACKLIST_URLS")
    val blacklistUrls: List<String> = emptyList(),
)
