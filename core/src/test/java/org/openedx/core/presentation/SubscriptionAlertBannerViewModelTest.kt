package org.openedx.core.presentation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.openedx.core.config.SubscriptionBannerConfig
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.SubscriptionBannerStorage
import org.openedx.core.domain.model.AppConfig

class SubscriptionAlertBannerViewModelTest {

    private val corePreferences = mockk<CorePreferences>()
    private val storage = mockk<SubscriptionBannerStorage>(relaxed = true)

    private lateinit var viewModel: SubscriptionAlertBannerViewModel

    private fun config(enabled: Boolean = true, maxSessions: Int = 4, url: String = "https://example.com") {
        val appConfig = mockk<AppConfig>()
        val bannerConfig = SubscriptionBannerConfig(
            isEnabled = enabled,
            maxSessions = maxSessions,
            url = url,
        )
        every { appConfig.subscriptionBannerConfig } returns bannerConfig
        every { corePreferences.appConfig } returns appConfig
    }

    @Before
    fun setUp() {
        viewModel = SubscriptionAlertBannerViewModel(corePreferences, storage)
    }

    @Test
    fun `isBannerVisible returns false when feature is disabled`() {
        config(enabled = false)

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns true on first launch when session count is zero`() {
        config(enabled = true, maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 0
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns false when banner is dismissed for that screen`() {
        config()
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 1
        every { storage.isSubscriptionBannerDismissed("discovery") } returns true

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns true when dismiss count is below max`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 0
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns false when dismiss count reaches max`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 4
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible discovery and profile are independent by dismiss count`() {
        config(maxSessions = 4)

        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 4
        every { storage.isSubscriptionBannerDismissed("discovery") } returns true

        every { storage.getSubscriptionBannerDismissCount("profile") } returns 1
        every { storage.isSubscriptionBannerDismissed("profile") } returns false

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.PROFILE))
    }

    @Test
    fun `dismiss increments dismiss count for correct screen`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 2

        viewModel.dismiss(SubscriptionAlertBannerViewModel.Screen.DISCOVERY)

        verify { storage.setSubscriptionBannerDismissCount("discovery", 3) }
        verify { storage.setSubscriptionBannerDismissed("discovery", true) }
    }

    @Test
    fun `dismiss does not increment dismiss count above max`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 4

        viewModel.dismiss(SubscriptionAlertBannerViewModel.Screen.DISCOVERY)

        verify(exactly = 0) { storage.setSubscriptionBannerDismissCount("discovery", any()) }
        verify { storage.setSubscriptionBannerDismissed("discovery", true) }
    }

    @Test
    fun `dismiss profile only affects profile`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("profile") } returns 1

        viewModel.dismiss(SubscriptionAlertBannerViewModel.Screen.PROFILE)

        verify { storage.setSubscriptionBannerDismissCount("profile", 2) }
        verify(exactly = 0) { storage.setSubscriptionBannerDismissCount("discovery", any()) }
    }

    @Test
    fun `isBannerVisible returns true when not dismissed and below max count`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 2
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `getBannerUrl returns url from config`() {
        config(url = "https://my.banner.url")

        assert(viewModel.getBannerUrl() == "https://my.banner.url")
    }
}
