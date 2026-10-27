package org.openedx.course.domain.model

data class PipPlayerState(
    val playerType: PipPlayerType = PipPlayerType.NONE,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val isPipMode: Boolean = false,
)
enum class PipPlayerType {
    NONE,
    EXOPLAYER,
    YOUTUBE,
}

sealed class PipAction {
    data object Play : PipAction()
    data object Pause : PipAction()
    data object SeekForward : PipAction()
    data object SeekBackward : PipAction()
    data object Replay : PipAction()
}
