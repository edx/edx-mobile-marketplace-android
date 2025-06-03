package org.openedx.discussion.presentation.threads

sealed class DiscussionThreadsUIState {
    data class Threads(
        val data: List<org.openedx.discussion.domain.model.Thread>,
        val isPostingEnabled: Boolean,
    ) : DiscussionThreadsUIState()

    object Loading : DiscussionThreadsUIState()
}