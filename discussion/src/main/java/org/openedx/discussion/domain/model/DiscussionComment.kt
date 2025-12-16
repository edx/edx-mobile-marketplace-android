package org.openedx.discussion.domain.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.openedx.core.domain.model.ProfileImage

@Parcelize
data class DiscussionComment(
    val id: String,
    val author: String,
    val authorLabel: String,
    val createdAt: String,
    val updatedAt: String,
    val rawBody: String,
    val renderedBody: String,
    val abuseFlagged: Boolean,
    val voted: Boolean,
    val voteCount: Int,
    val editableFields: List<String>,
    val canDelete: Boolean,
    val threadId: String,
    val parentId: String,
    val endorsed: Boolean,
    val endorsedBy: String,
    val endorsedByLabel: String,
    val endorsedAt: String,
    val childCount: Int,
    val children: List<String>,
    val profileImage: ProfileImage?,
    val users: Map<String, DiscussionProfile>?,
    var isAuthor: Boolean,
) : Parcelable {
    @IgnoredOnParcel
    var shouldHighlight: Boolean = false
}
