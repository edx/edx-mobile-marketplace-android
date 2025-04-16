package org.openedx.featuremanagement.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.openedx.core.di.KoinModuleProvider

/**
 * A provider for feature management DI modules.
 * It conditionally registers multiple vendor-specific FeatureManagementService implementations
 * based on configuration and aggregates them into a single FeatureManager.
 */
class FeatureManagementModuleProvider : KoinModuleProvider {
    private val module: Module = module {
    }

    override fun getModules(): List<Module> = listOf(module)
}
