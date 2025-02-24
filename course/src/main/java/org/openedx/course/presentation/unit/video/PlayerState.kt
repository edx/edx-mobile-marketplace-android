package org.openedx.course.presentation.unit.video

internal data class PlayerState(
    val selectedLanguage: String = "",
    val isPlayerSetUp: Boolean = false,
    val isCastActive: Boolean = false,
    val isVideoEnded: Boolean = false,
    val isSubtitlesReady: Boolean = false,
)
