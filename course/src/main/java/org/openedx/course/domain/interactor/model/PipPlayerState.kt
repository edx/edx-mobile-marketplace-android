package org.openedx.course.domain.interactor.model


data class PipPlayerState(
    val playerType: PipPlayerType = PipPlayerType.NONE,
    val isPlaying: Boolean = false,
    val isPipMode: Boolean = false,
    val isEnded: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
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
