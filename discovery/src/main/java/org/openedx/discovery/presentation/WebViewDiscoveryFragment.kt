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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(view)
        setupSubscriptionBanner(view)
        setupWebView(view)
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

            // Apply horizontal insets in landscape so Xpert Chat icon and banner edges are never cut
            view.updatePadding(
                left = horizontalCutout,
                right = horizontalCutout
            )

            // Give margin top to toolbar instead of padding top
            val extraTopMargin = (8 * resources.displayMetrics.density).toInt()
            headerToolbar?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarInset.top + extraTopMargin
            }

            // In prelogin, apply navigation bar bottom padding so register and sign in buttons are never cut
            val authButtonsView = view.findViewWithTag<View>("auth_buttons_compose_view")
            if (viewModel.isPreLogin) {
                authButtonsView?.updatePadding(bottom = navBarInset.bottom)
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
        val totalBannerHeight = if (isBannerVisible) {
            val params = bannerLayout.layoutParams as? ViewGroup.MarginLayoutParams
            val margins = (params?.topMargin ?: 0) + (params?.bottomMargin ?: 0)
            bannerLayout.height + margins
        } else {
            0
        }

        val clampedOffset = if (totalBannerHeight > 0) offset.coerceIn(0, totalBannerHeight) else 0
        currentScrollOffset = clampedOffset

        val targetContainerHeight = frameHeight + totalBannerHeight
        if (container.layoutParams.height != targetContainerHeight) {
            container.updateLayoutParams { height = targetContainerHeight }
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
                applyScrollOffset(view, currentScrollOffset)
            }
        )
    }

    private var isXpertChatOpen = false

    private fun handleXpertChatStateChanged(view: View, isOpen: Boolean) {
        isXpertChatOpen = isOpen
        val bannerLayout = view.findViewWithTag<View>("subscription_banner_layout")
        val container = view.findViewWithTag<LinearLayout>("banner_and_webview_container")
        val contentFrame = view.findViewWithTag<FrameLayout>("content_frame")
        val webView = view.findViewWithTag<WebView>("discovery_webview")

        if (isOpen) {
            bannerLayout?.visibility = View.GONE
            container?.translationY = 0f
            val frameHeight = contentFrame?.height ?: ViewGroup.LayoutParams.MATCH_PARENT
            container?.updateLayoutParams { height = frameHeight }
            webView?.updateLayoutParams { height = frameHeight }
        } else {
            val isBannerVisible = viewModel.isSubscriptionBannerVisible.value
            bannerLayout?.visibility = if (isBannerVisible) View.VISIBLE else View.GONE
            container?.translationY = 0f
            view.post {
                val frameHeight = contentFrame?.height ?: 0
                if (frameHeight > 0) {
                    val totalBannerHeight = if (isBannerVisible) {
                        val params = bannerLayout?.layoutParams as? ViewGroup.MarginLayoutParams
                        val margins = (params?.topMargin ?: 0) + (params?.bottomMargin ?: 0)
                        (bannerLayout?.height ?: 0) + margins
                    } else {
                        0
                    }
                    container?.updateLayoutParams { height = frameHeight + totalBannerHeight }
                    webView?.updateLayoutParams { height = frameHeight }
                    val scrollOffset = if (totalBannerHeight > 0) webView?.scrollY?.coerceIn(0, totalBannerHeight) ?: 0 else 0
                    container?.translationY = -scrollOffset.toFloat()
                }
            }
        }
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
                    applyScrollOffset(view, initialOffset + deltaY.toInt())
                }
            }
            false // Return false so webView and banner still receive click, tap, and text input events
        }

        bannerLayout.setOnTouchListener(touchListener)
        webView.setOnTouchListener(touchListener)
    }

    private fun observeViewModel(view: View) {
        val bannerLayout = view.findViewWithTag<View>("subscription_banner_layout")
        val container = view.findViewWithTag<LinearLayout>("banner_and_webview_container")
        val loadingIndicator = view.findViewWithTag<ProgressBar>("loading_indicator")
        val errorViewContainer = view.findViewWithTag<View>("error_view_container")
        val webView = view.findViewWithTag<WebView>("discovery_webview")
        val contentFrame = view.findViewWithTag<FrameLayout>("content_frame")

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSubscriptionBannerVisible.collect { isVisible ->
                        bannerLayout?.visibility = if (isVisible) View.VISIBLE else View.GONE
                        applyScrollOffset(view, currentScrollOffset)
                        val frameHeight = contentFrame?.height ?: 0
                        if (frameHeight > 0) {
                            if (!isVisible) {
                                container?.translationY = 0f
                                container?.updateLayoutParams { height = frameHeight }
                                webView?.updateLayoutParams { height = frameHeight }
                            } else {
                                val params = bannerLayout?.layoutParams as? ViewGroup.MarginLayoutParams
                                val margins = (params?.topMargin ?: 0) + (params?.bottomMargin ?: 0)
                                val bannerH = (bannerLayout?.height ?: 0) + margins
                                container?.updateLayoutParams { height = frameHeight + bannerH }
                                webView?.updateLayoutParams { height = frameHeight }
                            }
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

    private fun openExternalUrl(url: String) {
        if (url.isBlank()) return
        val ctx = context ?: return
        val act = activity ?: return
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        view?.post {
            if (isAdded && !isDetached) {
                try {
                    ActionDialogFragment.newInstance(
                        title = ctx.getString(CoreR.string.core_leaving_the_app),
                        message = ctx.getString(
                            CoreR.string.core_leaving_the_app_message,
                            ctx.getString(CoreR.string.platform_name)
                        ),
                        url = formattedUrl,
                        source = DiscoveryAnalyticsScreen.DISCOVERY.screenName
                    ).show(
                        act.supportFragmentManager,
                        ActionDialogFragment::class.simpleName
                    )
                } catch (e: Exception) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(formattedUrl)
                        )
                        ctx.startActivity(intent)
                    } catch (ignored: Exception) {}
                }
            }
        }
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






