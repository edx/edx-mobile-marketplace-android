package org.openedx.discovery.presentation.info

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.UIMessage
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.dialog.alert.InfoDialogFragment
import org.openedx.core.presentation.global.webview.WebViewUIAction
import org.openedx.core.presentation.global.webview.WebViewUIState
import org.openedx.core.system.AppCookieManager
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPLoaderType
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.FullScreenErrorView
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.IAPErrorDialog
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.UnlockingAccessView
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.windowSizeValue
import org.openedx.discovery.R
import org.openedx.discovery.presentation.DiscoveryAnalyticsScreen
import org.openedx.discovery.presentation.catalog.CatalogWebViewScreen
import org.openedx.discovery.presentation.catalog.WebViewLink.Authority
import java.util.concurrent.atomic.AtomicReference
import org.openedx.core.R as CoreR

class CourseInfoFragment : Fragment() {

    private val viewModel by viewModel<CourseInfoViewModel> {
        parametersOf(
            requireArguments().getString(ARG_PATH_ID, ""),
            requireArguments().getString(ARG_INFO_TYPE, "")
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val fragmentActivity = requireActivity()
        setContent {
            OpenEdXTheme {
                val uiMessage by viewModel.uiMessage.collectAsState(initial = null)
                val showAlert by viewModel.showAlert.collectAsState(initial = false)
                val uiState by viewModel.uiState.collectAsState()
                val webViewState by viewModel.webViewState.collectAsState()
                val cookiesReady by viewModel.cookiesReady.collectAsState()
                val iapState by viewModel.iapState.collectAsState()
                val windowSize = rememberWindowSize()
                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }

                // Auto-start purchase once price is loaded (no certificate-preview dialog)
                LaunchedEffect(iapState) {
                    when {
                        iapState is IAPUIState.ProductData && viewModel.shouldAutoStartPurchase -> {
                            viewModel.startPurchaseFlow(fragmentActivity)
                        }
                        iapState is IAPUIState.CourseDataUpdated -> {
                            viewModel.purchaseFlowData.courseId?.let { courseId ->
                                viewModel.onSuccessfulCourseEnrollment(
                                    fragmentManager = fragmentActivity.supportFragmentManager,
                                    courseId = courseId,
                                    showTrackSelection = false
                                )
                            }
                            viewModel.clearIAPState()
                        }
                        else -> {}
                    }
                }

                LaunchedEffect(showAlert) {
                    if (showAlert) {
                        InfoDialogFragment.newInstance(
                            title = context.getString(CoreR.string.core_enrollment_error),
                            message = context.getString(
                                CoreR.string.core_enrollment_error_message,
                                getString(CoreR.string.platform_name)
                            )
                        ).show(
                            requireActivity().supportFragmentManager,
                            InfoDialogFragment::class.simpleName
                        )
                    }
                }

                LaunchedEffect((uiState as CourseInfoUIState.CourseInfo).enrollmentSuccess.get()) {
                    val courseInfo = uiState as CourseInfoUIState.CourseInfo
                    if (courseInfo.enrollmentSuccess.get().isNotEmpty()) {
                        viewModel.onSuccessfulCourseEnrollment(
                            fragmentManager = requireActivity().supportFragmentManager,
                            courseId = courseInfo.enrollmentSuccess.get(),
                            showTrackSelection = courseInfo.hadEnrollment.get().not(),
                        )
                        // Clear after navigation
                        courseInfo.hadEnrollment.set(false)
                        courseInfo.enrollmentSuccess.set("")
                    }
                }

                CourseInfoScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    webViewUIState = webViewState,
                    cookiesReady = cookiesReady,
                    uiMessage = uiMessage,
                    uriScheme = viewModel.uriScheme,
                    userAgent = viewModel.appUserAgent,
                    cookieManager = viewModel.cookieManager,
                    hasInternetConnection = hasInternetConnection,
                    onWebViewUIAction = { action ->
                        when (action) {
                            WebViewUIAction.WEB_PAGE_LOADED -> {
                                viewModel.onWebPageLoaded()
                            }

                            WebViewUIAction.WEB_PAGE_ERROR -> {
                                viewModel.onWebPageError()
                            }

                            WebViewUIAction.RELOAD_WEB_PAGE -> {
                                hasInternetConnection = viewModel.hasInternetConnection
                                viewModel.onWebPageLoading()
                            }
                        }
                    },
                    onRegisterClick = {
                        viewModel.navigateToSignUp(
                            parentFragmentManager,
                            viewModel.pathId,
                            viewModel.infoType
                        )
                    },
                    onSignInClick = {
                        viewModel.navigateToSignIn(
                            parentFragmentManager,
                            viewModel.pathId,
                            viewModel.infoType
                        )
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onUriClick = { param, type ->
                        when (type) {
                            Authority.PROGRAM_INFO -> {
                                viewModel.programInfoClickedEvent(param)
                                viewModel.infoCardClicked(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param,
                                    infoType = type.name
                                )
                            }

                            Authority.COURSE_INFO -> {
                                viewModel.courseInfoClickedEvent(param)
                                viewModel.infoCardClicked(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param,
                                    infoType = type.name
                                )
                            }

                            Authority.ENROLLED_COURSE_INFO -> {
                                viewModel.onSuccessfulCourseEnrollment(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    courseId = param,
                                    showTrackSelection = false
                                )
                            }

                            Authority.ENROLLED_PROGRAM_INFO -> {
                                viewModel.enrolledProgramInfoClicked(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param,
                                )
                            }

                            Authority.EXTERNAL -> {
                                context?.let { ctx ->
                                    activity?.let { act ->
                                        val dialog = ActionDialogFragment.newInstance(
                                            title = ctx.getString(CoreR.string.core_leaving_the_app),
                                            message = ctx.getString(
                                                CoreR.string.core_leaving_the_app_message,
                                                ctx.getString(CoreR.string.platform_name)
                                            ),
                                            url = param,
                                            source = DiscoveryAnalyticsScreen.COURSE_INFO.screenName
                                        )
                                        showActionDialogSafely(act, dialog)
                                    }
                                }
                            }

                            Authority.ENROLL -> {
                                viewModel.courseEnrollClickedEvent(param)
                                if ((uiState as CourseInfoUIState.CourseInfo).isPreLogin) {
                                    viewModel.navigateToSignUp(
                                        fragmentManager = requireActivity().supportFragmentManager,
                                        courseId = viewModel.pathId,
                                        infoType = viewModel.infoType
                                    )
                                } else {
                                    viewModel.enrollInACourse(courseId = param)
                                }
                            }
                            Authority.EARN_CERTIFICATE -> {
                                viewModel.setupAndLoadPurchase(param)
                            }

                            else -> {}
                        }
                    },
                    onRefreshSessionCookie = {
                        viewModel.refreshSessionCookie()
                    }
                )

                // IAP overlays ─────────────────────────────────────────────────
                if (iapState is IAPUIState.Loading &&
                    (iapState as IAPUIState.Loading).loaderType == IAPLoaderType.FULL_SCREEN
                ) {
                    UnlockingAccessView()
                }

                if (iapState is IAPUIState.Error) {
                    val iapException = (iapState as IAPUIState.Error).iapException
                    IAPErrorDialog(iapException = iapException) { iapAction ->
                        when (iapAction) {
                            IAPAction.ACTION_RELOAD_PRICE -> {
                                viewModel.eventLogger.logIAPErrorActionEvent(
                                    iapException.requestType.request,
                                    IAPAction.ACTION_RELOAD_PRICE.action
                                )
                                viewModel.loadPrice()
                            }
                            IAPAction.ACTION_CLOSE -> {
                                viewModel.eventLogger.logIAPErrorActionEvent(
                                    iapException.requestType.request,
                                    IAPAction.ACTION_CLOSE.action
                                )
                                viewModel.clearIAPState()
                            }
                            IAPAction.ACTION_OK -> {
                                viewModel.eventLogger.logIAPErrorActionEvent(
                                    iapException.requestType.request,
                                    IAPAction.ACTION_OK.action
                                )
                                viewModel.clearIAPState()
                            }
                            IAPAction.ACTION_REFRESH -> {
                                viewModel.eventLogger.logIAPErrorActionEvent(
                                    iapException.requestType.request,
                                    IAPAction.ACTION_REFRESH.action
                                )
                                viewModel.refreshCourse()
                            }
                            IAPAction.ACTION_GET_HELP -> {
                                viewModel.showFeedbackScreen(
                                    context,
                                    iapException.requestType.request,
                                    iapException.getFormattedErrorMessage()
                                )
                                viewModel.clearIAPState()
                            }
                            IAPAction.ACTION_RETRY -> {
                                viewModel.eventLogger.logIAPErrorActionEvent(
                                    iapException.requestType.request,
                                    IAPAction.ACTION_RETRY.action
                                )
                                if (iapException.requestType == IAPRequestType.CONSUME_CODE) {
                                    viewModel.retryToConsumeOrder()
                                } else if (iapException.requestType == IAPRequestType.CREATE_ORDER_CODE) {
                                    viewModel.retryCreateOrder()
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
    private fun showActionDialogSafely(
        act: FragmentActivity,
        dialog: DialogFragment
    ) {
        if (act.isFinishing || act.isDestroyed) return

        val fm = act.supportFragmentManager
        if (fm.isStateSaved) return

        dialog.show(fm, ActionDialogFragment::class.simpleName)
    }
    companion object {
        private const val ARG_PATH_ID = "path_id"
        private const val ARG_INFO_TYPE = "info_type"

        fun newInstance(
            pathId: String,
            infoType: String,
        ): CourseInfoFragment {
            val fragment = CourseInfoFragment()
            fragment.arguments = bundleOf(
                ARG_PATH_ID to pathId,
                ARG_INFO_TYPE to infoType
            )
            return fragment
        }
    }
}

@Composable
private fun CourseInfoScreen(
    windowSize: WindowSize,
    uiState: CourseInfoUIState,
    webViewUIState: WebViewUIState,
    cookiesReady: Boolean,
    uiMessage: UIMessage?,
    uriScheme: String,
    userAgent: String,
    cookieManager: AppCookieManager? = null,
    hasInternetConnection: Boolean,
    onWebViewUIAction: (WebViewUIAction) -> Unit,
    onRegisterClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBackClick: () -> Unit,
    onUriClick: (String, Authority) -> Unit,
    onRefreshSessionCookie: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current

    HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background,
        bottomBar = {
            if ((uiState as CourseInfoUIState.CourseInfo).isPreLogin) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp,
                        )
                        .navigationBarsPadding()
                ) {
                    AuthButtonsPanel(
                        onRegisterClick = onRegisterClick,
                        onSignInClick = onSignInClick
                    )
                }
            }
        }
    ) {
        val modifierScreenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Modifier.widthIn(Dp.Unspecified, 560.dp)
                    } else {
                        Modifier.widthIn(Dp.Unspecified, 650.dp)
                    },
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Toolbar(
                label = stringResource(id = R.string.discovery_Discovery),
                canShowBackBtn = true,
                onBackClick = onBackClick
            )

            Surface {
                Box(
                    modifier = modifierScreenWidth
                        .fillMaxHeight()
                        .background(Color.White)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if ((webViewUIState is WebViewUIState.Error).not()) {
                        if (hasInternetConnection) {
                            if (cookiesReady) {
                                CourseInfoWebView(
                                    contentUrl = (uiState as CourseInfoUIState.CourseInfo).initialUrl,
                                    uriScheme = uriScheme,
                                    userAgent = userAgent,
                                    cookieManager = cookieManager,
                                    isPreLogin = uiState.isPreLogin,
                                    onWebPageLoaded = { onWebViewUIAction(WebViewUIAction.WEB_PAGE_LOADED) },
                                    onUriClick = onUriClick,
                                    onWebPageLoadError = {
                                        onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                                    },
                                    onRefreshSessionCookie = onRefreshSessionCookie
                                )
                            }
                        } else {
                            onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                        }
                    }
                    if (webViewUIState is WebViewUIState.Error) {
                        FullScreenErrorView(errorType = webViewUIState.errorType) {
                            onWebViewUIAction(WebViewUIAction.RELOAD_WEB_PAGE)
                        }
                    }
                    if (webViewUIState is WebViewUIState.Loading && hasInternetConnection) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun CourseInfoWebView(
    contentUrl: String,
    uriScheme: String,
    userAgent: String,
    cookieManager: AppCookieManager?,
    isPreLogin: Boolean,
    onWebPageLoaded: () -> Unit,
    onUriClick: (String, Authority) -> Unit,
    onWebPageLoadError: () -> Unit,
    onRefreshSessionCookie: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val config = koinInject<Config>()
    val corePreferences = koinInject<CorePreferences>()
    val host = contentUrl.toUri().host
    val isDatadogWebViewTrackingEnabled = config.getDatadogConfig().enabled &&
        corePreferences.isDatadogEnabled &&
        !host.isNullOrEmpty()
    val webView = CatalogWebViewScreen(
        url = contentUrl,
        uriScheme = uriScheme,
        userAgent = userAgent,
        isAllLinksExternal = false,
        onWebPageLoaded = onWebPageLoaded,
        refreshSessionCookie = {
            if (cookieManager != null) {
                coroutineScope.launch {
                    cookieManager.tryToRefreshSessionCookie()
                }
            }
        },
        onUriClick = onUriClick,
        onWebPageLoadError = onWebPageLoadError,
        isDatadogWebViewTrackingEnabled = isDatadogWebViewTrackingEnabled
    )

    val consumeWindowInsets = if (isPreLogin) {
        WindowInsets.navigationBars
            .add(WindowInsets(bottom = 112.dp)) // The size of AuthButtonPanel
            .asPaddingValues()
    } else {
        WindowInsets(0.dp).asPaddingValues()
    }

    AndroidView(
        modifier = Modifier
            .consumeWindowInsets(consumeWindowInsets)
            .imePadding()
            .background(MaterialTheme.appColors.background),
        factory = {
            webView
        }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CourseInfoScreenPreview() {
    OpenEdXTheme {
        CourseInfoScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseInfoUIState.CourseInfo(
                initialUrl = "https://www.example.com/",
                isPreLogin = false,
                enrollmentSuccess = AtomicReference("")
            ),
            uiMessage = null,
            uriScheme = "",
            userAgent = "",
            hasInternetConnection = false,
            onWebViewUIAction = {},
            onRegisterClick = {},
            onSignInClick = {},
            onBackClick = {},
            onUriClick = { _, _ -> },
            webViewUIState = WebViewUIState.Loading,
            cookiesReady = true,
            onRefreshSessionCookie = {},
        )
    }
}
