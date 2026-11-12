    package org.openedx.discovery.presentation

    import android.annotation.SuppressLint
    import android.content.res.Configuration
    import android.os.Bundle
    import android.util.Log
    import android.view.MotionEvent
    import android.view.View
    import android.view.ViewGroup
    import android.view.WindowManager
    import android.webkit.WebView
    import android.widget.Button
    import android.widget.FrameLayout
    import android.widget.ImageButton
    import android.widget.ProgressBar
    import android.widget.TextView
    import android.widget.Toast
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

        private var currentBannerMaxHeight = -1


        private val isLandscape: Boolean
            get() = resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE

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
            val xpertWebView = view.findViewWithTag<WebView>("xpert_webview")

            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->

                val statusBarInset =
                    insets.getInsets(WindowInsetsCompat.Type.statusBars())

                val navBarInset =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars())

                val cutoutInset =
                    insets.getInsets(WindowInsetsCompat.Type.displayCutout())

                val systemBarsInset =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars())

                val isLandscape =
                    resources.configuration.orientation ==
                            Configuration.ORIENTATION_LANDSCAPE

                val holderCutout =
                    (activity as? InsetHolder)?.cutoutInset ?: 0

                val horizontalCutout = if (isLandscape) {
                    maxOf(
                        cutoutInset.left,
                        cutoutInset.right,
                        systemBarsInset.left,
                        systemBarsInset.right,
                        holderCutout
                    )
                } else {
                    0
                }

                view.updatePadding(
                    left = horizontalCutout,
                    right = horizontalCutout
                )

                val extraTopMargin =
                    (8 * resources.displayMetrics.density).toInt()

                headerToolbar?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarInset.top + extraTopMargin
                }

                val authButtonsView =
                    view.findViewWithTag<View>("auth_buttons_compose_view")

                if (viewModel.isPreLogin) {
                    val extraPadding =
                        (12 * resources.displayMetrics.density).toInt()

                    val bottomInset =
                        maxOf(0, navBarInset.bottom - extraPadding)

                    authButtonsView?.updatePadding(bottom = bottomInset)
                } else {
                    authButtonsView?.updatePadding(bottom = 0)
                }

                WindowInsetsCompat.CONSUMED
            }

            toolbarTitle?.text =
                getString(R.string.discovery_explore_the_catalog)

            btnBack?.visibility =
                if (viewModel.isPreLogin) View.VISIBLE else View.GONE

            btnBack?.setOnClickListener {
                requireActivity().supportFragmentManager.popBackStackImmediate()
            }


            updateXpertChatIcon(btnXpertChat, xpertWebView)

            btnXpertChat?.setOnClickListener {
                if (isXpertChatOpen) {
                    handleXpertChatStateChanged(view, isOpen = false)
                } else {
                    triggerXpertChat()
                }
            }
        }


        private fun updateXpertChatIcon(
            btnXpertChat: ImageButton?,
            xpertWebView: WebView?
        ) {
            val isDarkMode =
                resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES

            btnXpertChat?.setImageResource(
                if (isDarkMode) R.drawable.xpert_logo
                else R.drawable.xpert_logo_light
            )

            xpertWebView?.apply {
                setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                        requireContext(),
                        if (isDarkMode) CoreR.color.primary
                        else CoreR.color.background
                    )
                )

                evaluateJavascript(
                    """
                document.documentElement.style.backgroundColor =
                    '${if (isDarkMode) "#000000" else "#FFFFFF"}';
                document.body.style.backgroundColor =
                    '${if (isDarkMode) "#000000" else "#FFFFFF"}';
                """.trimIndent(),
                    null
                )
            }
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

        private var originalBannerHeight = -1
        private var originalBannerTopMargin = 0
        private var originalBannerBottomMargin = 0

        private fun applyScrollOffset(view: View, offset: Int) {
            val bannerLayout =
                view.findViewWithTag<View>("subscription_banner_layout") ?: return

            if (!viewModel.isSubscriptionBannerVisible.value) return

            val params = bannerLayout.layoutParams as? ViewGroup.MarginLayoutParams
                ?: return

            if (originalBannerHeight <= 0 && bannerLayout.height > 0) {
                originalBannerHeight = bannerLayout.height
                originalBannerTopMargin = params.topMargin
                originalBannerBottomMargin = params.bottomMargin
            }

            if (originalBannerHeight <= 0) return

            val maxHeight = currentBannerMaxHeight.takeIf { it > 0 }
                ?: originalBannerHeight

            val totalHeight = maxHeight +
                    originalBannerTopMargin +
                    originalBannerBottomMargin

            val clampedOffset = offset.coerceIn(0, totalHeight)
            currentScrollOffset = clampedOffset

            val remainingHeight = (totalHeight - clampedOffset)
                .coerceIn(0, totalHeight)

            val remainingTopMargin =
                minOf(originalBannerTopMargin, remainingHeight)

            val remainingBottomMargin =
                minOf(
                    originalBannerBottomMargin,
                    (remainingHeight - remainingTopMargin).coerceAtLeast(0)
                )

            val remainingContentHeight =
                (remainingHeight - remainingTopMargin - remainingBottomMargin)
                    .coerceAtLeast(0)

            params.topMargin = remainingTopMargin
            params.bottomMargin = remainingBottomMargin
            params.height = remainingContentHeight

            bannerLayout.layoutParams = params
        }

        private fun restoreBannerLayout(bannerLayout: View) {
            if (originalBannerHeight <= 0) return

            val params = bannerLayout.layoutParams as? ViewGroup.MarginLayoutParams
                ?: return

            currentBannerMaxHeight = if (isLandscape) {
                (145 * resources.displayMetrics.density).toInt()
            } else {
                originalBannerHeight
            }

            params.topMargin = originalBannerTopMargin
            params.bottomMargin = originalBannerBottomMargin
            params.height = currentBannerMaxHeight

            bannerLayout.layoutParams = params

            currentScrollOffset = 0
        }

        private fun setupWebView(view: View) {
            val webView =
                view.findViewWithTag<WebView>("discovery_webview") ?: return

            currentWebView = webView

            viewModel.setDiscoveryWebView(webView)

            val isDarkMode =
                resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES

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
                }
            )
            view.findViewWithTag<View>("subscription_banner_layout")?.let { banner ->
                installDiscoveryScrollListener(webView, view, banner)
            }
            setupXpertWebView(view)
        }
        private var isXpertWebViewReady = false
        private var isXpertChatPending = false


        private var isXpertPageLoaded = false

        @SuppressLint("SetJavaScriptEnabled")
        private fun setupXpertWebView(view: View) {

            val xpertWebView =
                view.findViewWithTag<WebView>("xpert_webview") ?: return

            isXpertWebViewReady = false
            isXpertPageLoaded = false
            isXpertChatPending = false

            xpertWebView.visibility = View.GONE

            viewModel.setXpertWebView(xpertWebView)
            Log.d("XpertChat", "Initializing Xpert WebView")
            Log.d("XpertChat", "URL: ${viewModel.discoveryUrl}")
            configureCatalogWebView(
                webView = xpertWebView,
                url = viewModel.discoveryUrl,
                uriScheme = viewModel.uriScheme,
                userAgent = viewModel.appUserAgent,

                isDarkTheme =
                    resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK ==
                            Configuration.UI_MODE_NIGHT_YES,

                webViewTrackingAnalytics = webViewTrackingAnalytics,

                enableXpertChat = true,

                onWebPageLoaded = {
                    isXpertPageLoaded = true
                    Log.d("XpertChat", "Xpert page loaded")
                },

                onUriClick = { param, authority ->
                    handleUriClick(param, authority)
                },

                onWebPageLoadError = {
                    isXpertPageLoaded = false
                    Log.e("XpertChat", "Xpert page loading failed")
                },

                onXpertChatReady = {
                    isXpertWebViewReady = true

                    Log.d("XpertChat", "Xpert chatbot ready callback received")
                    if (isXpertChatPending) {
                        isXpertChatPending = false
                        triggerXpertChat()
                    }
                },

                onXpertChatStateChanged = { isOpen ->
                    handleXpertChatStateChanged(view, isOpen)
                }


            )
        }


        private fun triggerXpertChat() {

            val rootView = view ?: return

            val xpertWebView =
                rootView.findViewWithTag<WebView>("xpert_webview")
            Log.d("XpertChat", "Ready: $isXpertWebViewReady")
            Log.d("XpertChat", "Pending: $isXpertChatPending")
            if (xpertWebView == null) {
                Log.e("XpertChat", "Xpert WebView not found")
                return
            }

            if (!isXpertWebViewReady) {
                Log.d("XpertChat", "Chatbot is not ready. Queuing request.")

                isXpertChatPending = true
                return
            }

            xpertWebView.evaluateJavascript(
                """
            (function() {
                if (typeof window.__triggerXpertChatbotAction === 'function') {
                    return window.__triggerXpertChatbotAction();
                }
                return false;
            })();
            """.trimIndent()
            ) { result ->

                Log.d("XpertChat", "Chat trigger result: $result")

                rootView.post {

                    if (!isAdded || view !== rootView) return@post

                    if (result == "true") {

                        handleXpertChatStateChanged(rootView, isOpen = true)

                        Log.d("XpertChat", "Chat screen opened")

                    } else {

                        Log.e(
                            "XpertChat",
                            "Unable to find chatbot button on webpage"
                        )

                        isXpertChatPending = false
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
            val bannerLayout =
                view.findViewWithTag<View>("subscription_banner_layout") ?: return

            bannerLayout.addOnLayoutChangeListener {
                    _, _, _, _, _, _, _, _, _ ->

                if (!isXpertChatOpen) {
                    applyScrollOffset(view, currentScrollOffset)
                }
            }
        }


        @SuppressLint("ClickableViewAccessibility")

        private fun installDiscoveryScrollListener(
            webView: WebView,
            view: View,
            bannerLayout: View
        ) {
            webView.setOnScrollChangeListener {
                    _, _, scrollY, _, _ ->

                if (!isXpertChatOpen) {
                    applyScrollOffset(
                        view = view,
                        offset = scrollY
                    )
                }
            }
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
                            if (isVisible) {
                                bannerLayout?.visibility = View.VISIBLE
                                bannerLayout?.post {
                                    if (bannerLayout.isVisible) restoreBannerLayout(bannerLayout)
                                }
                            } else {
                                bannerLayout?.visibility = View.GONE
                                currentScrollOffset = 0
                            }

                            view.post {
                                if (!isXpertChatOpen) {
                                    applyScrollOffset(view, currentScrollOffset)
                                }
                            }
                        }
                    }

                    launch {
                        viewModel.uiState.collect { uiState ->

                            val isLoading =
                                uiState is WebViewUIState.Loading &&
                                        viewModel.hasInternetConnection

                            loadingIndicator?.visibility =
                                if (isLoading && !isXpertChatOpen) {
                                    View.VISIBLE
                                } else {
                                    View.GONE
                                }

                            errorViewContainer?.visibility =
                                if (uiState is WebViewUIState.Error) {
                                    View.VISIBLE
                                } else {
                                    View.GONE
                                }

                            webView?.visibility =
                                if (uiState is WebViewUIState.Error ||
                                    !viewModel.hasInternetConnection
                                ) {
                                    View.GONE
                                } else {
                                    View.VISIBLE
                                }
                        }
                    }
                    launch {
                        viewModel.isDarkTheme.collect { isDark ->
                            val xpertWebView =
                                view.findViewWithTag<WebView>("xpert_webview")

                            xpertWebView?.apply {
                                setBackgroundColor(
                                    androidx.core.content.ContextCompat.getColor(
                                        requireContext(),
                                        if (isDark) CoreR.color.primary
                                        else CoreR.color.background
                                    )
                                )
                            }
                        }
                    }

                }


            }

        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)

            val rootView = view ?: return

            rootView.post {
                if (!isAdded || view !== rootView) return@post

                ViewCompat.requestApplyInsets(rootView)

                val btnXpertChat =
                    rootView.findViewWithTag<ImageButton>("btn_xpert_chat")

                val xpertWebView =
                    rootView.findViewWithTag<WebView>("xpert_webview")

                updateXpertChatIcon(btnXpertChat, xpertWebView)

                updateBannerForOrientation(rootView)

                rootView.requestLayout()

                rootView.findViewWithTag<WebView>("discovery_webview")
                    ?.requestLayout()

                xpertWebView?.requestLayout()
            }
        }



        override fun onResume() {
            super.onResume()
            if (viewModel.isPreLogin) {
                requireActivity().window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                )
            }


            viewModel.updateThemeMode()

            view?.let {
                val btnXpertChat = it.findViewWithTag<ImageButton>("btn_xpert_chat")
                val xpertWebView = it.findViewWithTag<WebView>("xpert_webview")
                updateXpertChatIcon(btnXpertChat, xpertWebView)
            }
        }


        private fun updateXpertChatVisibility(
            view: View,
            isOpen: Boolean
        ) {
            isXpertChatOpen = isOpen

            val rootLayout =
                view.findViewWithTag<View>("root_layout")

            val xpertWebView =
                view.findViewWithTag<WebView>("xpert_webview")

            val loadingIndicator =
                view.findViewWithTag<ProgressBar>("loading_indicator")

            val bannerLayout =
                view.findViewWithTag<View>("subscription_banner_layout")

            if (isOpen) {
                rootLayout?.visibility = View.VISIBLE
                loadingIndicator?.visibility = View.GONE

                bannerLayout?.visibility = View.GONE

                xpertWebView?.let { webView ->
                    val params = webView.layoutParams

                    if (params is ViewGroup.MarginLayoutParams) {
                        params.topMargin = 0
                        params.bottomMargin = 0
                        webView.layoutParams = params
                    }
                }

                xpertWebView?.visibility = View.VISIBLE

            } else {
                xpertWebView?.clearFocus()
                xpertWebView?.visibility = View.GONE

                xpertWebView?.updateLayoutParams<FrameLayout.LayoutParams> {
                    topMargin = 0
                    bottomMargin = 0
                }

                rootLayout?.visibility = View.VISIBLE

                val discoveryWebView =
                    view.findViewWithTag<WebView>("discovery_webview")

                val showBanner = viewModel.isSubscriptionBannerVisible.value

                bannerLayout?.visibility =
                    if (showBanner) View.VISIBLE else View.GONE

                if (showBanner) {
                    bannerLayout?.let { banner ->
                        restoreBannerLayout(banner)
                    }
                }

                discoveryWebView?.visibility = if (
                    viewModel.uiState.value is WebViewUIState.Error ||
                    !viewModel.hasInternetConnection
                ) View.GONE else View.VISIBLE

                discoveryWebView?.bringToFront()
                discoveryWebView?.isFocusableInTouchMode = true
                discoveryWebView?.requestFocus()

                discoveryWebView?.post {
                    if (!isAdded || this.view !== view || isXpertChatOpen) {
                        return@post
                    }

                    applyScrollOffset(view, discoveryWebView.scrollY)
                }
            }

        }

        private fun handleXpertChatStateChanged(
            root: View,
            isOpen: Boolean
        ) {
            root.post {
                if (!isAdded || view !== root) return@post

                updateXpertChatVisibility(root, isOpen)
                if (isOpen) {
                    val rootLayout =
                        view?.findViewWithTag<View>("root_layout")
                    val loadingIndicator =
                        view?.findViewWithTag<ProgressBar>("loading_indicator")

                    val bannerLayout =
                        view?.findViewWithTag<View>("subscription_banner_layout")


                    rootLayout?.visibility = View.GONE
                    loadingIndicator?.visibility = View.GONE
                    bannerLayout?.visibility = View.GONE
                    val xpertWebView =
                        root.findViewWithTag<WebView>("xpert_webview")

                    xpertWebView?.postDelayed({
                        if (isAdded && view === root && isXpertChatOpen) {
                            expandXpertDialog(xpertWebView)
                        }
                    }, 300L)
                } else {
                    restoreDiscoveryAfterXpertClose(root)
                }
            }
            }


        private fun expandXpertDialog(webView: WebView) {
            webView.evaluateJavascript(
                """
            (function() {
                var closeButton = document.querySelector(
                    'button[aria-label="Close Xpert dialog"]'
                );
                var dialog = closeButton && closeButton.closest('[role="dialog"]');
                if (!dialog) return;
    
                ['position', 'inset', 'width', 'height', 'max-width',
                 'max-height', 'margin', 'border-radius', 'transform'].forEach(
                    function(property) {
                        dialog.style.removeProperty(property);
                    }
                );
    
                dialog.style.setProperty('position', 'fixed', 'important');
                dialog.style.setProperty('inset', '0', 'important');
                dialog.style.setProperty('width', '100vw', 'important');
                dialog.style.setProperty('height', '100vh', 'important');
                dialog.style.setProperty('max-width', 'none', 'important');
                dialog.style.setProperty('max-height', 'none', 'important');
                dialog.style.setProperty('margin', '0', 'important');
                dialog.style.setProperty('border-radius', '0', 'important');
                dialog.style.setProperty('transform', 'none', 'important');
                dialog.style.setProperty('background-color', '#FFFFFF', 'important');
                dialog.style.setProperty('opacity', '1', 'important');
                dialog.style.setProperty('z-index', '2147483647', 'important');
            })();
            """.trimIndent(),
                null
            )
        }


        private fun restoreDiscoveryAfterXpertClose(root: View) {
            root.postDelayed({

                if (!isAdded || view !== root || isXpertChatOpen) {
                    return@postDelayed
                }

                val discoveryWebView =
                    root.findViewWithTag<WebView>("discovery_webview")
                        ?: return@postDelayed

                discoveryWebView.visibility = View.VISIBLE
                discoveryWebView.bringToFront()

                discoveryWebView.isFocusableInTouchMode = true
                discoveryWebView.requestFocus()

            }, XPERT_CLOSE_RESTORE_DELAY_MS)
        }


        override fun onDestroyView() {
            viewModel.clearXpertWebView()
            viewModel.setDiscoveryWebView(null)

            currentWebView = null

            isXpertWebViewReady = false
            isXpertChatPending = false
            isXpertChatOpen = false

            super.onDestroyView()
        }


        private fun updateBannerForOrientation(view: View) {
            val bannerLayout =
                view.findViewWithTag<View>("subscription_banner_layout")
                    ?: return

            bannerLayout.post {
                if (!isAdded || this.view !== view) return@post

                val params = bannerLayout.layoutParams as? ViewGroup.MarginLayoutParams
                    ?: return@post

                if (originalBannerHeight <= 0) {
                    val measuredHeight = bannerLayout.height
                    if (measuredHeight <= 0) return@post

                    originalBannerHeight = measuredHeight
                    originalBannerTopMargin = params.topMargin
                    originalBannerBottomMargin = params.bottomMargin
                }

                currentBannerMaxHeight = if (isLandscape) {
                    (145 * resources.displayMetrics.density).toInt()
                } else {
                    originalBannerHeight
                }

                params.topMargin = originalBannerTopMargin
                params.bottomMargin = originalBannerBottomMargin
                params.height = currentBannerMaxHeight

                bannerLayout.layoutParams = params

                currentScrollOffset = 0

                bannerLayout.requestLayout()
            }
        }

        companion object {

            private const val ARG_SEARCH_QUERY = "query_search"
            private const val XPERT_CLOSE_RESTORE_DELAY_MS = 100L

            fun newInstance(querySearch: String = ""): WebViewDiscoveryFragment {
                val fragment = WebViewDiscoveryFragment()
                fragment.arguments = bundleOf(ARG_SEARCH_QUERY to querySearch)
                return fragment
            }
        }
    }

