package org.openedx.discussion.presentation

import org.openedx.core.BaseViewModel
import org.openedx.core.extension.takeIfNotEmpty

open class BaseDiscussionViewModel(
    private val courseId: String,
    private val threadId: String,
    private val analytics: DiscussionAnalytics,
) : BaseViewModel() {

    fun logAllPostsClickedEvent() {
        logEvent(DiscussionAnalyticsEvent.DISCUSSION_ALL_POSTS_CLICKED)
    }

    fun logFollowingPostsClickedEvent() {
        logEvent(DiscussionAnalyticsEvent.DISCUSSION_FOLLOWING_POSTS_CLICKED)
    }

    fun logTopicClickedEvent(
        topicId: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_TOPIC_CLICKED,
            params = buildMap {
                put(DiscussionAnalyticsKey.TOPIC_ID.key, topicId)
            }
        )
    }

    fun logPostCreatedEvent(
        topicId: String,
        postType: String,
        followPost: Boolean,
        author: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_POST_CREATED,
            params = buildMap {
                put(DiscussionAnalyticsKey.TOPIC_ID.key, topicId)
                put(DiscussionAnalyticsKey.POST_TYPE.key, postType)
                put(DiscussionAnalyticsKey.FOLLOW_POST.key, followPost.toString())
                put(DiscussionAnalyticsKey.AUTHOR.key, author)
            }
        )
    }

    fun logResponseAddedEvent(
        responseId: String,
        author: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_RESPONSE_ADDED,
            params = buildMap {
                responseId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.RESPONSE_ID.key, responseId)
                }
                put(DiscussionAnalyticsKey.AUTHOR.key, author)
            }
        )
    }

    fun logCommentAddedEvent(
        responseId: String,
        commentId: String,
        author: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_COMMENT_ADDED,
            params = buildMap {
                responseId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.RESPONSE_ID.key, responseId)
                }
                commentId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.COMMENT_ID.key, commentId)
                }
                put(DiscussionAnalyticsKey.AUTHOR.key, author)
            }
        )
    }

    fun logFollowToggleEvent(
        followPost: Boolean,
        author: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_POST_FOLLOW_TOGGLE,
            params = buildMap {
                put(DiscussionAnalyticsKey.FOLLOW.key, followPost.toString())
                put(DiscussionAnalyticsKey.AUTHOR.key, author)
            }
        )
    }

    fun logLikeToggleEvent(
        responseId: String = "",
        commentId: String = "",
        discussionType: String,
        likePost: Boolean,
        author: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_LIKE_TOGGLE,
            params = buildMap {
                responseId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.RESPONSE_ID.key, responseId)
                }
                commentId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.COMMENT_ID.key, commentId)
                }
                put(DiscussionAnalyticsKey.DISCUSSION_TYPE.key, discussionType)
                put(DiscussionAnalyticsKey.LIKE.key, likePost.toString())
                put(DiscussionAnalyticsKey.AUTHOR.key, author)
            }
        )
    }

    fun logReportToggleEvent(
        responseId: String = "",
        commentId: String = "",
        discussionType: String,
        reportPost: Boolean,
        author: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_REPORT_TOGGLE,
            params = buildMap {
                responseId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.RESPONSE_ID.key, responseId)
                }
                commentId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.COMMENT_ID.key, commentId)
                }
                put(DiscussionAnalyticsKey.DISCUSSION_TYPE.key, discussionType)
                put(DiscussionAnalyticsKey.REPORT.key, reportPost.toString())
                put(DiscussionAnalyticsKey.AUTHOR.key, author)
            }
        )
    }


    private fun logEvent(event: DiscussionAnalyticsEvent, params: Map<String, Any?> = emptyMap()) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(DiscussionAnalyticsKey.NAME.key, event.biValue)
                put(DiscussionAnalyticsKey.COURSE_ID.key, courseId)
                threadId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsKey.THREAD_ID.key, threadId)
                }
                putAll(params)
            }
        )
    }
}
