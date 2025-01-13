package org.openedx.dashboard.presentation

import android.annotation.SuppressLint
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isInternetError
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.dialog.IAPDialogFragment
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPEventLogger
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.PushEvent
import org.openedx.core.system.notifier.PushNotifier
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.system.notifier.app.EnrolledCourseEvent
import org.openedx.core.system.notifier.app.RequestEnrolledCourseEvent
import org.openedx.dashboard.domain.CourseStatusFilter
import org.openedx.dashboard.domain.interactor.DashboardInteractor

@SuppressLint("StaticFieldLeak")
class DashboardListViewModel(
    private val context: Context,
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val iapNotifier: IAPNotifier,
    private val pushNotifier: PushNotifier,
    private val analytics: DashboardAnalytics,
    private val appNotifier: AppNotifier,
    private val preferencesManager: CorePreferences,
    private val iapInteractor: IAPInteractor,
    iapAnalytics: IAPAnalytics,
) : BaseViewModel() {

    private val coursesList = mutableListOf<EnrolledCourse>()
    private var page = 1
    private var isLoading = false

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableLiveData<DashboardUIState>(DashboardUIState.Loading)
    val uiState: LiveData<DashboardUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _iapUiState = MutableSharedFlow<IAPUIState?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val iapUiState: SharedFlow<IAPUIState?>
        get() = _iapUiState.asSharedFlow()

    private val _updating = MutableLiveData<Boolean>()
    val updating: LiveData<Boolean>
        get() = _updating

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    private val eventLogger = IAPEventLogger(analytics = iapAnalytics, isSilentIAPFlow = true)

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            discoveryNotifier.notifier.collect {
                if (it is CourseDashboardUpdate) {
                    updateCourses()
                }
            }
        }

        iapNotifier.notifier.onEach { event ->
            when (event) {
                is UpdateCourseData -> {
                    updateCourses(isIAPFlow = event.isPurchasedFromCourseDashboard.not())
                }
            }
        }.distinctUntilChanged().launchIn(viewModelScope)
    }

    init {
        getCourses()
        collectAppEvent()
    }

    fun getCourses() {
        _uiState.value = DashboardUIState.Loading
        coursesList.clear()
        internalLoadingCourses()
    }

    fun updateCourses(isIAPFlow: Boolean = false) {
        if (isLoading) {
            return
        }
        viewModelScope.launch {
            try {
                _updating.value = true
                isLoading = true
                page = 1
                val response = interactor.getEnrolledCourses(page)
                if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                coursesList.clear()
                coursesList.addAll(response.courses)
                if (coursesList.isEmpty()) {
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(
                        courses = ArrayList(coursesList),
                        isIAPEnabled = preferencesManager.appConfig.iapConfig.isEnabled
                    )
                }
                if (isIAPFlow) {
                    iapNotifier.send(CourseDataUpdated())
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
            _updating.value = false
            isLoading = false
        }
    }

    fun refreshPushBadgeCount() {
        viewModelScope.launch { pushNotifier.send(PushEvent.RefreshBadgeCount) }
    }

    fun processIAPAction(
        fragmentManager: FragmentManager,
        action: IAPAction,
        course: EnrolledCourse?,
        iapException: IAPException?,
    ) {
        when (action) {
            IAPAction.ACTION_USER_INITIATED -> {
                if (course != null) {
                    IAPDialogFragment.newInstance(
                        iapFlow = IAPFlow.USER_INITIATED,
                        screenName = IAPFlowSource.COURSE_ENROLLMENT.screen,
                        courseId = course.course.id,
                        courseName = course.course.name,
                        isSelfPaced = course.course.isSelfPaced,
                        productInfo = course.productInfo
                    ).show(
                        fragmentManager,
                        IAPDialogFragment.TAG
                    )
                }
            }

            IAPAction.ACTION_COMPLETION -> {
                IAPDialogFragment.newInstance(
                    IAPFlow.SILENT,
                    IAPFlowSource.COURSE_ENROLLMENT.screen
                ).show(
                    fragmentManager,
                    IAPDialogFragment.TAG
                )
                clearIAPState()
            }

            IAPAction.ACTION_UNFULFILLED -> {
                detectUnfulfilledPurchase()
            }

            IAPAction.ACTION_CLOSE -> {
                clearIAPState()
            }

            IAPAction.ACTION_ERROR_CLOSE -> {
                eventLogger.logIAPCancelEvent()
                clearIAPState()
            }

            IAPAction.ACTION_GET_HELP -> {
                iapException?.getFormattedErrorMessage()?.let {
                    showFeedbackScreen(it)
                }
                clearIAPState()
            }

            else -> {
            }
        }
    }

    private fun internalLoadingCourses() {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = if (networkConnection.isOnline() || page > 1) {
                    interactor.getEnrolledCourses(page)
                } else {
                    null
                }
                if (response != null) {
                    if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                        _canLoadMore.value = true
                        page++
                    } else {
                        _canLoadMore.value = false
                        page = -1
                    }
                    coursesList.addAll(response.courses)
                } else {
                    val cachedList = interactor.getEnrolledCoursesFromCache()
                    _canLoadMore.value = false
                    page = -1
                    coursesList.addAll(cachedList)
                }
                if (coursesList.isEmpty()) {
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(
                        courses = ArrayList(coursesList),
                        isIAPEnabled = preferencesManager.appConfig.iapConfig.isEnabled
                    )
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
            _updating.value = false
            isLoading = false
        }
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            internalLoadingCourses()
        }
    }

    private fun collectAppEvent() {
        appNotifier.notifier
            .onEach {
                if (it is AppUpgradeEvent) {
                    _appUpgradeEvent.value = it
                }
                if (it is RequestEnrolledCourseEvent) {
                    val enrolledCourses =
                        interactor.getAllUserCourses(status = CourseStatusFilter.ALL).courses
                    appNotifier.send(EnrolledCourseEvent(enrolledCourses))
                }
            }
            .distinctUntilChanged()
            .launchIn(viewModelScope)
    }

    fun dashboardCourseClickedEvent(courseId: String, courseName: String) {
        analytics.dashboardCourseClickedEvent(courseId, courseName)
    }

    private fun detectUnfulfilledPurchase() {
        viewModelScope.launch(Dispatchers.IO) {
            val enrolledCourses =
                interactor.getAllUserCourses(status = CourseStatusFilter.ALL).courses
            iapInteractor.detectUnfulfilledPurchase(
                enrolledCourses = enrolledCourses,
                purchaseVerified = { purchaseFlowData ->
                    eventLogger.apply {
                        this.purchaseFlowData = purchaseFlowData
                        this.logUnfulfilledPurchaseInitiatedEvent()
                    }
                },
                onSuccess = {
                    _iapUiState.tryEmit(IAPUIState.PurchasesFulfillmentCompleted)
                },
                onFailure = {
                    _iapUiState.tryEmit(
                        IAPUIState.Error(
                            IAPException(
                                IAPRequestType.UNFULFILLED_CODE,
                                it.httpErrorCode,
                                it.errorMessage,
                            )
                        )
                    )
                }
            )
        }
    }

    private fun showFeedbackScreen(message: String) {
        iapInteractor.showFeedbackScreen(context, message)
        eventLogger.logGetHelpEvent()
    }

    private fun clearIAPState() {
        viewModelScope.launch {
            _iapUiState.emit(null)
        }
    }
}
