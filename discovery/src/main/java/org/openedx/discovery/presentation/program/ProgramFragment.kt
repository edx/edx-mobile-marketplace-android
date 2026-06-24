package org.openedx.discovery.presentation.program

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.domain.model.iap.IAPFlow
import org.openedx.core.domain.model.iap.IAPFlowSource
import org.openedx.core.domain.model.iap.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.extension.takeIfNotEmpty
import org.openedx.core.extension.toastMessage
import org.openedx.core.presentation.dialog.IAPDialogFragment
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.dialog.alert.InfoDialogFragment
import org.openedx.core.presentation.global.webview.WebViewUIAction
import org.openedx.core.system.AppCookieManager
import org.openedx.core.ui.FullScreenErrorView
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.Toolbar
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
import org.openedx.core.R as coreR
import androidx.core.net.toUri
import org.koin.compose.koinInject
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.discovery.presentation.catalog.WebViewLink

class ProgramFragment : Fragment() {

    private val viewModel by viewModel<ProgramViewModel>()
    private var isNestedFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNestedFragment = arguments?.getBoolean(ARG_NESTED_FRAGMENT, false) ?: false
        if (isNestedFragment.not()) {
            lifecycle.addObserver(viewModel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val uiState by viewModel.uiState.collectAsState(initial = ProgramUIState.Loading)
                var hasInternetConnection by remember {
                    mutableStateOf(viewModel.hasInternetConnection)
                }

                if (isNestedFragment.not()) {
                    DisposableEffect(uiState is ProgramUIState.CourseEnrolled) {
                        if (uiState is ProgramUIState.CourseEnrolled) {

                            val courseId = (uiState as ProgramUIState.CourseEnrolled).courseId
                            val isEnrolled = (uiState as ProgramUIState.CourseEnrolled).isEnrolled

                            if (isEnrolled) {
                                viewModel.onEnrolledCourseClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    courseId = courseId,
                                    showTrackSelection = true,
                                )
                                context.toastMessage(getString(R.string.discovery_enrolled_successfully))
                            } else {
                                InfoDialogFragment.newInstance(
                                    title = getString(coreR.string.core_enrollment_error),
                                    message = getString(coreR.string.core_enrollment_error_message)
                                ).show(
                                    requireActivity().supportFragmentManager,
                                    InfoDialogFragment::class.simpleName
                                )
                            }
                        }
                        onDispose {}
                    }
                }

                ProgramInfoScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    contentUrl = getInitialUrl(),
                    cookieManager = viewModel.cookieManager,
                    canShowBackBtn = arguments?.getString(ARG_PATH_ID, "")
                        ?.isNotEmpty() == true,
                    isNestedFragment = isNestedFragment,
                    uriScheme = viewModel.uriScheme,
                    userAgent = viewModel.appUserAgent,
                    hasInternetConnection = hasInternetConnection,
                    onWebViewUIAction = { action ->
                        when (action) {
                            WebViewUIAction.WEB_PAGE_LOADED -> {
                                viewModel.showLoading(false)
                            }

                            WebViewUIAction.WEB_PAGE_ERROR -> {
                                viewModel.onPageLoadError()
                            }

                            WebViewUIAction.RELOAD_WEB_PAGE -> {
                                hasInternetConnection = viewModel.hasInternetConnection
                                viewModel.showLoading(true)
                            }
                        }
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStackImmediate()
                    },
                    onUriClick = { param, type ->
                        when (type) {
                            Authority.ENROLLED_COURSE_INFO -> {
                                viewModel.onEnrolledCourseClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    courseId = param,
                                    showTrackSelection = false,
                                )
                            }

                            Authority.ENROLLED_PROGRAM_INFO -> {
                                viewModel.onProgramCardClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    pathId = param
                                )
                            }

                            Authority.PROGRAM_INFO -> {
                                viewModel.onViewCourseClick(
                                    fragmentManager = requireActivity().supportFragmentManager,
                                    courseId = extractPathIdFromLink(param).ifBlank { param },
                                    infoType = type.name
                                )
                            }

                            Authority.COURSE_INFO -> {
                                startProgramNativePurchase(
                                    rawLink = param,
                                    defaultSku = SAMPLE_PROGRAM_SKU,
                                )
                            }

                            Authority.EARN_CERTIFICATE -> {
                                startProgramNativePurchase(
                                    rawLink = param,
                                    defaultSku = SAMPLE_PROGRAM_SKU,
                                )
                            }

                            Authority.COURSE -> {
                                viewModel.navigateToDiscovery()
                            }

                            Authority.EXTERNAL -> {
                                val normalizedParam = normalizeExternalUrl(param)
                                if (shouldOpenProgramPurchaseFromExternal(normalizedParam)) {
                                    startProgramNativePurchase(
                                        rawLink = normalizedParam,
                                        defaultSku = SAMPLE_PROGRAM_SKU,
                                    )
                                } else {
                                    context?.let { ctx ->
                                        activity?.let { act ->
                                            ActionDialogFragment.newInstance(
                                                title = ctx.getString(coreR.string.core_leaving_the_app),
                                                message = ctx.getString(
                                                    coreR.string.core_leaving_the_app_message,
                                                    ctx.getString(coreR.string.platform_name)
                                                ),
                                                url = param,
                                                source = DiscoveryAnalyticsScreen.PROGRAM.screenName
                                            ).show(
                                                act.supportFragmentManager,
                                                ActionDialogFragment::class.simpleName
                                            )
                                        }
                                    }
                                }
                            }

                            Authority.ENROLL -> TODO()
                        }
                    },
                )
            }
        }
    }

    private fun startProgramNativePurchase(rawLink: String, defaultSku: String) {
        val parsedLink = parseProgramPurchaseLink(rawLink)
        val purchaseIdentifier = parsedLink.purchaseIdentifier
            ?: arguments?.getString(ARG_PATH_ID)?.takeIf { it.isNotBlank() }
            ?: rawLink.takeIf { it.isNotBlank() && !it.contains("://") }

        if (purchaseIdentifier.isNullOrBlank()) {
            context?.toastMessage(getString(coreR.string.core_error_unknown_error))
            return
        }

        val resolvedSku = parsedLink.storeSku.ifBlank { defaultSku }
        val purchaseFlowData = PurchaseFlowData(
            iapFlow = IAPFlow.USER_INITIATED,
            screenName = IAPFlowSource.COURSE_ENROLLMENT.screen,
            courseId = purchaseIdentifier,
            courseName = parsedLink.title ?: getString(R.string.discovery_programs),
            productInfo = ProductInfo(
                storeSku = resolvedSku,
                lmsUSDPrice = parsedLink.price,
            )
        )

        IAPDialogFragment.newInstance(purchaseFlowData).show(
            requireActivity().supportFragmentManager,
            IAPDialogFragment.TAG
        )
    }

    private fun parseProgramPurchaseLink(rawLink: String): ProgramPurchaseLink {
        val uri = runCatching { Uri.parse(rawLink.replace("+", "%2B")) }.getOrNull()
        val purchaseIdentifier = uri?.getQueryParameter(WebViewLink.Param.COURSE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter(WebViewLink.Param.PROGRAM_ID)?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter(WebViewLink.Param.PATH_ID)?.takeIf { it.isNotBlank() }

        return ProgramPurchaseLink(
            purchaseIdentifier = purchaseIdentifier,
            storeSku = uri?.getQueryParameter(WebViewLink.Param.STORE_SKU)
                ?.takeIf { it.isNotBlank() }
                ?: uri?.getQueryParameter("sku").orEmpty(),
            price = uri?.getQueryParameter(WebViewLink.Param.PRICE)?.toDoubleOrNull() ?: 0.0,
            title = uri?.getQueryParameter(WebViewLink.Param.TITLE)?.takeIf { it.isNotBlank() },
        )
    }

    private fun extractPathIdFromLink(rawLink: String): String {
        if (!rawLink.contains("://")) return rawLink

        val uri = runCatching { Uri.parse(rawLink.replace("+", "%2B")) }.getOrNull()
        return uri?.getQueryParameter(WebViewLink.Param.PATH_ID)
            ?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter(WebViewLink.Param.COURSE_ID)
                ?.takeIf { it.isNotBlank() }
            ?: uri?.getQueryParameter(WebViewLink.Param.PROGRAM_ID)
                ?.takeIf { it.isNotBlank() }
            ?: ""
    }

    private fun isProgramPurchaseExternalUrl(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false

        val lowerRaw = rawUrl.lowercase()
        val hasPurchaseHint =
            lowerRaw.contains("earn_certificate") ||
                lowerRaw.contains("payment_page_redirect") ||
                lowerRaw.contains("checkout") ||
                lowerRaw.contains("course_modes/choose") ||
                lowerRaw.contains("verify_student/start-flow") ||
                lowerRaw.contains("upgrade") ||
                lowerRaw.contains("verified") ||
                lowerRaw.contains("purchase") ||
                lowerRaw.contains("basket")

        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return hasPurchaseHint
        val host = uri.host.orEmpty()
        val path = uri.encodedPath.orEmpty()

        val hasAnySku =
            !uri.getQueryParameter(WebViewLink.Param.STORE_SKU).isNullOrBlank() ||
                !uri.getQueryParameter("sku").isNullOrBlank()
        val hasBundle = !uri.getQueryParameter("bundle").isNullOrBlank()

        val isCommerceHost =
            host.contains("commerce-coordinator", ignoreCase = true) ||
                host.contains("ecommerce", ignoreCase = true) ||
                host.contains("checkout", ignoreCase = true) ||
                host.contains("payment", ignoreCase = true)

        val isPurchasePath =
            path.contains("payment_page_redirect", ignoreCase = true) ||
                path.contains("checkout", ignoreCase = true) ||
                path.contains("course_modes/choose", ignoreCase = true) ||
                path.contains("verify_student/start-flow", ignoreCase = true) ||
                path.contains("enroll", ignoreCase = true) ||
                path.contains("upgrade", ignoreCase = true) ||
                path.contains("verified", ignoreCase = true) ||
                path.contains("basket", ignoreCase = true)

        val isAuthnPurchaseGate =
            host.contains("authn", ignoreCase = true) &&
                (path.contains("login", ignoreCase = true) || path.contains("register", ignoreCase = true))

        return hasPurchaseHint ||
            isAuthnPurchaseGate ||
            (isCommerceHost && (hasAnySku || hasBundle || isPurchasePath)) ||
            (isPurchasePath && (hasAnySku || hasBundle))
    }

    private fun normalizeExternalUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return rawUrl

        val decoded = runCatching {
            URLDecoder.decode(rawUrl, StandardCharsets.UTF_8.name())
        }.getOrDefault(rawUrl)

        return when {
            rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
            decoded.startsWith("http://", ignoreCase = true) || decoded.startsWith("https://", ignoreCase = true) -> decoded
            else -> rawUrl
        }
    }

    private fun shouldOpenProgramPurchaseFromExternal(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false

        val lowerRaw = rawUrl.lowercase()
        val rawLooksLikeProgramPurchase =
            lowerRaw.contains("authn.edx.org/login") ||
                lowerRaw.contains("authn.edx.org/register") ||
                lowerRaw.contains("commerce-coordinator.edx.org") ||
                lowerRaw.contains("payment_page_redirect") ||
                lowerRaw.contains("checkout") ||
                lowerRaw.contains("course_modes/choose") ||
                lowerRaw.contains("verify_student/start-flow") ||
                lowerRaw.contains("upgrade") ||
                lowerRaw.contains("basket")

        if (rawLooksLikeProgramPurchase) return true

        val normalizedRaw = normalizeExternalUrl(rawUrl)
        if (isProgramPurchaseExternalUrl(normalizedRaw)) return true

        val uri = runCatching { Uri.parse(normalizedRaw) }.getOrNull() ?: return false
        val host = uri.host.orEmpty().lowercase()
        val path = uri.encodedPath.orEmpty().lowercase()

        // Explicitly handle authn.edx.org/login?next=<commerce/payment redirect>
        if (host.contains("authn") && (path.contains("login") || path.contains("register"))) {
            val nextRaw = uri.getQueryParameter("next").orEmpty()
            if (nextRaw.isNotBlank()) {
                val decodedNext = runCatching {
                    URLDecoder.decode(nextRaw, StandardCharsets.UTF_8.name())
                }.getOrDefault(nextRaw)

                val lowerNext = decodedNext.lowercase()
                if (
                    lowerNext.contains("commerce-coordinator") ||
                    lowerNext.contains("payment_page_redirect") ||
                    lowerNext.contains("checkout") ||
                    lowerNext.contains("course_modes/choose") ||
                    lowerNext.contains("verify_student/start-flow") ||
                    lowerNext.contains("upgrade") ||
                    lowerNext.contains("basket")
                ) {
                    return true
                }
            }
        }

        // For program detail pages, fallback to native purchase when the link stays in edX web domains.
        val hasProgramContext = !arguments?.getString(ARG_PATH_ID).isNullOrBlank()
        if (!hasProgramContext) return false

        val isEdxDomain =
            host.contains("edx.org") ||
                host.contains("courses.edx") ||
                host.contains("authn")

        val looksLikePurchaseStep =
            path.contains("login") ||
                path.contains("register") ||
                path.contains("checkout") ||
                path.contains("payment") ||
                path.contains("course_modes") ||
                path.contains("upgrade") ||
                path.contains("verify_student")

        return isEdxDomain && looksLikePurchaseStep
    }

    private fun getInitialUrl(): String {
        val pathId = arguments?.getString(ARG_PATH_ID, "")
        return pathId?.takeIfNotEmpty()?.let {
            viewModel.programConfig.programDetailUrlTemplate.replace("{$ARG_PATH_ID}", it)
        } ?: viewModel.programConfig.programUrl
    }

    companion object {
        private const val ARG_PATH_ID = "path_id"
        private const val ARG_NESTED_FRAGMENT = "nested_fragment"
        private const val SAMPLE_PROGRAM_SKU = "mobile.android.program_demo_100"

        fun newInstance(
            pathId: String = "",
            isNestedFragment: Boolean = false,
        ): ProgramFragment {
            return ProgramFragment().apply {
                arguments = bundleOf(
                    ARG_PATH_ID to pathId,
                    ARG_NESTED_FRAGMENT to isNestedFragment
                )
            }
        }
    }

    private data class ProgramPurchaseLink(
        val purchaseIdentifier: String?,
        val storeSku: String,
        val price: Double,
        val title: String?,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProgramInfoScreen(
    windowSize: WindowSize,
    uiState: ProgramUIState?,
    contentUrl: String,
    cookieManager: AppCookieManager,
    uriScheme: String,
    userAgent: String,
    canShowBackBtn: Boolean,
    isNestedFragment: Boolean,
    hasInternetConnection: Boolean,
    onWebViewUIAction: (WebViewUIAction) -> Unit,
    onBackClick: () -> Unit,
    onUriClick: (String, Authority) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    when (uiState) {
        is ProgramUIState.UiMessage -> {
            HandleUIMessage(uiMessage = uiState.uiMessage, scaffoldState = scaffoldState)
        }

        else -> {}
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->
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

        val statusBarPadding = if (isNestedFragment) {
            Modifier
        } else {
            Modifier.statusBarsInset()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(statusBarPadding)
                .displayCutoutForLandscape()
                .background(MaterialTheme.appColors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isNestedFragment) {
                Toolbar(
                    label = stringResource(id = R.string.discovery_programs),
                    canShowBackBtn = canShowBackBtn,
                    onBackClick = onBackClick,
                )
            }

            Surface {
                Box(
                    modifier = modifierScreenWidth
                        .fillMaxHeight()
                        .background(MaterialTheme.appColors.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if ((uiState is ProgramUIState.Error).not()) {
                        if (hasInternetConnection) {
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
                                isAllLinksExternal = true,
                                enableProgramPurchaseInterception = true,
                                onWebPageLoaded = { onWebViewUIAction(WebViewUIAction.WEB_PAGE_LOADED) },
                                refreshSessionCookie = {
                                    coroutineScope.launch {
                                        cookieManager.tryToRefreshSessionCookie()
                                    }
                                },
                                onUriClick = onUriClick,
                                onWebPageLoadError = { onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR) },
                                isDatadogWebViewTrackingEnabled = isDatadogWebViewTrackingEnabled
                            )

                            AndroidView(
                                modifier = Modifier
                                    .background(MaterialTheme.appColors.background),
                                factory = {
                                    webView
                                }
                            )
                        } else {
                            onWebViewUIAction(WebViewUIAction.WEB_PAGE_ERROR)
                        }
                    }

                    if (uiState is ProgramUIState.Error) {
                        FullScreenErrorView(errorType = uiState.errorType) {
                            onWebViewUIAction(WebViewUIAction.RELOAD_WEB_PAGE)
                        }
                    }

                    if (uiState == ProgramUIState.Loading && hasInternetConnection) {
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MyProgramsPreview() {
    OpenEdXTheme {
        ProgramInfoScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = ProgramUIState.Loading,
            contentUrl = "https://www.example.com/",
            cookieManager = koinViewModel<ProgramViewModel>().cookieManager,
            uriScheme = "",
            userAgent = "",
            canShowBackBtn = false,
            isNestedFragment = false,
            hasInternetConnection = false,
            onWebViewUIAction = {},
            onBackClick = {},
            onUriClick = { _, _ -> },
        )
    }
}
