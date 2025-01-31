package org.openedx.notifications.presentation.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.presentation.NotificationsAnalytics
import org.openedx.notifications.presentation.NotificationsAnalyticsEvent
import org.openedx.notifications.presentation.NotificationsAnalyticsKey

@SuppressLint("StaticFieldLeak")
class NotificationsSettingsViewModel(
    private val context: Context,
    private val interactor: NotificationsInteractor,
    private val analytics: NotificationsAnalytics,
    private val preference: NotificationsPreferences,
) : BaseViewModel() {

    private val _notificationsConfiguration = MutableStateFlow(preference.notifications)
    val notificationsConfiguration: StateFlow<NotificationsConfiguration>
        get() = _notificationsConfiguration

    private val _showPermissionRequestDialog = MutableStateFlow(false)
    val showPermissionRequestDialog: StateFlow<Boolean>
        get() = _showPermissionRequestDialog

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        if (checkPushNotificationPermission()) {
            fetchAndUpdateNotificationsSettings()
        } else {
            updateDiscussionPreference(updatedValue = false)
            _showPermissionRequestDialog.update { true }
        }
    }

    fun setDiscussionNotificationPreference(value: Boolean) {
        if (checkPushNotificationPermission()) {
            viewModelScope.launch {
                try {
                    val response = interactor.updateNotificationsConfiguration(value)
                    updateDiscussionPreference(updatedValue = response.updatedValue)

                    logDiscussionPermissionToggleEvent(isDiscussionPushEnabled = value)
                } catch (e: Exception) {
                    showErrorMessage()
                }
            }
        } else {
            _showPermissionRequestDialog.update { true }
        }
    }

    fun fetchAndUpdateNotificationsSettings() {
        viewModelScope.launch {
            try {
                val response = interactor.fetchNotificationsConfiguration()
                updateDiscussionPreference(updatedValue = response.discussionsPushEnabled)
            } catch (e: Exception) {
                showErrorMessage()
            }
        }
    }

    fun updateDiscussionPreference(updatedValue: Boolean) {
        _notificationsConfiguration.update { it.copy(discussionsPushEnabled = updatedValue) }
        preference.notifications = _notificationsConfiguration.value
    }

    private fun checkPushNotificationPermission(): Boolean {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        return notificationManagerCompat.areNotificationsEnabled()
    }

    fun dismissPermissionDialog() {
        _showPermissionRequestDialog.update { false }
        updateDiscussionPreference(updatedValue = false)
    }

    private suspend fun showErrorMessage() {
        _uiMessage.emit(
            UIMessage.SnackBarMessage(context.getString(R.string.core_service_unavailable_message))
        )

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
