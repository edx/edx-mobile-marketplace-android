package org.openedx.core.system

interface PushGlobalManager {
    suspend fun getUnreadNotificationsCount(): Int
}
