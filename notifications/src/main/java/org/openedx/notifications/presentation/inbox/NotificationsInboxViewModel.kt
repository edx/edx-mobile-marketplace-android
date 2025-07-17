package org.openedx.notifications.presentation.inbox

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.FragmentViewType
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.extension.isNotNullOrEmpty
import org.openedx.core.system.ResourceManager
import org.openedx.core.utils.Logger
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.domain.model.InboxSection
import org.openedx.notifications.domain.model.NotificationItem
import org.openedx.notifications.presentation.NotificationsAnalytics
import org.openedx.notifications.presentation.NotificationsAnalyticsEvent
import org.openedx.notifications.presentation.NotificationsAnalyticsKey
import org.openedx.notifications.presentation.NotificationsRouter
import java.util.Date
import org.openedx.core.R as coreR

class NotificationsInboxViewModel(
    private val interactor: NotificationsInteractor,
    private val notificationsRouter: NotificationsRouter,
    private val resourceManager: ResourceManager,
    private val analytics: NotificationsAnalytics,
) : BaseViewModel() {

    private val logger = Logger(TAG)

    private val _uiState = MutableStateFlow<InboxUIState>(InboxUIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore = _canLoadMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val notifications: MutableMap<InboxSection, MutableList<NotificationItem>> =
        mutableMapOf(
            InboxSection.RECENT to mutableListOf(),
            InboxSection.THIS_WEEK to mutableListOf(),
            InboxSection.OLDER to mutableListOf(),
        )

    private var isLoading = false
    private var nextPage = 1

    init {
        logScreenViewEvent()
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
                logger.e(throwable = e)
            }
        }
    }

    fun fetchMore() {
        if (!isLoading && _canLoadMore.value) {
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
                    InboxUIState.Data(notifications = notifications.mapValues { it.value.toList() })
                } else {
                    InboxUIState.Fallback(state = InboxFullScreenState.Empty)
                }
            } catch (e: Exception) {
                logger.e(throwable = e)
                if (uiState.value is InboxUIState.Data || _isRefreshing.value) {
                    emitErrorMessage(e)
                } else if (e.isInternetError()) {
                    _uiState.value =
                        InboxUIState.Fallback(state = InboxFullScreenState.NetworkError)
                } else {
                    _uiState.value = InboxUIState.Fallback(state = InboxFullScreenState.ServerError)
                }
            } finally {
                isLoading = false
                _isRefreshing.value = false
            }
        }
    }

    fun onReloadNotifications() {
        _canLoadMore.value = true
        nextPage = 1
        internalLoadNotifications()
    }

    fun onRefreshNotifications() {
        _isRefreshing.value = true
        nextPage = 1
        InboxSection.entries.forEach { section ->
            notifications[section] = mutableListOf()
        }
        internalLoadNotifications()
    }

    fun markNotificationAsRead(
        fm: FragmentManager,
        notification: NotificationItem,
        inboxSection: InboxSection,
    ) {
        viewModelScope.launch {
            try {
                val currentSection = notifications[inboxSection] ?: return@launch
                val index = currentSection.indexOfFirst { it.id == notification.id }
                if (index == -1) return@launch

                if (notification.isUnread() && interactor.markNotificationAsRead(notification.id)) {

                    // Locally update the lastRead timestamp to avoid refreshing the entire list.
                    currentSection[index] = currentSection[index].copy(lastRead = Date())

                    notifications[inboxSection] = currentSection
                    _uiState.value = InboxUIState.Data(
                        notifications = notifications.toMap()
                    )
                }
                logNotificationItemClickedEvent(notification)

                // Navigating the user to the related post or response in the Course Discussion Tab
                if (notification.courseId.isNotEmpty()) {
                    notificationsRouter.navigateToDiscussionThread(
                        fm = fm,
                        action = "Topic",
                        courseId = notification.courseId,
                        topicId = notification.contentContext.topicId,
                        threadId = notification.contentContext.threadId,
                        responseId = notification.contentContext.responseId,
                        commentId = notification.contentContext.commentId,
                        title = notification.contentContext.courseName,
                        viewType = FragmentViewType.FULL_CONTENT
                    )
                }

            } catch (e: Exception) {
                logger.e(
                    throwable = e,
                    metadata = mapOf("notification_id" to notification.id.toString())
                )
                emitErrorMessage(e)
            }
        }
    }

    fun markAllNotificationsAsRead() {
        logEvent(
            event = NotificationsAnalyticsEvent.INBOX_MARK_ALL_READ_CLICKED,
            params = buildMap {
                put(
                    NotificationsAnalyticsKey.NOTIFICATION_DOMAIN.key,
                    NotificationsAnalyticsKey.DISCUSSION.key
                )
            },
        )
        viewModelScope.launch {
            try {
                if (_uiState.value is InboxUIState.Data) {
                    interactor.markAllNotificationsAsRead()

                    notifications.forEach { (section, notificationItems) ->
                        notifications[section] = notificationItems
                            .map { it.copy(lastRead = Date()) }
                            .toMutableList()
                    }
                    _uiState.value = InboxUIState.Data(notifications = notifications.toMap())
                }
            } catch (e: Exception) {
                logger.e(throwable = e)
                emitErrorMessage(e)
            }
        }
    }

    fun navigateToPushNotificationsSettings(fm: FragmentManager) {
        logEvent(NotificationsAnalyticsEvent.INBOX_PUSH_NOTIFICATIONS_SETTINGS_CLICKED)
        notificationsRouter.navigateToPushNotificationsSettings(fm)
    }

    fun logInboxMenuClicked() {
        logEvent(NotificationsAnalyticsEvent.INBOX_MENU_CLICKED)
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

    private fun logNotificationItemClickedEvent(notification: NotificationItem) {
        val contentContext = notification.contentContext
        logEvent(
            event = NotificationsAnalyticsEvent.NOTIFICATION_INBOX_ITEM_CLICKED,
            params = buildMap<String, String?> {
                put(NotificationsAnalyticsKey.NOTIFICATION_DOMAIN.key, notification.appName)
                put(NotificationsAnalyticsKey.NOTIFICATION_TYPE.key, notification.notificationType)
                put(NotificationsAnalyticsKey.NOTIFICATION_ID.key, notification.id.toString())
                put(NotificationsAnalyticsKey.COURSE_ID.key, notification.courseId)
                put(NotificationsAnalyticsKey.TOPIC_ID.key, contentContext.topicId)
                put(NotificationsAnalyticsKey.THREAD_ID.key, contentContext.threadId)
                put(NotificationsAnalyticsKey.RESPONSE_ID.key, contentContext.responseId)
                put(NotificationsAnalyticsKey.COMMENT_ID.key, contentContext.commentId)
            }.filterValues { it.isNotNullOrEmpty() }
        )
    }

    private fun logScreenViewEvent() {
        val event = NotificationsAnalyticsEvent.NOTIFICATION_INBOX_VIEW
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(NotificationsAnalyticsKey.NAME.key, event.biValue)
                put(
                    NotificationsAnalyticsKey.CATEGORY.key,
                    NotificationsAnalyticsKey.NOTIFICATIONS.key
                )
            }
        )
    }

    private fun logEvent(
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

    companion object {
        private const val TAG = "NotificationsInboxViewModel"
    }
}
