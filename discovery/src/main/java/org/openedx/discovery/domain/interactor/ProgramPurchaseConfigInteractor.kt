package org.openedx.discovery.domain.interactor

import org.openedx.core.config.Config
import org.openedx.core.config.ProgramPurchaseConfig

class ProgramPurchaseConfigInteractor(
    private val config: Config
) {

    fun getConfig(): ProgramPurchaseConfig {
        return config.getProgramPurchaseConfig()
    }

    fun isEnabled(): Boolean {
        return getConfig().enabled
    }

    fun getSku(): String {
        return getConfig().sku
    }

    fun getPurchaseUrlHost(): String {
        return getConfig().purchaseUrlHost
    }

    fun getProgramUrlPath(): String {
        return getConfig().programUrlPath
    }
}