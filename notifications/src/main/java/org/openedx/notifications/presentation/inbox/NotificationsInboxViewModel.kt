package org.openedx.notifications.presentation.inbox

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.NotificationItem

class NotificationsInboxViewModel(
    private val interactor: NotificationsInteractor,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<InboxUIState>(InboxUIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore = _canLoadMore.asStateFlow()

    private val notifications: Map<InboxSection, MutableList<NotificationItem>> = mapOf(
        InboxSection.RECENT to mutableListOf(),
        InboxSection.THIS_WEEK to mutableListOf(),
        InboxSection.OLDER to mutableListOf(),
    )

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
                if (response.pagination.next.isNotEmpty() && nextPage < response.pagination.numPages) {
                    nextPage++
                    _canLoadMore.value = true
                } else {
                    nextPage = -1
                    _canLoadMore.value = false
                }

                // Add the new notifications to their respective sections in the existing map
                response.notifications.forEach { (section, items) ->
                    notifications[section]?.addAll(items)
                }

                // Update the UI state based on whether any notifications exist
                _uiState.value = if (notifications.values.any { it.isNotEmpty() }) {
                    InboxUIState.Data(notifications = notifications)
                } else {
                    InboxUIState.Empty
                }
            } catch (e: Exception) {
                if (nextPage == 1) {
                    _uiState.value = InboxUIState.Error
                } else {
                    _canLoadMore.value = true
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun onReloadNotifications() {
        _canLoadMore.value = true
        nextPage = 1
        internalLoadNotifications()
    }
}
