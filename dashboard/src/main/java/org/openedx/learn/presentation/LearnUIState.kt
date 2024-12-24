package org.openedx.learn.presentation

import org.openedx.learn.LearnType

data class LearnUIState(
    val learnType: LearnType,
    val showNotificationIcon: Boolean = false,
    val hasUnreadNotifications: Boolean = false
)
