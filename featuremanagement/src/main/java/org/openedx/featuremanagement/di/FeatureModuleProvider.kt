package org.openedx.featuremanagement.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.core.config.Config
import org.openedx.core.di.KoinModuleProvider
import org.openedx.core.feature.FeatureManager
import org.openedx.core.feature.FeatureService
import org.openedx.featuremanagement.FeatureManagerImpl
import org.openedx.featuremanagement.OptimizelyFeatureService

internal val OptimizelyQualifier = named("optimizely")

/**
 * A provider for feature management DI modules.
 * It conditionally registers multiple vendor-specific FeatureService implementations
 * based on configuration and aggregates them into a single FeatureManager.
 */
class FeatureModuleProvider : KoinModuleProvider {
    private val module: Module = module {

        single<FeatureService>(
            qualifier = OptimizelyQualifier,
            createdAtStart = false,
        ) {
            OptimizelyFeatureService(
                context = get(),
                config = get(),
                preferences = get()
            )
        }

        single<FeatureManager> {
            val config = get<Config>()
            val services = mutableListOf<FeatureService>()

            if (config.getOptimizelyConfig().enabled) {
                services.add(get(OptimizelyQualifier))
            }
            FeatureManagerImpl(services)
        }
    }

    override fun getModules(): List<Module> = listOf(module)
}
