package org.openedx.learn.presentation

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.DashboardNavigator
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.system.PushGlobalManager
import org.openedx.core.system.notifier.PushEvent
import org.openedx.core.system.notifier.PushNotifier
import org.openedx.core.utils.Logger
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardAnalyticsEvent
import org.openedx.dashboard.presentation.DashboardAnalyticsKey
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.learn.LearnType

class LearnViewModel(
    openTab: String,
    private val config: Config,
    private val dashboardRouter: DashboardRouter,
    private val analytics: DashboardAnalytics,
    private val pushManager: PushGlobalManager,
    private val pushNotifier: PushNotifier
) : BaseViewModel(resourceManager) {

    private val logger = Logger(TAG)

    private val _uiState = MutableStateFlow(
        LearnUIState(
            if (openTab == LearnTab.PROGRAMS.name) {
                LearnType.PROGRAMS
            } else {
                LearnType.COURSES
            },
            showNotificationIcon = config.isPushNotificationsEnabled()
        )
    )

    val uiState: StateFlow<LearnUIState>
        get() = _uiState.asStateFlow()

    private val dashboardType get() = config.getDashboardConfig().getType()
    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    fun onSettingsClick(fragmentManager: FragmentManager) {
        dashboardRouter.navigateToSettings(fragmentManager)
    }

    val getDashboardFragment get() = DashboardNavigator(dashboardType).getDashboardFragment()

    val getProgramFragment get() = dashboardRouter.getProgramFragment()

    init {
        viewModelScope.launch {
            pushNotifier.notifier.collect { event ->
                if (event is PushEvent.RefreshBadgeCount) {
                    checkNotificationCount()
                }
            }
        }
        logTabClickedEvent(_uiState.value.learnType)
        checkNotificationCount()
    }

    fun updateLearnType(learnType: LearnType) {
        viewModelScope.launch {
            if (learnType != _uiState.value.learnType) {
                _uiState.update { it.copy(learnType = learnType) }
                logTabClickedEvent(learnType)
            }
        }
    }


    fun logMyCoursesTabClickedEvent() {
        logScreenEvent(DashboardAnalyticsEvent.MY_COURSES)
    }

    fun logMyProgramsTabClickedEvent() {
        logScreenEvent(DashboardAnalyticsEvent.MY_PROGRAMS)
    }

    private fun checkNotificationCount() {
        if (config.isPushNotificationsEnabled()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val unreadNotifications = pushManager.getUnreadNotificationsCount()
                    _uiState.update { it.copy(hasUnreadNotifications = unreadNotifications > 0) }
                } catch (e: Exception) {
                    logger.e(throwable = e)
                }
            }
        }
    }

    fun onNotificationBadgeClick(fm: FragmentManager) {
        pushManager.logNotificationBellClickedEvent(_uiState.value.hasUnreadNotifications)
        dashboardRouter.navigateToNotificationsInbox(fm)
        _uiState.update { it.copy(hasUnreadNotifications = false) }
    }

    private fun logTabClickedEvent(learnType: LearnType) {
        when (learnType) {
            LearnType.COURSES -> logScreenEvent(DashboardAnalyticsEvent.LEARN_MY_COURSES)
            LearnType.PROGRAMS -> logScreenEvent(DashboardAnalyticsEvent.LEARN_MY_PROGRAMS)
        }
    }

    private fun logScreenEvent(event: DashboardAnalyticsEvent) {
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(DashboardAnalyticsKey.NAME.key, event.biValue)
                put(DashboardAnalyticsKey.CATEGORY.key, DashboardAnalyticsKey.LEARN.key)
            }
        )
    }

    companion object {
        private const val TAG = "LearnViewModel"
    }
}
