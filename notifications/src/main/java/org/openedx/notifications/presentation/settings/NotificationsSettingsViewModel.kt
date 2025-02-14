package org.openedx.notifications.presentation.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.presentation.NotificationsAnalytics
import org.openedx.notifications.presentation.NotificationsAnalyticsEvent
import org.openedx.notifications.presentation.NotificationsAnalyticsKey

@SuppressLint("StaticFieldLeak")
class NotificationsSettingsViewModel(
    private val context: Context,
    private val interactor: NotificationsInteractor,
    private val analytics: NotificationsAnalytics,
    preference: NotificationsPreferences,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<NotificationsSettingsUiState>(
        NotificationsSettingsUiState.Configuration(
            showPermissionRequestDialog = false,
            discussionsPushEnabled = preference.notifications.discussionsPushEnabled,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        if (hasPushNotificationPermission()) {
            fetchAndUpdateNotificationsSettings()
        } else {
            enablePushNotifications(enabled = false)
        }
    }

    fun setDiscussionNotificationPreference(value: Boolean) {
        if (hasPushNotificationPermission()) {
            viewModelScope.launch {
                try {
                    val response = interactor.updateNotificationsConfiguration(value)
                    enablePushNotifications(enabled = response.updatedValue)

                    logDiscussionPermissionToggleEvent(isDiscussionPushEnabled = value)
                } catch (e: Exception) {
                    showErrorMessage()
                }
            }
        } else {
            showPermissionDialog()
        }
    }

    fun fetchAndUpdateNotificationsSettings() {
        viewModelScope.launch {
            try {
                val response = interactor.fetchNotificationsConfiguration()
                enablePushNotifications(enabled = response.discussionsPushEnabled)
            } catch (e: Exception) {
                showErrorMessage()
            }
        }
    }

    fun enablePushNotifications(enabled: Boolean) {
        _uiState.update {
            NotificationsSettingsUiState.Configuration(
                discussionsPushEnabled = enabled,
            )
        }
    }

    private fun hasPushNotificationPermission(): Boolean {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        return notificationManagerCompat.areNotificationsEnabled()
    }

    private fun showPermissionDialog() {
        _uiState.update {
            NotificationsSettingsUiState.Configuration(
                showPermissionRequestDialog = true,
            )
        }
    }

    fun dismissPermissionDialog() {
        _uiState.update { NotificationsSettingsUiState.Configuration() }
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
