package org.openedx.app

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.app.deeplink.DeepLink
import org.openedx.app.deeplink.DeepLinkRouter
import org.openedx.app.system.push.RefreshFirebaseTokenWorker
import org.openedx.app.system.push.SyncFirebaseTokenWorker
import org.openedx.core.BaseViewModel
import org.openedx.core.DatabaseManager
import org.openedx.core.SingleEventLiveData
import org.openedx.core.config.Config
import org.openedx.core.data.model.User
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.PushGlobalManager
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.core.system.notifier.app.SignInEvent
import org.openedx.core.ui.theme.ThemeManager
import org.openedx.core.utils.CrashlyticsHelper
import org.openedx.core.utils.FileUtil
import org.openedx.core.utils.Logger

@SuppressLint("StaticFieldLeak")
class AppViewModel(
    private val config: Config,
    private val notifier: AppNotifier,
    private val databaseManager: DatabaseManager,
    private val preferencesManager: CorePreferences,
    private val dispatcher: CoroutineDispatcher,
    private val analytics: AppAnalytics,
    private val deepLinkRouter: DeepLinkRouter,
    private val fileUtil: FileUtil,
    private val context: Context,
    private val pushManager: PushGlobalManager,
) : BaseViewModel() {

    private val logger = Logger(TAG)

    private val _logoutUser = SingleEventLiveData<Unit>()
    val logoutUser: LiveData<Unit>
        get() = _logoutUser

    val isLogistrationEnabled get() = config.isPreLoginExperienceEnabled()

    private var logoutHandledAt: Long = 0

    val isBranchEnabled get() = config.getBranchConfig().enabled
    private val canResetAppDirectory get() = preferencesManager.canResetAppDirectory

    init {
        logAppLaunchEvent()
        setUserId(preferencesManager.user)
        setAppThemeFromPreference()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        val user = preferencesManager.user
        if (user != null && preferencesManager.pushToken.isNotEmpty()) {
            SyncFirebaseTokenWorker.schedule(context)
        }

        if (canResetAppDirectory) {
            resetAppDirectory()
        }

        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is SignInEvent && config.getFirebaseConfig().isCloudMessagingEnabled) {
                    SyncFirebaseTokenWorker.schedule(context)
                } else if (event is LogoutEvent) {
                    handleLogoutEvent(event)
                }
            }
        }
    }

    private fun logAppLaunchEvent() {
        analytics.logEvent(
            event = AppAnalyticsEvent.LAUNCH.eventName,
            params = buildMap {
                put(AppAnalyticsKey.NAME.key, AppAnalyticsEvent.LAUNCH.biValue)
            }
        )
    }

    private fun resetAppDirectory() {
        fileUtil.deleteOldAppDirectory()
        preferencesManager.canResetAppDirectory = false
    }

    fun makeExternalRoute(fm: FragmentManager, deepLink: DeepLink) {
        deepLinkRouter.makeRoute(fm, deepLink)
    }

    private fun setUserId(user: User?) {
        user?.let {
            analytics.setUserIdForSession(it.id)
            CrashlyticsHelper.setUserId(it.id.toString())
        }
    }

    private fun setAppThemeFromPreference() {
        ThemeManager.applyThemeMode(context, preferencesManager.appThemeMode)
    }

    private suspend fun handleLogoutEvent(event: LogoutEvent) {
        if (System.currentTimeMillis() - logoutHandledAt > 5000) {
            if (event.isForced) {
                logoutHandledAt = System.currentTimeMillis()
                preferencesManager.clear()
                withContext(dispatcher) {
                    databaseManager.clearTables()
                }
                analytics.logoutEvent(true)
                _logoutUser.value = Unit
            }

            if (config.getFirebaseConfig().isCloudMessagingEnabled) {
                RefreshFirebaseTokenWorker.schedule(context)
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
            }
        }
    }

    fun handleDiscussionNotification(deepLink: DeepLink) {
        viewModelScope.launch {
            try {
                deepLink.notificationDomain?.let { pushManager.logNotificationTappedEvent(deepLink.toMap()) }
                deepLink.notificationId?.let { pushManager.markNotificationAsRead(it) }
            } catch (e: Exception) {
                logger.e(throwable = e)
            }
        }
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}
