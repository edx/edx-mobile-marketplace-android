package org.openedx.discussion.data.model.response

import com.google.gson.annotations.SerializedName
import org.openedx.discussion.domain.model.DiscussionConfig as DomainDiscussionConfig

data class DiscussionConfig(
    @SerializedName("id")
    val id: String,

    @SerializedName("is_posting_enabled")
    val isPostingEnabled: Boolean,

    @SerializedName("blackouts")
    val blackouts: List<Blackout>,

    @SerializedName("thread_list_url")
    val threadListUrl: String,

    @SerializedName("following_thread_list_url")
    val followingThreadListUrl: String,

    @SerializedName("topics_url")
    val topicsUrl: String,

    @SerializedName("allow_anonymous")
    val allowAnonymous: Boolean,

    @SerializedName("allow_anonymous_to_peers")
    val allowAnonymousToPeers: Boolean,

    @SerializedName("user_roles")
    val userRoles: List<String>,

    @SerializedName("has_moderation_privileges")
    val hasModerationPrivileges: Boolean,

    @SerializedName("is_group_ta")
    val isGroupTa: Boolean,

    @SerializedName("is_user_admin")
    val isUserAdmin: Boolean,

    @SerializedName("is_course_staff")
    val isCourseStaff: Boolean,

    @SerializedName("is_course_admin")
    val isCourseAdmin: Boolean,

    @SerializedName("provider")
    val provider: String,

    @SerializedName("enable_in_context")
    val enableInContext: Boolean,

    @SerializedName("group_at_subsection")
    val groupAtSubsection: Boolean,

    @SerializedName("edit_reasons")
    val editReasons: List<Reason>,

    @SerializedName("post_close_reasons")
    val postCloseReasons: List<Reason>,

    @SerializedName("show_discussions")
    val showDiscussions: Boolean,

    @SerializedName("captcha_settings")
    val captchaSettings: CaptchaSettings,
) {
    data class CaptchaSettings(
        @SerializedName("enabled")
        val enabled: Boolean = false,
    )

    fun mapToDomain(): DomainDiscussionConfig {
        return DomainDiscussionConfig(
            isPostingEnabled = isPostingEnabled,
            isCaptchaEnabled = captchaSettings.enabled,
        )
    }
}
