package org.openedx.app.deeplink

class DeepLink(params: Map<String, String>) {

    private val screenName = params[Keys.SCREEN_NAME.value]
    val notificationId = params[Keys.NOTIFICATION_ID.value]?.toIntOrNull()
    val courseId = params[Keys.COURSE_ID.value]
    val pathId = params[Keys.PATH_ID.value]
    val componentId = params[Keys.COMPONENT_ID.value]
    val topicId = params[Keys.TOPIC_ID.value]
    val threadId = params[Keys.THREAD_ID.value]
    val commentId = params[Keys.COMMENT_ID.value]
    val parentId = params[Keys.PARENT_ID.value]
    val notificationDomain = params[Keys.NOTIFICATION_DOMAIN.value]
    val type = DeepLinkType.typeOf(screenName ?: "")

    enum class Keys(val value: String) {
        SCREEN_NAME("screen_name"),
        NOTIFICATION_ID("notification_id"),
        COURSE_ID("course_id"),
        PATH_ID("path_id"),
        COMPONENT_ID("component_id"),
        TOPIC_ID("topic_id"),
        THREAD_ID("thread_id"),
        COMMENT_ID("comment_id"),
        PARENT_ID("parent_id"),
        NOTIFICATION_DOMAIN("notification_domain"),
    }

    fun toMap(): Map<String, String> {
        return mapOf(
            Keys.SCREEN_NAME.value to screenName.orEmpty(),
            Keys.NOTIFICATION_ID.value to notificationId?.toString().orEmpty(),
            Keys.COURSE_ID.value to courseId.orEmpty(),
            Keys.PATH_ID.value to pathId.orEmpty(),
            Keys.COMPONENT_ID.value to componentId.orEmpty(),
            Keys.TOPIC_ID.value to topicId.orEmpty(),
            Keys.THREAD_ID.value to threadId.orEmpty(),
            Keys.COMMENT_ID.value to commentId.orEmpty(),
            Keys.PARENT_ID.value to parentId.orEmpty(),
            Keys.NOTIFICATION_DOMAIN.value to notificationDomain.orEmpty(),
        ).filterValues { it.isNotEmpty() }
    }
}

enum class DeepLinkType(val type: String) {
    DISCOVERY("discovery"),
    DISCOVERY_COURSE_DETAIL("discovery_course_detail"),
    DISCOVERY_PROGRAM_DETAIL("discovery_program_detail"),
    COURSE_DASHBOARD("course_dashboard"),
    COURSE_VIDEOS("course_videos"),
    COURSE_DISCUSSION("course_discussion"),
    COURSE_DATES("course_dates"),
    COURSE_HANDOUT("course_handout"),
    COURSE_ANNOUNCEMENT("course_announcement"),
    COURSE_COMPONENT("course_component"),
    PROGRAM("program"),
    DISCUSSION_TOPIC("discussion_topic"),
    DISCUSSION_POST("discussion_post"),
    DISCUSSION_COMMENT("discussion_comment"),
    PROFILE("profile"),
    USER_PROFILE("user_profile"),
    ENROLL("enroll"),
    UNENROLL("unenroll"),
    ADD_BETA_TESTER("add_beta_tester"),
    REMOVE_BETA_TESTER("remove_beta_tester"),
    FORUM_RESPONSE("forum_response"),
    FORUM_COMMENT("forum_comment"),
    NONE("");

    companion object {
        fun typeOf(type: String): DeepLinkType {
            return entries.firstOrNull { it.type == type } ?: NONE
        }
    }
}
