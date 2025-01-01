package org.openedx.notifications.presentation.inbox

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.NotificationItem
import java.util.Date
import org.openedx.core.R as coreR

class NotificationsInboxViewModel(
    private val interactor: NotificationsInteractor,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<InboxUIState>(InboxUIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore = _canLoadMore.asStateFlow()

    private val notifications: MutableMap<InboxSection, MutableList<NotificationItem>> =
        mutableMapOf(
            InboxSection.RECENT to mutableListOf(),
            InboxSection.THIS_WEEK to mutableListOf(),
            InboxSection.OLDER to mutableListOf(),
        )

    private var isLoading = false
    private var nextPage = 1

    init {
        getInboxNotifications()
        markNotificationsAsSeen()
    }

    private fun getInboxNotifications() {
        _uiState.value = InboxUIState.Loading
        internalLoadNotifications()
    }

    private fun markNotificationsAsSeen() {
        viewModelScope.launch {
            try {
                interactor.markNotificationsAsSeen()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    fun markNotificationAsRead(
        notification: NotificationItem,
        inboxSection: InboxSection,
    ) {
        viewModelScope.launch {
            try {
                if (notification.isUnread() && interactor.markNotificationAsRead(notification.id)) {
                    val currentSection = notifications[inboxSection] ?: return@launch

                    val index = currentSection.indexOfFirst { it.id == notification.id }
                    if (index == -1) return@launch

                    // Locally update the lastRead timestamp to avoid refreshing the entire list.
                    currentSection[index] = currentSection[index].copy(lastRead = Date())

                    notifications[inboxSection] = currentSection
                    _uiState.value = InboxUIState.Data(
                        notifications = notifications.toMap()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emitErrorMessage(e)
            }
        }
    }

    private suspend fun emitErrorMessage(e: Exception) {
        if (e.isInternetError()) {
            _uiMessage.emit(
                UIMessage.SnackBarMessage(resourceManager.getString(coreR.string.core_error_no_connection))
            )
        } else {
            _uiMessage.emit(
                UIMessage.SnackBarMessage(resourceManager.getString(coreR.string.core_error_unknown_error))
            )
        }
    }
}
