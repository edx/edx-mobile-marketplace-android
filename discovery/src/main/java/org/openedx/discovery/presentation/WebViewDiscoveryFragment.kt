package org.openedx.discovery.presentation

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.presentation.WebViewTrackingAnalytics
import org.openedx.core.presentation.dialog.alert.ActionDialogFragment
import org.openedx.core.presentation.global.InsetHolder
import org.openedx.core.presentation.global.webview.WebViewUIState
import org.openedx.core.ui.AuthButtonsPanel
import org.openedx.core.ui.SubscriptionBanner
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.discovery.R
import org.openedx.discovery.presentation.catalog.WebViewLink
import org.openedx.discovery.presentation.catalog.configureCatalogWebView
import org.openedx.core.R as CoreR

class WebViewDiscoveryFragment : Fragment(R.layout.fragment_webview_discovery) {

    private val viewModel by viewModel<WebViewDiscoveryViewModel> {
        parametersOf(requireArguments().getString(ARG_SEARCH_QUERY, ""))
    }

    private val webViewTrackingAnalytics by inject<WebViewTrackingAnalytics>()

    private var currentWebView: WebView? = null

    private var isXpertChatOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(view)
        setupSubscriptionBanner(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cookiesReady.collect { ready ->
                if (ready && currentWebView == null) {
                    setupWebView(view)
                }
            }
        }
        setupSubscriptionBannerScrolling(view)
        setupErrorView(view)
        setupAuthButtons(view)
        setupBackNavigation(view)
        observeViewModel(view)

        viewModel.refreshSubscriptionBannerVisibility()
    }

    private fun setupToolbar(view: View) {
        val headerToolbar = view.findViewWithTag<View>("header_toolbar")
        val toolbarTitle = view.findViewWithTag<TextView>("toolbar_title")
        val btnBack = view.findViewWithTag<ImageButton>("btn_back")
        val btnXpertChat = view.findViewWithTag<ImageButton>("btn_xpert_chat")

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val cutoutInset = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val systemBarsInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val holderCutout = (activity as? InsetHolder)?.cutoutInset ?: 0
            val horizontalCutout = if (isLandscape) {
                maxOf(cutoutInset.left, cutoutInset.right, systemBarsInset.left, systemBarsInset.right, holderCutout)
            } else {
                0
            }

            view.updatePadding(
                left = horizontalCutout,
                right = horizontalCutout
            )

            val extraTopMargin = (8 * resources.displayMetrics.density).toInt()
            headerToolbar?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarInset.top + extraTopMargin
            }

            val authButtonsView = view.findViewWithTag<View>("auth_buttons_compose_view")
            if (viewModel.isPreLogin) {
                val extraPadding = (12 * resources.displayMetrics.density).toInt()
                val bottomInset = maxOf(0, navBarInset.bottom - extraPadding)
                authButtonsView?.updatePadding(bottom = bottomInset)
            } else {
                authButtonsView?.updatePadding(bottom = 0)
            }

            WindowInsetsCompat.CONSUMED
        }

        toolbarTitle?.text = getString(R.string.discovery_explore_the_catalog)

        btnBack?.visibility = if (viewModel.isPreLogin) View.VISIBLE else View.GONE
        btnBack?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStackImmediate()
        }

        updateXpertChatIcon(btnXpertChat)
        btnXpertChat?.setOnClickListener {
            isXpertChatOpen = true
            applyScrollOffset(view, currentScrollOffset)
            viewModel.triggerXpertChat()
        }
    }

    private fun updateXpertChatIcon(btnXpertChat: ImageButton?) {
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        btnXpertChat?.setImageResource(
            if (isDarkMode) R.drawable.xpert_logo else R.drawable.xpert_logo_light
        )
    }

    private fun setupSubscriptionBanner(view: View) {
        val bannerComposeView = view.findViewWithTag<ComposeView>("subscription_banner_layout") ?: return
        bannerComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        val rawBannerUrl = viewModel.subscriptionBannerUrl.ifBlank { "https://www.edx.org" }
        val bannerUrl = if (!rawBannerUrl.startsWith("http://") && !rawBannerUrl.startsWith("https://")) {
            "https://$rawBannerUrl"
        } else {
            rawBannerUrl
        }

        bannerComposeView.setContent {
            OpenEdXTheme {
                SubscriptionBanner(
                    visible = true,
                    modifier = Modifier.fillMaxWidth(),
                    url = bannerUrl,
                    onDismiss = { viewModel.dismissSubscriptionBanner() },
                    onCtaClick = { ctaUrl ->
                        viewModel.onSubscriptionBannerCtaClicked(ctaUrl)
                    }
                )
            }
        }
    }

    private var currentScrollOffset = 0

    private fun applyScrollOffset(view: View, offset: Int) {
        val bannerLayout = view.findViewWithTag<View>("subscription_banner_layout") ?: return
        val container = view.findViewWithTag<LinearLayout>("banner_and_webview_container") ?: return
        val contentFrame = view.findViewWithTag<FrameLayout>("content_frame") ?: return
        val webView = view.findViewWithTag<WebView>("discovery_webview") ?: return

        val frameHeight = contentFrame.height
        if (frameHeight <= 0) return

        val isBannerVisible = viewModel.isSubscriptionBannerVisible.value && bannerLayout.isVisible
        if (!isBannerVisible) {
            container.updateLayoutParams {
                height = frameHeight
            }

            webView.updateLayoutParams {
                height = frameHeight
            }

            container.translationY = 0f
            currentScrollOffset = 0
            return
        }
        val totalBannerHeight = if (isBannerVisible) {
            val params = bannerLayout.layoutParams as? ViewGroup.MarginLayoutParams
            val margins = (params?.topMargin ?: 0) + (params?.bottomMargin ?: 0)
            bannerLayout.height + margins
        } else {
            0
        }

        val clampedOffset = if (totalBannerHeight > 0) offset.coerceIn(0, totalBannerHeight) else 0
        currentScrollOffset = clampedOffset

        val targetContainerHeight = if (isBannerVisible) {
            frameHeight + totalBannerHeight
        } else {
            frameHeight
        }

        container.updateLayoutParams {
            height = targetContainerHeight
        }

        webView.updateLayoutParams {
            height = frameHeight
        }

        val targetWebViewHeight = if (isXpertChatOpen) {
            frameHeight - totalBannerHeight + clampedOffset
        } else {
            frameHeight
        }
        if (webView.layoutParams.height != targetWebViewHeight) {
            webView.updateLayoutParams { height = targetWebViewHeight }
        }

        container.translationY = -clampedOffset.toFloat()
    }

    private fun setupWebView(view: View) {
        val webView = view.findViewWithTag<WebView>("discovery_webview") ?: return
        currentWebView = webView

        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        configureCatalogWebView(
            webView = webView,
            url = viewModel.discoveryUrl,
            uriScheme = viewModel.uriScheme,
            userAgent = viewModel.appUserAgent,
            isDarkTheme = isDarkMode,
            webViewTrackingAnalytics = webViewTrackingAnalytics,
            onWebPageLoaded = {
                viewModel.onWebPageLoaded()
                view.post {
                    applyScrollOffset(view, currentScrollOffset)
                }

            },
            onWebPageUpdated = { url ->
                viewModel.updateDiscoveryUrl(url)
            },
            onUriClick = { param, authority ->
                handleUriClick(param, authority)
            },
            onWebPageLoadError = {
                viewModel.onWebPageLoadError()
            },
            refreshSessionCookie = {
                viewModel.refreshSessionCookie()
            },
            onXpertChatReady = { readyWebView ->
                viewModel.setXpertWebView(readyWebView)
            },
            onXpertChatStateChanged = { isOpen ->
                isXpertChatOpen = isOpen
                view.post {
                    applyScrollOffset(view, currentScrollOffset)
                }
            }
        )
    }

    private fun handleUriClick(param: String, authority: WebViewLink.Authority) {
        when (authority) {
            WebViewLink.Authority.COURSE_INFO -> {
                viewModel.courseInfoClickedEvent(param)
                viewModel.infoCardClicked(
                    fragmentManager = requireActivity().supportFragmentManager,
                    pathId = param,
                    infoType = authority.name
                )
            }

            WebViewLink.Authority.PROGRAM_INFO -> {
                viewModel.programInfoClickedEvent(param)
                viewModel.infoCardClicked(
                    fragmentManager = requireActivity().supportFragmentManager,
                    pathId = param,
                    infoType = authority.name
                )
            }

            WebViewLink.Authority.EXTERNAL -> {
                context?.let { ctx ->
                    activity?.let { act ->
                        view?.post {
                            if (isAdded && !isDetached) {
                                try {
                                    ActionDialogFragment.newInstance(
                                        title = ctx.getString(CoreR.string.core_leaving_the_app),
                                        message = ctx.getString(
                                            CoreR.string.core_leaving_the_app_message,
                                            ctx.getString(CoreR.string.platform_name)
                                        ),
                                        url = param,
                                        source = DiscoveryAnalyticsScreen.DISCOVERY.screenName
                                    ).show(
                                        act.supportFragmentManager,
                                        ActionDialogFragment::class.simpleName
                                    )
                                } catch (e: IllegalStateException) {
                                    Log.e("WebViewDiscovery", "Failed to show dialog", e)
                                }
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }

    private fun setupErrorView(view: View) {
        val btnReload = view.findViewWithTag<Button>("btn_error_reload")
        btnReload?.setOnClickListener {
            viewModel.onWebPageLoading()
            currentWebView?.reload()
        }
    }

    private fun setupAuthButtons(view: View) {
        val authComposeView = view.findViewWithTag<ComposeView>("auth_buttons_compose_view") ?: return
        if (viewModel.isPreLogin) {
            authComposeView.visibility = View.VISIBLE
            authComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            authComposeView.setContent {
                OpenEdXTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        AuthButtonsPanel(
                            onRegisterClick = {
                                viewModel.navigateToSignUp(parentFragmentManager)
                            },
                            onSignInClick = {
                                viewModel.navigateToSignIn(parentFragmentManager)
                            }
                        )
                    }
                }
            }
        } else {
            authComposeView.visibility = View.GONE
        }
    }

    private fun setupBackNavigation(view: View) {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val webView = view.findViewWithTag<WebView>("discovery_webview")
                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSubscriptionBannerScrolling(view: View) {
        val webView = view.findViewWithTag<WebView>("discovery_webview") ?: return
        val bannerLayout = view.findViewWithTag<View>("subscription_banner_layout") ?: return
        val contentFrame = view.findViewWithTag<FrameLayout>("content_frame") ?: return

        contentFrame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyScrollOffset(view, currentScrollOffset)
        }

        webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (!isXpertChatOpen) {
                applyScrollOffset(view, scrollY)
            }
        }

        bannerLayout.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyScrollOffset(view, currentScrollOffset)
        }

        var touchStartY = 0f
        var initialOffset = 0

        val touchListener = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartY = event.rawY
                    initialOffset = currentScrollOffset
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = touchStartY - event.rawY
                    if (!isXpertChatOpen) {
                        applyScrollOffset(view, initialOffset + deltaY.toInt())
                    }
                }
            }
            false
        }

        bannerLayout.setOnTouchListener(touchListener)
        webView.setOnTouchListener(touchListener)
    }

    private fun observeViewModel(view: View) {
        val bannerLayout = view.findViewWithTag<View>("subscription_banner_layout")
        val loadingIndicator = view.findViewWithTag<ProgressBar>("loading_indicator")
        val errorViewContainer = view.findViewWithTag<View>("error_view_container")
        val webView = view.findViewWithTag<WebView>("discovery_webview")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSubscriptionBannerVisible.collect { isVisible ->
                        bannerLayout?.visibility = if (isVisible) View.VISIBLE else View.GONE
                        view.post {
                            applyScrollOffset(view, currentScrollOffset)
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { uiState ->
                        val hasInternet = viewModel.hasInternetConnection
                        loadingIndicator?.visibility =
                            if (uiState is WebViewUIState.Loading && hasInternet) View.VISIBLE else View.GONE
                        errorViewContainer?.visibility =
                            if (uiState is WebViewUIState.Error) View.VISIBLE else View.GONE
                        webView?.visibility =
                            if (uiState is WebViewUIState.Error || !hasInternet) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val v = view ?: return
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscape) {
            v.updatePadding(left = 0, right = 0)
        }
        val btnXpertChat = v.findViewWithTag<ImageButton>("btn_xpert_chat")
        updateXpertChatIcon(btnXpertChat)
        v.post {
            ViewCompat.requestApplyInsets(v)
            val contentFrame = v.findViewWithTag<FrameLayout>("content_frame") ?: return@post
            val webView = v.findViewWithTag<WebView>("discovery_webview") ?: return@post
            val container = v.findViewWithTag<LinearLayout>("banner_and_webview_container") ?: return@post
            val bannerLayout = v.findViewWithTag<View>("subscription_banner_layout") ?: return@post

            container.updateLayoutParams { width = ViewGroup.LayoutParams.MATCH_PARENT }
            webView.updateLayoutParams { width = ViewGroup.LayoutParams.MATCH_PARENT }
            applyScrollOffset(v, currentScrollOffset)
            val frameHeight = contentFrame.height
            if (frameHeight > 0) {
                val bannerH = if (bannerLayout.isVisible) {
                    val params = bannerLayout.layoutParams as? ViewGroup.MarginLayoutParams
                    val margins = (params?.topMargin ?: 0) + (params?.bottomMargin ?: 0)
                    bannerLayout.height + margins
                } else {
                    0
                }
                container.updateLayoutParams { height = frameHeight + bannerH }
                webView.updateLayoutParams { height = frameHeight }
                val scrollOffset = if (bannerH > 0) webView.scrollY.coerceIn(0, bannerH) else 0
                container.translationY = -scrollOffset.toFloat()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateThemeMode()
        view?.let {
            val btnXpertChat = it.findViewWithTag<ImageButton>("btn_xpert_chat")
            updateXpertChatIcon(btnXpertChat)
        }
    }

    override fun onDestroyView() {
        viewModel.clearXpertWebView()
        currentWebView = null
        super.onDestroyView()
    }

    companion object {

        private const val ARG_SEARCH_QUERY = "query_search"

        fun newInstance(querySearch: String = ""): WebViewDiscoveryFragment {
            val fragment = WebViewDiscoveryFragment()
            fragment.arguments = bundleOf(ARG_SEARCH_QUERY to querySearch)
            return fragment
        }
    }
}






