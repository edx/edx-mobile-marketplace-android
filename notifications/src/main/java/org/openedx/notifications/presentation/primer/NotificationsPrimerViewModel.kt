package org.openedx.notifications.presentation.primer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsPrimerConfiguration
import org.openedx.notifications.presentation.NotificationsAnalytics
import org.openedx.notifications.presentation.NotificationsAnalyticsEvent
import org.openedx.notifications.presentation.NotificationsAnalyticsKey

class NotificationsPrimerViewModel(
    private val interactor: NotificationsInteractor,
    private val preferences: NotificationsPreferences,
    private val analytics: NotificationsAnalytics,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<PrimerUIState>(PrimerUIState.ShowDialog)
    val uiState: StateFlow<PrimerUIState> = _uiState.asStateFlow()

    init {
        logPrimerScreenEvent()
    }

    fun enableDiscussionNotificationsPreference() {
        viewModelScope.launch {
            try {
                _uiState.value = PrimerUIState.Loading
                interactor.updateNotificationsConfiguration(true)
                resetNotificationsPrimerConfiguration()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.value = PrimerUIState.DismissDialog
            }
        }
    }

    private fun resetNotificationsPrimerConfiguration() {
        preferences.primer = NotificationsPrimerConfiguration()
    }

    fun hideDialog() {
        _uiState.value = PrimerUIState.HideDialog
    }

    fun dismissDialog() {
        _uiState.value = PrimerUIState.DismissDialog
    }

    fun showRationaleDialog() {
        _uiState.value = PrimerUIState.ShowRationaleDialog
    }

    fun logPrimerActionEvent(action: NotificationsAnalyticsKey) {
        val event = NotificationsAnalyticsEvent.DISCUSSION_PRIMER_ACTION
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(NotificationsAnalyticsKey.NAME.key, event.biValue)
                put(NotificationsAnalyticsKey.ACTION.key, action.key)
                put(
                    NotificationsAnalyticsKey.CATEGORY.key,
                    NotificationsAnalyticsKey.NOTIFICATIONS.key
                )
            }
        )
    }

    private fun logPrimerScreenEvent() {
        val event = NotificationsAnalyticsEvent.DISCUSSION_PRIMER_VIEWED
        val dialogFrequency = preferences.primer.dismissalCount
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(NotificationsAnalyticsKey.NAME.key, event.biValue)
                put(NotificationsAnalyticsKey.PRIMER_DIALOG_FREQUENCY.key, dialogFrequency)
                put(
                    NotificationsAnalyticsKey.CATEGORY.key,
                    NotificationsAnalyticsKey.NOTIFICATIONS.key
                )
            }
        )
    }
}
