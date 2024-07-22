package org.openedx.profile.presentation.settings

import android.content.Context
import androidx.compose.ui.text.intl.Locale
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openedx.core.AppUpdateState
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isInternetError
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.presentation.IAPAnalyticsScreen
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPFlow
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.system.notifier.app.LogoutEvent
import org.openedx.core.utils.EmailUtil
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.domain.model.Configuration
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileAnalyticsEvent
import org.openedx.profile.presentation.ProfileAnalyticsKey
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.system.notifier.AccountDeactivated
import org.openedx.profile.system.notifier.ProfileNotifier

class SettingsViewModel(
    private val appData: AppData,
    private val config: Config,
    private val resourceManager: ResourceManager,
    private val corePreferences: CorePreferences,
    private val interactor: ProfileInteractor,
    private val iapAnalytics: IAPAnalytics,
    private val iapInteractor: IAPInteractor,
    private val cookieManager: AppCookieManager,
    private val workerController: DownloadWorkerController,
    private val analytics: ProfileAnalytics,
    private val router: ProfileRouter,
    private val appNotifier: AppNotifier,
    private val profileNotifier: ProfileNotifier,
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<SettingsUIState> =
        MutableStateFlow(SettingsUIState.Data(configuration))
    internal val uiState: StateFlow<SettingsUIState> = _uiState.asStateFlow()

    private val _iapUiState: MutableStateFlow<IAPUIState?> = MutableStateFlow(null)
    val iapUiState: StateFlow<IAPUIState?>
        get() = _iapUiState.asStateFlow()

    private val _successLogout = MutableSharedFlow<Boolean>()
    val successLogout: SharedFlow<Boolean>
        get() = _successLogout.asSharedFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _appUpgradeEvent = MutableStateFlow<AppUpgradeEvent?>(null)
    val appUpgradeEvent: StateFlow<AppUpgradeEvent?>
        get() = _appUpgradeEvent.asStateFlow()

    val isLogistrationEnabled get() = config.isPreLoginExperienceEnabled()

    private val configuration
        get() = Configuration(
            agreementUrls = config.getAgreement(Locale.current.language),
            faqUrl = config.getFaqUrl(),
            supportEmail = config.getFeedbackEmailAddress(),
            versionName = appData.versionName,
        )

    init {
        collectAppUpgradeEvent()
        collectProfileEvent()
    }

    fun logout() {
        logProfileEvent(ProfileAnalyticsEvent.LOGOUT_CLICKED)
        viewModelScope.launch {
            try {
                workerController.removeModels()
                withContext(Dispatchers.IO) {
                    interactor.logout()
                }
                logProfileEvent(
                    event = ProfileAnalyticsEvent.LOGGED_OUT,
                    params = buildMap {
                        put(ProfileAnalyticsKey.FORCE.key, ProfileAnalyticsKey.FALSE.key)
                    }
                )
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            } finally {
                cookieManager.clearWebViewCookie()
                appNotifier.send(LogoutEvent(false))
                _successLogout.emit(true)
            }
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is AppUpgradeEvent) {
                    _appUpgradeEvent.value = event
                }
            }
        }
    }

    private fun collectProfileEvent() {
        viewModelScope.launch {
            profileNotifier.notifier.collect {
                if (it is AccountDeactivated) {
                    logout()
                }
            }
        }
    }

    fun videoSettingsClicked(fragmentManager: FragmentManager) {
        router.navigateToVideoSettings(fragmentManager)
        logProfileEvent(ProfileAnalyticsEvent.VIDEO_SETTING_CLICKED)
    }

    fun privacyPolicyClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_privacy_policy),
            url = configuration.agreementUrls.privacyPolicyUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.PRIVACY_POLICY_CLICKED)
    }

    fun cookiePolicyClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_cookie_policy),
            url = configuration.agreementUrls.cookiePolicyUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.COOKIE_POLICY_CLICKED)
    }

    fun dataSellClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_data_sell),
            url = configuration.agreementUrls.dataSellConsentUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.DATA_SELL_CLICKED)
    }

    fun faqClicked() {
        logProfileEvent(ProfileAnalyticsEvent.FAQ_CLICKED)
    }

    fun termsOfUseClicked(fragmentManager: FragmentManager) {
        router.navigateToWebContent(
            fm = fragmentManager,
            title = resourceManager.getString(R.string.core_terms_of_use),
            url = configuration.agreementUrls.tosUrl,
        )
        logProfileEvent(ProfileAnalyticsEvent.TERMS_OF_USE_CLICKED)
    }

    fun emailSupportClicked(context: Context) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            subject = context.getString(R.string.core_error_upgrading_course_in_app),
            appVersion = appData.versionName
        )
        logProfileEvent(ProfileAnalyticsEvent.CONTACT_SUPPORT_CLICKED)
    }

    fun appVersionClickedEvent(context: Context) {
        AppUpdateState.openPlayMarket(context)
    }

    fun manageAccountClicked(fragmentManager: FragmentManager) {
        router.navigateToManageAccount(fragmentManager)
    }

    fun calendarSettingsClicked(fragmentManager: FragmentManager) {
        router.navigateToCalendarSettings(fragmentManager)
    }

    fun restartApp(fragmentManager: FragmentManager) {
        router.restartApp(
            fragmentManager,
            isLogistrationEnabled
        )
    }

    private fun logProfileEvent(
        event: ProfileAnalyticsEvent,
        params: Map<String, Any?> = emptyMap(),
    ) {
        analytics.logEvent(
            event = event.eventName,
            params = buildMap {
                put(ProfileAnalyticsKey.NAME.key, event.biValue)
                put(ProfileAnalyticsKey.CATEGORY.key, ProfileAnalyticsKey.PROFILE.key)
                putAll(params)
            }
        )
    }

    fun restorePurchase() {
        iapAnalytics.logIAPEvent(
            event = IAPAnalyticsEvent.IAP_RESTORE_PURCHASE_CLICKED,
            screenName = IAPAnalyticsScreen.PROFILE.screenName,
        )
        viewModelScope.launch(Dispatchers.IO) {
            val userId = corePreferences.user?.id ?: return@launch

            _iapUiState.emit(IAPUIState.Loading(IAPLoaderType.RESTORE_PURCHASES))
            // delay to show loading state
            delay(2000)

            runCatching {
                iapInteractor.processUnfulfilledPurchase(userId)
            }.onSuccess {
                if (it) {
                    iapAnalytics.logIAPEvent(
                        event = IAPAnalyticsEvent.IAP_UNFULFILLED_PURCHASE_INITIATED,
                        screenName = IAPAnalyticsScreen.PROFILE.screenName,
                    )
                    _iapUiState.emit(IAPUIState.PurchasesFulfillmentCompleted)
                } else {
                    _iapUiState.emit(IAPUIState.FakePurchasesFulfillmentCompleted)
                }
            }.onFailure {
                if (it is IAPException) {
                    _iapUiState.emit(
                        IAPUIState.Error(
                            IAPException(
                                IAPRequestType.RESTORE_CODE,
                                it.httpErrorCode,
                                it.errorMessage
                            )
                        )
                    )
                }
            }
        }
    }

    fun logIAPCancelEvent() {
        iapAnalytics.logIAPEvent(
            event = IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION,
            buildMap {
                put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, IAPAction.ACTION_RESTORE.action)
                put(IAPAnalyticsKeys.ERROR_ACTION.key, IAPAction.ACTION_CLOSE.action)
            }.toMutableMap(),
            screenName = IAPAnalyticsScreen.PROFILE.screenName,
        )
    }

    fun showFeedbackScreen(context: Context, message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        iapAnalytics.logIAPEvent(
            event = IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION,
            params = buildMap {
                put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, IAPAction.ACTION_UNFULFILLED.action)
                put(IAPAnalyticsKeys.ERROR_ACTION.key, IAPAction.ACTION_GET_HELP.action)
            }.toMutableMap(),
            screenName = IAPAnalyticsScreen.PROFILE.screenName,
        )
    }

    fun onRestorePurchaseCancel() {
        iapAnalytics.logIAPEvent(
            event = IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION,
            params = buildMap {
                put(
                    IAPAnalyticsKeys.ACTION.key,
                    IAPAction.ACTION_CLOSE.action
                )
            }.toMutableMap(),
            screenName = IAPAnalyticsScreen.PROFILE.screenName,
        )
    }

    fun clearIAPState() {
        viewModelScope.launch {
            _iapUiState.emit(null)
        }
    }
}
