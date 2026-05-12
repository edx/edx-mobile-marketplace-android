package org.openedx.course.domain.model

/**
 * Domain models for PiP state management.
 * Pure Kotlin — no Android dependencies.
 */

/**
 * Represents the current state of PiP playback.
 */
data class PipPlayerState(
    val playerType: PipPlayerType = PipPlayerType.NONE,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
    val isPipMode: Boolean = false,
)

/**
 * The type of player currently registered for PiP.
 */
enum class PipPlayerType {
    NONE,
    EXOPLAYER,
    YOUTUBE,
}

/**
 * Actions that can be triggered from the PiP overlay.
 */
sealed class PipAction {
    data object Play : PipAction()
    data object Pause : PipAction()
    data object SeekForward : PipAction()
    data object SeekBackward : PipAction()
    data object Replay : PipAction()
}
