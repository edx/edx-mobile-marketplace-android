package org.openedx.core.presentation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.openedx.core.config.Config
import org.openedx.core.config.SubscriptionBannerConfig
import org.openedx.core.data.storage.SubscriptionBannerStorage

class SubscriptionAlertBannerViewModelTest {

    private val config = mockk<Config>()
    private val storage = mockk<SubscriptionBannerStorage>(relaxed = true)

    private lateinit var viewModel: SubscriptionAlertBannerViewModel

    private fun config(enabled: Boolean = true, maxSessions: Int = 4, url: String = "https://example.com") {
        every { config.getSubscriptionBannerConfig() } returns SubscriptionBannerConfig(
            isEnabled = enabled,
            maxSessions = maxSessions,
            url = url,
        )
    }

    @Before
    fun setUp() {
        viewModel = SubscriptionAlertBannerViewModel(config, storage)
    }

    @Test
    fun `isBannerVisible returns false when feature is disabled`() {
        config(enabled = false)
        every { storage.isSubscriptionBannerDismissed(any()) } returns false
        every { storage.getSubscriptionBannerSessionCount() } returns 1

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns false when maxSessions is zero`() {
        config(enabled = true, maxSessions = 0)
        every { storage.isSubscriptionBannerDismissed(any()) } returns false
        every { storage.getSubscriptionBannerSessionCount() } returns 1

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns false when banner is dismissed for that screen`() {
        config()
        every { storage.isSubscriptionBannerDismissed("discovery") } returns true
        every { storage.getSubscriptionBannerSessionCount() } returns 1

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns true for session 1`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed(any()) } returns false
        every { storage.getSubscriptionBannerSessionCount() } returns 1

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns true for session 4`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed(any()) } returns false
        every { storage.getSubscriptionBannerSessionCount() } returns 4

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns false for session 5`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed(any()) } returns false
        every { storage.getSubscriptionBannerSessionCount() } returns 5

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible discovery and profile are independent`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed("discovery") } returns true
        every { storage.isSubscriptionBannerDismissed("profile") } returns false
        every { storage.getSubscriptionBannerSessionCount() } returns 2

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.DISCOVERY))
        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBannerViewModel.Screen.PROFILE))
    }

    @Test
    fun `dismiss sets dismissed flag for correct screen key`() {
        viewModel.dismiss(SubscriptionAlertBannerViewModel.Screen.DISCOVERY)

        verify { storage.setSubscriptionBannerDismissed("discovery", true) }
    }

    @Test
    fun `dismiss profile sets dismissed flag for profile key`() {
        viewModel.dismiss(SubscriptionAlertBannerViewModel.Screen.PROFILE)

        verify { storage.setSubscriptionBannerDismissed("profile", true) }
    }

    @Test
    fun `getBannerUrl returns url from config`() {
        config(url = "https://my.banner.url")

        assert(viewModel.getBannerUrl() == "https://my.banner.url")
    }
}

