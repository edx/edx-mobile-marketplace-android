package org.openedx.discussion.presentation

import org.openedx.core.BaseViewModel
import org.openedx.core.extension.takeIfNotEmpty

open class BaseDiscussionViewModel(
    private val courseId: String,
    private val threadId: String,
    private val analytics: DiscussionAnalytics,
) : BaseViewModel() {

    fun logPostCreatedEvent(
        topicId: String,
        postType: String,
        followPost: Boolean,
        author: String,
    ) {
        logEvent(
            event = DiscussionAnalyticsEvent.DISCUSSION_POST_CREATED,
            params = buildMap {
                put(DiscussionAnalyticsParam.TOPIC_ID.key, topicId)
                put(DiscussionAnalyticsParam.POST_TYPE.key, postType)
                put(DiscussionAnalyticsParam.FOLLOW_POST.key, followPost.toString())
                put(DiscussionAnalyticsParam.AUTHOR.key, author)
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
                    put(DiscussionAnalyticsParam.RESPONSE_ID.key, responseId)
                }
                put(DiscussionAnalyticsParam.AUTHOR.key, author)
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
                    put(DiscussionAnalyticsParam.RESPONSE_ID.key, responseId)
                }
                commentId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsParam.COMMENT_ID.key, commentId)
                }
                put(DiscussionAnalyticsParam.AUTHOR.key, author)
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
                put(DiscussionAnalyticsParam.FOLLOW.key, followPost.toString())
                put(DiscussionAnalyticsParam.AUTHOR.key, author)
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
                    put(DiscussionAnalyticsParam.RESPONSE_ID.key, responseId)
                }
                commentId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsParam.COMMENT_ID.key, commentId)
                }
                put(DiscussionAnalyticsParam.DISCUSSION_TYPE.key, discussionType)
                put(DiscussionAnalyticsParam.LIKE.key, likePost.toString())
                put(DiscussionAnalyticsParam.AUTHOR.key, author)
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
                    put(DiscussionAnalyticsParam.RESPONSE_ID.key, responseId)
                }
                commentId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsParam.COMMENT_ID.key, commentId)
                }
                put(DiscussionAnalyticsParam.DISCUSSION_TYPE.key, discussionType)
                put(DiscussionAnalyticsParam.REPORT.key, reportPost.toString())
                put(DiscussionAnalyticsParam.AUTHOR.key, author)
            }
        )
    }


    private fun logEvent(event: DiscussionAnalyticsEvent, params: Map<String, Any?>) {
        analytics.logEvent(
            event = event.name,
            params = buildMap {
                put(DiscussionAnalyticsParam.NAME.key, event.biValue)
                put(DiscussionAnalyticsParam.COURSE_ID.key, courseId)
                threadId.takeIfNotEmpty()?.let {
                    put(DiscussionAnalyticsParam.THREAD_ID.key, threadId)
                }
                putAll(params)
            }
        )
    }
}
