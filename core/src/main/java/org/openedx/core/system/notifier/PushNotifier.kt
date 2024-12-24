package org.openedx.core.system.notifier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PushNotifier {
    private val channel = MutableSharedFlow<PushEvent>(replay = 0, extraBufferCapacity = 0)

    val notifier: Flow<PushEvent> = channel.asSharedFlow()

    suspend fun send(event: PushEvent.RefreshBadgeCount) = channel.emit(event)
}
