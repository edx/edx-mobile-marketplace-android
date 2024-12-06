package org.openedx.app

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.NavigationToDiscovery
import org.openedx.discovery.presentation.DiscoveryNavigator

@SuppressLint("StaticFieldLeak")
class MainViewModel(
    private val context: Context,
    private val config: Config,
    private val notifier: DiscoveryNotifier,
    private val analytics: AppAnalytics,
) : BaseViewModel() {

    private val _isBottomBarEnabled = MutableLiveData(true)
    val isBottomBarEnabled: LiveData<Boolean>
        get() = _isBottomBarEnabled

    private val _navigateToDiscovery = MutableSharedFlow<Boolean>()
    val navigateToDiscovery: SharedFlow<Boolean>
        get() = _navigateToDiscovery.asSharedFlow()

    val isDiscoveryTypeWebView get() = config.getDiscoveryConfig().isViewTypeWebView()
    val getDiscoveryFragment get() = DiscoveryNavigator(isDiscoveryTypeWebView).getDiscoveryFragment()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        notifier.notifier
            .onEach {
                if (it is NavigationToDiscovery) {
                    _navigateToDiscovery.emit(true)
                }
            }
            .distinctUntilChanged()
            .launchIn(viewModelScope)
        logSettingPermissionStatusEvent()
    }

    fun enableBottomBar(enable: Boolean) {
        _isBottomBarEnabled.value = enable
    }

    fun logLearnTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.LEARN)
    }

    fun logDiscoveryTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.DISCOVER)
    }

    fun logProfileTabClickedEvent() {
        logScreenEvent(AppAnalyticsEvent.PROFILE)
    }

    private fun logScreenEvent(event: AppAnalyticsEvent) {
        analytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(AppAnalyticsKey.NAME.key, event.biValue)
            }
        )
    }

    private fun logSettingPermissionStatusEvent() {
        val event = AppAnalyticsEvent.NOTIFICATION_PERMISSION
        val permissionStatus =
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PermissionStatus.AUTHORIZED
            } else {
                PermissionStatus.DENIED
            }
        analytics.logEvent(event.eventName, buildMap {
            put(AppAnalyticsKey.NAME.key, event.biValue)
            put(AppAnalyticsKey.STATUS.key, permissionStatus.status)
        })
    }
}
