package org.openedx.notifications.presentation.inbox

import org.openedx.core.BaseViewModel
import org.openedx.notifications.domain.interactor.NotificationsInteractor

class NotificationsInboxViewModel(
    private val interactor: NotificationsInteractor,
) : BaseViewModel() {

}
