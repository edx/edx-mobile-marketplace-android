package org.openedx.discussion.presentation

interface DiscussionAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class DiscussionAnalyticsEvent(val eventName: String, val biValue: String) {
    DISCUSSION_ALL_POSTS_CLICKED(
        "Discussion:All Posts Clicked",
        "edx.bi.app.discussion.all_posts.clicked"
    ),
    DISCUSSION_FOLLOWING_POSTS_CLICKED(
        "Discussion:Following Posts Clicked",
        "edx.bi.app.discussion.following_posts.clicked"
    ),
    DISCUSSION_TOPIC_CLICKED(
        "Discussion:Topic Clicked",
        "edx.bi.app.discussion.topic.clicked"
    ),
    DISCUSSION_TOPIC_VIEWED(
        "Discussion:Topic Viewed",
        "edx.bi.app.discussion.topic.viewed"
    ),
    DISCUSSION_POST_VIEWED(
        "Discussion:Post Viewed",
        "edx.bi.app.discussion.post.viewed"
    ),
    DISCUSSION_RESPONSE_VIEWED(
        "Discussion:Response Viewed",
        "edx.bi.app.discussion.response.viewed"
    ),
    DISCUSSION_POST_CREATED(
        "Discussion:Post Created",
        "edx.bi.app.discussion.post.created"
    ),
    DISCUSSION_RESPONSE_ADDED(
        "Discussion:Response Added",
        "edx.bi.app.discussion.response.added"
    ),
    DISCUSSION_COMMENT_ADDED(
        "Discussion:Comment Added",
        "edx.bi.app.discussion.comment.added"
    ),
    DISCUSSION_POST_FOLLOW_TOGGLE(
        "Discussion:Post Follow Toggle",
        "edx.bi.app.discussion.follow.toggle"
    ),
    DISCUSSION_LIKE_TOGGLE(
        "Discussion:Like Toggle",
        "edx.bi.app.discussion.like.toggle"
    ),
    DISCUSSION_REPORT_TOGGLE(
        "Discussion:Report Toggle",
        "edx.bi.app.discussion.report.toggle"
    )
}

enum class DiscussionAnalyticsKey(val key: String) {
    NAME("name"),
    COURSE_ID("course_id"),
    TOPIC_ID("topic_id"),
    POST_TYPE("post_type"),
    FOLLOW_POST("follow_post"),
    AUTHOR("author"),
    THREAD_ID("thread_id"),
    RESPONSE_ID("response_id"),
    COMMENT_ID("comment_id"),
    DISCUSSION_TYPE("discussion_type"),
    LIKE("like"),
    FOLLOW("follow"),
    REPORT("report"),
}

enum class DiscussionAnalyticsType(val value: String) {
    THREAD("thread"),
    RESPONSE("response"),
    COMMENT("comment"),
}
