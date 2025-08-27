package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class VideoPlayerConfig(
    @SerializedName("BLACKLIST_URLS")
    val blacklistUrls: List<String> = emptyList(),
)
