package org.openedx.notifications.presentation.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.presentation.NotificationsAnalytics
import org.openedx.notifications.presentation.NotificationsAnalyticsEvent
import org.openedx.notifications.presentation.NotificationsAnalyticsKey

class NotificationsSettingsViewModel(
    private val interactor: NotificationsInteractor,
    private val analytics: NotificationsAnalytics,
    private val preference: NotificationsPreferences,
) : BaseViewModel() {

    private val _notificationsConfiguration = MutableStateFlow(preference.notifications)
    val notificationsConfiguration: StateFlow<NotificationsConfiguration>
        get() = _notificationsConfiguration

    init {
        viewModelScope.launch {
            fetchAndUpdateNotificationsSettings()
        }
    }

    fun setDiscussionNotificationPreference(value: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.updateNotificationsConfiguration(value)
                updateDiscussionPreference(updatedValue = response.updatedValue)

                logDiscussionPermissionToggleEvent(isDiscussionPushEnabled = value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchAndUpdateNotificationsSettings() {
        try {
            val response = interactor.fetchNotificationsConfiguration()
            updateDiscussionPreference(updatedValue = response.discussionsPushEnabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateDiscussionPreference(updatedValue: Boolean) {
        _notificationsConfiguration.update { it.copy(discussionsPushEnabled = updatedValue) }
        preference.notifications = _notificationsConfiguration.value
    }

    private fun logDiscussionPermissionToggleEvent(
        isDiscussionPushEnabled: Boolean,
    ) {
        val event = NotificationsAnalyticsEvent.DISCUSSION_PERMISSION_TOGGLE
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(NotificationsAnalyticsKey.NAME.key, event.biValue)
                put(NotificationsAnalyticsKey.ACTION.key, isDiscussionPushEnabled)
                put(
                    NotificationsAnalyticsKey.CATEGORY.key,
                    NotificationsAnalyticsKey.NOTIFICATIONS.key
                )
            }
        )
    }
}
