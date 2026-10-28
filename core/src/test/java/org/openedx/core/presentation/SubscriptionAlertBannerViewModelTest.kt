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
import org.openedx.core.module.subscriptionBanner.SubscriptionAlertBanner

class SubscriptionAlertBannerViewModelTest {

    private val corePreferences = mockk<CorePreferences>()
    private val storage = mockk<SubscriptionBannerStorage>(relaxed = true)

    private lateinit var viewModel: SubscriptionAlertBanner

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
        viewModel = SubscriptionAlertBanner(corePreferences, storage)
    }

    @Test
    fun `isBannerVisible returns false when feature is disabled`() {
        config(enabled = false)

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns true on first launch when session count is zero`() {
        config(enabled = true, maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false
        every { storage.getSubscriptionBannerScreenSessionCount("discovery") } returns 0
        every { storage.getSubscriptionBannerSessionCount() } returns 1
        every { storage.getSubscriptionBannerScreenLastSeenAppSession("discovery") } returns 0

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
        verify { storage.setSubscriptionBannerScreenSessionCount("discovery", 1) }
        verify { storage.setSubscriptionBannerScreenLastSeenAppSession("discovery", 1) }
    }

    @Test
    fun `isBannerVisible counts fresh install session when app session starts at zero`() {
        config(enabled = true, maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false
        every { storage.getSubscriptionBannerScreenSessionCount("discovery") } returns 0
        every { storage.getSubscriptionBannerSessionCount() } returns 0
        every { storage.getSubscriptionBannerScreenLastSeenAppSession("discovery") } returns 0

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
        verify { storage.setSubscriptionBannerScreenSessionCount("discovery", 1) }
        verify { storage.setSubscriptionBannerScreenLastSeenAppSession("discovery", 1) }
    }

    @Test
    fun `isBannerVisible returns false when banner is dismissed for that screen`() {
        config()
        every { storage.isSubscriptionBannerDismissed("discovery") } returns true

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns true when dismiss count is below max`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false
        every { storage.getSubscriptionBannerScreenSessionCount("discovery") } returns 2
        every { storage.getSubscriptionBannerSessionCount() } returns 3
        every { storage.getSubscriptionBannerScreenLastSeenAppSession("discovery") } returns 2

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
        verify { storage.setSubscriptionBannerScreenSessionCount("discovery", 3) }
        verify { storage.setSubscriptionBannerScreenLastSeenAppSession("discovery", 3) }
    }

    @Test
    fun `isBannerVisible returns false when shown sessions reaches max`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false
        every { storage.getSubscriptionBannerScreenSessionCount("discovery") } returns 4
        every { storage.getSubscriptionBannerSessionCount() } returns 5
        every { storage.getSubscriptionBannerScreenLastSeenAppSession("discovery") } returns 4

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
    }

    @Test
    fun `isBannerVisible returns true on last allowed session (4th with maxSessions 4)`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false
        every { storage.getSubscriptionBannerScreenSessionCount("discovery") } returns 3
        every { storage.getSubscriptionBannerSessionCount() } returns 4
        every { storage.getSubscriptionBannerScreenLastSeenAppSession("discovery") } returns 3

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
        verify { storage.setSubscriptionBannerScreenSessionCount("discovery", 4) }
        verify { storage.setSubscriptionBannerScreenLastSeenAppSession("discovery", 4) }
    }

    @Test
    fun `isBannerVisible discovery and profile are independent by screen session count`() {
        config(maxSessions = 4)

        every { storage.isSubscriptionBannerDismissed("discovery") } returns true

        every { storage.getSubscriptionBannerScreenSessionCount("profile") } returns 1
        every { storage.isSubscriptionBannerDismissed("profile") } returns false
        every { storage.getSubscriptionBannerSessionCount() } returns 2
        every { storage.getSubscriptionBannerScreenLastSeenAppSession("profile") } returns 1

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.PROFILE))
    }

    @Test
    fun `dismiss increments dismiss count for correct screen`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 2

        viewModel.dismiss(SubscriptionAlertBanner.Screen.DISCOVERY)

        verify { storage.setSubscriptionBannerDismissCount("discovery", 3) }
        verify { storage.setSubscriptionBannerDismissed("discovery", true) }
    }

    @Test
    fun `dismiss does not increment dismiss count above max`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 4

        viewModel.dismiss(SubscriptionAlertBanner.Screen.DISCOVERY)

        verify(exactly = 0) { storage.setSubscriptionBannerDismissCount("discovery", any()) }
        verify { storage.setSubscriptionBannerDismissed("discovery", true) }
    }

    @Test
    fun `dismiss profile only affects profile`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("profile") } returns 1

        viewModel.dismiss(SubscriptionAlertBanner.Screen.PROFILE)

        verify { storage.setSubscriptionBannerDismissCount("profile", 2) }
        verify(exactly = 0) { storage.setSubscriptionBannerDismissCount("discovery", any()) }
    }

    @Test
    fun `isBannerVisible returns true when not dismissed and below max count`() {
        config(maxSessions = 4)
        every { storage.isSubscriptionBannerDismissed("discovery") } returns false
        every { storage.getSubscriptionBannerScreenSessionCount("discovery") } returns 2
        every { storage.getSubscriptionBannerSessionCount() } returns 2
        every { storage.getSubscriptionBannerScreenLastSeenAppSession("discovery") } returns 2

        assertTrue(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
        verify(exactly = 0) { storage.setSubscriptionBannerScreenSessionCount(any(), any()) }
        verify(exactly = 0) { storage.setSubscriptionBannerScreenLastSeenAppSession(any(), any()) }
    }

    @Test
    fun `dismiss once keeps banner hidden regardless of session count`() {
        config(maxSessions = 4)
        every { storage.getSubscriptionBannerDismissCount("discovery") } returns 1
        every { storage.isSubscriptionBannerDismissed("discovery") } returns true

        assertFalse(viewModel.isBannerVisible(SubscriptionAlertBanner.Screen.DISCOVERY))
    }

    @Test
    fun `getBannerUrl returns url from config`() {
        config(url = "https://my.banner.url")

        assert(viewModel.getBannerUrl() == "https://my.banner.url")
    }
}
