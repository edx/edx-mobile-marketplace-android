package org.openedx.notifications.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.FragmentViewType

interface NotificationsRouter {
    fun navigateToPushNotificationsSettings(fm: FragmentManager)

    fun navigateToDiscussionThread(
        fm: FragmentManager,
        action: String,
        courseId: String,
        topicId: String,
        threadId: String,
        responseId: String,
        commentId:String,
        title: String,
        viewType: FragmentViewType
    )
}
