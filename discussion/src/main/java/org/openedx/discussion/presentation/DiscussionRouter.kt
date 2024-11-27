package org.openedx.discussion.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.FragmentViewType
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.Thread

interface DiscussionRouter {

    fun navigateToDiscussionThread(
        fm: FragmentManager,
        action: String,
        courseId: String,
        topicId: String,
        title: String,
        viewType: FragmentViewType
    )

    fun navigateToDiscussionComments(
        fm: FragmentManager,
        courseId: String,
        thread: Thread,
    )

    fun navigateToDiscussionResponses(
        fm: FragmentManager,
        courseId: String,
        threadId: String,
        comment: DiscussionComment,
        isClosed: Boolean,
    )

    fun navigateToAddThread(
        fm: FragmentManager,
        topicId: String,
        courseId: String
    )

    fun navigateToSearchThread(
        fm: FragmentManager,
        courseId: String
    )

    fun navigateToAnothersProfile(
        fm: FragmentManager,
        username: String
    )
}