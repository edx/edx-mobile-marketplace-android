package org.openedx.core.di

import org.koin.core.module.Module

/**
 * A contract for any plugin that provides Koin modules for dependency injection.
 */
interface KoinModuleProvider {
    /**
     * Returns a list of Koin modules that this plugin wants to register.
     */
    fun getModules(): List<Module>
}
