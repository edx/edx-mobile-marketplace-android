package org.openedx.discussion.domain.model

data class DiscussionConfig(
    val courseId: String,
    val isPostingEnabled: Boolean,
    val blackoutPeriods: List<Blackout>,
    val threadListUrl: String,
    val followingThreadListUrl: String,
    val topicsUrl: String,
    val allowAnonymousPosts: Boolean,
    val allowAnonymousToPeers: Boolean,
    val userRoles: List<String>,
    val canModerate: Boolean,
    val isGroupTA: Boolean,
    val isUserAdmin: Boolean,
    val isCourseStaff: Boolean,
    val isCourseAdmin: Boolean,
    val provider: String,
    val isInContextEnabled: Boolean,
    val isGroupSubsectionEnabled: Boolean,
    val editReasons: List<Reason>,
    val postCloseReasons: List<Reason>,
    val showDiscussions: Boolean
)
