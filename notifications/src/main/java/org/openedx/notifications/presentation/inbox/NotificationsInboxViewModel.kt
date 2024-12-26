package org.openedx.notifications.presentation.inbox

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.NotificationItem

class NotificationsInboxViewModel(
    private val interactor: NotificationsInteractor,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<InboxUIState>(InboxUIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore = _canLoadMore.asStateFlow()

    val notifications: MutableList<NotificationItem> = mutableListOf()

    private var isLoading = false
    private var nextPage = 1

    init {
        getInboxNotifications()
    }

    private fun getInboxNotifications() {
        _uiState.value = InboxUIState.Loading
        internalLoadNotifications()
    }

    fun fetchMore() {
        if (!isLoading && nextPage != -1) {
            internalLoadNotifications()
        }
    }

    private fun internalLoadNotifications() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = interactor.getInboxNotifications(nextPage)
                if (response.pagination.next.isNotEmpty() && nextPage != response.pagination.numPages) {
                    nextPage++
                    _canLoadMore.value = true
                } else {
                    nextPage = -1
                    _canLoadMore.value = false
                }
                notifications.addAll(response.notifications)

                if (notifications.isNotEmpty()) {
                    _uiState.value = InboxUIState.Data(notifications = notifications)
                } else {
                    _uiState.value = InboxUIState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = InboxUIState.Error
            } finally {
                isLoading = false
            }
        }
    }
}
