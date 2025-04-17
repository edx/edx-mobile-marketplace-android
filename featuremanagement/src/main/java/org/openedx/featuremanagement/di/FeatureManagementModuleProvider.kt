package org.openedx.featuremanagement.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.core.config.Config
import org.openedx.core.di.KoinModuleProvider
import org.openedx.core.feature.FeatureManagementService
import org.openedx.core.feature.FeatureManager
import org.openedx.featuremanagement.FeatureManagerImpl
import org.openedx.featuremanagement.OptimizelyFeatureManagementService

/**
 * A provider for feature management DI modules.
 * It conditionally registers multiple vendor-specific FeatureManagementService implementations
 * based on configuration and aggregates them into a single FeatureManager.
 */
class FeatureManagementModuleProvider : KoinModuleProvider {
    private val module: Module = module {

        single<FeatureManagementService>(named("optimizely")) {
            OptimizelyFeatureManagementService(get(), get(), get())
        }

        single<FeatureManager> {
            val config = this.get<Config>()
            val services = mutableListOf<FeatureManagementService>()

            if (config.getOptimizelyConfig().enabled) {
                services.add(get(named("optimizely")))
            }
            FeatureManagerImpl(services)
        }
    }

    override fun getModules(): List<Module> = listOf(module)
}
