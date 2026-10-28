package org.openedx.discovery.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleOwner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.AppThemeMode
import org.openedx.core.module.subscriptionBanner.SubscriptionAlertBanner
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.Logger

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewDiscoveryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val config = mockk<Config>(relaxed = true)
    private val appData = mockk<AppData>(relaxed = true)
    private val networkConnection = mockk<NetworkConnection>(relaxed = true)
    private val corePreferences = mockk<CorePreferences>(relaxed = true)
    private val router = mockk<DiscoveryRouter>(relaxed = true)
    private val analytics = mockk<DiscoveryAnalytics>(relaxed = true)
    private val appCookieManager = mockk<AppCookieManager>(relaxed = true)
    private val subscriptionAlertBanner = mockk<SubscriptionAlertBanner>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { networkConnection.isOnline() } returns true
        every { corePreferences.appThemeMode } returns AppThemeMode.LIGHT

        // Mock Logger to prevent Koin initialization
        mockkConstructor(Logger::class)
        every { anyConstructed<Logger>().e(throwable = any()) } returns Unit
        every { anyConstructed<Logger>().d(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init refreshes cookie when missing or expired and marks cookies ready`() = runTest {
        every { appCookieManager.isSessionCookieMissingOrExpired() } returns true

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
            subscriptionAlertBanner = subscriptionAlertBanner,
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { appCookieManager.tryToRefreshSessionCookie() }
        assertTrue(viewModel.cookiesReady.value)
    }

    @Test
    fun `init still marks cookies ready when refresh throws`() = runTest {
        every { appCookieManager.isSessionCookieMissingOrExpired() } returns true
        coEvery { appCookieManager.tryToRefreshSessionCookie() } throws RuntimeException("boom")

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
            subscriptionAlertBanner = subscriptionAlertBanner,
        )

        advanceUntilIdle()

        // Cookie should still be marked ready even after exception
        assertTrue(viewModel.cookiesReady.value)
    }

    @Test
    fun `refreshSessionCookie refreshes only when cookie is missing or expired`() = runTest {
        every { appCookieManager.isSessionCookieMissingOrExpired() } returns true

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
            subscriptionAlertBanner = subscriptionAlertBanner,
        )
        advanceUntilIdle()

        viewModel.refreshSessionCookie()
        advanceUntilIdle()

        // 1 call from init + 1 call from explicit refresh
        coVerify(exactly = 2) { appCookieManager.tryToRefreshSessionCookie() }
    }

    @Test
    fun `refreshSessionCookie does not refresh when cookie is fresh`() = runTest {
        every { appCookieManager.isSessionCookieMissingOrExpired() } returns false

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
            subscriptionAlertBanner = subscriptionAlertBanner,
        )
        advanceUntilIdle()

        viewModel.refreshSessionCookie()
        advanceUntilIdle()

        coVerify(exactly = 0) { appCookieManager.tryToRefreshSessionCookie() }
        assertTrue(viewModel.cookiesReady.value)
    }

    @Test
    fun `updateThemeMode sets isDarkTheme to false when theme mode is LIGHT`() = runTest {
        every { corePreferences.appThemeMode } returns AppThemeMode.LIGHT

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
        )

        advanceUntilIdle()

        assertFalse(viewModel.isDarkTheme.value)
    }

    @Test
    fun `updateThemeMode sets isDarkTheme to true when theme mode is DARK`() = runTest {
        every { corePreferences.appThemeMode } returns AppThemeMode.DARK

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
        )

        advanceUntilIdle()

        assertTrue(viewModel.isDarkTheme.value)
    }

    @Test
    fun `updateThemeMode sets isDarkTheme to false when theme mode is MATCH_DEVICE`() = runTest {
        every { corePreferences.appThemeMode } returns AppThemeMode.MATCH_DEVICE

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
        )

        advanceUntilIdle()

        // MATCH_DEVICE defaults to false in ViewModel; Compose will override based on system theme
        assertFalse(viewModel.isDarkTheme.value)
    }

    @Test
    fun `onResume updates theme from LIGHT to DARK`() = runTest {
        every { corePreferences.appThemeMode } returns AppThemeMode.LIGHT

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
        )

        advanceUntilIdle()
        assertFalse(viewModel.isDarkTheme.value)

        // Change the mock to return DARK
        every { corePreferences.appThemeMode } returns AppThemeMode.DARK

        // Simulate onResume
        val lifecycleOwner = mockk<LifecycleOwner>()
        viewModel.onResume(lifecycleOwner)

        // Verify theme updated
        assertTrue(viewModel.isDarkTheme.value)
    }

    @Test
    fun `onResume refreshes subscription banner visibility state`() = runTest {
        every { appCookieManager.isSessionCookieMissingOrExpired() } returns false
        every { subscriptionAlertBanner.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY) } returns true

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
            subscriptionAlertBanner = subscriptionAlertBanner,
        )
        advanceUntilIdle()

        viewModel.onResume(mockk())

        assertTrue(viewModel.isSubscriptionBannerVisible.value)
    }

    @Test
    fun `onResume updates theme from DARK to LIGHT`() = runTest {
        every { corePreferences.appThemeMode } returns AppThemeMode.DARK
    @Test
    fun `dismissSubscriptionBanner hides subscription banner immediately`() = runTest {
        every { appCookieManager.isSessionCookieMissingOrExpired() } returns false
        every { subscriptionAlertBanner.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY) } returns true

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
            subscriptionAlertBanner = subscriptionAlertBanner,
        )
        advanceUntilIdle()

        viewModel.dismissSubscriptionBanner()

        assertFalse(viewModel.isSubscriptionBannerVisible.value)
    }
}

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
        )

        advanceUntilIdle()
        assertTrue(viewModel.isDarkTheme.value)

        // Change the mock to return LIGHT
        every { corePreferences.appThemeMode } returns AppThemeMode.LIGHT

        // Simulate onResume
        val lifecycleOwner = mockk<LifecycleOwner>()
        viewModel.onResume(lifecycleOwner)

        // Verify theme updated
        assertFalse(viewModel.isDarkTheme.value)
    }

    @Test
    fun `onResume updates theme from MATCH_DEVICE to DARK`() = runTest {
        every { corePreferences.appThemeMode } returns AppThemeMode.MATCH_DEVICE

        val viewModel = WebViewDiscoveryViewModel(
            querySearch = "",
            appData = appData,
            config = config,
            networkConnection = networkConnection,
            corePreferences = corePreferences,
            router = router,
            analytics = analytics,
            appCookieManager = appCookieManager,
        )

        advanceUntilIdle()
        assertFalse(viewModel.isDarkTheme.value)

        // Change the mock to return DARK
        every { corePreferences.appThemeMode } returns AppThemeMode.DARK

        val lifecycleOwner = mockk<LifecycleOwner>()
        viewModel.onResume(lifecycleOwner)

        assertTrue(viewModel.isDarkTheme.value)
    }
}