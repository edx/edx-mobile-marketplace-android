package org.openedx.course.presentation.unit.video

internal data class PlayerState(
    val selectedLanguage: String = "",
    var activePlayerType: PlayerType = PlayerType.NONE,
    val isVideoEnded: Boolean = false,
    val isSubtitlesReady: Boolean = false,
)

enum class PlayerType {
    EXO_REGULAR,
    EXO_FULL_SCREEN,
    CHROME_CAST,
    NONE
}
