package org.openedx.notifications.presentation.primer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.notifications.data.storage.NotificationsPreferences
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationsConfiguration
import org.openedx.notifications.domain.model.NotificationsPrimerConfiguration

class NotificationsPrimerViewModel(
    private val interactor: NotificationsInteractor,
    private val notificationsPreferences: NotificationsPreferences,
) : BaseViewModel() {

    private val _shouldShowDialog = MutableStateFlow(true)
    val shouldShowDialog: StateFlow<Boolean> = _shouldShowDialog.asStateFlow()

    fun enableDiscussionNotificationsPreference() {
        viewModelScope.launch {
            try {
                interactor.updateNotificationsConfiguration(true)
                notificationsPreferences.notifications = NotificationsConfiguration(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetNotificationsPrimerConfiguration() {
        notificationsPreferences.primer = NotificationsPrimerConfiguration()
    }

    fun hideDialog() {
        _shouldShowDialog.value = false
    }
}
