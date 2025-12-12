package org.openedx.featuremanagement.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.openedx.core.di.KoinModuleProvider
import org.openedx.core.feature.FeatureManager
import org.openedx.core.feature.FeatureService


/**
 * A provider for feature management DI modules.
 * It conditionally registers multiple vendor-specific [FeatureService] implementations
 * based on configuration and aggregates them into a single [FeatureManager].
 */
class FeatureModuleProvider : KoinModuleProvider {
    private val module: Module = module {


    }

    override fun getModules(): List<Module> {
        TODO("Not yet implemented")
    }
}
