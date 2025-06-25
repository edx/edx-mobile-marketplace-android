package org.openedx.discussion.data.model.response

data class CachedDiscussionConfig(
    val config: DiscussionConfig,
    val timestamp: Long // milliseconds
)
