package org.openedx.notifications.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.openedx.core.di.KoinModuleProvider
import org.openedx.notifications.data.repository.NotificationsRepository
import org.openedx.notifications.domain.interactor.NotificationsInteractor
import org.openedx.notifications.presentation.inbox.NotificationsInboxViewModel
import org.openedx.notifications.presentation.primer.NotificationsPrimerViewModel
import org.openedx.notifications.presentation.settings.NotificationsSettingsViewModel

class NotificationsModuleProvider : KoinModuleProvider {

    private val notificationsModule = module {
        single { NotificationsRepository(get(), get()) }
        factory { NotificationsInteractor(get()) }

        viewModel { NotificationsInboxViewModel(get(), get(), get(), get()) }
        viewModel { NotificationsSettingsViewModel(get(), get(), get(), get()) }
        viewModel { NotificationsPrimerViewModel(get(), get(), get()) }
    }

    override fun getModules(): List<Module> = listOf(notificationsModule)
}
