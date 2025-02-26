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

class NotificationsPrimerViewModel(
    private val interactor: NotificationsInteractor,
    private val notificationsPreferences: NotificationsPreferences,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<PrimerUIState>(PrimerUIState.ShowDialog)
    val uiState: StateFlow<PrimerUIState> = _uiState.asStateFlow()

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
        notificationsPreferences.primer = NotificationsPrimerConfiguration()
    }

    fun hideDialog() {
        _uiState.value = PrimerUIState.HideDialog
    }

    fun dismissDialog() {
        _uiState.value = PrimerUIState.DismissDialog
    }

    fun showRationalDialog() {
        _uiState.value = PrimerUIState.ShowRationalDialog
    }
}
