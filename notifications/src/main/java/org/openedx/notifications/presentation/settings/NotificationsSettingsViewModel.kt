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
            discussionsPushEnabled = preference.notifications.discussionsPushEnabled,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<NotificationsSettingsUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        if (hasPushNotificationPermission()) {
            fetchAndUpdateNotificationsSettings()
        } else {
            enablePushNotifications(enabled = false)
        }
        logScreenEvent(NotificationsAnalyticsEvent.PUSH_NOTIFICATIONS_SETTINGS)
    }

    fun setDiscussionNotificationPreference(value: Boolean) {
        logDiscussionPermissionToggleEvent(isDiscussionPushEnabled = value)
        if (hasPushNotificationPermission()) {
            viewModelScope.launch {
                try {
                    val response = interactor.updateNotificationsConfiguration(value)
                    enablePushNotifications(enabled = response.updatedValue)
                } catch (e: Exception) {
                    showErrorMessage()
                }
            }
        } else {
            requestPermission()
        }
    }

    private fun fetchAndUpdateNotificationsSettings() {
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

    private fun requestPermission() {
        viewModelScope.launch {
            _uiEvent.emit(NotificationsSettingsUiEvent.RequestPermission)
        }
    }

    fun showPermissionDialogRationale() {
        viewModelScope.launch {
            _uiEvent.emit(NotificationsSettingsUiEvent.ShowPermissionDialogRationale)
        }
    }

    fun dismissPermissionDialog() {
        viewModelScope.launch {
            _uiEvent.emit(NotificationsSettingsUiEvent.None)
        }
    }

    private suspend fun showErrorMessage() {
        _uiMessage.emit(
            UIMessage.SnackBarMessage(context.getString(R.string.core_service_unavailable_message))
        )
    }

    fun logBatchPermissionToggleEvent() {
        val discussionToggle =
            (uiState.value as NotificationsSettingsUiState.Configuration).discussionsPushEnabled
        logEvent(
            event = NotificationsAnalyticsEvent.PUSH_PREFERENCES_BATCH_TOGGLE_STATE,
            params = buildMap {
                put(NotificationsAnalyticsKey.DISCUSSIONS_ACTIVITY.key, discussionToggle)
            }
        )
    }

    private fun logDiscussionPermissionToggleEvent(
        isDiscussionPushEnabled: Boolean,
    ) {
        logEvent(
            event = NotificationsAnalyticsEvent.DISCUSSION_PREFERENCE_TOGGLE,
            params = buildMap {
                put(NotificationsAnalyticsKey.ACTION.key, isDiscussionPushEnabled)
            }
        )
    }

    fun logPermissionDialogActionEvent(
        event: NotificationsAnalyticsEvent,
        action: NotificationsAnalyticsKey
    ) {
        logEvent(
            event = event,
            params = buildMap {
                put(NotificationsAnalyticsKey.ACTION.key, action.key)
                put(
                    NotificationsAnalyticsKey.SOURCE.key,
                    NotificationsAnalyticsKey.PUSH_SETTINGS.key
                )
            }
        )
    }

    fun logPermissionDialogScreenEvent(event: NotificationsAnalyticsEvent) {
        logScreenEvent(
            event = event,
            params = buildMap {
                put(
                    NotificationsAnalyticsKey.SOURCE.key,
                    NotificationsAnalyticsKey.PUSH_SETTINGS.key
                )
            }
        )
    }

    fun logScreenEvent(
        event: NotificationsAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(NotificationsAnalyticsKey.NAME.key, event.biValue)
                put(
                    NotificationsAnalyticsKey.CATEGORY.key,
                    NotificationsAnalyticsKey.NOTIFICATIONS.key
                )
                putAll(params)
            }
        )
    }

    fun logEvent(
        event: NotificationsAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(NotificationsAnalyticsKey.NAME.key, event.biValue)
                put(
                    NotificationsAnalyticsKey.CATEGORY.key,
                    NotificationsAnalyticsKey.NOTIFICATIONS.key
                )
                putAll(params)
            }
        )
    }
}
