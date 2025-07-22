package org.openedx.discussion.domain.model

data class DiscussionConfig(
    val isPostingEnabled: Boolean,
    val isCaptchaEnabled: Boolean,
)
